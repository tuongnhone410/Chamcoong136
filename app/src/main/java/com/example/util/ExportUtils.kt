package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import com.example.auth.UserSession
import com.example.data.AttendanceRecord
import com.example.data.SalaryCalculator
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class SalarySummary(
    val workingDays: Int,
    val standardHours: Double,
    val otDayHours: Double,
    val otNightHours: Double,
    val tienOtNgay: Double,
    val tienOtDem: Double,
    val tongTienCom: Double,
    val phuCap: Double,
    val phuCapXangXe: Double = 0.0,
    val phuCapDienThoai: Double = 0.0,
    val phuCapNhaO: Double = 0.0,
    val phuCapChuyenCan: Double = 0.0,
    val thuong: Double,
    val tienBh: Double,
    val doanPhi: Double,
    val tienKhauTruNghi: Double,
    val luongThucNhan: Double,
    val baseBasicSalary: Double = 0.0,
    val expectedWorkDays: Int = 26,
    val standardWorkDays: Int = 26,
    val isCurrentMonth: Boolean = false,
    
    val pcKyThuatVal: Double = 0.0,
    val pcTrachNhiemVal: Double = 0.0,
    val pcChucVuVal: Double = 0.0,
    val pcHieuSuatVal: Double = 0.0,
    val pcSanPhamVal: Double = 0.0,
    val pcComCaVal: Double = 0.0,
    val pcComOtVal: Double = 0.0,
    val pcNhaOVal: Double = 0.0,
    val pcDocHaiVal: Double = 0.0,
    val pcDtDoanhThuVal: Double = 0.0,
    val pcXangXeVal: Double = 0.0,
    val pcThamNienVal: Double = 0.0,
    val pcKhac1Val: Double = 0.0,
    val pcKhacVal: Double = 0.0,
    val pcCaDemVal: Double = 0.0,
    val caDemCount: Int = 0,
    val tienChuNhat: Double = 0.0,
    val chuNhatHours: Double = 0.0,
    val otLeHours: Double = 0.0,
    val tienOtLe: Double = 0.0
)

object ExportUtils {

