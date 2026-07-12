package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.data.repository.TimeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SalarySummary(
    val workingDays: Double = 0.0,
    val overtimeHoursWeekday: Double = 0.0,
    val overtimeHoursSunday: Double = 0.0,
    val overtimeHoursHoliday: Double = 0.0,
    val baseSalaryReceived: Double = 0.0,
    val responsibilityPayReceived: Double = 0.0,
    val technicalAllowanceReceived: Double = 0.0,
    val diligenceAllowanceReceived: Double = 0.0,
    // Flat allowances
    val pcChucVu: Double = 0.0,
    val pcHieuSuat: Double = 0.0,
    val pcSanPham: Double = 0.0,
    val pcComCa: Double = 0.0,
    val pcComOt: Double = 0.0,
    val pcNhaO: Double = 0.0,
    val pcDocHai: Double = 0.0,
    val pcDtDoanhThu: Double = 0.0,
    val pcXangXe: Double = 0.0,
    val pcThamNien: Double = 0.0,
    val pcKhac1: Double = 0.0,
    val pcKhac: Double = 0.0,
    val thuong: Double = 0.0,
    val tienComNhan: Double = 0.0,
    // Overtime
    val overtimePayWeekday: Double = 0.0,
    val overtimePaySunday: Double = 0.0,
    val overtimePayHoliday: Double = 0.0,
    val totalOvertimePay: Double = 0.0,
    // Gross/Net
    val totalAllowances: Double = 0.0,
    val bhxhDeduction: Double = 0.0,
    val unionFeeDeduction: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val netSalary: Double = 0.0
)

class TimeSnapViewModel(private val repository: TimeRepository) : ViewModel() {

    private val _currentMonth = MutableStateFlow("")
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    private val userId = "default_user"

    init {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        _currentMonth.value = sdf.format(Date())
    }

