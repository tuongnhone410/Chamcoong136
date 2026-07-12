package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SalaryCalculator {

    fun isHoliday(dateString: String): Boolean {
        if (dateString.length >= 5) {
            val dayMonth = dateString.substring(0, 5) // "dd/MM"
            val holidays = setOf(
                "01/01", // New Year
                "30/04", // Reunification Day
                "01/05", // Labor Day
                "02/09", // National Day
                "10/03"  // Hung Kings Commemoration (March 10 Lunar, represented statically)
            )
            return holidays.contains(dayMonth)
        }
        return false
    }

    fun isSunday(dateString: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(dateString) ?: return false
            val cal = Calendar.getInstance()
            cal.time = date
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        } catch (e: Exception) {
            false
        }
    }

    fun isNightShift(clockInTime: Long, clockOutTime: Long?): Boolean {
        val outTime = clockOutTime ?: System.currentTimeMillis()
        val calIn = Calendar.getInstance().apply { timeInMillis = clockInTime }
        val startHour = calIn.get(Calendar.HOUR_OF_DAY)
        if (startHour >= 21 || startHour <= 5) return true

        val calOut = Calendar.getInstance().apply { timeInMillis = outTime }
        val endHour = calOut.get(Calendar.HOUR_OF_DAY)
        if (endHour >= 22 || endHour <= 6) return true

        return false
    }

    fun getDayTypeLabel(dateString: String): String {
        return when {
            isHoliday(dateString) -> "NGÀY LỄ"
            isSunday(dateString) -> "CHỦ NHẬT"
            else -> "NGÀY THƯỜNG"
        }
    }

    fun calculateRecordDetails(
        record: AttendanceRecord,
        hourlyRate: Double,
        forceEnableCoefficients: Boolean = true,
        heSoNormal: Double = 1.5,
        heSoSunday: Double = 2.0,
        heSoHoliday: Double = 3.0
    ): RecordCalculation {
        if (record.status == "PaidLeave" || record.status == "PaidHolidayLeave") {
            val regEarnings = 8.0 * hourlyRate
            return RecordCalculation(
                totalHours = 8.0,
                regularHours = 8.0,
                otHours = 0.0,
                regularCoefficient = 1.0,
                otCoefficient = 1.0,
                regularEarnings = regEarnings,
                otEarnings = 0.0,
                totalEarnings = regEarnings,
                isSunday = false,
                isHoliday = record.status == "PaidHolidayLeave",
                isNightShift = false
            )
        }

        val clockOut = record.clockOutTime ?: System.currentTimeMillis()
        val durationMillis = clockOut - record.clockInTime
        val totalHours = durationMillis.toDouble() / (1000.0 * 60.0 * 60.0)

        val regularHours: Double
        val otHours: Double

        if (totalHours <= 8.0) {
            regularHours = totalHours
            otHours = 0.0
        } else if (totalHours <= 8.5) {
            regularHours = 8.0
            otHours = 0.0
        } else {
            regularHours = 8.0
            otHours = totalHours - 8.0
        }

        val isSun = isSunday(record.dateString)
        val isHol = isHoliday(record.dateString)
        val isNight = isNightShift(record.clockInTime, record.clockOutTime)

        val regCoeff: Double
        val otCoeff: Double

        if (forceEnableCoefficients) {
            when {
                isHol -> {
                    regCoeff = heSoHoliday
                    otCoeff = heSoHoliday
                }
                isSun -> {
                    regCoeff = heSoSunday
                    otCoeff = heSoSunday
                }
                else -> {
                    regCoeff = 1.0
                    otCoeff = if (isNight) 1.75 else heSoNormal
                }
            }
        } else {
            regCoeff = 1.0
            otCoeff = if (isNight) 1.75 else heSoNormal
        }

        val regEarnings = regularHours * hourlyRate * regCoeff
        val otEarnings = otHours * hourlyRate * otCoeff
        val totalEarnings = regEarnings + otEarnings

        return RecordCalculation(
            totalHours = totalHours,
            regularHours = regularHours,
            otHours = otHours,
            regularCoefficient = regCoeff,
            otCoefficient = otCoeff,
            regularEarnings = regEarnings,
            otEarnings = otEarnings,
            totalEarnings = totalEarnings,
            isSunday = isSun,
            isHoliday = isHol,
            isNightShift = isNight
        )
    }

    /**
     * Calculate progressive Personal Income Tax (PIT) for Vietnam.
     */
    fun calculatePIT(taxableIncome: Double): Double {
        if (taxableIncome <= 0) return 0.0
        return when {
            taxableIncome <= 5000000 -> taxableIncome * 0.05
            taxableIncome <= 10000000 -> taxableIncome * 0.10 - 250000
            taxableIncome <= 18000000 -> taxableIncome * 0.15 - 750000
            taxableIncome <= 32000000 -> taxableIncome * 0.20 - 1650000
            taxableIncome <= 52000000 -> taxableIncome * 0.25 - 3250000
            taxableIncome <= 80000000 -> taxableIncome * 0.30 - 5850000
            else -> taxableIncome * 0.35 - 9850000
        }
    }
}

data class RecordCalculation(
    val totalHours: Double,
    val regularHours: Double,
    val otHours: Double,
    val regularCoefficient: Double,
    val otCoefficient: Double,
    val regularEarnings: Double,
    val otEarnings: Double,
    val totalEarnings: Double,
    val isSunday: Boolean,
    val isHoliday: Boolean,
    val isNightShift: Boolean
)
