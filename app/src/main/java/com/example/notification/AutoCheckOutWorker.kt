package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.SalaryCalculator
import com.example.data.model.UserConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AutoCheckOutWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        val scheduledTimeMs = inputData.getLong("scheduledTimeMs", 0L)

        android.util.Log.d("AutoCheckOutWorker", "Thực hiện tự động ra ca cho user: $uid, scheduledTime: $scheduledTimeMs")

        try {
            val database = AppDatabase.getInstance(context)
            val active = database.timeEntryDao().getActiveEntry(uid)

            if (active != null && active.isWorking) {
                val checkoutMs = if (scheduledTimeMs > 0) scheduledTimeMs else System.currentTimeMillis()
                val cal = Calendar.getInstance().apply { timeInMillis = checkoutMs }
                val hour = cal.get(Calendar.HOUR_OF_DAY)

                val sId = if (active.shiftId == "ca1" && hour >= 20) "ca2" else active.shiftId ?: "ca1"
                val sType = if (sId == "ca2") "DAY_REST" else active.shiftType ?: "DAY"

                val updated = active.copy(
                    checkOutTime = checkoutMs,
                    isWorking = false,
                    shiftId = sId,
                    shiftType = sType,
                    note = if (active.note.isNullOrBlank()) "🤖 Tự động ra ca theo lịch hẹn" else "${active.note} (🤖 Tự động ra ca)"
                )

                val userConfig = database.userConfigDao().getConfigForUser(uid) ?: UserConfig(userId = uid)
                val calculated = SalaryCalculator.calculateSingleEntry(updated, userConfig)

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
                    android.util.Log.e("AutoCheckOutWorker", "Lỗi đồng bộ Firestore sau khi tự động ra ca: ${e.message}")
                }

                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(checkoutMs))

                // Hiển thị thông báo hoàn thành tự động ra ca
                NotificationHelper.showNotification(
                    context = context,
                    title = "🤖 Tự động ra ca thành công!",
                    message = "Hệ thống đã tự động bấm ra ca lúc $timeStr cho bạn.",
                    notificationId = 1002
                )
            } else {
                android.util.Log.d("AutoCheckOutWorker", "Không tìm thấy ca làm việc active nào để tự động ra ca.")
            }

            // Lên lịch nhắc Check-in cho ca làm tiếp theo
            NotificationHelper.scheduleNextCheckInReminder(context, uid)

            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AutoCheckOutWorker", "Lỗi trong quá trình tự động ra ca: ${e.message}")
            return Result.failure()
        }
    }
}
