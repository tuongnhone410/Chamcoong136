package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val companyId: String = "default_company",
    val name: String,
    val startTime: String, // HH:mm format
    val endTime: String,   // HH:mm format
    val breakMinutes: Int = 0,
    val standardHours: Double = 8.0,
    val crossesMidnight: Boolean = false,
    val enabled: Boolean = true
) {
    companion object {
        private val TIME_REGEX = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")

        fun isValidTimeFormat(timeStr: String): Boolean {
            val trimmed = timeStr.trim()
            if (!TIME_REGEX.matches(trimmed)) return false
            val parts = trimmed.split(":")
            if (parts.size != 2) return false
            val hh = parts[0].toIntOrNull() ?: return false
            val mm = parts[1].toIntOrNull() ?: return false
            return hh in 0..23 && mm in 0..59
        }

        fun validateShift(
            name: String,
            startTime: String,
            endTime: String,
            breakMinutes: Int,
            standardHours: Double
        ): String? {
            if (name.trim().isEmpty()) return "Tên ca không được để trống"
            if (!isValidTimeFormat(startTime)) return "Giờ bắt đầu phải theo định dạng HH:mm (00:00 - 23:59)"
            if (!isValidTimeFormat(endTime)) return "Giờ kết thúc phải theo định dạng HH:mm (00:00 - 23:59)"
            if (breakMinutes < 0) return "Thời gian nghỉ không được âm"
            if (standardHours < 0.0) return "Số giờ tiêu chuẩn không được âm"
            return null
        }

        fun isOvernight(startTime: String, endTime: String): Boolean {
            if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) return false
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            val startTotalMin = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endTotalMin = endParts[0].toInt() * 60 + endParts[1].toInt()
            return endTotalMin <= startTotalMin
        }

        fun calculateDurationHours(
            startTime: String,
            endTime: String,
            crossesMidnight: Boolean,
            breakMinutes: Int = 0
        ): Double {
            if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) return 0.0

            val startParts = startTime.split(":")
            val endParts = endTime.split(":")

            val startHour = startParts[0].toInt()
            val startMin = startParts[1].toInt()
            val endHour = endParts[0].toInt()
            val endMin = endParts[1].toInt()

            val calStart = Calendar.getInstance().apply {
                set(2026, Calendar.AUGUST, 3, startHour, startMin, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val calEnd = Calendar.getInstance().apply {
                set(2026, Calendar.AUGUST, 3, endHour, endMin, 0)
                set(Calendar.MILLISECOND, 0)
                if (crossesMidnight || (endHour * 60 + endMin <= startHour * 60 + startMin)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val durationMs = calEnd.timeInMillis - calStart.timeInMillis
            val rawHours = durationMs / (1000.0 * 60.0 * 60.0)
            val breakHours = breakMinutes / 60.0
            return (rawHours - breakHours).coerceAtLeast(0.0)
        }
    }
}
