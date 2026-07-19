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
        val parts = dateStr.split("-")
        if (parts.size >= 3) {
            val md = "${parts[1]}-${parts[2]}"
            return md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02"
        }
        return false
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

        val expectedWorkDaysCount = 26
        val standardWorkDaysInMonth = 26
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
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                if (isHolidayDate(dateStr)) {
                    if (!isCurrentSelectedMonth || dateStr <= todayStr) {
                        holidayDatesInMonth.add(dateStr)
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
            if (isCurrentSelectedMonth && e.date > todayStr) {
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

            val isSunday = (e.dayType == "SUNDAY" || isSundayDate(e.date))

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

            val finalCheckIn = SalaryCalculator.getRoundedTime(e.checkInTime, true)
            val finalCheckOut = SalaryCalculator.getRoundedTime(e.checkOutTime, false)

            val durationMs = (finalCheckOut - finalCheckIn).coerceAtLeast(0L)
            val rawHours = durationMs / 3600000.0
            
            val breakHours = if (config.tinhKhauTruNghi) config.soGioNghiGiaiLao else 0.0
            val eBreakHours = e.customBreakHours ?: breakHours
            val actualHours = (rawHours - eBreakHours).coerceAtLeast(0.0)

            val finalStandardHours = actualHours.coerceAtMost(8.0)
            val finalOtHours = (actualHours - 8.0).coerceAtLeast(0.0)

            if (isSunday) {
                actualPresenceDaysCount++
                totalSundayHours += actualHours
                val dayPay = actualHours * hourlySalary * config.heSoOtChuNhat
                sundayPay += dayPay
                if (finalOtHours >= 2.0) {
                    comOtDaysCount++
                }
            } else {
                workingDaysCount++
                actualPresenceDaysCount++
                totalStandardHours += finalStandardHours

                if (finalOtHours >= 2.0) {
                    comOtDaysCount++
                }

                if (finalOtHours > 0.0) {
                    if (e.dayType == "HOLIDAY") {
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

        val pcKyThuatPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcKyThuat", config.pcKyThuat, config.getCalcTypeFor("pcKyThuat"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcTrachNhiemPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcTrachNhiem", config.pcTrachNhiem, config.getCalcTypeFor("pcTrachNhiem"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcChucVuPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcChucVu", config.pcChucVu, config.getCalcTypeFor("pcChucVu"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcHieuSuatPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcHieuSuat", config.pcHieuSuat, config.getCalcTypeFor("pcHieuSuat"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcSanPhamPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcSanPham", config.pcSanPham, config.getCalcTypeFor("pcSanPham"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcComCaPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcComCa", config.pcComCa, config.getCalcTypeFor("pcComCa"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcComOtPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcComOt", config.pcComOt, config.getCalcTypeFor("pcComOt"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcNhaOPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcNhaO", config.pcNhaO, config.getCalcTypeFor("pcNhaO"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcDocHaiPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcDocHai", config.pcDocHai, config.getCalcTypeFor("pcDocHai"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcDtDoanhThuPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcDtDoanhThu", config.pcDtDoanhThu, config.getCalcTypeFor("pcDtDoanhThu"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcXangXePr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcXangXe", config.pcXangXe, config.getCalcTypeFor("pcXangXe"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcThamNienPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcThamNien", config.pcThamNien, config.getCalcTypeFor("pcThamNien"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcKhac1Pr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcKhac1", config.pcKhac1, config.getCalcTypeFor("pcKhac1"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)
        val pcKhacPr = com.example.data.SalaryCalculator.calculateAllowanceValue("pcKhac", config.pcKhac, config.getCalcTypeFor("pcKhac"), workingDaysCount.toDouble(), actualPresenceDaysCount, comOtDaysCount, nightShiftsCount)

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
                nightShiftsCount
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
        val width = 800
        var estimatedHeight = 550 // Header and margins
        estimatedHeight += 180 // Profile rows
        estimatedHeight += 45 // LCB
        if (selectedTab == 1 && remainingSundays > 0 && includeSundayInProjection) estimatedHeight += 45
        if (selectedTab == 1 && customOt15DaysCount > 0.0) estimatedHeight += 45
        if (config.pcKyThuat > 0.0) estimatedHeight += 45
        if (config.pcTrachNhiem > 0.0) estimatedHeight += 45
        if (config.pcChucVu > 0.0) estimatedHeight += 45
        if (config.pcHieuSuat > 0.0) estimatedHeight += 45
        if (config.pcSanPham > 0.0) estimatedHeight += 45
        if (pcComCaShowPNG > 0.0) estimatedHeight += 45
        if (pcComOtShowPNG > 0.0) estimatedHeight += 45
        if (config.pcNhaO > 0.0) estimatedHeight += 45
        if (config.pcDocHai > 0.0) estimatedHeight += 45
        if (config.pcDtDoanhThu > 0.0) estimatedHeight += 45
        if (config.pcXangXe > 0.0) estimatedHeight += 45
        if (config.pcKhac > 0.0) estimatedHeight += 45
        if (config.pcKhac1 > 0.0) estimatedHeight += 45
        if (config.pcThamNien > 0.0) estimatedHeight += 45
        if (pcChuyenCanShowPNG > 0.0) estimatedHeight += 45
        if (summary.caDemCount > 0) estimatedHeight += 45
        if (summary.tongTienCom > 0.0) estimatedHeight += 45
        if (summary.tienOtNgay > 0.0) estimatedHeight += 45
        if (summary.tienChuNhat > 0.0) estimatedHeight += 45
        estimatedHeight += 60 // Deductions Header
        if (summary.tienBh > 0.0) estimatedHeight += 45
        if (summary.doanPhi > 0.0) estimatedHeight += 45
        if (selectedTab == 0 && summary.tienKhauTruNghi > 0.0) estimatedHeight += 45
        estimatedHeight += 120 // Total
        estimatedHeight += 150 // Footer

        val height = estimatedHeight.coerceAtLeast(1400)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintCard = Paint().apply { color = android.graphics.Color.parseColor("#1C1C1C") }
        val paintDivider = Paint().apply { color = android.graphics.Color.parseColor("#2C2C2C"); strokeWidth = 2f }
        val paintTextTitle = Paint().apply { color = android.graphics.Color.parseColor("#2F80ED"); textSize = 34f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        val paintTextSubTitle = Paint().apply { color = android.graphics.Color.WHITE; textSize = 20f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        val paintTextMonth = Paint().apply { color = android.graphics.Color.parseColor("#E0E0E0"); textSize = 24f; textAlign = Paint.Align.CENTER }
        val paintLabel = Paint().apply { color = android.graphics.Color.parseColor("#828282"); textSize = 22f }
        val paintValue = Paint().apply { color = android.graphics.Color.WHITE; textSize = 22f; isFakeBoldText = true }
        val paintGreen = Paint().apply { color = android.graphics.Color.parseColor("#27AE60"); textSize = 22f; isFakeBoldText = true }
        val paintRed = Paint().apply { color = android.graphics.Color.parseColor("#EB5757"); textSize = 22f; isFakeBoldText = true }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintCard)

        var currentY = 100f
        val titleText = "TIMESNAP PRO"
        val textWidth = paintTextTitle.measureText(titleText)
        val cX = (width / 2f) - (textWidth / 2f) - 30f
        val cY = currentY - 10f
        canvas.drawCircle(cX, cY, 18f, Paint().apply { color = android.graphics.Color.parseColor("#1535A3FF") })
        canvas.drawCircle(cX, cY, 18f, Paint().apply { color = android.graphics.Color.parseColor("#35A3FF"); style = Paint.Style.STROKE; strokeWidth = 2.5f })
        canvas.drawText("$", cX, cY + 7f, Paint().apply { color = android.graphics.Color.parseColor("#35A3FF"); textSize = 20f; isFakeBoldText = true; textAlign = Paint.Align.CENTER })

        canvas.drawText(titleText, (width / 2f) + 15f, currentY, paintTextTitle)
        currentY += 45f
        val docTypeTitle = if (selectedTab == 1) "PHIẾU LƯƠNG DỰ KIẾN CUỐI THÁNG" else "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT"
        canvas.drawText(docTypeTitle, (width / 2).toFloat(), currentY, paintTextSubTitle)
        currentY += 40f
        canvas.drawText("Kỳ lương: $monthLabel", (width / 2).toFloat(), currentY, paintTextMonth)
        currentY += 45f
        if (selectedTab == 1) {
            canvas.drawText("🔮 ĐÃ BÙ TOÀN BỘ CÁC NGÀY CÒN LẠI", (width / 2).toFloat(), currentY, Paint().apply { color = android.graphics.Color.parseColor("#35A3FF"); textSize = 21f; isFakeBoldText = true; textAlign = Paint.Align.CENTER })
            currentY += 45f
        } else if (summary.isCurrentMonth) {
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            canvas.drawText("⚠️ TẠM TÍNH ĐẾN NGÀY $currentDay", (width / 2).toFloat(), currentY, Paint().apply { color = android.graphics.Color.parseColor("#F2994A"); textSize = 21f; isFakeBoldText = true; textAlign = Paint.Align.CENTER })
            currentY += 45f
        }

        canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
        currentY += 45f

        fun drawRow(label: String, value: String, isGreenVal: Boolean = false, isRedVal: Boolean = false) {
            canvas.drawText(label, 80f, currentY, paintLabel)
            val rectValue = paintValue.measureText(value)
            val p = when { isGreenVal -> paintGreen; isRedVal -> paintRed; else -> paintValue }
            canvas.drawText(value, (width - 80) - rectValue, currentY, p)
            currentY += 45f
        }

        val employeeName = if (!config.hoVaTen.isNullOrBlank()) config.hoVaTen else (userSession?.displayName ?: "N/A")
        val employeeCode = if (!config.maNhanVien.isNullOrBlank()) config.maNhanVien else (userSession?.uid?.take(10) ?: "N/A")

        drawRow("Nhân viên:", employeeName)
        drawRow("Mã nhân viên (UID):", employeeCode)
        drawRow("Mức lương cơ bản:", "${fmt.format(config.luongCoBan)}đ")
        if (selectedTab == 1) drawRow("Số ngày công dự kiến:", "$soNgayCongDuKien / ${summary.standardWorkDays} ngày")
        else drawRow("Số ngày chấm công:", "${summary.workingDays} / ${if (summary.isCurrentMonth) summary.expectedWorkDays else summary.standardWorkDays} ngày")

        currentY += 10f
        canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
        currentY += 45f
        canvas.drawText(if (selectedTab == 1) "KHOẢN CỘNG LƯƠNG DỰ KIẾN (+)" else "KHOẢN CỘNG LƯƠNG (+)", 80f, currentY, paintGreen)
        currentY += 45f

        if (selectedTab == 1) {
            drawRow("Lương Cơ Bản Thỏa Thuận", "+${fmt.format(config.luongCoBan)}đ", true)
            if (summary.standardWorkDays == 27) drawRow("Bù công dôi dư tháng 31 ngày (1 ngày LCB)", "+${fmt.format(dailySalary)}đ", true)
        } else {
            drawRow(if (summary.isCurrentMonth) "Lương Cơ Bản Tạm Tính" else "Thực Nhận", "+${fmt.format(summary.baseBasicSalary)}đ", true)
            if (summary.standardWorkDays == 27) drawRow("Bù công dôi dư tháng 31 ngày (1 ngày LCB)", "+${fmt.format(dailySalary)}đ", true)
        }
        
        if (pcChuyenCanShowPNG > 0.0) drawRow("Chuyên cần", "+${fmt.format(pcChuyenCanShowPNG)}đ", true)
        if (config.pcTrachNhiem > 0.0) drawRow("Trách nhiệm", "+${fmt.format(config.pcTrachNhiem)}đ", true)
        if (config.pcKyThuat > 0.0) drawRow("Kỹ thuật", "+${fmt.format(config.pcKyThuat)}đ", true)
        if (config.pcHieuSuat > 0.0) drawRow("Hiệu suất", "+${fmt.format(config.pcHieuSuat)}đ", true)
        if (config.pcSanPham > 0.0) drawRow("Sản phẩm", "+${fmt.format(config.pcSanPham)}đ", true)
        if (config.pcChucVu > 0.0) drawRow("Chức vụ", "+${fmt.format(config.pcChucVu)}đ", true)
        if (config.pcDocHai > 0.0) drawRow("Độc hại", "+${fmt.format(config.pcDocHai)}đ", true)
        if (config.pcDtDoanhThu > 0.0) drawRow("Doanh thu", "+${fmt.format(config.pcDtDoanhThu)}đ", true)
        if (config.pcThamNien > 0.0) drawRow("Thâm niên", "+${fmt.format(config.pcThamNien)}đ", true)
        if (pcComCaShowPNG > 0.0) drawRow("Cơm/ ca", "+${fmt.format(pcComCaShowPNG)}đ", true)
        if (pcComOtShowPNG > 0.0) drawRow("Cơm OT", "+${fmt.format(pcComOtShowPNG)}đ", true)
        if (summary.tienOtNgay > 0.0) drawRow("OT 1.5 (${df.format(summary.otDayHours)}h)", "+${fmt.format(summary.tienOtNgay)}đ", true)
        if (summary.tienOtDem > 0.0) drawRow("OT đêm (${df.format(summary.otNightHours)}h)", "+${fmt.format(summary.tienOtDem)}đ", true)
        if (selectedTab == 1 && customOt15DaysCount > 0.0) drawRow("OT 1.5 (${df.format(customOt15DaysCount)} ngày)", "+${fmt.format(customOt15Pay)}đ", true)
        if (summary.tienChuNhat > 0.0) drawRow("OT 2.0 (${df.format(summary.chuNhatHours)}h)", "+${fmt.format(summary.tienChuNhat)}đ", true)
        if (selectedTab == 1 && includeSundayInProjection && remainingSundays > 0) drawRow("OT 2.0 ($remainingSundays)", "+${fmt.format(remainingSundays * dailySalary * config.heSoOtChuNhat)}đ", true)
        if (summary.tienOtLe > 0.0) drawRow("OT 3.0 (${df.format(summary.otLeHours)}h)", "+${fmt.format(summary.tienOtLe)}đ", true)
        
        val finalPcCaDemCountPNG = if (selectedTab == 1 && selectedOt15Shift == "Đêm") summary.caDemCount + customOt15DaysCount.toInt() else summary.caDemCount
        val finalPcCaDemPNG = if (selectedTab == 1) (summary.pcCaDemVal + customNightAllowance) else summary.pcCaDemVal
        if (finalPcCaDemPNG > 0.0) drawRow("Phụ cấp đêm ($finalPcCaDemCountPNG)", "+${fmt.format(finalPcCaDemPNG)}đ", true)
        if (config.pcXangXe > 0.0) drawRow("Xăng xe", "+${fmt.format(config.pcXangXe)}đ", true)
        if (config.pcNhaO > 0.0) drawRow("Nhà ở", "+${fmt.format(config.pcNhaO)}đ", true)
        if (config.pcKhac1 > 0.0) drawRow("Khác 1", "+${fmt.format(config.pcKhac1)}đ", true)

        currentY += 10f
        canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
        currentY += 45f
        canvas.drawText("KHOẢN TRỪ LƯƠNG (-)", 80f, currentY, paintRed)
        currentY += 45f
        if (summary.tienBh > 0.0) drawRow("BHXH/BHYT Khấu trừ (10.5%)", "-${fmt.format(summary.tienBh)}đ", isRedVal = true)
        if (summary.doanPhi > 0.0) drawRow("Phí Công Đoàn Bắt Buộc", "-${fmt.format(summary.doanPhi)}đ", isRedVal = true)
        if (selectedTab == 0 && summary.tienKhauTruNghi > 0.0) {
            val missed = ((if (summary.isCurrentMonth) summary.expectedWorkDays else summary.standardWorkDays) - summary.workingDays).coerceAtLeast(0)
            drawRow("Khấu trừ vắng làm ($missed ngày)", "-${fmt.format(summary.tienKhauTruNghi)}đ", isRedVal = true)
        }

        currentY += 10f
        canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
        currentY += 55f
        val netLabelPNG = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN:" else "THỰC NHẬN:"
        val netValuePNG = if (selectedTab == 1) luongDuKienVal else summary.luongThucNhan
        canvas.drawText(netLabelPNG, 80f, currentY, Paint().apply { color = android.graphics.Color.WHITE; textSize = 30f; isFakeBoldText = true })
        val textVal = "${fmt.format(netValuePNG)}đ"
        canvas.drawText(textVal, (width - 80) - Paint().apply { color = android.graphics.Color.parseColor("#27AE60"); textSize = 40f; isFakeBoldText = true }.measureText(textVal), currentY, Paint().apply { color = android.graphics.Color.parseColor("#27AE60"); textSize = 40f; isFakeBoldText = true })

        currentY += 60f
        canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
        currentY += 50f
        canvas.drawText("* ĐÃ ĐƯỢC PHÊ DUYỆT BỞI HỆ THỐNG TIMESNAP PRO *", (width / 2f), currentY, Paint().apply { color = android.graphics.Color.parseColor("#828282"); textSize = 15f; textAlign = Paint.Align.CENTER })
        currentY += 35f
        canvas.drawText("SÁNG LẬP & PHÁT TRIỂN BỞI TRUONGVANKHOA", (width / 2f), currentY, Paint().apply { color = android.graphics.Color.parseColor("#35A3FF"); textSize = 17f; isFakeBoldText = true; textAlign = Paint.Align.CENTER })

        try {
            val filename = "TimeSnap_Pro_Batch_${config.userId}_${selectedMonth}_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/TimeSnapPro/BatchExports")
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
                val file = java.io.File(imagesDir, "TimeSnapPro/BatchExports")
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