    val userConfig: StateFlow<UserConfig> = repository.getUserConfig(userId)
        .map { it ?: UserConfig(userId = userId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserConfig(userId = userId)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthTimeEntries: StateFlow<List<TimeEntry>> = _currentMonth
        .flatMapLatest { month ->
            repository.getTimeEntriesByMonth(userId, month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val salarySummary: StateFlow<SalarySummary> = combine(userConfig, monthTimeEntries) { config, entries ->
        calculateSalarySummary(config, entries)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalarySummary()
    )

    fun setMonth(month: String) {
        _currentMonth.value = month
    }

    fun saveConfig(config: UserConfig) {
        viewModelScope.launch {
            repository.saveUserConfig(config.copy(userId = userId))
        }
    }

    fun saveTimeEntry(entry: TimeEntry) {
        viewModelScope.launch {
            repository.insertTimeEntry(entry.copy(userId = userId))
        }
    }

    fun deleteTimeEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteTimeEntryById(id)
        }
    }

    private fun calculateSalarySummary(config: UserConfig, entries: List<TimeEntry>): SalarySummary {
        // 1. Calculate working days (ngày công thực tế)
        var workingDays = 0.0
        var overtimeHoursWeekday = 0.0
        var overtimeHoursSunday = 0.0
        var overtimeHoursHoliday = 0.0

        for (entry in entries) {
            // Check day type and work status
            val durationHours = if (entry.checkInTime != null && entry.checkOutTime != null) {
                (entry.checkOutTime - entry.checkInTime).toDouble() / (1000.0 * 3600.0)
            } else {
                0.0
            }

            val countsAsWorkDay = entry.dayType == "NORMAL" && (entry.isWorking || durationHours > 0)
            val countsAsPaidLeave = entry.dayType == "PAID_LEAVE"
            val countsAsHoliday = entry.dayType == "HOLIDAY"

            if (countsAsWorkDay || countsAsPaidLeave || countsAsHoliday) {
                workingDays += 1.0
            }

            // Overtime calculations
            if (durationHours > 0) {
                when (entry.dayType) {
                    "NORMAL" -> {
                        if (durationHours > 8.0) {
                            overtimeHoursWeekday += (durationHours - 8.0)
                        }
                    }
                    "SUNDAY" -> {
                        overtimeHoursSunday += durationHours
                    }
                    "HOLIDAY" -> {
                        overtimeHoursHoliday += durationHours
                    }
                }
            }
        }

        // 2. Calculations based on formula: ROUND((Định_mức_tối_đa / 26) * Số_ngày_công_thực_tế)
        val baseSalaryReceived = Math.round((config.luongCoBan / 26.0) * workingDays).toDouble()
        val responsibilityPayReceived = Math.round((config.pcTrachNhiem / 26.0) * workingDays).toDouble()
        val technicalAllowanceReceived = Math.round((config.pcKyThuat / 26.0) * workingDays).toDouble()
        val diligenceAllowanceReceived = Math.round((config.tienChuyenCanGoc / 26.0) * workingDays).toDouble()

        // Hourly rate for overtime
        val hourlyRate = (config.luongCoBan / 26.0) / 8.0
        val overtimePayWeekday = overtimeHoursWeekday * hourlyRate * config.heSoOtNgayThuong
        val overtimePaySunday = overtimeHoursSunday * hourlyRate * config.heSoOtChuNhat
        val overtimePayHoliday = overtimeHoursHoliday * hourlyRate * config.heSoOtNgayLe
        val totalOvertimePay = overtimePayWeekday + overtimePaySunday + overtimePayHoliday

        // Com ca based on working days
        val tienComNhan = config.tienComMoiNgay * workingDays

        // Total flat allowances
        val flatAllowances = config.pcChucVu + config.pcHieuSuat + config.pcSanPham +
                config.pcComCa + config.pcComOt + config.pcNhaO + config.pcDocHai +
                config.pcDtDoanhThu + config.pcXangXe + config.pcThamNien + config.pcKhac1 + config.pcKhac

        val totalAllowances = flatAllowances + responsibilityPayReceived + technicalAllowanceReceived +
                diligenceAllowanceReceived + tienComNhan

        // Deductions
        val bhxhDeduction = config.luongDongBaoHiem * (config.tiLeDongBaoHiem / 100.0)
        val unionFeeDeduction = config.doanPhiCongDoan
        val totalDeductions = bhxhDeduction + unionFeeDeduction

        // Gross/Net
        val grossIncome = baseSalaryReceived + totalOvertimePay + totalAllowances + config.thuong
        val netSalary = (grossIncome - totalDeductions).coerceAtLeast(0.0)

        return SalarySummary(
            workingDays = workingDays,
            overtimeHoursWeekday = overtimeHoursWeekday,
            overtimeHoursSunday = overtimeHoursSunday,
            overtimeHoursHoliday = overtimeHoursHoliday,
            baseSalaryReceived = baseSalaryReceived,
            responsibilityPayReceived = responsibilityPayReceived,
            technicalAllowanceReceived = technicalAllowanceReceived,
            diligenceAllowanceReceived = diligenceAllowanceReceived,
            pcChucVu = config.pcChucVu,
            pcHieuSuat = config.pcHieuSuat,
            pcSanPham = config.pcSanPham,
            pcComCa = config.pcComCa,
            pcComOt = config.pcComOt,
            pcNhaO = config.pcNhaO,
            pcDocHai = config.pcDocHai,
            pcDtDoanhThu = config.pcDtDoanhThu,
            pcXangXe = config.pcXangXe,
            pcThamNien = config.pcThamNien,
            pcKhac1 = config.pcKhac1,
            pcKhac = config.pcKhac,
            thuong = config.thuong,
            tienComNhan = tienComNhan,
            overtimePayWeekday = overtimePayWeekday,
            overtimePaySunday = overtimePaySunday,
            overtimePayHoliday = overtimePayHoliday,
            totalOvertimePay = totalOvertimePay,
            totalAllowances = totalAllowances,
            bhxhDeduction = bhxhDeduction,
            unionFeeDeduction = unionFeeDeduction,
            totalDeductions = totalDeductions,
            netSalary = netSalary
        )
    }
}
