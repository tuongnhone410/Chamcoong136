package com.example.domain.calculation

import com.example.data.ShiftConfig
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.viewmodel.SalarySummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * LegacyCalculationEngine encapsulates the exact existing company calculation logic
 * to guarantee 100% backward compatibility for existing users.
 */
class LegacyCalculationEngine : CalculationEngine {

    override val shifts: Map<String, ShiftConfig> = mapOf(
        "ca1" to ShiftConfig(
            shiftId = "ca1",
            shiftType = "DAY",
            startTime = "07:30",
            endTime = "19:30",
            checkInWindowStart = "07:00",
            checkInWindowEnd = "07:30",
            checkOutWindowStart = "19:30",
            checkOutWindowEnd = "20:00",
            breakHours = 0.0,
            standardHours = 8.0
        ),
        "ca2" to ShiftConfig(
            shiftId = "ca2",
            shiftType = "DAY_REST",
            startTime = "07:30",
            endTime = "20:00",
            checkInWindowStart = "07:00",
            checkInWindowEnd = "07:30",
            checkOutWindowStart = "20:00",
            checkOutWindowEnd = "20:30",
            breakHours = 1.5,
            standardHours = 8.0
        ),
        "ca_dem" to ShiftConfig(
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            startTime = "19:30",
            endTime = "07:30",
            checkInWindowStart = "19:00",
            checkInWindowEnd = "19:30",
            checkOutWindowStart = "07:30",
            checkOutWindowEnd = "08:00",
            breakHours = 0.0,
            standardHours = 8.0
        )
    )

