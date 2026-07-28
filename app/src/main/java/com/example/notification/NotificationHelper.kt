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

    // Lên lịch nhắc Check-in cho ca tiếp theo (Nối đuôi/Vòng lặp)
    fun scheduleNextCheckInReminder(context: Context, uid: String) {
        val now = Calendar.getInstance()

        // Tìm mốc thời gian nhắc tiếp theo:
        // Mốc 1: Hôm nay lúc 07:15 (cho ca ngày bắt đầu lúc 07:30, nhắc trước 15 phút)
        // Mốc 2: Hôm nay lúc 19:15 (cho ca đêm bắt đầu lúc 19:30, nhắc trước 15 phút)
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

        val targetTime = when {
            now.before(reminder1) -> reminder1
            now.before(reminder2) -> reminder2
            else -> {
                // Đã qua 19:15 hôm nay, hẹn mốc 07:15 ngày mai
                (reminder1.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        val delayMs = targetTime.timeInMillis - now.timeInMillis

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
    }
}
