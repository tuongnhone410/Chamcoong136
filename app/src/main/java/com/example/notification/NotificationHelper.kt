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
import com.example.data.model.TimeEntry
import com.example.data.SalaryCalculator
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
        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) {
            android.util.Log.d("NotificationHelper", "Thông báo đã bị tắt. Không lên lịch nhắc check-out.")
            return
        }

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
        android.util.Log.d("NotificationHelper", "Đã đặt lịch nhắc Check-out sau ${delayMs / 1000 / 60} phút (${delayMs / 1000}s) cho ca $shiftId")
    }

    // Lên lịch nhắc Check-out dựa trên ca làm việc thực tế đang diễn ra (Tính 11 tiếng 45 phút / trước 12 tiếng 15 phút)
    fun scheduleCheckOutReminderForActiveEntry(context: Context, uid: String, activeEntry: TimeEntry) {
        if (!activeEntry.isWorking) return

        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) return

        val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
        if (autoCheckoutEnabled) {
            android.util.Log.d("NotificationHelper", "Chế độ tự động ra ca đang bật. Tắt thông báo nhắc nhở ra ca.")
            return
        }

        val checkInTime = activeEntry.checkInTime ?: System.currentTimeMillis()
        val shift = com.example.data.SalaryCalculator.getShiftForEntry(activeEntry)

        // Bật thông báo nhắc nhở: tính trước 12 tiếng 15 phút = 11 tiếng 45 phút kể từ lúc check-in (nếu nhân viên mới) hoặc trước giờ hết ca 15 phút
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = checkInTime
            add(Calendar.HOUR_OF_DAY, 11)
            add(Calendar.MINUTE, 45)
        }

        val now = System.currentTimeMillis()
        var delayMs = targetCal.timeInMillis - now

        if (delayMs <= 0) {
            delayMs = 2000L
        }

        scheduleCheckOutReminder(context, uid, delayMs, shift.shiftId)
    }

    // Ước tính giờ ra ca dựa trên lịch sử làm việc cũ theo tuần (1, 2, 3 tuần) hoặc tháng (1 tháng gần nhất)
    suspend fun estimateHistoricalCheckoutTime(context: Context, uid: String, activeEntry: TimeEntry): Long {
        val checkInMs = activeEntry.checkInTime ?: System.currentTimeMillis()
        val shift = com.example.data.SalaryCalculator.getShiftForEntry(activeEntry)
        val calCheckIn = Calendar.getInstance().apply { timeInMillis = checkInMs }
        
        var targetHour = -1
        var targetMin = -1

        try {
            val db = com.example.data.db.AppDatabase.getInstance(context)
            val entries = db.timeEntryDao().getLastCompletedEntries(uid, 60)
            if (entries.isNotEmpty()) {
                val nowCal = Calendar.getInstance()
                val currentDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)
                val oldestEntryTime = entries.last().checkInTime ?: nowCal.timeInMillis
                val spanDays = (nowCal.timeInMillis - oldestEntryTime) / (1000 * 60 * 60 * 24)

                val targetEntries = if (spanDays >= 28) {
                    // Trống quá lâu (ví dụ 1 tháng): Học theo 1 tháng gần nhất (không học 2,3 tháng)
                    val monthEntries = entries.filter { (nowCal.timeInMillis - (it.checkInTime ?: 0L)) <= 30L * 24 * 3600 * 1000 }
                    val weekOfMonth = ((nowCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1
                    val matching = monthEntries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        ((entryCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1 == weekOfMonth
                    }
                    if (matching.isNotEmpty()) matching else monthEntries
                } else {
                    // Học theo thứ tự 1, 2, 3 tuần (tuần 1, tuần 2, tuần 3)
                    val sameDay = entries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        entryCal.get(Calendar.DAY_OF_WEEK) == currentDayOfWeek
                    }
                    if (sameDay.isNotEmpty()) sameDay else entries.take(7)
                }

                val validCheckoutTimes = targetEntries.mapNotNull { it.checkOutTime }
                if (validCheckoutTimes.isNotEmpty()) {
                    var totalMins = 0L
                    for (coTime in validCheckoutTimes) {
                        val calCO = Calendar.getInstance().apply { timeInMillis = coTime }
                        totalMins += calCO.get(Calendar.HOUR_OF_DAY) * 60 + calCO.get(Calendar.MINUTE)
                    }
                    val avgMins = (totalMins / validCheckoutTimes.size).toInt()
                    targetHour = avgMins / 60
                    targetMin = avgMins % 60
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Lỗi lấy lịch sử checkout: ${e.message}")
        }

        if (targetHour == -1) {
            // Nhân viên mới chưa có lịch sử (Tự động ra ca): Tính đúng 12 tiếng kể từ lúc bấm vào ca
            return checkInMs + 12 * 60 * 60 * 1000L
        }

        val targetCal = Calendar.getInstance().apply {
            timeInMillis = checkInMs
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (shift.shiftType == "NIGHT" || targetCal.before(calCheckIn)) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return targetCal.timeInMillis
    }

    // Ước tính giờ vào ca dựa trên cấu hình tùy chỉnh hoặc học theo lịch sử (1, 2, 3 tuần hoặc 1 tháng gần nhất)
    suspend fun estimateHistoricalCheckInTime(context: Context, uid: String): Long {
        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val customTime = sharedPrefs.getString("custom_check_in_time", "") ?: ""
        
        if (customTime.isNotBlank()) {
            val parts = customTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.before(Calendar.getInstance())) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        var targetHour = 7
        var targetMin = 30
        try {
            val db = com.example.data.db.AppDatabase.getInstance(context)
            val entries = db.timeEntryDao().getLastCompletedEntries(uid, 60)
            if (entries.isNotEmpty()) {
                val nowCal = Calendar.getInstance()
                val currentDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)
                val oldestEntryTime = entries.last().checkInTime ?: nowCal.timeInMillis
                val spanDays = (nowCal.timeInMillis - oldestEntryTime) / (1000 * 60 * 60 * 24)

                val targetEntries = if (spanDays >= 28) {
                    // Trống quá lâu (ví dụ 1 tháng): Học theo 1 tháng gần nhất (chỉ cần học 1 tháng, không học 2,3 tháng)
                    val monthEntries = entries.filter { (nowCal.timeInMillis - (it.checkInTime ?: 0L)) <= 30L * 24 * 3600 * 1000 }
                    val weekOfMonth = ((nowCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1
                    val matching = monthEntries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        ((entryCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1 == weekOfMonth
                    }
                    if (matching.isNotEmpty()) matching else monthEntries
                } else {
                    // Học theo thứ tự 1, 2, 3 tuần (tuần 1, tuần 2, tuần 3)
                    val sameDay = entries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        entryCal.get(Calendar.DAY_OF_WEEK) == currentDayOfWeek
                    }
                    if (sameDay.isNotEmpty()) sameDay else entries.take(7)
                }

                val validCheckInTimes = targetEntries.mapNotNull { it.checkInTime }
                if (validCheckInTimes.isNotEmpty()) {
                    var totalMins = 0L
                    for (ciTime in validCheckInTimes) {
                        val c = Calendar.getInstance().apply { timeInMillis = ciTime }
                        totalMins += c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
                    }
                    val avgMins = (totalMins / validCheckInTimes.size).toInt()
                    targetHour = avgMins / 60
                    targetMin = avgMins % 60
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Lỗi học lịch sử checkin: ${e.message}")
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    // Đặt lịch Hẹn giờ / Tự động Vào Ca
    fun scheduleAutoCheckIn(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        val delayMs = (targetTimeMs - now).coerceAtLeast(1000L)

        val data = Data.Builder()
            .putString("uid", uid)
            .putLong("scheduledTimeMs", targetTimeMs)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoCheckInWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("auto_checkin_$uid")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "auto_checkin_$uid",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        android.util.Log.d("NotificationHelper", "Đã đặt lịch Tự động vào ca sau ${delayMs / 1000 / 60} phút cho user $uid")
    }

    // Hủy Tự động Vào Ca
    fun cancelAutoCheckIn(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("auto_checkin_$uid")
        android.util.Log.d("NotificationHelper", "Đã hủy Tự động vào ca cho user $uid")
    }

    // Đặt lịch Hẹn giờ / Tự động Ra Ca
    fun scheduleAutoCheckOut(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        val delayMs = (targetTimeMs - now).coerceAtLeast(1000L)

        // Tắt nhắc nhở ra ca thông thường để tránh tạo tín hiệu nhắc nhở khi đã bật tự động ra ca
        cancelCheckOutReminder(context, uid)

        val data = Data.Builder()
            .putString("uid", uid)
            .putLong("scheduledTimeMs", targetTimeMs)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoCheckOutWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("auto_checkout_$uid")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "auto_checkout_$uid",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        android.util.Log.d("NotificationHelper", "Đã đặt lịch Hẹn giờ / Tự động ra ca sau ${delayMs / 1000 / 60} phút cho user $uid")
    }

    // Hủy Hẹn giờ / Tự động Ra Ca
    fun cancelAutoCheckOut(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("auto_checkout_$uid")
        android.util.Log.d("NotificationHelper", "Đã hủy Tự động ra ca cho user $uid")
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

                val reminderMinutesStr = sharedPrefs.getString("reminder_minutes_before", "15") ?: "15"
                val reminderMinutes = reminderMinutesStr.toIntOrNull() ?: 15

                // Định nghĩa các mốc nhắc nhở (nhắc nhở trước giờ vào ca: Ca ngày 07:30, Ca đêm 19:30)
                val reminder1 = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 7)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -reminderMinutes)
                }

                val reminder2 = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 19)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -reminderMinutes)
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
