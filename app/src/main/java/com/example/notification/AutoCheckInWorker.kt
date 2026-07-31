package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.SalaryCalculator
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AutoCheckInWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        val scheduledTimeMs = inputData.getLong("scheduledTimeMs", 0L)

        android.util.Log.d("AutoCheckInWorker", "Thực hiện tự động vào ca cho user: $uid, scheduledTime: $scheduledTimeMs")

        try {
            val database = AppDatabase.getInstance(context)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(if (scheduledTimeMs > 0) scheduledTimeMs else System.currentTimeMillis()))
            val existing = database.timeEntryDao().getEntryByDate(uid, todayStr)

            if (existing != null && existing.checkInTime != null) {
                // User đã chủ động bấm vào ca thủ công trong ngày hôm nay. Không tự động check-in, nhưng hỗ trợ đặt lịch tự động ra ca.
                android.util.Log.d("AutoCheckInWorker", "User đã check-in thủ công hôm nay. Bỏ qua tự động vào ca, chuẩn bị đặt lịch tự động ra ca.")
                val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
                if (autoCheckoutEnabled) {
                    val checkoutMs = NotificationHelper.estimateHistoricalCheckoutTime(context, uid, existing)
                    NotificationHelper.scheduleAutoCheckOut(context, uid, checkoutMs)
                }
            } else {
                val checkInMs = if (scheduledTimeMs > 0) scheduledTimeMs else System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = checkInMs }
                val hour = cal.get(Calendar.HOUR_OF_DAY)

                val sId = if (hour >= 15) "ca2" else "ca1"
                val sType = if (sId == "ca2") "NIGHT" else "DAY"

                val newEntry = TimeEntry(
                    userId = uid,
                    date = todayStr,
                    checkInTime = checkInMs,
                    checkOutTime = null,
                    isWorking = true,
                    shiftId = sId,
                    shiftType = sType,
                    note = "🤖 Tự động vào ca theo lịch hẹn"
                )

                val userConfig = database.userConfigDao().getConfigForUser(uid) ?: UserConfig(userId = uid)
                val calculated = SalaryCalculator.calculateSingleEntry(newEntry, userConfig)

                database.timeEntryDao().insertOrUpdate(calculated)

                // Đồng bộ Firestore
                try {
                    val record = com.example.data.AttendanceRecord(
                        id = calculated.id.toLong(),
                        uid = calculated.userId,
                        dateString = calculated.date,
                        clockInTime = calculated.checkInTime ?: 0L,
                        clockOutTime = calculated.checkOutTime,
                        status = calculated.dayType,
                        notes = calculated.note ?: ""
                    )
                    com.example.data.FirestoreService.saveAttendanceRecord(record)
                } catch (e: Exception) {
                    android.util.Log.e("AutoCheckInWorker", "Lỗi đồng bộ Firestore sau khi tự động vào ca: ${e.message}")
                }

                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(checkInMs))

                // Hiển thị thông báo theo yêu cầu: "hh:mm Tôi đã tự động chấm công cho bạn"
                NotificationHelper.showNotification(
                    context = context,
                    title = "🤖 Tự động chấm công thành công!",
                    message = "$timeStr Tôi đã tự động chấm công cho bạn",
                    notificationId = 1003
                )

                // Nếu bật tự động ra ca, lên lịch auto checkout cho ca này
                val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
                val autoCheckoutEnabled = sharedPrefs.getBoolean("auto_checkout_enabled", false)
                if (autoCheckoutEnabled) {
                    val checkoutMs = NotificationHelper.estimateHistoricalCheckoutTime(context, uid, calculated)
                    NotificationHelper.scheduleAutoCheckOut(context, uid, checkoutMs)
                }
            }

            // Lên lịch tự động vào ca cho ngày mai
            val nextCheckInMs = NotificationHelper.estimateHistoricalCheckInTime(context, uid)
            NotificationHelper.scheduleAutoCheckIn(context, uid, nextCheckInMs)

            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AutoCheckInWorker", "Lỗi trong quá trình tự động vào ca: ${e.message}")
            return Result.failure()
        }
    }
}
