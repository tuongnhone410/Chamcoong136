package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {
    const val CHANNEL_ID = "timesnap_reminder_channel"
    private const val CHANNEL_NAME = "Nhắc nhở chấm công"
    private const val CHANNEL_DESC = "Kênh thông báo nhắc nhở nhân viên thực hiện check-in/check-out đúng giờ"

    // Khởi tạo Notification Channel với mức độ ưu tiên cao (Android 10+ / API 26+)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Hiển thị thông báo ngay lập tức
    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Sử dụng ic_launcher làm icon mặc định hoặc bất cứ icon nào hợp lệ
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    // Kiểm tra quyền thông báo (Android 13+ / API 33+)
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Lên lịch nhắc Check-out nối đuôi động
    fun scheduleCheckOutReminder(context: Context, uid: String, delayMs: Long, shiftId: String) {
        val data = Data.Builder()
            .putString("uid", uid)
            .putString("reminderType", "CHECK_OUT")
            .putString("shiftId", shiftId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("checkout_reminder_$uid")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "checkout_reminder_$uid",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        android.util.Log.d("NotificationHelper", "Đã đặt lịch nhắc Check-out sau ${delayMs / 1000 / 60} phút (ca $shiftId)")
    }

    // Hủy nhắc nhở Check-out (gọi khi nhân viên check-out thủ công trước giờ)
    fun cancelCheckOutReminder(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("checkout_reminder_$uid")
        android.util.Log.d("NotificationHelper", "Đã hủy lịch nhắc Check-out cho user $uid")
    }

    // Lên lịch nhắc Check-in cho ca tiếp theo (Nối đuôi/Vòng lặp thông minh)
    fun scheduleNextCheckInReminder(context: Context, uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Calendar.getInstance()
                
                val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
                if (!notificationsEnabled) {
                    WorkManager.getInstance(context.applicationContext).cancelUniqueWork("checkin_reminder_$uid")
                    android.util.Log.d("NotificationHelper", "Thông báo đã bị tắt bởi người dùng. Không lên lịch nhắc check-in.")
                    return@launch
                }

                val smartLearningEnabled = sharedPrefs.getBoolean("smart_learning_enabled", true)
                var habit = "UNKNOWN"
                var isTransitionDay = false

                if (smartLearningEnabled) {
                    // 1. Kiểm tra ngày chuyển tiếp (Thứ hai hoặc sau ngày nghỉ/lễ)
                    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                    isTransitionDay = (dayOfWeek == Calendar.MONDAY)

                    // 2. Dự đoán thói quen đi ca từ lịch sử
                    val db = com.example.data.db.AppDatabase.getInstance(context)
                    val entries = db.timeEntryDao().getLastCompletedEntries(uid, 12)
                    if (entries.size >= 3) {
                        var dayShiftCount = 0
                        var nightShiftCount = 0
                        for (entry in entries) {
                            val checkIn = entry.checkInTime ?: continue
                            val cal = Calendar.getInstance().apply { timeInMillis = checkIn }
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            if (hour in 5..15) {
                                dayShiftCount++
                            } else {
                                nightShiftCount++
                            }
                        }
                        
                        habit = when {
                            dayShiftCount >= 3 && dayShiftCount > nightShiftCount -> "DAY"
                            nightShiftCount >= 3 && nightShiftCount > dayShiftCount -> "NIGHT"
                            else -> "UNKNOWN"
                        }
                        android.util.Log.d("NotificationHelper", "Smart Learning - Habit: $habit, Day Count: $dayShiftCount, Night Count: $nightShiftCount, Is Transition: $isTransitionDay")
                    }
                }

                // Định nghĩa các mốc nhắc nhở (nhắc nhở trước 15 phút)
                val reminder1 = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 7)
                    set(Calendar.MINUTE, 15)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val reminder2 = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 19)
                    set(Calendar.MINUTE, 15)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val targetTime = Calendar.getInstance()

                if (smartLearningEnabled && !isTransitionDay && habit != "UNKNOWN") {
                    // Nếu đã xác định rõ thói quen và KHÔNG phải ngày chuyển tiếp:
                    if (habit == "DAY") {
                        // Chỉ nhắc Ca Ngày (07:15)
                        if (now.before(reminder1)) {
                            targetTime.timeInMillis = reminder1.timeInMillis
                        } else {
                            // Đã qua 07:15 hôm nay, đặt cho 07:15 ngày mai
                            targetTime.timeInMillis = reminder1.timeInMillis + 24 * 60 * 60 * 1000L
                        }
                    } else {
                        // Chỉ nhắc Ca Đêm (19:15)
                        if (now.before(reminder2)) {
                            targetTime.timeInMillis = reminder2.timeInMillis
                        } else {
                            // Đã qua 19:15 hôm nay, đặt cho 19:15 ngày mai
                            targetTime.timeInMillis = reminder2.timeInMillis + 24 * 60 * 60 * 1000L
                        }
                    }
                } else {
                    // Nếu ở ngày chuyển tiếp hoặc chưa rõ thói quen (UNKNOWN) hoặc tắt nhắc nhở thông minh:
                    // Xếp lịch nối đuôi bình thường (cả 2 ca) để đảm bảo không bỏ sót
                    val computedTarget = when {
                        now.before(reminder1) -> reminder1
                        now.before(reminder2) -> reminder2
                        else -> {
                            (reminder1.clone() as Calendar).apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                    }
                    targetTime.timeInMillis = computedTarget.timeInMillis
                }

                val delayMs = targetTime.timeInMillis - now.timeInMillis
                if (delayMs < 0) return@launch // Đảm bảo không âm giờ

                val data = Data.Builder()
                    .putString("uid", uid)
                    .putString("reminderType", "CHECK_IN")
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("checkin_reminder_$uid")
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    "checkin_reminder_$uid",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                android.util.Log.d("NotificationHelper", "Đã đặt lịch nhắc Check-in tiếp theo vào ${targetTime.time} (delay: ${delayMs / 1000}s)")
            } catch (e: Exception) {
                android.util.Log.e("NotificationHelper", "Lỗi trong scheduleNextCheckInReminder: ${e.message}")
            }
        }
    }
}
