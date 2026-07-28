package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        val reminderType = inputData.getString("reminderType") ?: return Result.failure()

        android.util.Log.d("ReminderWorker", "Bắt đầu kiểm tra cho loại nhắc nhở: $reminderType, user: $uid")

        when (reminderType) {
            "CHECK_IN" -> {
                try {
                    // 1. Last-second filter (Kiểm tra sát nút ngày nghỉ & ngày lễ & ca làm việc)
                    val today = Calendar.getInstance()
                    
                    if (isSunday(today)) {
                        android.util.Log.d("ReminderWorker", "Hôm nay là Chủ nhật (ngày nghỉ), không gửi thông báo Check-in.")
                        // Tự động lên lịch cho ca tiếp theo (để vòng lặp nối đuôi tiếp diễn)
                        NotificationHelper.scheduleNextCheckInReminder(context, uid)
                        return Result.success()
                    }

                    if (isHoliday(today)) {
                        android.util.Log.d("ReminderWorker", "Hôm nay là ngày nghỉ lễ, không gửi thông báo Check-in.")
                        NotificationHelper.scheduleNextCheckInReminder(context, uid)
                        return Result.success()
                    }

                    if (!isUserScheduledToWorkToday(uid, today)) {
                        android.util.Log.d("ReminderWorker", "Hôm nay nhân viên không có lịch phân ca, không gửi thông báo Check-in.")
                        NotificationHelper.scheduleNextCheckInReminder(context, uid)
                        return Result.success()
                    }

                    // 2. Kiểm tra nếu nhân viên ĐÃ check-in từ trước thì không gửi nhắc nhở nữa
                    val database = AppDatabase.getInstance(context)
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val existingEntry = database.timeEntryDao().getEntryByDate(uid, todayStr)
                    if (existingEntry != null && existingEntry.checkInTime != null) {
                        android.util.Log.d("ReminderWorker", "Nhân viên đã check-in từ trước, không cần nhắc nhở.")
                        NotificationHelper.scheduleNextCheckInReminder(context, uid)
                        return Result.success()
                    }

                    // 3. Hiển thị thông báo nhắc Check-in
                    NotificationHelper.showNotification(
                        context = context,
                        title = "🔔 Nhắc nhở vào ca làm việc",
                        message = "Đã đến giờ vào ca của bạn rồi! Hãy nhanh chóng check-in trên ứng dụng nhé.",
                        notificationId = 1001
                    )

                    // 4. Lên lịch tiếp theo
                    NotificationHelper.scheduleNextCheckInReminder(context, uid)

                } catch (e: Exception) {
                    android.util.Log.e("ReminderWorker", "Lỗi khi chạy nhắc nhở Check-in: ${e.message}")
                }
            }

            "CHECK_OUT" -> {
                try {
                    // Kiểm tra xem nhân viên có đang trong ca hay không (isWorking == true)
                    val database = AppDatabase.getInstance(context)
                    val activeEntry = database.timeEntryDao().getActiveEntry(uid)
                    
                    if (activeEntry != null && activeEntry.isWorking) {
                        // Nếu nhân viên chưa check-out, hiển thị thông báo
                        NotificationHelper.showNotification(
                            context = context,
                            title = "🔔 Nhắc nhở ra ca làm việc",
                            message = "Đã hết thời gian ca làm việc dự kiến của bạn. Hãy thực hiện check-out để ghi nhận đầy đủ giờ công nhé!",
                            notificationId = 1002
                        )
                    } else {
                        android.util.Log.d("ReminderWorker", "Nhân viên đã check-out thủ công hoặc không có ca làm việc hoạt động, bỏ qua thông báo.")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReminderWorker", "Lỗi khi chạy nhắc nhở Check-out: ${e.message}")
                }
            }
        }

        return Result.success()
    }

    // Các hàm kiểm tra ngày nghỉ & ngày lễ
    private fun isSunday(cal: Calendar): Boolean {
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    private fun isHoliday(cal: Calendar): Boolean {
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH) + 1
        val mdStr = String.format(Locale.US, "%02d-%02d", d, m)
        // Các ngày lễ cố định: Tết dương lịch, 30/4, Quốc tế lao động 1/5, Quốc khánh 2/9
        return mdStr == "01-01" || mdStr == "30-04" || mdStr == "01-05" || mdStr == "02-09"
    }

    /**
     * Hàm giả lập (mock data) kiểm tra phân lịch làm việc cho từng ca ngày/đêm của nhân viên
     */
    private fun isUserScheduledToWorkToday(uid: String, cal: Calendar): Boolean {
        // Giả lập lịch làm việc:
        // Đa số nhân viên đều được phân lịch từ thứ Hai đến thứ Bảy hàng tuần.
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        
        // Bạn có thể mở rộng logic kiểm tra lịch thực tế từ Firestore/Database ở đây
        return true
    }
}