    override fun getShiftForEntry(entry: TimeEntry): ShiftConfig {
        val shiftId = entry.shiftId
        if (shiftId != null && shifts.containsKey(shiftId)) {
            return shifts[shiftId]!!
        }
        if (entry.shiftType == "NIGHT" || entry.dayType == "NIGHT") {
            return shifts["ca_dem"]!!
        }
        val inTime = entry.checkInTime ?: return shifts["ca1"]!!
        val cal = Calendar.getInstance().apply { timeInMillis = inTime }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour >= 15 || hour < 6) {
            return shifts["ca_dem"]!!
        }
        val outTime = entry.checkOutTime
        if (outTime != null) {
            val calOut = Calendar.getInstance().apply { timeInMillis = outTime }
            val outHour = calOut.get(Calendar.HOUR_OF_DAY)
            val outMin = calOut.get(Calendar.MINUTE)
            val outTotalMin = outHour * 60 + outMin
            if (outTotalMin >= 19 * 60 + 45) { // 19:45
                return shifts["ca2"]!!
            }
        }
        return shifts["ca1"]!!
    }

    private fun getMillisForTime(baseTimeMs: Long, timeStr: String, dayOffset: Int = 0): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = baseTimeMs }
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val min = parts[1].toInt()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (dayOffset != 0) {
            cal.add(Calendar.DAY_OF_MONTH, dayOffset)
        }
        return cal.timeInMillis
    }

    override fun normalizeDateToDmy(dateStr: String): String {
        val s = dateStr.trim()
        if (s.contains("-")) {
            val parts = s.split("-")
            if (parts.size == 3) {
                return if (parts[0].length == 4) {
                    val dd = parts[2].padStart(2, '0')
                    val mm = parts[1].padStart(2, '0')
                    val yyyy = parts[0]
                    "$dd/$mm/$yyyy"
                } else if (parts[2].length == 4) {
                    val dd = parts[0].padStart(2, '0')
                    val mm = parts[1].padStart(2, '0')
                    val yyyy = parts[2]
                    "$dd/$mm/$yyyy"
                } else {
                    s.replace("-", "/")
                }
            }
        } else if (s.contains("/")) {
            val parts = s.split("/")
            if (parts.size == 3) {
                val dd = parts[0].padStart(2, '0')
                val mm = parts[1].padStart(2, '0')
                val yyyy = parts[2]
                return "$dd/$mm/$yyyy"
            }
        }
        return s.replace("-", "/")
    }

    override fun isLeaveType(dayType: String?): Boolean {
        if (dayType.isNullOrBlank()) return false
        val upper = dayType.uppercase(Locale.ROOT)
        return upper == "PAID_LEAVE" || upper == "UNPAID_LEAVE" || upper == "UNAUTHORIZED_LEAVE" || upper == "HOLIDAY_LEAVE" ||
               upper == "PAIDLEAVE" || upper == "UNPAIDLEAVE" || upper == "UNAUTHORIZEDLEAVE" || upper == "HOLIDAYLEAVE" ||
               upper == "PAID" || upper == "UNPAID" || upper == "UNAUTHORIZED" ||
               upper == "NP" || upper == "PHEP" || upper == "KP" || upper == "KHONGPHEP" || upper == "ABSENT" ||
               upper.contains("LEAVE") || upper.contains("PHÉP") || upper.contains("PHEP") || upper.contains("NGHỈ") || upper.contains("NGHI")
    }

    override fun calculateSingleEntry(entry: TimeEntry, config: UserConfig?): TimeEntry {
        if (isLeaveType(entry.dayType)) {
            val upper = entry.dayType.uppercase(Locale.ROOT)
            val workD = if (upper.contains("PAID") || upper == "NP" || upper.contains("PHEP") || upper.contains("PHÉP") || upper.contains("HOLIDAY") || upper.contains("LỄ") || upper.contains("LE")) 1.0 else 0.0
            return entry.copy(
                workDay = workD,
                otHours = 0.0,
                lateMinutes = 0,
                earlyLeaveMinutes = 0,
                rawCheckIn = null,
                rawCheckOut = null,
                normalizedCheckIn = null,
                normalizedCheckOut = null
            )
        }

        val workingEntry = entry
        val rawInRaw = workingEntry.checkInTime ?: return workingEntry.copy(workDay = 0.0, otHours = 0.0, lateMinutes = 0, earlyLeaveMinutes = 0)
        val rawIn = Math.round(rawInRaw / 60000.0) * 60000L
        val rawOutRaw = workingEntry.checkOutTime
        val rawOut = rawOutRaw?.let { Math.round(it / 60000.0) * 60000L }

        val shift = getShiftForEntry(workingEntry)
        val stdInMs = getMillisForTime(rawIn, shift.startTime, 0)

        val normInMs = if (rawIn <= stdInMs) {
            stdInMs
        } else {
            rawIn
        }

        val normOutMs = if (rawOut != null) {
            val dayOffset = if (shift.shiftType == "NIGHT") 1 else 0
            val stdOutMs = getMillisForTime(rawIn, shift.endTime, dayOffset)

            if (rawOut >= stdOutMs) {
                val diffMs = rawOut - stdOutMs
                if (diffMs < 30 * 60000L) {
                    stdOutMs
                } else {
                    rawOut
                }
            } else {
                rawOut
            }
        } else {
            null
        }

        val lateMin = if (normInMs > stdInMs) {
            ((normInMs - stdInMs) / 60000.0).toInt()
        } else {
            0
        }

        val earlyLeaveMin = if (normOutMs != null) {
            val dayOffset = if (shift.shiftType == "NIGHT") 1 else 0
            val stdOutMs = getMillisForTime(rawIn, shift.endTime, dayOffset)
            if (normOutMs < stdOutMs) {
                ((stdOutMs - normOutMs) / 60000.0).toInt()
            } else {
                0
            }
        } else {
            0
        }

        val resolvedBreakDeduction = workingEntry.customBreakDeduction ?: config?.tinhKhauTruNghi ?: (shift.breakHours > 0.0)
        val breakHrsToUse = if (resolvedBreakDeduction) {
            workingEntry.customBreakHours ?: config?.soGioNghiGiaiLao ?: shift.breakHours
        } else {
            0.0
        }

        val maxLateOrEarly = Math.max(lateMin, earlyLeaveMin)
        val workD = if (rawOut == null) {
            if (lateMin < 15) 1.0 else if (lateMin < 120) 0.5 else 0.0
        } else {
            val outMs = normOutMs ?: rawOut
            val workedHrs = (outMs - normInMs) / 3600000.0
            val actualWorkedHrs = (workedHrs - breakHrsToUse).coerceAtLeast(0.0)
            when {
                actualWorkedHrs >= 8.0 -> 1.0
                actualWorkedHrs >= 4.0 -> 0.5
                maxLateOrEarly < 15 -> 1.0
                maxLateOrEarly < 120 -> 0.5
                else -> 0.0
            }
        }

        val otHrs = if (normOutMs != null) {
            val workedHrs = (normOutMs - normInMs) / 3600000.0
            val actualWorkedHrs = (workedHrs - breakHrsToUse).coerceAtLeast(0.0)
            (actualWorkedHrs - shift.standardHours).coerceAtLeast(0.0)
        } else {
            0.0
        }

        return workingEntry.copy(
            shiftId = shift.shiftId,
            shiftType = shift.shiftType,
            rawCheckIn = rawIn,
            rawCheckOut = rawOut,
            normalizedCheckIn = normInMs,
            normalizedCheckOut = normOutMs,
            workDay = workD,
            otHours = otHrs,
            lateMinutes = lateMin,
            earlyLeaveMinutes = earlyLeaveMin,
            customBreakDeduction = resolvedBreakDeduction,
            customBreakHours = breakHrsToUse
        )
    }

    override fun calculateAllowanceValue(
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
        return when (calcType) {
            "MONTHLY_PRO_RATED" -> {
                val ratio = (totalWorkDays / 26.0).coerceAtMost(1.0)
                allowanceValue * ratio
            }
            "MONTHLY_FLAT" -> {
                allowanceValue
            }
            "PER_WORK_DAY" -> {
                if (fieldName == "pcComCa") {
                    comCaCount * allowanceValue
                } else {
                    totalWorkDays * allowanceValue
                }
            }
            "OT_MEAL_GE_2H", "OT_MEAL_GE_1H" -> {
                comOtCount * allowanceValue
            }
            "PER_NIGHT_SHIFT" -> {
                nightShiftsCount * allowanceValue
            }
            else -> {
                val ratio = (totalWorkDays / 26.0).coerceAtMost(1.0)
                allowanceValue * ratio
            }
        }
    }

    override fun calculateMonthlySalary(
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
        val luongBasic = config.luongCoBan
        val dailySalary = luongBasic / 26.0
        val hourlySalary = dailySalary / 8.0

        val processedEntries = entries.map { calculateSingleEntry(it, config) }

        val workedHolidayDates = processedEntries.filter { e ->
            holidayDatesInMonth.contains(e.date) && e.rawCheckIn != null
        }.map { it.date }.toSet()

        val unworkedHolidaysCount = (holidayDatesInMonth - workedHolidayDates).size

        var totalWorkDays = unworkedHolidaysCount.toDouble()
        var actualPresenceDaysCount = 0
        var totalStandardHours = unworkedHolidaysCount * 8.0
        var totalOtDayHours = 0.0
        var totalOtNightHours = 0.0
        var totalOtLeHours = 0.0
        var totalSundayHours = 0.0

        var otDayPay = 0.0
        var otLePay = 0.0
        var otNightPay = 0.0
        var sundayPay = 0.0
        var nightShiftsCount = 0

        val breakHours = if (config.tinhKhauTruNghi) config.soGioNghiGiaiLao else 0.0

        for (e in processedEntries) {
            if (isCurrentSelectedMonth && e.date > todayStr && e.rawCheckIn == null) {
                continue
            }

            val isHolidayDateVal = holidayDatesInMonth.contains(e.date)
            if (isHolidayDateVal && e.rawCheckIn == null) {
                continue
            }

            if (e.dayType == "PAID_LEAVE" || e.dayType == "HOLIDAY_LEAVE") {
                totalWorkDays += 1.0
                totalStandardHours += 8.0
                continue
            }
            if (e.dayType == "UNPAID_LEAVE" || e.dayType == "UNAUTHORIZED_LEAVE") {
                continue
            }

            if (e.rawCheckIn == null) continue

            if (e.shiftType == "NIGHT") {
                nightShiftsCount++
            }

            val isSundayVal = e.dayType == "SUNDAY" || isSunday(e.date)

            if (e.rawCheckOut == null && e.isWorking) {
                if (isSundayVal) {
                    actualPresenceDaysCount++
                    totalSundayHours += 8.0
                    sundayPay += 8.0 * hourlySalary * config.heSoOtChuNhat
                } else {
                    totalWorkDays += e.workDay
                    actualPresenceDaysCount++
                    totalStandardHours += 8.0
                }
                continue
            }

            if (e.rawCheckOut == null) continue

            val eBreakHours = e.customBreakHours ?: breakHours

            if (isSundayVal) {
                actualPresenceDaysCount++
                val workedHrs = (e.normalizedCheckOut!! - e.normalizedCheckIn!!) / 3600000.0
                val actualHours = (workedHrs - eBreakHours).coerceAtLeast(0.0)
                totalSundayHours += actualHours
                sundayPay += actualHours * hourlySalary * config.heSoOtChuNhat
            } else {
                totalWorkDays += e.workDay
                actualPresenceDaysCount++
                
                val workedHrs = (e.normalizedCheckOut!! - e.normalizedCheckIn!!) / 3600000.0
                val actualHours = (workedHrs - eBreakHours).coerceAtLeast(0.0)
                val finalStandardHours = actualHours.coerceAtMost(8.0)
                totalStandardHours += finalStandardHours

                val finalOtHours = e.otHours
                if (finalOtHours > 0.0) {
                    if (e.dayType == "HOLIDAY") {
                        totalOtLeHours += finalOtHours
                        otLePay += finalOtHours * (hourlySalary * config.heSoOtNgayLe)
                    } else if (e.shiftType == "NIGHT") {
                        totalOtNightHours += finalOtHours
                        otNightPay += finalOtHours * (hourlySalary * config.heSoOtDem)
                    } else {
                        totalOtDayHours += finalOtHours
                        otDayPay += finalOtHours * (hourlySalary * config.heSoOtNgayThuong)
                    }
                }
            }
        }

        var comCaCount = 0
        var comOtCount = 0

        for (e in processedEntries) {
            if (isCurrentSelectedMonth && e.date > todayStr && e.rawCheckIn == null) {
                continue
            }
            if (e.rawCheckIn == null) continue

            if (e.workDay >= 1.0) {
                comCaCount++
            }
            if (e.otHours >= 1.0) {
                comOtCount++
            }
        }

        val pcKyThuatPr = calculateAllowanceValue("pcKyThuat", config.pcKyThuat, config.getCalcTypeFor("pcKyThuat"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcTrachNhiemPr = calculateAllowanceValue("pcTrachNhiem", config.pcTrachNhiem, config.getCalcTypeFor("pcTrachNhiem"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcChucVuPr = calculateAllowanceValue("pcChucVu", config.pcChucVu, config.getCalcTypeFor("pcChucVu"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcHieuSuatPr = calculateAllowanceValue("pcHieuSuat", config.pcHieuSuat, config.getCalcTypeFor("pcHieuSuat"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcSanPhamPr = calculateAllowanceValue("pcSanPham", config.pcSanPham, config.getCalcTypeFor("pcSanPham"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcComCaPr = calculateAllowanceValue("pcComCa", config.pcComCa, config.getCalcTypeFor("pcComCa"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcComOtPr = calculateAllowanceValue("pcComOt", config.pcComOt, config.getCalcTypeFor("pcComOt"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcNhaOPr = calculateAllowanceValue("pcNhaO", config.pcNhaO, config.getCalcTypeFor("pcNhaO"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcDocHaiPr = calculateAllowanceValue("pcDocHai", config.pcDocHai, config.getCalcTypeFor("pcDocHai"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcDtDoanhThuPr = calculateAllowanceValue("pcDtDoanhThu", config.pcDtDoanhThu, config.getCalcTypeFor("pcDtDoanhThu"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcXangXePr = calculateAllowanceValue("pcXangXe", config.pcXangXe, config.getCalcTypeFor("pcXangXe"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcThamNienPr = calculateAllowanceValue("pcThamNien", config.pcThamNien, config.getCalcTypeFor("pcThamNien"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcKhac1Pr = calculateAllowanceValue("pcKhac1", config.pcKhac1, config.getCalcTypeFor("pcKhac1"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)
        val pcCaDemPr = calculateAllowanceValue("pcCaDem", config.pcCaDem, config.getCalcTypeFor("pcCaDem"), totalWorkDays, comCaCount, comOtCount, nightShiftsCount, scheduledDaysSoFar, totalScheduledDaysInMonth)

        val phuCapTong = pcKyThuatPr + pcTrachNhiemPr + pcChucVuPr + pcHieuSuatPr + 
                pcSanPhamPr + pcComCaPr + pcComOtPr + pcNhaOPr + 
                pcDocHaiPr + pcDtDoanhThuPr + pcXangXePr + pcThamNienPr + 
                pcKhac1Pr + pcCaDemPr

        val hasUnpaidOrAbsent = processedEntries.any { 
            (it.dayType == "UNPAID_LEAVE" || it.dayType == "UNAUTHORIZED_LEAVE") && (earliestDate == null || it.date >= earliestDate)
        } || (scheduledDaysSoFar > 0 && totalWorkDays < scheduledDaysSoFar)

        val chuyenCanValue = if (hasUnpaidOrAbsent) {
            0.0
        } else {
            calculateAllowanceValue(
                "tienChuyenCanGoc",
                config.tienChuyenCanGoc,
                config.getCalcTypeFor("tienChuyenCanGoc"),
                totalWorkDays,
                comCaCount,
                comOtCount,
                nightShiftsCount,
                scheduledDaysSoFar,
                totalScheduledDaysInMonth
            )
        }

        val tieuBaoHiem = Math.round(config.luongDongBaoHiem * (config.tiLeDongBaoHiem / 100.0)).toDouble()
        val doanPhi = config.doanPhiCongDoan
        val tienKhauTruNghi = 0.0

        val baseBasicSalary = Math.round(totalWorkDays * dailySalary).toDouble()

        val roundedOtDay = Math.round(otDayPay).toDouble()
        val roundedOtLePay = Math.round(otLePay).toDouble()
        val roundedOtNight = Math.round(otNightPay).toDouble()
        val roundedSundayPay = Math.round(sundayPay).toDouble()

        val grossAdditions = baseBasicSalary + roundedOtDay + roundedOtLePay + roundedOtNight + roundedSundayPay + phuCapTong + chuyenCanValue
        val totalDeductions = tieuBaoHiem + doanPhi + tienKhauTruNghi
        val luongThucNhan = Math.round(grossAdditions - totalDeductions).coerceAtLeast(0L).toDouble()

        return SalarySummary(
            workingDays = totalWorkDays.toInt(),
            standardHours = totalStandardHours,
            otDayHours = totalOtDayHours,
            otNightHours = totalOtNightHours,
            tienOtNgay = roundedOtDay,
            tienOtDem = roundedOtNight,
            tongTienCom = pcComCaPr + pcComOtPr,
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
            expectedWorkDays = totalScheduledDaysInMonth,
            standardWorkDays = 26,
            scheduledDaysSoFar = scheduledDaysSoFar,
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
            pcKhacVal = pcCaDemPr,
            pcCaDemVal = pcCaDemPr,
            caDemCount = nightShiftsCount,
            
            tienChuNhat = roundedSundayPay,
            otLeHours = totalOtLeHours,
            tienOtLe = roundedOtLePay,
            chuNhatHours = totalSundayHours
        )
    }

    override fun isHoliday(dateString: String): Boolean {
        return try {
            val parser = if (dateString.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            }
            val date = parser.parse(dateString) ?: return false
            val cal = Calendar.getInstance().apply { time = date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val md = String.format(Locale.US, "%02d/%02d", day, month)
            val holidays = setOf("01/01", "30/04", "01/05", "02/09")
            holidays.contains(md)
        } catch (e: Exception) {
            false
        }
    }

    override fun isSunday(dateString: String): Boolean {
        return try {
            val parser = if (dateString.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.US)
            }
            val date = parser.parse(dateString) ?: return false
            val cal = Calendar.getInstance()
            cal.time = date
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        } catch (e: Exception) {
            false
        }
    }

    override fun isNightShift(clockInTime: Long, clockOutTime: Long?): Boolean {
        val outTime = clockOutTime ?: System.currentTimeMillis()
        val calIn = Calendar.getInstance().apply { timeInMillis = clockInTime }
        val startHour = calIn.get(Calendar.HOUR_OF_DAY)
        if (startHour >= 18 || startHour <= 5) return true

        val calOut = Calendar.getInstance().apply { timeInMillis = outTime }
        val endHour = calOut.get(Calendar.HOUR_OF_DAY)
        if (endHour >= 22 || endHour <= 7) return true

        return false
    }

    override fun getDayTypeLabel(dateString: String): String {
        return when {
            isHoliday(dateString) -> "NGÀY LỄ"
            isSunday(dateString) -> "CHỦ NHẬT"
            else -> "NGÀY THƯỜNG"
        }
    }

    override fun calculatePIT(taxableIncome: Double): Double {
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
