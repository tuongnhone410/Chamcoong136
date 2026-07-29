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

    fun isRecordInMonth(dateStr: String, monthYmd: String): Boolean {
        if (monthYmd.isEmpty()) return true
        val parts = monthYmd.split("-")
        if (parts.size < 2) return dateStr.contains(monthYmd)
        val year = parts[0]
        val monthStr = parts[1]
        val monthIntStr = monthStr.toIntOrNull()?.toString() ?: monthStr
        
        val slashPattern1 = "/$monthStr/$year"
        val slashPattern2 = "/$monthIntStr/$year"
        val dashPattern1 = "-$monthStr-$year"
        val dashPattern2 = "-$monthIntStr-$year"
        val altDashPattern1 = "$year-$monthStr"
        val altDashPattern2 = "$year-$monthIntStr"
        
        return dateStr.startsWith(altDashPattern1) || 
               dateStr.startsWith(altDashPattern2) ||
               dateStr.contains(slashPattern1) || 
               dateStr.contains(slashPattern2) || 
               dateStr.contains(dashPattern1) || 
               dateStr.contains(dashPattern2) || 
               dateStr.endsWith(slashPattern1) || 
               dateStr.endsWith(slashPattern2)
    }

    fun calculateSalarySummary(
        entries: List<TimeEntry>, 
        config: UserConfig, 
        selectedMonth: String,
        firstEntryDate: String? = null
    ): SalarySummary {
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

        val maxDaysInMo = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        val holidayDatesInMonth = mutableSetOf<String>()
        for (day in 1..maxDaysInMo) {
            val dateStrYmd = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            val dateStrDmy = String.format(Locale.US, "%02d/%02d/%04d", day, targetMonth, targetYear)
            if (isHolidayDate(dateStrYmd) || isHolidayDate(dateStrDmy)) {
                if (!isCurrentSelectedMonth || dateStrYmd <= todayStr) {
                    holidayDatesInMonth.add(dateStrYmd)
                    holidayDatesInMonth.add(dateStrDmy)
                }
            }
        }

        val effectiveJoinDate: String? = if (config.ngayVaoLam.isNotBlank()) {
            config.ngayVaoLam.trim()
        } else if (firstEntryDate != null && firstEntryDate.startsWith(selectedMonth)) {
            firstEntryDate
        } else {
            null
        }

        val effectiveJoinDateYmd: String? = if (!effectiveJoinDate.isNullOrBlank()) {
            val s = effectiveJoinDate.trim()
            if (s.contains("/")) {
                val p = s.split("/")
                if (p.size == 3) {
                    val d = p[0].padStart(2, '0')
                    val m = p[1].padStart(2, '0')
                    val y = p[2]
                    "$y-$m-$d"
                } else s
            } else if (s.contains("-") && !s.startsWith("20")) {
                val p = s.split("-")
                if (p.size == 3) {
                    val d = p[0].padStart(2, '0')
                    val m = p[1].padStart(2, '0')
                    val y = p[2]
                    "$y-$m-$d"
                } else s
            } else s
        } else null

        var expectedWorkDaysSoFar = 0
        var totalWorkDaysInMonth = 0

        for (day in 1..maxDaysInMo) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            val cal = Calendar.getInstance()
            cal.set(targetYear, targetMonth - 1, day)
            val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            val isHoliday = isHolidayDate(dateStr)
            if (!isSunday && !isHoliday) {
                totalWorkDaysInMonth++
            }
        }

        if (isCurrentSelectedMonth) {
            for (day in 1 until todayDayOfMonth) {
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
                if (effectiveJoinDateYmd != null && dateStr < effectiveJoinDateYmd) {
                    continue
                }
                val cal = Calendar.getInstance()
                cal.set(currentYear, currentMonth - 1, day)
                val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                val isHoliday = isHolidayDate(dateStr)
                if (!isSunday && !isHoliday) {
                    expectedWorkDaysSoFar++
                }
            }
        } else {
            if (effectiveJoinDateYmd != null && effectiveJoinDateYmd.startsWith(selectedMonth)) {
                for (day in 1..maxDaysInMo) {
                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                    if (dateStr < effectiveJoinDateYmd) {
                        continue
                    }
                    val cal = Calendar.getInstance()
                    cal.set(targetYear, targetMonth - 1, day)
                    val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    val isHoliday = isHolidayDate(dateStr)
                    if (!isSunday && !isHoliday) {
                        expectedWorkDaysSoFar++
                    }
                }
            } else {
                expectedWorkDaysSoFar = totalWorkDaysInMonth
            }
        }

        val monthEntries = entries.filter { e ->
            val parts = selectedMonth.split("-")
            val isSameMonth = if (parts.size == 2) {
                e.date.endsWith("/${parts[1]}/${parts[0]}") || e.date.contains("/${parts[1]}/${parts[0]}") || e.date.startsWith(selectedMonth)
            } else {
                e.date.contains(selectedMonth)
            }
            isSameMonth
        }

        val vmSummary = com.example.data.SalaryCalculator.calculateMonthlySalary(
            entries = monthEntries,
            config = config,
            scheduledDaysSoFar = expectedWorkDaysSoFar,
            totalScheduledDaysInMonth = totalWorkDaysInMonth,
            earliestDate = effectiveJoinDateYmd,
            selectedMonth = selectedMonth,
            todayStr = todayStr,
            isCurrentSelectedMonth = isCurrentSelectedMonth,
            holidayDatesInMonth = holidayDatesInMonth
        )

        return SalarySummary(
            workingDays = vmSummary.workingDays,
            standardHours = vmSummary.standardHours,
            otDayHours = vmSummary.otDayHours,
            otNightHours = vmSummary.otNightHours,
            tienOtNgay = vmSummary.tienOtNgay,
            tienOtDem = vmSummary.tienOtDem,
            tongTienCom = vmSummary.tongTienCom,
            phuCap = vmSummary.phuCap,
            phuCapXangXe = vmSummary.phuCapXangXe,
            phuCapDienThoai = vmSummary.phuCapDienThoai,
            phuCapNhaO = vmSummary.phuCapNhaO,
            phuCapChuyenCan = vmSummary.phuCapChuyenCan,
            thuong = vmSummary.thuong,
            tienBh = vmSummary.tienBh,
            doanPhi = vmSummary.doanPhi,
            tienKhauTruNghi = vmSummary.tienKhauTruNghi,
            luongThucNhan = vmSummary.luongThucNhan,
            baseBasicSalary = vmSummary.baseBasicSalary,
            expectedWorkDays = vmSummary.expectedWorkDays,
            standardWorkDays = vmSummary.standardWorkDays,
            isCurrentMonth = vmSummary.isCurrentMonth,
            pcKyThuatVal = vmSummary.pcKyThuatVal,
            pcTrachNhiemVal = vmSummary.pcTrachNhiemVal,
            pcChucVuVal = vmSummary.pcChucVuVal,
            pcHieuSuatVal = vmSummary.pcHieuSuatVal,
            pcSanPhamVal = vmSummary.pcSanPhamVal,
            pcComCaVal = vmSummary.pcComCaVal,
            pcComOtVal = vmSummary.pcComOtVal,
            pcNhaOVal = vmSummary.pcNhaOVal,
            pcDocHaiVal = vmSummary.pcDocHaiVal,
            pcDtDoanhThuVal = vmSummary.pcDtDoanhThuVal,
            pcXangXeVal = vmSummary.pcXangXeVal,
            pcThamNienVal = vmSummary.pcThamNienVal,
            pcKhac1Val = vmSummary.pcKhac1Val,
            pcKhacVal = vmSummary.pcKhacVal,
            pcCaDemVal = vmSummary.pcCaDemVal,
            caDemCount = vmSummary.caDemCount,
            tienChuNhat = vmSummary.tienChuNhat,
            chuNhatHours = vmSummary.chuNhatHours,
            otLeHours = vmSummary.otLeHours,
            tienOtLe = vmSummary.tienOtLe
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
        val soNgayCongDuKienDouble = soNgayCongDuKien.toDouble()
        fun calcPrPNG(fieldName: String, valRaw: Double): Double {
            return com.example.data.SalaryCalculator.calculateAllowanceValue(
                fieldName = fieldName,
                allowanceValue = valRaw,
                calcType = config.getCalcTypeFor(fieldName),
                totalWorkDays = soNgayCongDuKienDouble,
                comCaCount = summary.workingDays,
                comOtCount = 0,
                nightShiftsCount = summary.caDemCount,
                scheduledDaysSoFar = summary.workingDays,
                totalScheduledDaysInMonth = 26
            )
        }

        val pcKyThuatShowPNG = if (selectedTab == 1) calcPrPNG("pcKyThuat", config.pcKyThuat) else summary.pcKyThuatVal
        val pcTrachNhiemShowPNG = if (selectedTab == 1) calcPrPNG("pcTrachNhiem", config.pcTrachNhiem) else summary.pcTrachNhiemVal
        val pcChucVuShowPNG = if (selectedTab == 1) calcPrPNG("pcChucVu", config.pcChucVu) else summary.pcChucVuVal
        val pcHieuSuatShowPNG = if (selectedTab == 1) calcPrPNG("pcHieuSuat", config.pcHieuSuat) else summary.pcHieuSuatVal
        val pcSanPhamShowPNG = if (selectedTab == 1) calcPrPNG("pcSanPham", config.pcSanPham) else summary.pcSanPhamVal

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

        val pcNhaOShowPNG = if (selectedTab == 1) calcPrPNG("pcNhaO", config.pcNhaO) else summary.pcNhaOVal
        val pcDocHaiShowPNG = if (selectedTab == 1) calcPrPNG("pcDocHai", config.pcDocHai) else summary.pcDocHaiVal
        val pcDtDoanhThuShowPNG = if (selectedTab == 1) calcPrPNG("pcDtDoanhThu", config.pcDtDoanhThu) else summary.pcDtDoanhThuVal
        val pcXangXeShowPNG = if (selectedTab == 1) calcPrPNG("pcXangXe", config.pcXangXe) else summary.pcXangXeVal
        val pcKhacShowPNG = if (selectedTab == 1) calcPrPNG("pcKhac", config.pcKhac) else summary.pcKhacVal
        val pcKhac1ShowPNG = if (selectedTab == 1) calcPrPNG("pcKhac1", config.pcKhac1) else summary.pcKhac1Val
        val pcThamNienShowPNG = if (selectedTab == 1) calcPrPNG("pcThamNien", config.pcThamNien) else summary.pcThamNienVal

        val pcChuyenCanShowPNG = if (selectedTab == 1) {
            if (hasLoggedUnpaidOrAbsent) 0.0 else calcPrPNG("tienChuyenCanGoc", config.tienChuyenCanGoc)
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
        val formattedMonthLabel = if (monthLabel.startsWith("Tháng", ignoreCase = true)) monthLabel else "Tháng $monthLabel"
        val statusText = if (selectedTab == 1) "Trạng thái: Dự kiến" else "Trạng thái: Đã phê duyệt"
        canvas.drawText("$formattedMonthLabel | $statusText", 60f, currentY, paintTextMonth)

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
        
        val luongDuKienBaseSalary = Math.round((config.luongCoBan / 26.0) * soNgayCongDuKienDouble).toDouble()
        val baseSalaryLabel = if (selectedTab == 1) "LCB thực nhận ($soNgayCongDuKien / ${summary.standardWorkDays})" 
                              else "LCB thực nhận (${summary.workingDays} / ${summary.standardWorkDays})"
        val baseSalaryValue = if (selectedTab == 1) luongDuKienBaseSalary else summary.baseBasicSalary
        drawRow(baseSalaryLabel, "+${fmt.format(baseSalaryValue)}đ", paintGreen)
        
        if (pcChuyenCanShowPNG > 0.0) drawRow("Phụ cấp chuyên cần", "+${fmt.format(pcChuyenCanShowPNG)}đ", paintGreen)
        if (pcTrachNhiemShowPNG > 0.0) drawRow("Phụ cấp trách nhiệm", "+${fmt.format(pcTrachNhiemShowPNG)}đ", paintGreen)
        if (pcKyThuatShowPNG > 0.0) drawRow("Phụ cấp kỹ thuật", "+${fmt.format(pcKyThuatShowPNG)}đ", paintGreen)
        if (pcHieuSuatShowPNG > 0.0) drawRow("Phụ cấp hiệu suất", "+${fmt.format(pcHieuSuatShowPNG)}đ", paintGreen)
        if (pcSanPhamShowPNG > 0.0) drawRow("Phụ cấp sản phẩm", "+${fmt.format(pcSanPhamShowPNG)}đ", paintGreen)
        if (pcChucVuShowPNG > 0.0) drawRow("Phụ cấp chức vụ", "+${fmt.format(pcChucVuShowPNG)}đ", paintGreen)
        if (pcDocHaiShowPNG > 0.0) drawRow("Phụ cấp độc hại", "+${fmt.format(pcDocHaiShowPNG)}đ", paintGreen)
        if (pcDtDoanhThuShowPNG > 0.0) drawRow("Phụ cấp doanh thu", "+${fmt.format(pcDtDoanhThuShowPNG)}đ", paintGreen)
        if (pcThamNienShowPNG > 0.0) drawRow("Phụ cấp thâm niên", "+${fmt.format(pcThamNienShowPNG)}đ", paintGreen)
        if (pcComCaShowPNG > 0.0) drawRow("Phụ cấp cơm ca", "+${fmt.format(pcComCaShowPNG)}đ", paintGreen)
        if (pcComOtShowPNG > 0.0) drawRow("Phụ cấp cơm OT", "+${fmt.format(pcComOtShowPNG)}đ", paintGreen)
        
        if (summary.tienOtNgay > 0.0) drawRow("Tăng ca 1.5 (${df.format(summary.otDayHours)}h)", "+${fmt.format(summary.tienOtNgay)}đ", paintGreen)
        if (summary.tienChuNhat > 0.0) drawRow("Tăng ca chủ nhật (${df.format(summary.chuNhatHours)}h)", "+${fmt.format(summary.tienChuNhat)}đ", paintGreen)
        if (summary.tienOtLe > 0.0) drawRow("Tăng ca ngày lễ (${df.format(summary.otLeHours)}h)", "+${fmt.format(summary.tienOtLe)}đ", paintGreen)
        if (summary.tienOtDem > 0.0) drawRow("OTĐ 1.5 (${df.format(summary.otNightHours)}h)", "+${fmt.format(summary.tienOtDem)}đ", paintGreen)
        
        if (summary.pcCaDemVal > 0.0) drawRow("Phụ cấp ca đêm (${summary.caDemCount} ca)", "+${fmt.format(summary.pcCaDemVal)}đ", paintGreen)
        if (pcXangXeShowPNG > 0.0) drawRow("Phụ cấp xăng xe", "+${fmt.format(pcXangXeShowPNG)}đ", paintGreen)
        if (pcNhaOShowPNG > 0.0) drawRow("Phụ cấp nhà ở", "+${fmt.format(pcNhaOShowPNG)}đ", paintGreen)
        if (pcKhac1ShowPNG > 0.0) drawRow("Phụ cấp khác 1", "+${fmt.format(pcKhac1ShowPNG)}đ", paintGreen)

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
    var rawOut = this.clockOutTime
    if (this.clockInTime > 0 && rawOut != null && rawOut <= this.clockInTime) {
        rawOut += 24 * 3600 * 1000L
    }
    return TimeEntry(
        id = this.id.toInt(),
        userId = this.uid,
        date = this.dateString,
        checkInTime = this.clockInTime,
        checkOutTime = rawOut,
        dayType = if (this.status.isBlank()) "NORMAL" else this.status,
        isWorking = this.clockOutTime == null && this.clockInTime > 0, // Simplified guess
        note = this.notes
    )
}
