package com.example.data

import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.domain.calculation.CalculationEngine
import com.example.domain.calculation.LegacyCalculationEngine
import com.example.domain.calculation.ConfigurableCalculationEngine
import com.example.viewmodel.SalarySummary

data class ShiftConfig(
    val shiftId: String,
    val shiftType: String, // "DAY", "DAY_REST", "NIGHT"
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val checkInWindowStart: String,
    val checkInWindowEnd: String,
    val checkOutWindowStart: String,
    val checkOutWindowEnd: String,
    val breakHours: Double,
    val standardHours: Double = 8.0
)

/**
 * SalaryCalculator serves as the backward-compatible facade delegating calculation calls
 * to LegacyCalculationEngine or ConfigurableCalculationEngine based on entry profile.
 */
object SalaryCalculator {

    val legacyEngine: CalculationEngine = LegacyCalculationEngine()
    val configurableEngine: CalculationEngine = ConfigurableCalculationEngine()

    fun getEngine(entry: TimeEntry? = null): CalculationEngine {
        if (entry != null && (entry.workRuleId != null || entry.overtimeRuleId != null || entry.snapshotStandardHours != null)) {
            return configurableEngine
        }
        return legacyEngine
    }

    val SHIFTS: Map<String, ShiftConfig>
        get() = legacyEngine.shifts

    fun getShiftForEntry(entry: TimeEntry): ShiftConfig {
        return getEngine(entry).getShiftForEntry(entry)
    }

    fun normalizeDateToDmy(dateStr: String): String {
        return legacyEngine.normalizeDateToDmy(dateStr)
    }

    fun isLeaveType(dayType: String?): Boolean {
        return legacyEngine.isLeaveType(dayType)
    }

    fun calculateSingleEntry(entry: TimeEntry, config: UserConfig? = null): TimeEntry {
        return getEngine(entry).calculateSingleEntry(entry, config)
    }

    fun calculateAllowanceValue(
        fieldName: String,
        allowanceValue: Double,
        calcType: String,
        totalWorkDays: Double,
        comCaCount: Int,
        comOtCount: Int,
        nightShiftsCount: Int,
        scheduledDaysSoFar: Int,
        totalScheduledDaysInMonth: Int
    ): Double {
        return legacyEngine.calculateAllowanceValue(
            fieldName, allowanceValue, calcType, totalWorkDays,
            comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth
        )
    }

    fun calculateMonthlySalary(
        entries: List<TimeEntry>,
        config: UserConfig,
        scheduledDaysSoFar: Int,
        totalScheduledDaysInMonth: Int,
        earliestDate: String?,
        selectedMonth: String,
        todayStr: String,
        isCurrentSelectedMonth: Boolean,
        holidayDatesInMonth: Set<String>
    ): SalarySummary {
        // If any entry uses configurable engine, use configurable engine for monthly salary
        val useConfigurable = entries.any { it.workRuleId != null || it.overtimeRuleId != null || it.snapshotStandardHours != null }
        val engineToUse = if (useConfigurable) configurableEngine else legacyEngine
        return engineToUse.calculateMonthlySalary(
            entries, config, scheduledDaysSoFar, totalScheduledDaysInMonth,
            earliestDate, selectedMonth, todayStr, isCurrentSelectedMonth, holidayDatesInMonth
        )
    }

    fun isHoliday(dateString: String): Boolean {
        return legacyEngine.isHoliday(dateString)
    }

    fun isSunday(dateString: String): Boolean {
        return legacyEngine.isSunday(dateString)
    }

    fun isNightShift(clockInTime: Long, clockOutTime: Long?): Boolean {
        return legacyEngine.isNightShift(clockInTime, clockOutTime)
    }

    fun getDayTypeLabel(dateString: String): String {
        return legacyEngine.getDayTypeLabel(dateString)
    }

    fun getRoundedTime(timeMillis: Long, isClockIn: Boolean): Long {
        return timeMillis
    }

    fun calculatePIT(taxableIncome: Double): Double {
        return legacyEngine.calculatePIT(taxableIncome)
    }
}
