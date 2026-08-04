package com.example.domain.calculation

import com.example.data.ShiftConfig
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.viewmodel.SalarySummary

/**
 * Interface defining the attendance & salary calculation engine.
 */
interface CalculationEngine {
    val shifts: Map<String, ShiftConfig>
    
    fun getShiftForEntry(entry: TimeEntry): ShiftConfig
    
    fun normalizeDateToDmy(dateStr: String): String
    
    fun isLeaveType(dayType: String?): Boolean
    
    fun calculateSingleEntry(entry: TimeEntry, config: UserConfig? = null): TimeEntry
    
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
    ): Double
    
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
    ): SalarySummary

    fun isHoliday(dateString: String): Boolean
    
    fun isSunday(dateString: String): Boolean
    
    fun isNightShift(clockInTime: Long, clockOutTime: Long?): Boolean
    
    fun getDayTypeLabel(dateString: String): String
    
    fun calculatePIT(taxableIncome: Double): Double
}