    fun isHolidayDate(dateStr: String): Boolean {
        return try {
            val parser = if (dateStr.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            }
            val date = parser.parse(dateStr)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val m = cal.get(Calendar.MONTH) + 1
                val mdStr = String.format(Locale.US, "%02d-%02d", d, m)
                mdStr == "01-01" || mdStr == "30-04" || mdStr == "01-05" || mdStr == "02-09"
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isSundayDate(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return false
            val cal = Calendar.getInstance()
            cal.time = date
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        } catch (e: Exception) {
            false
        }
    }

    fun calculateSalarySummary(
        entries: List<TimeEntry>, 
        config: UserConfig, 
        selectedMonth: String,
        firstEntryDate: String? = null
    ): SalarySummary {
        val luongBasic = config.luongCoBan

        var targetYear = 2026
        var targetMonth = 5
        try {
            val parts = selectedMonth.split("-")
            targetYear = parts[0].toInt()
            targetMonth = parts[1].toInt()
        } catch (e: Exception) {}

        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

        val isCurrentSelectedMonth = (targetYear == currentYear && targetMonth == currentMonth)
        val todayStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, todayDayOfMonth)

        val calMo = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth - 1)
        }
        val maxDays = calMo.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        var totalScheduledDaysInMonth = 0
        var scheduledDaysSoFar = 0
        for (d in 1..maxDays) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, d)
            calMo.set(Calendar.DAY_OF_MONTH, d)
            val isSunday = calMo.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val isHoliday = isHolidayDate(dateStr)
            if (!isSunday && !isHoliday) {
                totalScheduledDaysInMonth++
                if (!isCurrentSelectedMonth || dateStr < todayStr) {
                    scheduledDaysSoFar++
                }
            }
        }

        val expectedWorkDaysCount = totalScheduledDaysInMonth
        val standardWorkDaysInMonth = totalScheduledDaysInMonth
        val dailySalary = luongBasic / 26.0
        val hourlySalary = dailySalary / 8.0

        // Find all public holidays in the selected month
        val holidayDatesInMonth = mutableSetOf<String>()
        try {
            val maxDaysInMo = Calendar.getInstance().apply {
                set(Calendar.YEAR, targetYear)
                set(Calendar.MONTH, targetMonth - 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (day in 1..maxDaysInMo) {
                val dateStrYmd = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                val dateStrDmy = String.format(Locale.US, "%02d/%02d/%04d", day, targetMonth, targetYear)
                if (isHolidayDate(dateStrDmy)) {
                    if (!isCurrentSelectedMonth || dateStrYmd <= todayStr) {
                        holidayDatesInMonth.add(dateStrDmy)
                        holidayDatesInMonth.add(dateStrYmd)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val workedHolidayDates = entries.filter { e ->
            holidayDatesInMonth.contains(e.date) && e.checkInTime != null
        }.map { it.date }.toSet()

        val unworkedHolidaysCount = (holidayDatesInMonth - workedHolidayDates).size

        var workingDaysCount = unworkedHolidaysCount
        var actualPresenceDaysCount = 0
        var totalStandardHours = unworkedHolidaysCount * 8.0
        var totalOtDayHours = 0.0
        var totalOtNightHours = 0.0

        var otDayPay = 0.0
        var totalOtLeHours = 0.0
        var otLePay = 0.0
        var otNightPay = 0.0
        var comOtDaysCount = 0
        var totalSundayHours = 0.0
        var sundayPay = 0.0
        var nightShiftsCount = 0

        for (e in entries) {
            // Ensure entry belongs to selected month (handles yyyy-MM-dd and dd/MM/yyyy)
            val parts = selectedMonth.split("-")
            val isSameMonth = if (parts.size == 2) {
                e.date.endsWith("/${parts[1]}/${parts[0]}") || e.date.contains("/${parts[1]}/${parts[0]}") || e.date.startsWith(selectedMonth)
            } else {
                e.date.contains(selectedMonth)
            }
            
            if (!isSameMonth) continue

            val entryTime = try {
                val parser = if (e.date.contains("/")) {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                } else {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                }
                parser.parse(e.date)?.time ?: 0L
            } catch (ex: Exception) { 0L }

            val todayTime = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(todayStr)?.time ?: 0L
            } catch (ex: Exception) { 0L }

            if (isCurrentSelectedMonth && entryTime > todayTime) {
                continue
            }

            val isHolidayDateVal = holidayDatesInMonth.contains(e.date)
            if (isHolidayDateVal && e.checkInTime == null) {
                continue
            }

            if (e.dayType == "PAID_LEAVE" || e.dayType == "HOLIDAY_LEAVE") {
                workingDaysCount++
                totalStandardHours += 8.0
                continue
            }
            if (e.dayType == "UNPAID_LEAVE") {
                continue
            }

            if (e.checkInTime == null) continue

            val inCal = Calendar.getInstance()
            inCal.timeInMillis = e.checkInTime
            val inHour = inCal.get(Calendar.HOUR_OF_DAY)
            val inMin = inCal.get(Calendar.MINUTE)
            val inTotalMin = inHour * 60 + inMin
            val isNightShift = (inTotalMin in (18 * 60)..(19 * 60 + 30)) || 
                               inHour >= 22 || inHour <= 6 || 
                               e.dayType == "NIGHT"

            if (isNightShift) {
                nightShiftsCount++
            }

            val isSunday = (e.dayType == "SUNDAY" || com.example.data.SalaryCalculator.isSunday(e.date))

            if (e.isWorking) {
                if (isSunday) {
                    actualPresenceDaysCount++
                    totalSundayHours += 8.0
                    sundayPay += 8.0 * hourlySalary * config.heSoOtChuNhat
                } else {
                    workingDaysCount++
                    actualPresenceDaysCount++
                    totalStandardHours += 8.0
                }
                continue
            }

            if (e.checkOutTime == null) continue

            val finalCheckIn = e.checkInTime
            val finalCheckOut = e.checkOutTime

            val durationMs = (finalCheckOut - finalCheckIn).coerceAtLeast(0L)
            val rawHours = durationMs / 3600000.0
            
            val breakHours = if (config.tinhKhauTruNghi) config.soGioNghiGiaiLao else 0.0
            val eBreakHours = e.customBreakHours ?: breakHours
            val actualHours = (rawHours - eBreakHours).coerceAtLeast(0.0)

            val finalStandardHours = actualHours.coerceAtMost(8.0)
            val finalOtHours = (actualHours - 8.0).coerceAtLeast(0.0)

            // Meal OT Count: >= 10h total (including 8h shift + 2h OT) OR >= 2h OT
            if (actualHours >= 10.0 || finalOtHours >= 2.0) {
                comOtDaysCount++
            }

            if (isSunday) {
                actualPresenceDaysCount++
                totalSundayHours += actualHours
                val dayPay = actualHours * hourlySalary * config.heSoOtChuNhat
                sundayPay += dayPay
            } else {
                workingDaysCount++
                actualPresenceDaysCount++
                totalStandardHours += finalStandardHours

                if (finalOtHours > 0.0) {
                    if (e.dayType == "HOLIDAY" || com.example.data.SalaryCalculator.isHoliday(e.date)) {
                        totalOtLeHours += finalOtHours
                        otLePay += finalOtHours * (hourlySalary * config.heSoOtNgayLe)
                    } else {
                        // OT Night Logic (3:30 - 7:30)
                        val outCal = Calendar.getInstance().apply { timeInMillis = finalCheckOut }
                        val outHour = outCal.get(Calendar.HOUR_OF_DAY) + outCal.get(Calendar.MINUTE) / 60.0
                        
                        var nightOtHours = 0.0
                        var normalOtHours = 0.0

                        if (outHour > 3.5 && outHour <= 7.5) {
                            nightOtHours = Math.min(finalOtHours, outHour - 3.5)
                            normalOtHours = finalOtHours - nightOtHours
                        } else if (outHour > 7.5) {
                            nightOtHours = Math.min(finalOtHours, 4.0)
                            normalOtHours = finalOtHours - nightOtHours
                        } else {
                            normalOtHours = finalOtHours
                            nightOtHours = 0.0
                        }

                        if (nightOtHours > 0.0) {
                            totalOtNightHours += nightOtHours
                            otNightPay += nightOtHours * (hourlySalary * config.heSoOtDem)
                        }
                        if (normalOtHours > 0.0) {
                            totalOtDayHours += normalOtHours
                            otDayPay += normalOtHours * (hourlySalary * config.heSoOtNgayThuong)
                        }
                    }
                }
            }
        }
        val allowanceDivisor = 26.0

        val pcKyThuatPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcKyThuat", config.pcKyThuat, config.getCalcTypeFor("pcKyThuat"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcTrachNhiemPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcTrachNhiem", config.pcTrachNhiem, config.getCalcTypeFor("pcTrachNhiem"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcChucVuPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcChucVu", config.pcChucVu, config.getCalcTypeFor("pcChucVu"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcHieuSuatPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcHieuSuat", config.pcHieuSuat, config.getCalcTypeFor("pcHieuSuat"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcSanPhamPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcSanPham", config.pcSanPham, config.getCalcTypeFor("pcSanPham"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcComCaPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcComCa", config.pcComCa, config.getCalcTypeFor("pcComCa"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcComOtPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcComOt", config.pcComOt, config.getCalcTypeFor("pcComOt"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcNhaOPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcNhaO", config.pcNhaO, config.getCalcTypeFor("pcNhaO"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcDocHaiPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcDocHai", config.pcDocHai, config.getCalcTypeFor("pcDocHai"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcDtDoanhThuPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcDtDoanhThu", config.pcDtDoanhThu, config.getCalcTypeFor("pcDtDoanhThu"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcXangXePr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcXangXe", config.pcXangXe, config.getCalcTypeFor("pcXangXe"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcThamNienPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcThamNien", config.pcThamNien, config.getCalcTypeFor("pcThamNien"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcKhac1Pr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcKhac1", config.pcKhac1, config.getCalcTypeFor("pcKhac1"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcKhacPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcKhac", config.pcKhac, config.getCalcTypeFor("pcKhac"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)

        val pcCaDemPr = pcKhacPr

        val phuCapTong = pcKyThuatPr + pcTrachNhiemPr + pcChucVuPr + pcHieuSuatPr + 
                pcSanPhamPr + pcComCaPr + pcComOtPr + pcNhaOPr + 
                pcDocHaiPr + pcDtDoanhThuPr + pcXangXePr + pcThamNienPr + 
                pcKhac1Pr + pcKhacPr + pcCaDemPr

        var missedDays = 0
        val effectiveJoinDate: String? = if (config.ngayVaoLam.isNotBlank()) {
            config.ngayVaoLam.trim()
        } else if (firstEntryDate != null && firstEntryDate.startsWith(selectedMonth)) {
            firstEntryDate
        } else {
            null
        }

        if (isCurrentSelectedMonth) {
            try {
                for (day in 1 until todayDayOfMonth) {
                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
                    if (effectiveJoinDate != null && dateStr < effectiveJoinDate) {
                        continue
                    }
                    val cal = Calendar.getInstance()
                    cal.set(currentYear, currentMonth - 1, day)
                    val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    val isHoliday = isHolidayDate(dateStr)
                    if (!isSunday && !isHoliday) {
                        val entryForDay = entries.find { it.date == dateStr }
                        val workedOrPaid = entryForDay != null && (
                            entryForDay.checkInTime != null || 
                            entryForDay.dayType == "PAID_LEAVE" || 
                            entryForDay.dayType == "HOLIDAY_LEAVE" || 
                            entryForDay.isWorking
                        )
                        if (!workedOrPaid) {
                            missedDays++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (effectiveJoinDate != null && effectiveJoinDate.startsWith(selectedMonth)) {
                try {
                    val maxDaysInMo = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, targetMonth - 1)
                    }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    var expectedDaysFromJoin = 0
                    for (day in 1..maxDaysInMo) {
                        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                        if (dateStr < effectiveJoinDate) {
                            continue
                        }
                        val cal = Calendar.getInstance()
                        cal.set(targetYear, targetMonth - 1, day)
                        val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                        val isHoliday = isHolidayDate(dateStr)
                        if (!isSunday && !isHoliday) {
                            expectedDaysFromJoin++
                        }
                    }
                    missedDays = (expectedDaysFromJoin - workingDaysCount).coerceAtLeast(0)
                } catch (e: Exception) {
                    missedDays = (expectedWorkDaysCount - workingDaysCount).coerceAtLeast(0)
                }
            } else {
                missedDays = (expectedWorkDaysCount - workingDaysCount).coerceAtLeast(0)
            }
        }

        val hasUnpaidOrAbsent = missedDays > 0 || entries.any { 
            it.dayType == "UNPAID_LEAVE" && (effectiveJoinDate == null || it.date >= effectiveJoinDate)
        }
        val chuyenCanValue = if (hasUnpaidOrAbsent) {
            0.0
        } else {
            com.example.data.SalaryCalculator.calculateAllowanceValue(
                "tienChuyenCanGoc",
                config.tienChuyenCanGoc,
                config.getCalcTypeFor("tienChuyenCanGoc"),
                workingDaysCount.toDouble(),
                actualPresenceDaysCount,
                comOtDaysCount,
                nightShiftsCount,
                scheduledDaysSoFar,
                totalScheduledDaysInMonth
            )
        }

        val tongCom = pcComCaPr + pcComOtPr

        val tieuBaoHiem = Math.round(config.luongDongBaoHiem * (config.tiLeDongBaoHiem / 100.0)).toDouble()
        val doanPhi = config.doanPhiCongDoan

        val baseBasicSalary = Math.round((luongBasic / 26.0) * workingDaysCount).toDouble()
        val tienKhauTruNghi = 0.0

        val roundedOtDay = Math.round(otDayPay).toDouble()
        val roundedOtLePay = Math.round(otLePay).toDouble()
        val roundedOtNight = Math.round(otNightPay).toDouble()
        val roundedSundayPay = Math.round(sundayPay).toDouble()

        val grossAdditions = baseBasicSalary + roundedOtDay + roundedOtLePay + roundedOtNight + roundedSundayPay + phuCapTong + chuyenCanValue
        val totalDeductions = tieuBaoHiem + doanPhi + tienKhauTruNghi
        val luongThucNhan = Math.round(grossAdditions - totalDeductions).coerceAtLeast(0L).toDouble()

        return SalarySummary(
            workingDays = workingDaysCount,
            standardHours = totalStandardHours,
            otDayHours = totalOtDayHours,
            otNightHours = totalOtNightHours,
            tienOtNgay = roundedOtDay,
            tienOtDem = roundedOtNight,
            tongTienCom = tongCom,
            phuCap = phuCapTong,
            phuCapXangXe = pcXangXePr,
            phuCapDienThoai = pcDtDoanhThuPr,
            phuCapNhaO = pcNhaOPr,
            phuCapChuyenCan = chuyenCanValue,
            thuong = 0.0,
            tienBh = tieuBaoHiem,
            doanPhi = doanPhi,
            tienKhauTruNghi = tienKhauTruNghi,
            luongThucNhan = luongThucNhan,
            baseBasicSalary = baseBasicSalary,
            expectedWorkDays = expectedWorkDaysCount,
            standardWorkDays = standardWorkDaysInMonth,
            isCurrentMonth = isCurrentSelectedMonth,
            
            pcKyThuatVal = pcKyThuatPr,
            pcTrachNhiemVal = pcTrachNhiemPr,
            pcChucVuVal = pcChucVuPr,
            pcHieuSuatVal = pcHieuSuatPr,
            pcSanPhamVal = pcSanPhamPr,
            pcComCaVal = pcComCaPr,
            pcComOtVal = pcComOtPr,
            pcNhaOVal = pcNhaOPr,
            pcDocHaiVal = pcDocHaiPr,
            pcDtDoanhThuVal = pcDtDoanhThuPr,
            pcXangXeVal = pcXangXePr,
            pcThamNienVal = pcThamNienPr,
            pcKhac1Val = pcKhac1Pr,
            pcKhacVal = pcKhacPr,
            pcCaDemVal = pcCaDemPr,
            caDemCount = nightShiftsCount,
            
            tienChuNhat = roundedSundayPay,
            otLeHours = totalOtLeHours,
            tienOtLe = roundedOtLePay,
            chuNhatHours = totalSundayHours
        )
    }

    fun savePayslipAsPngImage(
        context: Context,
        summary: SalarySummary,
        config: UserConfig,
        userSession: UserSession?,
        monthLabel: String,
        selectedMonth: String,
        selectedTab: Int = 0,
        includeSundayInProjection: Boolean = false,
        remainingWeekdays: Int = 0,
        remainingSundays: Int = 0,
        dailySalary: Double = 0.0,
        luongDuKienVal: Double = 0.0,
        soNgayCongDuKien: Int = 0,
        customOt15DaysCount: Double = 0.0,
        customOt15Pay: Double = 0.0,
        selectedOt15Shift: String = "Đêm",
        customNightAllowance: Double = 0.0,
        hasLoggedUnpaidOrAbsent: Boolean = false
    ): Boolean {
        val df = DecimalFormat("#.#")
        val fmt = DecimalFormat("#,###")

        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val isCurrentSelectedMonth = selectedMonth.startsWith(String.format(Locale.US, "%04d-%02d", currentYear, currentMonth))

        // UI Pre-calculations
        val pcKyThuatShowPNG = if (selectedTab == 1) config.pcKyThuat else summary.pcKyThuatVal
        val pcTrachNhiemShowPNG = if (selectedTab == 1) config.pcTrachNhiem else summary.pcTrachNhiemVal
        val pcChucVuShowPNG = if (selectedTab == 1) config.pcChucVu else summary.pcChucVuVal
        val pcHieuSuatShowPNG = if (selectedTab == 1) config.pcHieuSuat else summary.pcHieuSuatVal
        val pcSanPhamShowPNG = if (selectedTab == 1) config.pcSanPham else summary.pcSanPhamVal

        val pcComCaShowPNG = if (selectedTab == 1) {
            if (isCurrentSelectedMonth) {
                summary.pcComCaVal + (remainingWeekdays * config.pcComCa) + (if (includeSundayInProjection) remainingSundays * config.pcComCa else 0.0)
            } else {
                summary.pcComCaVal
            }
        } else {
            summary.pcComCaVal
        }

        val pcComOtShowPNG = if (selectedTab == 1) {
            summary.pcComOtVal + (customOt15DaysCount * config.pcComOt)
        } else {
            summary.pcComOtVal
        }

        val pcNhaOShowPNG = if (selectedTab == 1) config.pcNhaO else summary.pcNhaOVal
        val pcDocHaiShowPNG = if (selectedTab == 1) config.pcDocHai else summary.pcDocHaiVal
        val pcDtDoanhThuShowPNG = if (selectedTab == 1) config.pcDtDoanhThu else summary.pcDtDoanhThuVal
        val pcXangXeShowPNG = if (selectedTab == 1) config.pcXangXe else summary.pcXangXeVal
        val pcKhacShowPNG = if (selectedTab == 1) config.pcKhac else summary.pcKhacVal
        val pcKhac1ShowPNG = if (selectedTab == 1) config.pcKhac1 else summary.pcKhac1Val
        val pcThamNienShowPNG = if (selectedTab == 1) config.pcThamNien else summary.pcThamNienVal

        val pcChuyenCanShowPNG = if (selectedTab == 1) {
            if (hasLoggedUnpaidOrAbsent) 0.0 else config.tienChuyenCanGoc
        } else {
            summary.phuCapChuyenCan
        }

        // 1. Create offline Bitmap with Dynamic Height
        val width = 1000
        var estimatedHeight = 1850
        
        val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Paints
        val paintBg = Paint().apply { color = android.graphics.Color.parseColor("#0F111A") }
        val paintHeader = Paint().apply { color = android.graphics.Color.parseColor("#1A1D2E") }
        val paintDivider = Paint().apply { color = android.graphics.Color.parseColor("#2C3149"); strokeWidth = 1.5f }
        val paintTextTitle = Paint().apply { color = android.graphics.Color.parseColor("#4C84FF"); textSize = 42f; isFakeBoldText = true; textAlign = Paint.Align.LEFT }
        val paintTextSubTitle = Paint().apply { color = android.graphics.Color.WHITE; textSize = 28f; isFakeBoldText = true }
        val paintTextMonth = Paint().apply { color = android.graphics.Color.parseColor("#8F9BB3"); textSize = 22f }
        val paintLabel = Paint().apply { color = android.graphics.Color.parseColor("#8F9BB3"); textSize = 24f }
        val paintValue = Paint().apply { color = android.graphics.Color.WHITE; textSize = 24f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val paintGreen = Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 24f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val paintRed = Paint().apply { color = android.graphics.Color.parseColor("#FF5252"); textSize = 24f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
        val paintBrand = Paint().apply { color = android.graphics.Color.parseColor("#4C84FF"); textSize = 18f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }

        // Background
        canvas.drawRect(0f, 0f, width.toFloat(), estimatedHeight.toFloat(), paintBg)
        
        // Header Card
        canvas.drawRect(0f, 0f, width.toFloat(), 220f, paintHeader)
        
        var currentY = 80f
        canvas.drawText("TIMESNAP PRO", 60f, currentY, paintTextTitle)
        
        currentY += 50f
        val docType = if (selectedTab == 1) "BẢNG LƯƠNG DỰ KIẾN (TỰ ĐỘNG)" else "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT"
        canvas.drawText(docType, 60f, currentY, paintTextSubTitle)
        
        currentY += 40f
        canvas.drawText("Tháng $monthLabel | Trạng thái: Đã phê duyệt", 60f, currentY, paintTextMonth)

        // Main content starts
        currentY = 280f
        
        fun drawSectionHeader(title: String) {
            currentY += 20f
            canvas.drawText(title, 60f, currentY, Paint().apply { color = android.graphics.Color.parseColor("#4C84FF"); textSize = 20f; isFakeBoldText = true })
            currentY += 25f
            canvas.drawLine(60f, currentY, (width - 60).toFloat(), currentY, paintDivider)
            currentY += 50f
        }

        fun drawRow(label: String, value: String, color: Paint = paintValue) {
            canvas.drawText(label, 60f, currentY, paintLabel)
            canvas.drawText(value, (width - 60).toFloat(), currentY, color)
            currentY += 55f
        }

        // Section 1: Thông tin nhân sự
        drawSectionHeader("THÔNG TIN NHÂN SỰ")
        val employeeName = config.hoVaTen.ifBlank { userSession?.displayName ?: "N/A" }
        val employeeCode = config.maNhanVien.ifBlank { userSession?.uid?.take(8) ?: "N/A" }
        drawRow("Họ và tên:", employeeName)
        drawRow("Mã nhân viên:", employeeCode)
        if (config.boPhan.isNotBlank()) {
            drawRow("Bộ phận:", config.boPhan)
        }
        if (config.emailDangKy.isNotBlank()) {
            drawRow("Email:", config.emailDangKy)
        }
        drawRow("Mức lương cơ bản:", "${fmt.format(config.luongCoBan)}đ")
        
        val attendanceInfo = if (selectedTab == 1) "$soNgayCongDuKien / ${summary.standardWorkDays} ngày" 
                             else "${summary.workingDays} / ${if (summary.isCurrentMonth) summary.expectedWorkDays else summary.standardWorkDays} ngày"
        drawRow("Công làm việc:", attendanceInfo)

        // Section 2: Thu nhập chi tiết
        currentY += 20f
        drawSectionHeader("THU NHẬP CHI TIẾT (+)")
        
        val baseSalaryLabel = if (selectedTab == 1) "Lương theo công dự kiến" else "Lương theo công thực tế"
        val baseSalaryValue = if (selectedTab == 1) luongDuKienVal else summary.baseBasicSalary
        drawRow(baseSalaryLabel, "+${fmt.format(baseSalaryValue)}đ", paintGreen)
        
        if (pcChuyenCanShowPNG > 0.0) drawRow("Phụ cấp chuyên cần", "+${fmt.format(pcChuyenCanShowPNG)}đ", paintGreen)
        if (config.pcTrachNhiem > 0.0) drawRow("Phụ cấp trách nhiệm", "+${fmt.format(config.pcTrachNhiem)}đ", paintGreen)
        if (config.pcKyThuat > 0.0) drawRow("Phụ cấp kỹ thuật", "+${fmt.format(config.pcKyThuat)}đ", paintGreen)
        if (pcComCaShowPNG > 0.0) drawRow("Phụ cấp cơm ca", "+${fmt.format(pcComCaShowPNG)}đ", paintGreen)
        
        if (summary.tienOtNgay > 0.0) drawRow("Tăng ca 1.5 (${df.format(summary.otDayHours)}h)", "+${fmt.format(summary.tienOtNgay)}đ", paintGreen)
        if (summary.tienChuNhat > 0.0) drawRow("Tăng ca chủ nhật (${df.format(summary.chuNhatHours)}h)", "+${fmt.format(summary.tienChuNhat)}đ", paintGreen)
        if (summary.tienOtLe > 0.0) drawRow("Tăng ca ngày lễ (${df.format(summary.otLeHours)}h)", "+${fmt.format(summary.tienOtLe)}đ", paintGreen)
        if (summary.tienOtDem > 0.0) drawRow("OTĐ 1.5 (${df.format(summary.otNightHours)}h)", "+${fmt.format(summary.tienOtDem)}đ", paintGreen)
        
        if (summary.pcCaDemVal > 0.0) drawRow("Phụ cấp ca đêm (${summary.caDemCount} ca)", "+${fmt.format(summary.pcCaDemVal)}đ", paintGreen)
        if (config.pcXangXe > 0.0) drawRow("Phụ cấp xăng xe", "+${fmt.format(config.pcXangXe)}đ", paintGreen)
        if (config.pcNhaO > 0.0) drawRow("Phụ cấp nhà ở", "+${fmt.format(config.pcNhaO)}đ", paintGreen)

        // Section 3: Khấu trừ & Nghĩa vụ
        currentY += 20f
        drawSectionHeader("KHẤU TRỪ & NGHĨA VỤ (-)")
        if (summary.tienBh > 0.0) drawRow("Bảo hiểm xã hội (10.5%)", "-${fmt.format(summary.tienBh)}đ", paintRed)
        if (summary.doanPhi > 0.0) drawRow("Kinh phí công đoàn", "-${fmt.format(summary.doanPhi)}đ", paintRed)
        
        // Total Footer
        currentY += 40f
        canvas.drawRect(60f, currentY, (width - 60).toFloat(), currentY + 120f, Paint().apply { color = android.graphics.Color.parseColor("#1A1D2E") })
        
        currentY += 75f
        val totalLabel = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN" else "TỔNG LƯƠNG THỰC NHẬN"
        val totalValue = if (selectedTab == 1) luongDuKienVal else summary.luongThucNhan
        canvas.drawText(totalLabel, 90f, currentY, paintTextSubTitle)
        
        val netText = "${fmt.format(totalValue)} VNĐ"
        canvas.drawText(netText, (width - 90).toFloat(), currentY, Paint().apply { color = android.graphics.Color.parseColor("#00E676"); textSize = 38f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT })

        // Footer Brand
        currentY = estimatedHeight - 100f
        canvas.drawText("XUẤT TỪ HỆ THỐNG QUẢN LÝ TIMESNAP PRO", (width / 2).toFloat(), currentY, paintBrand)
        currentY += 30f
        canvas.drawText("DEVELOPED BY TRUONGVANKHOA", (width / 2).toFloat(), currentY, Paint().apply { color = android.graphics.Color.parseColor("#8F9BB3"); textSize = 14f; textAlign = Paint.Align.CENTER })

        try {
            val filename = "TimeSnap_Payslip_${employeeCode}_${selectedMonth.replace("-","_")}_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/TimeSnapPro")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val imageUri = context.contentResolver.insert(collection, contentValues)
                if (imageUri != null) {
                    fos = context.contentResolver.openOutputStream(imageUri)
                    if (fos != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush(); fos.close()
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(imageUri, contentValues, null, null)
                        return true
                    }
                }
            } else {
                val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).toString()
                val file = java.io.File(imagesDir, "TimeSnapPro")
                if (!file.exists()) file.mkdirs()
                val outFile = java.io.File(file, filename)
                fos = java.io.FileOutputStream(outFile)
            }
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush(); fos.close()
                return true
            }
        } catch (e: Exception) { e.printStackTrace() }
        return false
    }
}

fun AttendanceRecord.toTimeEntry(): TimeEntry {
    return TimeEntry(
        id = this.id.toInt(),
        userId = this.uid,
        date = this.dateString,
        checkInTime = this.clockInTime,
        checkOutTime = this.clockOutTime,
        dayType = if (this.status.isBlank()) "NORMAL" else this.status,
        isWorking = this.clockOutTime == null && this.clockInTime > 0, // Simplified guess
        note = this.notes
    )
}
