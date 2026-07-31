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
import java.util.Date

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

    // Lên lịch nhắc Check-in
    fun scheduleCheckInReminder(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        var delayMs = targetTimeMs - now
        
        if (delayMs < 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = targetTimeMs
                add(Calendar.DAY_OF_YEAR, 1)
            }
            delayMs = cal.timeInMillis - now
        }

        val data = Data.Builder()
            .putString("uid", uid)
            .putString("reminderType", "CHECK_IN")
            .putLong("scheduledTimeMs", targetTimeMs)
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
    }

    // Hủy nhắc nhở Check-in
    fun cancelCheckInReminder(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("checkin_reminder_$uid")
    }

    // Lên lịch nhắc Check-out nối đuôi động
    fun scheduleCheckOutReminder(context: Context, uid: String, delayMs: Long, shiftId: String) {
        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) return

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
    }

    // Hủy nhắc nhở Check-out
    fun cancelCheckOutReminder(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("checkout_reminder_$uid")
    }

    fun scheduleCheckOutReminderForActiveEntry(context: Context, uid: String, activeEntry: TimeEntry) {
        if (!activeEntry.isWorking) return

        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        if (!notificationsEnabled) return

        val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
        if (autoCheckoutEnabled) return

        val checkInTime = activeEntry.checkInTime ?: System.currentTimeMillis()
        val shift = SalaryCalculator.getShiftForEntry(activeEntry)

        val targetCal = Calendar.getInstance().apply {
            timeInMillis = checkInTime
            add(Calendar.HOUR_OF_DAY, 11)
            add(Calendar.MINUTE, 45)
        }

        val now = System.currentTimeMillis()
        var delayMs = targetCal.timeInMillis - now
        if (delayMs <= 0) delayMs = 2000L

        scheduleCheckOutReminder(context, uid, delayMs, shift.shiftId)
    }

    // Ước tính giờ ra ca dựa trên lịch sử
    suspend fun estimateHistoricalCheckoutTime(context: Context, uid: String, activeEntry: TimeEntry): Long {
        val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val customTime = sharedPrefs.getString("custom_checkout_time", "") ?: ""
        val checkInMs = activeEntry.checkInTime ?: System.currentTimeMillis()
        val shift = SalaryCalculator.getShiftForEntry(activeEntry)
        val calCheckIn = Calendar.getInstance().apply { timeInMillis = checkInMs }
        
        if (customTime.isNotBlank()) {
            try {
                val parts = customTime.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 19
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
                val cal = Calendar.getInstance().apply {
                    timeInMillis = checkInMs
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= checkInMs) cal.add(Calendar.DAY_OF_YEAR, 1)
                return cal.timeInMillis
            } catch (e: Exception) {}
        }
        
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
                    val monthEntries = entries.filter { (nowCal.timeInMillis - (it.checkInTime ?: 0L)) <= 30L * 24 * 3600 * 1000 }
                    val weekOfMonth = ((nowCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1
                    val matching = monthEntries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        ((entryCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1 == weekOfMonth
                    }
                    if (matching.isNotEmpty()) matching else monthEntries
                } else {
                    val sameDay = entries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        entryCal.get(Calendar.DAY_OF_WEEK) == currentDayOfWeek
                    }
                    if (sameDay.isNotEmpty()) sameDay else entries.take(7)
                }

                val validCheckoutTimes = targetEntries.mapNotNull { it.checkOutTime }
                if (validCheckoutTimes.isNotEmpty()) {
                    val isCurrentNight = calCheckIn.get(Calendar.HOUR_OF_DAY) >= 15
                    val filteredTimes = validCheckoutTimes.filter { time ->
                        val c = Calendar.getInstance().apply { timeInMillis = time }
                        val hour = c.get(Calendar.HOUR_OF_DAY)
                        if (isCurrentNight) (hour in 4..11) else (hour in 15..23)
                    }.ifEmpty { validCheckoutTimes }

                    var totalMins = 0L
                    for (coTime in filteredTimes) {
                        val calCO = Calendar.getInstance().apply { timeInMillis = coTime }
                        totalMins += calCO.get(Calendar.HOUR_OF_DAY) * 60 + calCO.get(Calendar.MINUTE)
                    }
                    val avgMins = (totalMins / filteredTimes.size).toInt()
                    targetHour = avgMins / 60
                    targetMin = avgMins % 60
                }
            }
        } catch (e: Exception) {}

        if (targetHour == -1) return checkInMs + 12 * 60 * 60 * 1000L

        val targetCal = Calendar.getInstance().apply {
            timeInMillis = checkInMs
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (shift.shiftType == "NIGHT" || targetCal.before(calCheckIn)) targetCal.add(Calendar.DAY_OF_YEAR, 1)
        return targetCal.timeInMillis
    }

    // Ước tính giờ vào ca dựa trên lịch sử
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
            if (cal.before(Calendar.getInstance())) cal.add(Calendar.DAY_OF_YEAR, 1)
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
                    val monthEntries = entries.filter { (nowCal.timeInMillis - (it.checkInTime ?: 0L)) <= 30L * 24 * 3600 * 1000 }
                    val weekOfMonth = ((nowCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1
                    val matching = monthEntries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        ((entryCal.get(Calendar.DAY_OF_MONTH) - 1) / 7) + 1 == weekOfMonth
                    }
                    if (matching.isNotEmpty()) matching else monthEntries
                } else {
                    val sameDay = entries.filter { entry ->
                        val entryCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime ?: 0L }
                        entryCal.get(Calendar.DAY_OF_WEEK) == currentDayOfWeek
                    }
                    if (sameDay.isNotEmpty()) sameDay else entries.take(7)
                }

                val validCheckInTimes = targetEntries.mapNotNull { it.checkInTime }
                if (validCheckInTimes.isNotEmpty()) {
                    val recentEntries = entries.take(3)
                    val isRecentNight = recentEntries.count { 
                        val c = Calendar.getInstance().apply { timeInMillis = it.checkInTime ?: 0L }
                        c.get(Calendar.HOUR_OF_DAY) >= 15
                    } >= 2
                    
                    val filteredTimes = validCheckInTimes.filter { time ->
                        val c = Calendar.getInstance().apply { timeInMillis = time }
                        val isNight = c.get(Calendar.HOUR_OF_DAY) >= 15
                        isNight == isRecentNight
                    }.ifEmpty { validCheckInTimes }

                    var totalMins = 0L
                    for (ciTime in filteredTimes) {
                        val c = Calendar.getInstance().apply { timeInMillis = ciTime }
                        totalMins += c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
                    }
                    val avgMins = (totalMins / filteredTimes.size).toInt()
                    targetHour = avgMins / 60
                    targetMin = avgMins % 60
                }
            }
        } catch (e: Exception) {}

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.before(Calendar.getInstance())) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    // Đặt lịch Tự động Vào Ca
    fun scheduleAutoCheckIn(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        var delayMs = targetTimeMs - now
        if (delayMs < 0) {
            if (delayMs > -15 * 60 * 1000) delayMs = 0 else return
        }

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
    }

    // Hủy Tự động Vào Ca
    fun cancelAutoCheckIn(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("auto_checkin_$uid")
    }

    // Đặt lịch Tự động Ra Ca
    fun scheduleAutoCheckOut(context: Context, uid: String, targetTimeMs: Long) {
        val now = System.currentTimeMillis()
        var delayMs = targetTimeMs - now
        if (delayMs < 0) {
            if (delayMs > -15 * 60 * 1000) delayMs = 0 else return
        }

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
    }

    // Hủy Tự động Ra Ca
    fun cancelAutoCheckOut(context: Context, uid: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("auto_checkout_$uid")
    }

    // Lên lịch nhắc Check-in cho ca tiếp theo
    fun scheduleNextCheckInReminder(context: Context, uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val autoInEnabled = sharedPrefs.getBoolean("auto_check_in_enabled", false)
                val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
                
                if (!autoInEnabled && !notificationsEnabled) {
                    cancelCheckInReminder(context, uid)
                    cancelAutoCheckIn(context, uid)
                    return@launch
                }

                val targetMs = estimateHistoricalCheckInTime(context, uid)
                
                if (autoInEnabled) {
                    scheduleAutoCheckIn(context, uid, targetMs)
                    cancelCheckInReminder(context, uid)
                } else {
                    scheduleCheckInReminder(context, uid, targetMs)
                    cancelAutoCheckIn(context, uid)
                }
            } catch (e: Exception) {}
        }
    }
}
