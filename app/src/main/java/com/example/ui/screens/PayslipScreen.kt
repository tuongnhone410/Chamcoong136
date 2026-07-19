
package com.example.ui.screens
import androidx.compose.foundation.interaction.collectIsFocusedAsState

import android.content.ContentValues
import android.content.Context
import com.example.auth.UserSession
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.LightGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.White
import com.example.viewmodel.SalarySummary
import com.example.viewmodel.TimeSnapViewModel
import com.example.data.SalaryCalculator
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipScreen(
    viewModel: TimeSnapViewModel
) {
    val context = LocalContext.current
    val userSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.currentSelectedMonth.collectAsStateWithLifecycle()
    val summary by viewModel.salarySummaryState.collectAsStateWithLifecycle()
    val config by viewModel.userConfig.collectAsStateWithLifecycle()
    val entries by viewModel.monthTimeEntries.collectAsStateWithLifecycle(emptyList())

    var customOt15DaysCount by remember { mutableStateOf(0.0) }
    var selectedOt15Shift by remember { mutableStateOf("Ngày") }
    LaunchedEffect(selectedMonth) {
        customOt15DaysCount = 0.0
        selectedOt15Shift = "Ngày"
    }

    val fmt = DecimalFormat("#,###")
    val df = DecimalFormat("#.#")

    val monthLabel = remember(selectedMonth) {
        try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val d = parser.parse(selectedMonth) ?: Date()
            val formatter = SimpleDateFormat("MMMM / yyyy", Locale("vi", "VN"))
            formatter.format(d).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            selectedMonth
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phiếu Lương Điện Tử", fontWeight = FontWeight.Bold, color = White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Month Switcher Controller
            val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
            val currentMonthDate = remember(selectedMonth) { sdfMonth.parse(selectedMonth) ?: Date() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Month
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.time = currentMonthDate
                    cal.add(Calendar.MONTH, -1)
                    viewModel.selectMonth(sdfMonth.format(cal.time))
                }) {
                    Icon(Icons.Default.ArrowBackIosNew, "Tháng trước", tint = NeonBlue)
                }

                // Month Label
                Text(
                    text = monthLabel,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Next Month
                IconButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.time = currentMonthDate
                    cal.add(Calendar.MONTH, 1)
                    viewModel.selectMonth(sdfMonth.format(cal.time))
                }) {
                    Icon(Icons.Default.ArrowForwardIos, "Tháng sau", tint = NeonBlue)
                }
            }
            
            if (summary == null || config == null) {
                // Empty state setup
                Spacer(modifier = Modifier.height(60.dp))
                Icon(Icons.Default.ReceiptLong, "Receipt Empty", modifier = Modifier.size(72.dp), tint = MediumGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chưa có thông tin phiếu lương", color = LightGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Vui lòng check-in hoặc cấu hình mức lương trước.", color = MediumGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            } else {
                val s = summary!!
                val c = config!!

                // Part 1: Break duration interpretation (Giờ nghỉ giải ca)
                val hourBreakConverted = c.soGioNghiGiaiLao

                // Part 2: Dates calculations
                val todayCal = Calendar.getInstance()
                val currentYear = todayCal.get(Calendar.YEAR)
                val currentMonth = todayCal.get(Calendar.MONTH) + 1
                val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

                val parts = selectedMonth.split("-")
                val targetYear = parts.getOrNull(0)?.toIntOrNull() ?: currentYear
                val targetMonth = parts.getOrNull(1)?.toIntOrNull() ?: currentMonth

                val isCurrentSelectedMonth = (targetYear == currentYear && targetMonth == currentMonth)

                val nightShiftsCount = remember(entries) {
                    entries.count { e ->
                        try {
                            val inCal = Calendar.getInstance()
                            e.checkInTime?.let {
                                inCal.timeInMillis = it
                                val inHour = inCal.get(Calendar.HOUR_OF_DAY)
                                inHour >= 22 || inHour <= 6 || e.dayType == "NIGHT"
                            } ?: false
                        } catch (ex: Exception) { false }
                    }
                }
                
                val tinhDenNgay = if (isCurrentSelectedMonth) {
                    todayDayOfMonth
                } else {
                    val tempCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, targetMonth - 1)
                    }
                    tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                }

                // Remaining days calculation
                val maxDaysInMonth = remember(targetYear, targetMonth) {
                    val tempCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, targetMonth - 1)
                    }
                    tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                }

                val defaultRemainingSundays = remember(targetYear, targetMonth, todayDayOfMonth, isCurrentSelectedMonth) {
                    if (!isCurrentSelectedMonth) 0 else {
                        val cal = Calendar.getInstance()
                        var count = 0
                        for (day in (todayDayOfMonth + 1)..maxDaysInMonth) {
                            cal.set(Calendar.YEAR, targetYear)
                            cal.set(Calendar.MONTH, targetMonth - 1)
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                                count++
                            }
                        }
                        count
                    }
                }
                var remainingSundays by remember(defaultRemainingSundays) { mutableStateOf(defaultRemainingSundays) }

                val remainingWeekdays = remember(targetYear, targetMonth, todayDayOfMonth, isCurrentSelectedMonth) {
                    if (!isCurrentSelectedMonth) 0 else {
                        val cal = Calendar.getInstance()
                        var count = 0
                        for (day in (todayDayOfMonth + 1)..maxDaysInMonth) {
                            cal.set(Calendar.YEAR, targetYear)
                            cal.set(Calendar.MONTH, targetMonth - 1)
                            cal.set(Calendar.DAY_OF_MONTH, day)
                            if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                                count++
                            }
                        }
                        count
                    }
                }

                // Dynamic identification of Sunday scheduling/work history
                val hasWorkedSunday = remember(entries) {
                    entries.any { e ->
                        try {
                            // Check day of week
                            val cal = Calendar.getInstance()
                            val partsDate = e.date.split("-")
                            if (partsDate.size >= 3) {
                                cal.set(partsDate[0].toInt(), partsDate[1].toInt() - 1, partsDate[2].toInt())
                                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && e.checkInTime != null
                            } else false
                        } catch (ex: Exception) { false }
                    }
                }

                var includeSundayInProjection by remember { mutableStateOf(false) }
                LaunchedEffect(hasWorkedSunday) {
                    includeSundayInProjection = hasWorkedSunday
                }

                val dailySalary = remember(c.luongCoBan, s.standardWorkDays) {
                    if (s.standardWorkDays == 27) {
                        c.luongCoBan / 26.0
                    } else {
                        c.luongCoBan / s.standardWorkDays.coerceAtLeast(1)
                    }
                }
                val hourlySalary = dailySalary / 8.0

                val additionalWeekdaysPay = remainingWeekdays * dailySalary
                val additionalSundaysPay = if (includeSundayInProjection) {
                    remainingSundays * dailySalary * c.heSoOtChuNhat
                } else {
                    0.0
                }

                // TAB / SEGMENT CONTROL
                var selectedTab by remember { mutableStateOf(0) }

                val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                val hasLoggedUnpaidOrAbsent = remember(entries, todayStr, isCurrentSelectedMonth) {
                    entries.any { e ->
                        val isPastOrToday = !isCurrentSelectedMonth || e.date <= todayStr
                        if (isPastOrToday) {
                            if (e.dayType == "UNPAID_LEAVE") {
                                true
                            } else if (e.checkInTime == null && e.dayType != "PAID_LEAVE" && e.dayType != "HOLIDAY") {
                                try {
                                    val cal = Calendar.getInstance()
                                    val partsDate = e.date.split("-")
                                    if (partsDate.size >= 3) {
                                        cal.set(partsDate[0].toInt(), partsDate[1].toInt() - 1, partsDate[2].toInt())
                                        val isSun = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                                        val isHol = com.example.data.SalaryCalculator.isHoliday(e.date)
                                        !isSun && !isHol
                                    } else false
                                } catch (ex: Exception) { false }
                            } else false
                        } else false
                    }
                }

                val pcKyThuatShow = if (selectedTab == 1) c.pcKyThuat else s.pcKyThuatVal
                val pcTrachNhiemShow = if (selectedTab == 1) c.pcTrachNhiem else s.pcTrachNhiemVal
                val pcChucVuShow = if (selectedTab == 1) c.pcChucVu else s.pcChucVuVal
                val pcHieuSuatShow = if (selectedTab == 1) c.pcHieuSuat else s.pcHieuSuatVal
                val pcSanPhamShow = if (selectedTab == 1) c.pcSanPham else s.pcSanPhamVal

                val pcComCaShow = if (selectedTab == 1) {
                    if (isCurrentSelectedMonth) {
                        s.pcComCaVal + (remainingWeekdays * c.pcComCa) + (if (includeSundayInProjection) remainingSundays * c.pcComCa else 0.0)
                    } else {
                        s.pcComCaVal
                    }
                } else {
                    s.pcComCaVal
                }

                val otMealAllowance = if (selectedTab == 1) customOt15DaysCount * c.pcComOt else 0.0
                val pcComOtShow = if (selectedTab == 1) s.pcComOtVal + otMealAllowance else s.pcComOtVal

                val pcNhaOShow = if (selectedTab == 1) c.pcNhaO else s.pcNhaOVal
                val pcDocHaiShow = if (selectedTab == 1) c.pcDocHai else s.pcDocHaiVal
                val pcDtDoanhThuShow = if (selectedTab == 1) c.pcDtDoanhThu else s.pcDtDoanhThuVal
                val pcXangXeShow = if (selectedTab == 1) c.pcXangXe else s.pcXangXeVal
                val pcKhacShow = if (selectedTab == 1) c.pcKhac else s.pcKhacVal
                val pcKhac1Show = if (selectedTab == 1) c.pcKhac1 else s.pcKhac1Val
                val pcThamNienShow = if (selectedTab == 1) c.pcThamNien else s.pcThamNienVal

                val pcChuyenCanShow = if (selectedTab == 1) {
                    if (hasLoggedUnpaidOrAbsent) 0.0 else c.tienChuyenCanGoc
                } else {
                    s.phuCapChuyenCan
                }

                val currentProratedAllowancesSum = s.pcKyThuatVal + s.pcTrachNhiemVal + s.pcChucVuVal + s.pcHieuSuatVal +
                        s.pcSanPhamVal + s.pcComCaVal + s.pcComOtVal + s.pcNhaOVal + s.pcDocHaiVal + 
                        s.pcDtDoanhThuVal + s.pcXangXeVal + s.pcKhacVal + s.pcKhac1Val + s.pcThamNienVal + s.phuCapChuyenCan +
                        s.pcCaDemVal

                val fullProjectedAllowancesSum = c.pcKyThuat + c.pcTrachNhiem + c.pcChucVu + c.pcHieuSuat +
                        c.pcSanPham + pcComCaShow + pcComOtShow + c.pcNhaO + c.pcDocHai + 
                        c.pcDtDoanhThu + c.pcXangXe + c.pcKhac + c.pcKhac1 + c.pcThamNien + (if (hasLoggedUnpaidOrAbsent) 0.0 else c.tienChuyenCanGoc) +
                        s.pcCaDemVal

                val allowanceAdjustment = fullProjectedAllowancesSum - currentProratedAllowancesSum

                val baseSalaryAdjustment = if (isCurrentSelectedMonth) additionalWeekdaysPay else 0.0
                val breakHours = if (c.tinhKhauTruNghi) c.soGioNghiGiaiLao else 0.0
                val totalOtHours = customOt15DaysCount * (4.0 - breakHours).coerceAtLeast(0.0)
                val customOt15Pay = totalOtHours * hourlySalary * c.heSoOtNgayThuong
                val customNightAllowance = if (selectedOt15Shift == "Đêm") {
                    customOt15DaysCount * c.pcKhac
                } else 0.0
                val luongDuKienVal = s.luongThucNhan + baseSalaryAdjustment + additionalSundaysPay + allowanceAdjustment + customOt15Pay + customNightAllowance
                val soNgayCongDuKien = if (isCurrentSelectedMonth) {
                    s.workingDays + remainingWeekdays + (if (includeSundayInProjection) remainingSundays else 0)
                } else {
                    s.standardWorkDays
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF161618), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) NeonBlue else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 12.dp)
                            .testTag("actual_payslip_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "THỰC TẾ ĐẾN NAY",
                            color = if (selectedTab == 0) White else LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) NeonBlue else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 12.dp)
                            .testTag("projected_payslip_tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCurrentSelectedMonth) "DỰ KIẾN CUỐI THÁNG" else "LƯƠNG FULL THÁNG",
                            color = if (selectedTab == 1) White else LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // ELEGANT THERMAL PAPER DARK INVOICE CARD STYLE
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Header ticket logo info
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonBlue.copy(alpha = 0.15f))
                                        .border(1.5.dp, NeonBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalAtm,
                                        contentDescription = null,
                                        tint = NeonBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "TIMESNAP PRO",
                                    color = NeonBlue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedTab == 1) "PHIẾU LƯƠNG DỰ KIẾN CUỐI THÁNG" else "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT",
                                color = if (selectedTab == 1) AccentOrange else White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kỳ lương: $monthLabel",
                                color = LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (selectedTab == 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🔮 ĐÃ BÙ TOÀN BỘ CÁC NGÀY CÒN LẠI",
                                    color = NeonBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (s.isCurrentMonth) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "⚠️ TẠM TÍNH ĐẾN NGÀY $tinhDenNgay",
                                    color = AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Separator dashes
                            HorizontalDivider(
                                color = Color(0xFF2C2C2C),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }

                        // Employee Profile rows
                        val employeeName = if (!c.hoVaTen.isNullOrBlank()) c.hoVaTen else (userSession?.displayName ?: "N/A")
                        val employeeCode = if (!c.maNhanVien.isNullOrBlank()) c.maNhanVien else (userSession?.uid?.take(10) ?: "N/A")

                        PayslipProfileRow(label = "Nhân viên:", value = employeeName)
                        PayslipProfileRow(label = "Mã nhân viên (UID):", value = employeeCode, isMono = true)
                        PayslipProfileRow(label = "Mức lương cơ bản:", value = "${fmt.format(c.luongCoBan)}đ")
                        
                        if (selectedTab == 1) {
                            PayslipProfileRow(
                                label = "Số ngày công dự kiến:", 
                                value = "$soNgayCongDuKien / ${s.standardWorkDays} ngày"
                            )
                            if (isCurrentSelectedMonth) {
                                PayslipProfileRow(
                                    label = "Trong đó làm thêm:", 
                                    value = "$remainingWeekdays ngày thường" + (if (includeSundayInProjection && remainingSundays > 0) " + $remainingSundays Chủ Nhật" else "")
                                )
                            }
                        } else {
                            PayslipProfileRow(
                                label = "Số ngày chấm công:", 
                                value = "${s.workingDays} / ${if (s.isCurrentMonth) s.expectedWorkDays else s.standardWorkDays} ngày"
                            )
                        }

                        // Interactive Projection Switch inside Receipt Paper
                        if (selectedTab == 1 && isCurrentSelectedMonth) {
                            HorizontalDivider(
                                color = Color(0xFF2C2C2C),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { includeSundayInProjection = !includeSundayInProjection },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Lịch làm việc có Chủ Nhật", color = LightGray, fontSize = 12.sp)
                                    Switch(
                                        checked = includeSundayInProjection,
                                        onCheckedChange = { includeSundayInProjection = it },
                                        modifier = Modifier.scale(0.85f).testTag("sunday_projection_switch")
                                    )
                                }

                                if (includeSundayInProjection) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Số ngày CN còn lại:", color = LightGray, fontSize = 12.sp)
                                            Text("(Tối đa: $defaultRemainingSundays ngày)", color = Color.Gray, fontSize = 10.sp)
                                        }
                                        
                                        var sundayInputText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(remainingSundays.toString())) }
                                        LaunchedEffect(remainingSundays) {
                                            if (sundayInputText.text != remainingSundays.toString()) {
                                                sundayInputText = sundayInputText.copy(
                                                    text = remainingSundays.toString(),
                                                    selection = androidx.compose.ui.text.TextRange(remainingSundays.toString().length)
                                                )
                                            }
                                        }
                                        
                                        OutlinedTextField(
                                            value = sundayInputText,
                                            onValueChange = { newValue ->
                                                val cleanText = newValue.text.filter { it.isDigit() }
                                                if (cleanText.isEmpty()) {
                                                    sundayInputText = newValue.copy(text = "")
                                                    remainingSundays = 0
                                                } else {
                                                    cleanText.toIntOrNull()?.let { parsed ->
                                                        if (parsed <= defaultRemainingSundays) {
                                                            sundayInputText = newValue.copy(text = cleanText)
                                                            remainingSundays = parsed
                                                        } else {
                                                            val cappedStr = defaultRemainingSundays.toString()
                                                            sundayInputText = androidx.compose.ui.text.input.TextFieldValue(
                                                                text = cappedStr,
                                                                selection = androidx.compose.ui.text.TextRange(cappedStr.length)
                                                            )
                                                            remainingSundays = defaultRemainingSundays
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .width(72.dp)
                                                .testTag("sunday_count_input"),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                textAlign = TextAlign.Center, 
                                                color = White, 
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonBlue,
                                                unfocusedBorderColor = Color(0xFF3C3C3C),
                                                focusedContainerColor = Color(0xFF1E1E1E),
                                                unfocusedContainerColor = Color(0xFF161616),
                                                focusedTextColor = White,
                                                unfocusedTextColor = White,
                                                cursorColor = NeonBlue
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedTab == 1) {
                            HorizontalDivider(
                                color = Color(0xFF2C2C2C),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dự kiến OT1.5:", color = LightGray, fontSize = 13.sp)
                                    if (customOt15DaysCount > 0.0) {
                                        Text(
                                            text = "+${fmt.format(customOt15Pay)}đ dự kiến",
                                            color = AccentGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedOt15Shift == "Ngày",
                                            onClick = { selectedOt15Shift = "Ngày" },
                                            colors = RadioButtonDefaults.colors(selectedColor = NeonBlue, unselectedColor = LightGray)
                                        )
                                        Text("Ngày", color = White, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        RadioButton(
                                            selected = selectedOt15Shift == "Đêm",
                                            onClick = { selectedOt15Shift = "Đêm" },
                                            colors = RadioButtonDefaults.colors(selectedColor = NeonBlue, unselectedColor = LightGray)
                                        )
                                        Text("Đêm", color = White, fontSize = 13.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(if (customOt15DaysCount == 0.0) "" else df.format(customOt15DaysCount))) }
                                        LaunchedEffect(customOt15DaysCount) {
                                            val str = if (customOt15DaysCount == 0.0) "" else df.format(customOt15DaysCount)
                                            if (textFieldValue.text != str && textFieldValue.text.toDoubleOrNull() != customOt15DaysCount) {
                                                textFieldValue = textFieldValue.copy(text = str)
                                            }
                                        }
                                        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        val isFocused by interactionSource.collectIsFocusedAsState()
                                        LaunchedEffect(isFocused) {
                                            if (isFocused) {
                                                kotlinx.coroutines.delay(50)
                                                textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange(0, textFieldValue.text.length))
                                            }
                                        }
                                        TextField(
                                            value = textFieldValue,
                                            onValueChange = {
                                                textFieldValue = it
                                                val raw = it.text.toDoubleOrNull() ?: 0.0
                                                val maxPossible = (remainingWeekdays + (if (includeSundayInProjection) remainingSundays else 0)).toDouble()
                                                if (raw > maxPossible) {
                                                    customOt15DaysCount = maxPossible
                                                    val cappedStr = df.format(maxPossible)
                                                    textFieldValue = textFieldValue.copy(
                                                        text = cappedStr,
                                                        selection = androidx.compose.ui.text.TextRange(cappedStr.length)
                                                    )
                                                } else {
                                                    customOt15DaysCount = raw
                                                }
                                            },
                                            modifier = Modifier.width(60.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, color = White),
                                            singleLine = true,
                                            interactionSource = interactionSource,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = NeonBlue,
                                                unfocusedIndicatorColor = Color.Transparent
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ngày", color = LightGray, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFF2C2C2C),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // ADDITIONS Header
                        Text(
                            text = if (selectedTab == 1) "KHOẢN CỘNG LƯƠNG DỰ KIẾN (+)" else "KHOẢN CỘNG LƯƠNG (+)",
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 1. Lương cơ bản
                        if (selectedTab == 1) {
                            PayslipMoneyRow(label = "Lương Cơ Bản Thỏa Thuận", value = c.luongCoBan, isAddition = true)
                            if (s.standardWorkDays == 27) {
                                PayslipMoneyRow(label = "Bù công dôi dư tháng 31 ngày (1 ngày LCB)", value = dailySalary, isAddition = true)
                            }
                        } else {
                            val label = if (s.isCurrentMonth) "Lương Cơ Bản Tạm Tính" else "Thực Nhận"
                            PayslipMoneyRow(label = label, value = s.baseBasicSalary, isAddition = true)
                            if (s.standardWorkDays == 27) {
                                PayslipMoneyRow(label = "Bù công dôi dư tháng 31 ngày (1 ngày LCB)", value = dailySalary, isAddition = true)
                            }
                        }
                        
                        // 2. Chuyên cần
                        if (pcChuyenCanShow > 0.0) {
                            PayslipMoneyRow(label = "Chuyên cần", value = pcChuyenCanShow, isAddition = true)
                        }

                        // 3. Trách nhiệm
                        if (c.pcTrachNhiem > 0.0) {
                            PayslipMoneyRow(label = "Trách nhiệm", value = pcTrachNhiemShow, isAddition = true)
                        }

                        // 4. Kỹ thuật
                        if (c.pcKyThuat > 0.0) {
                            PayslipMoneyRow(label = "Kỹ thuật", value = pcKyThuatShow, isAddition = true)
                        }

                        // 5. Hiệu suất
                        if (c.pcHieuSuat > 0.0) {
                            PayslipMoneyRow(label = "Hiệu suất", value = pcHieuSuatShow, isAddition = true)
                        }

                        // 6. Sản phẩm
                        if (c.pcSanPham > 0.0) {
                            PayslipMoneyRow(label = "Sản phẩm", value = pcSanPhamShow, isAddition = true)
                        }

                        // 7. Chức vụ
                        if (c.pcChucVu > 0.0) {
                            PayslipMoneyRow(label = "Chức vụ", value = pcChucVuShow, isAddition = true)
                        }

                        // 8. Độc hại
                        if (c.pcDocHai > 0.0) {
                            PayslipMoneyRow(label = "Độc hại", value = pcDocHaiShow, isAddition = true)
                        }

                        // 9. Doanh thu
                        if (c.pcDtDoanhThu > 0.0) {
                            PayslipMoneyRow(label = "Doanh thu", value = pcDtDoanhThuShow, isAddition = true)
                        }

                        // 10. Thâm niên
                        if (c.pcThamNien > 0.0) {
                            PayslipMoneyRow(label = "Thâm niên", value = pcThamNienShow, isAddition = true)
                        }

                        // 11. Cơm/ca
                        if (pcComCaShow > 0.0) {
                            PayslipMoneyRow(label = "Cơm/ ca", value = pcComCaShow, isAddition = true)
                        }

                        // 12. Cơm OT
                        if (pcComOtShow > 0.0) {
                            PayslipMoneyRow(label = "Cơm OT", value = pcComOtShow, isAddition = true)
                        }

                        // 13. OT 1.5
                        if (s.tienOtNgay > 0.0) {
                            PayslipMoneyRow(label = "OT 1.5 (${df.format(s.otDayHours)}h)", value = s.tienOtNgay, isAddition = true, isAccent = true)
                        }
                        if (selectedTab == 1 && customOt15DaysCount > 0.0) {
                            PayslipMoneyRow(label = "OT 1.5 (${df.format(customOt15DaysCount)} ngày)", value = customOt15Pay, isAddition = true, isAccent = true)
                        }

                        // 14. OT 2.0
                        if (s.tienChuNhat > 0.0) {
                            PayslipMoneyRow(label = "OT 2.0 (${df.format(s.chuNhatHours)}h)", value = s.tienChuNhat, isAddition = true, isAccent = true)
                        }
                        if (selectedTab == 1 && isCurrentSelectedMonth && includeSundayInProjection && remainingSundays > 0) {
                            PayslipMoneyRow(label = "OT 2.0 ($remainingSundays)", value = additionalSundaysPay, isAddition = true, isAccent = true)
                        }

                        // 15. OT 3.0
                        if (s.tienOtLe > 0.0) {
                            PayslipMoneyRow(label = "OT 3.0 (${df.format(s.otLeHours)}h)", value = s.tienOtLe, isAddition = true, isAccent = true)
                        }

                        // 15.1 OT đêm
                        if (s.tienOtDem > 0.0) {
                            PayslipMoneyRow(label = "OTĐ 1.5 (${df.format(s.otNightHours)}h)", value = s.tienOtDem, isAddition = true, isAccent = true)
                        }

                        // 16. Phụ cấp đêm
                        val finalPcCaDemCount = if (selectedTab == 1 && selectedOt15Shift == "Đêm") s.caDemCount + customOt15DaysCount.toInt() else s.caDemCount
                        val finalPcCaDem = if (selectedTab == 1) (s.pcCaDemVal + customNightAllowance) else s.pcCaDemVal
                        if (finalPcCaDem > 0.0) {
                            PayslipMoneyRow(label = "Phụ cấp đêm ($finalPcCaDemCount)", value = finalPcCaDem, isAddition = true)
                        }

                        // 17. Xăng xe
                        if (c.pcXangXe > 0.0) {
                            PayslipMoneyRow(label = "Xăng xe", value = pcXangXeShow, isAddition = true)
                        }

                        // 18. Nhà ở
                        if (c.pcNhaO > 0.0) {
                            PayslipMoneyRow(label = "Nhà ở", value = pcNhaOShow, isAddition = true)
                        }

                        // 19. Khác (Now merged into Night Allowance)
                        // if (c.pcKhac > 0.0) {
                        //     PayslipMoneyRow(label = "Khác", value = pcKhacShow, isAddition = true)
                        // }

                        // 20. Khác 1
                        if (c.pcKhac1 > 0.0) {
                            PayslipMoneyRow(label = "Khác 1", value = pcKhac1Show, isAddition = true)
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        // DEDUCTIONS Header
                        Text(
                            text = "KHOẢN TRỪ LƯƠNG (-)",
                            color = AccentRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (s.tienBh > 0.0) {
                            PayslipMoneyRow(label = "BHXH/BHYT Khấu trừ (10.5%)", value = s.tienBh, isAddition = false)
                        }
                        if (s.doanPhi > 0.0) {
                            PayslipMoneyRow(label = "Phí Công Đoàn Bắt Buộc", value = s.doanPhi, isAddition = false)
                        }
                        
                        // Missed days deduction - only visible on actual payslip
                        if (selectedTab == 0 && s.tienKhauTruNghi > 0.0) {
                            val missedCount = ((if (s.isCurrentMonth) s.expectedWorkDays else s.standardWorkDays) - s.workingDays).coerceAtLeast(0)
                            PayslipMoneyRow(
                                label = "Khấu trừ vắng làm ($missedCount ngày)",
                                value = s.tienKhauTruNghi,
                                isAddition = false
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFF2C2C2C),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // FINAL NET SALARY
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN:" else "THỰC NHẬN:",
                                color = White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${fmt.format(if (selectedTab == 1) luongDuKienVal else s.luongThucNhan)}đ",
                                color = AccentGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "* ĐÃ ĐƯỢC PHÊ DUYỆT BỞI HỆ THỐNG TIMESNAP PRO *",
                                color = MediumGray,
                                fontSize = 8.sp,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "SÁNG LẬP & PHÁT TRIỂN BỞI TRUONGVANKHOA",
                                color = NeonBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // JSON structure removed per user request

                // EXPORT HIGH-QUALITY PNG PORTABLE RECEIPT ACTION BUTTON
                Button(
                    onClick = {
                        val isSaved = savePayslipAsPngImage(
                            context = context,
                            summary = s,
                            config = c,
                            userSession = userSession,
                            monthLabel = monthLabel,
                            selectedMonth = selectedMonth,
                            selectedTab = selectedTab,
                            includeSundayInProjection = includeSundayInProjection,
                            remainingWeekdays = remainingWeekdays,
                            remainingSundays = remainingSundays,
                            dailySalary = dailySalary,
                            luongDuKienVal = luongDuKienVal,
                            soNgayCongDuKien = soNgayCongDuKien,
                            customOt15DaysCount = customOt15DaysCount,
                            customOt15Pay = customOt15Pay,
                            selectedOt15Shift = selectedOt15Shift,
                            customNightAllowance = customNightAllowance,
                            hasLoggedUnpaidOrAbsent = hasLoggedUnpaidOrAbsent
                        )
                        if (isSaved) {
                            Toast.makeText(context, "Đã lưu phiếu lương thành công vào Gallery ứng dụng của điện thoại!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Lỗi khi lưu ảnh phiếu lương! Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_payslip_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "XUẤT PHIẾU LƯƠNG",
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun PayslipProfileRow(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            color = MediumGray, 
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PayslipMoneyRow(
    label: String,
    value: Double,
    isAddition: Boolean,
    isAccent: Boolean = false
) {
    val fmt = DecimalFormat("#,###")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isAccent) NeonBlue else LightGray,
            fontSize = 13.sp,
            fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isAddition) "+${fmt.format(value)}đ" else "-${fmt.format(value)}đ",
            color = if (isAddition) AccentGreen else AccentRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

// -------------------------------------------------------------
// HIGH QUALITY PRISTINE VECTOR PNG BITMAP GENERATION ENGINE
// -------------------------------------------------------------
fun savePayslipAsPngImage(
    context: Context,
    summary: SalarySummary,
    config: com.example.data.model.UserConfig,
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

    // 1. Create offline Bitmap with Dynamic Height to prevent truncation or empty gaps
    val width = 800
    var estimatedHeight = 550 // Header and margins
    estimatedHeight += 180 // Profile rows
    
    // Additions: LCB
    estimatedHeight += 45
    if (selectedTab == 1 && remainingSundays > 0 && includeSundayInProjection) {
        estimatedHeight += 45 // Extra lines for sundays projection additions
    }
    if (selectedTab == 1 && customOt15DaysCount > 0.0) {
        estimatedHeight += 45 // Extra line for custom OT 1.5 projection additions
    }
    // Allowances
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
    
    if (summary.tongTienCom > 0.0) estimatedHeight += 45 // Total meal
    
    if (summary.tienOtNgay > 0.0) estimatedHeight += 45
    if (summary.tienChuNhat > 0.0) estimatedHeight += 45
    
    // Deductions header & lines
    estimatedHeight += 60 // Deductions Header
    if (summary.tienBh > 0.0) estimatedHeight += 45 // BHXH
    if (summary.doanPhi > 0.0) estimatedHeight += 45 // Union Fee
    if (selectedTab == 0 && summary.tienKhauTruNghi > 0.0) estimatedHeight += 45
    
    // Total receipt
    estimatedHeight += 120
    
    // Footer credits
    estimatedHeight += 150
    
    val height = estimatedHeight.coerceAtLeast(1400)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Setup Paints
    val paintBg = Paint().apply { color = android.graphics.Color.parseColor("#121212") }
    val paintCard = Paint().apply { color = android.graphics.Color.parseColor("#1C1C1C") }
    val paintNeon = Paint().apply {
        color = android.graphics.Color.parseColor("#2F80ED")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val paintDivider = Paint().apply {
        color = android.graphics.Color.parseColor("#2C2C2C")
        strokeWidth = 2f
    }
    
    val paintTextTitle = Paint().apply {
        color = android.graphics.Color.parseColor("#2F80ED")
        textSize = 34f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    
    val paintTextSubTitle = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 20f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    val paintTextMonth = Paint().apply {
        color = android.graphics.Color.parseColor("#E0E0E0")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    val paintLabel = Paint().apply {
        color = android.graphics.Color.parseColor("#828282")
        textSize = 22f
    }

    val paintValue = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        isFakeBoldText = true
    }

    val paintGreen = Paint().apply {
        color = android.graphics.Color.parseColor("#27AE60")
        textSize = 22f
        isFakeBoldText = true
    }

    val paintRed = Paint().apply {
        color = android.graphics.Color.parseColor("#EB5757")
        textSize = 22f
        isFakeBoldText = true
    }

    // Draw solid full-bleed background
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintCard)

    var currentY = 100f

    // Draw logo circle to the left of TIMESNAP PRO
    val titleText = "TIMESNAP PRO"
    val textWidth = paintTextTitle.measureText(titleText)
    val cX = (width / 2f) - (textWidth / 2f) - 30f
    val cY = currentY - 10f

    val paintCircleBg = Paint().apply {
        color = android.graphics.Color.parseColor("#1535A3FF") // 15% opacity neon blue
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cX, cY, 18f, paintCircleBg)

    val paintCircleBorder = Paint().apply {
        color = android.graphics.Color.parseColor("#35A3FF")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    canvas.drawCircle(cX, cY, 18f, paintCircleBorder)

    val paintCircleSymbol = Paint().apply {
        color = android.graphics.Color.parseColor("#35A3FF")
        textSize = 20f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("$", cX, cY + 7f, paintCircleSymbol)

    // Draw header content with slight shift for logo alignment
    canvas.drawText(titleText, (width / 2f) + 15f, currentY, paintTextTitle)
    currentY += 45f
    val docTypeTitle = if (selectedTab == 1) "PHIẾU LƯƠNG DỰ KIẾN CUỐI THÁNG" else "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT"
    canvas.drawText(docTypeTitle, (width / 2).toFloat(), currentY, paintTextSubTitle)
    currentY += 40f
    canvas.drawText("Kỳ lương: $monthLabel", (width / 2).toFloat(), currentY, paintTextMonth)
    currentY += 45f
    if (selectedTab == 1) {
        val paintWarning = Paint().apply {
            color = android.graphics.Color.parseColor("#35A3FF")
            textSize = 21f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🔮 ĐÃ BÙ TOÀN BỘ CÁC NGÀY CÒN LẠI", (width / 2).toFloat(), currentY, paintWarning)
        currentY += 45f
    } else if (summary.isCurrentMonth) {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val paintWarning = Paint().apply {
            color = android.graphics.Color.parseColor("#F2994A") // AccentOrange orange
            textSize = 21f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚠️ TẠM TÍNH ĐẾN NGÀY $currentDay", (width / 2).toFloat(), currentY, paintWarning)
        currentY += 45f
    }

    canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
    currentY += 45f

    // Profile parameters
    fun drawRow(label: String, value: String, isGreenVal: Boolean = false, isRedVal: Boolean = false) {
        canvas.drawText(label, 80f, currentY, paintLabel)
        val rectValue = paintValue.measureText(value)
        val paintToUse = when {
            isGreenVal -> paintGreen
            isRedVal -> paintRed
            else -> paintValue
        }
        canvas.drawText(value, (width - 80) - rectValue, currentY, paintToUse)
        currentY += 45f
    }

    val employeeName = if (!config.hoVaTen.isNullOrBlank()) config.hoVaTen else (userSession?.displayName ?: "N/A")
    val employeeCode = if (!config.maNhanVien.isNullOrBlank()) config.maNhanVien else (userSession?.uid?.take(10) ?: "N/A")

    drawRow("Nhân viên:", employeeName)
    drawRow("Mã nhân viên (UID):", employeeCode)
    drawRow("Mức lương cơ bản:", "${fmt.format(config.luongCoBan)}đ")
    
    if (selectedTab == 1) {
        drawRow("Số ngày công dự kiến:", "$soNgayCongDuKien / ${summary.standardWorkDays} ngày")
    } else {
        drawRow("Số ngày chấm công:", "${summary.workingDays} / ${if (summary.isCurrentMonth) summary.expectedWorkDays else summary.standardWorkDays} ngày")
    }

    currentY += 10f
    canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
    currentY += 45f

    // Additions Section
    val additionsHeaderPNG = if (selectedTab == 1) "KHOẢN CỘNG LƯƠNG DỰ KIẾN (+)" else "KHOẢN CỘNG LƯƠNG (+)"
    canvas.drawText(additionsHeaderPNG, 80f, currentY, paintGreen)
    currentY += 45f

    // 1. Lương cơ bản
    if (selectedTab == 1) {
        drawRow("Lương Cơ Bản Thỏa Thuận", "+${fmt.format(config.luongCoBan)}đ", isGreenVal = true)
        if (summary.standardWorkDays == 27) {
            drawRow("Bù công dôi dư tháng 31 ngày (1 ngày LCB)", "+${fmt.format(dailySalary)}đ", isGreenVal = true)
        }
    } else {
        val label = if (summary.isCurrentMonth) "Lương Cơ Bản Tạm Tính" else "Thực Nhận"
        drawRow(label, "+${fmt.format(summary.baseBasicSalary)}đ", isGreenVal = true)
        if (summary.standardWorkDays == 27) {
            drawRow("Bù công dôi dư tháng 31 ngày (1 ngày LCB)", "+${fmt.format(dailySalary)}đ", isGreenVal = true)
        }
    }
    
    // 2. Chuyên cần
    if (pcChuyenCanShowPNG > 0.0) {
        drawRow("Chuyên cần", "+${fmt.format(pcChuyenCanShowPNG)}đ", isGreenVal = true)
    }

    // 3. Trách nhiệm
    if (config.pcTrachNhiem > 0.0) {
        drawRow("Trách nhiệm", "+${fmt.format(pcTrachNhiemShowPNG)}đ", isGreenVal = true)
    }

    // 4. Kỹ thuật
    if (config.pcKyThuat > 0.0) {
        drawRow("Kỹ thuật", "+${fmt.format(pcKyThuatShowPNG)}đ", isGreenVal = true)
    }

    // 5. Hiệu suất
    if (config.pcHieuSuat > 0.0) {
        drawRow("Hiệu suất", "+${fmt.format(pcHieuSuatShowPNG)}đ", isGreenVal = true)
    }

    // 6. Sản phẩm
    if (config.pcSanPham > 0.0) {
        drawRow("Sản phẩm", "+${fmt.format(pcSanPhamShowPNG)}đ", isGreenVal = true)
    }

    // 7. Chức vụ
    if (config.pcChucVu > 0.0) {
        drawRow("Chức vụ", "+${fmt.format(pcChucVuShowPNG)}đ", isGreenVal = true)
    }

    // 8. Độc hại
    if (config.pcDocHai > 0.0) {
        drawRow("Độc hại", "+${fmt.format(pcDocHaiShowPNG)}đ", isGreenVal = true)
    }

    // 9. Doanh thu
    if (config.pcDtDoanhThu > 0.0) {
        drawRow("Doanh thu", "+${fmt.format(pcDtDoanhThuShowPNG)}đ", isGreenVal = true)
    }

    // 10. Thâm niên
    if (config.pcThamNien > 0.0) {
        drawRow("Thâm niên", "+${fmt.format(pcThamNienShowPNG)}đ", isGreenVal = true)
    }

    // 11. Cơm/ca
    if (pcComCaShowPNG > 0.0) {
        drawRow("Cơm/ ca", "+${fmt.format(pcComCaShowPNG)}đ", isGreenVal = true)
    }

    // 12. Cơm OT
    if (pcComOtShowPNG > 0.0) {
        drawRow("Cơm OT", "+${fmt.format(pcComOtShowPNG)}đ", isGreenVal = true)
    }

    // 13. OT 1.5
    if (summary.tienOtNgay > 0.0) {
        drawRow("OT 1.5 (${df.format(summary.otDayHours)}h)", "+${fmt.format(summary.tienOtNgay)}đ", isGreenVal = true)
    }
    if (selectedTab == 1 && customOt15DaysCount > 0.0) {
        drawRow("OT 1.5 (${df.format(customOt15DaysCount)} ngày)", "+${fmt.format(customOt15Pay)}đ", isGreenVal = true)
    }

    // 14. OT 2.0
    if (summary.tienChuNhat > 0.0) {
        drawRow("OT 2.0 (${df.format(summary.chuNhatHours)}h)", "+${fmt.format(summary.tienChuNhat)}đ", isGreenVal = true)
    }
    if (selectedTab == 1 && includeSundayInProjection && remainingSundays > 0) {
        drawRow("OT 2.0 ($remainingSundays)", "+${fmt.format(remainingSundays * dailySalary * config.heSoOtChuNhat)}đ", isGreenVal = true)
    }

    // 15. OT 3.0
    if (summary.tienOtLe > 0.0) {
        drawRow("OT 3.0 (${df.format(summary.otLeHours)}h)", "+${fmt.format(summary.tienOtLe)}đ", isGreenVal = true)
    }

    // 15.1 OTĐ 1.5
    if (summary.tienOtDem > 0.0) {
        drawRow("OTĐ 1.5 (${df.format(summary.otNightHours)}h)", "+${fmt.format(summary.tienOtDem)}đ", isGreenVal = true)
    }

    // 16. Phụ cấp đêm
    val finalPcCaDemCountPNG = if (selectedTab == 1 && selectedOt15Shift == "Đêm") summary.caDemCount + customOt15DaysCount.toInt() else summary.caDemCount
    val finalPcCaDemPNG = if (selectedTab == 1) (summary.pcCaDemVal + customNightAllowance) else summary.pcCaDemVal
    if (finalPcCaDemPNG > 0.0) {
        drawRow("Phụ cấp đêm ($finalPcCaDemCountPNG)", "+${fmt.format(finalPcCaDemPNG)}đ", isGreenVal = true)
    }

    // 17. Xăng xe
    if (config.pcXangXe > 0.0) {
        drawRow("Xăng xe", "+${fmt.format(pcXangXeShowPNG)}đ", isGreenVal = true)
    }

    // 18. Nhà ở
    if (config.pcNhaO > 0.0) {
        drawRow("Nhà ở", "+${fmt.format(pcNhaOShowPNG)}đ", isGreenVal = true)
    }

    // 19. Khác (Merged into Night Allowance)
    // if (config.pcKhac > 0.0) {
    //     drawRow("Khác", "+${fmt.format(pcKhacShowPNG)}đ", isGreenVal = true)
    // }

    // 20. Khác 1
    if (config.pcKhac1 > 0.0) {
        drawRow("Khác 1", "+${fmt.format(pcKhac1ShowPNG)}đ", isGreenVal = true)
    }
    currentY += 10f
    canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
    currentY += 45f

    // Deductions Section
    canvas.drawText("KHOẢN TRỪ LƯƠNG (-)", 80f, currentY, paintRed)
    currentY += 45f

    if (summary.tienBh > 0.0) {
        drawRow("BHXH/BHYT Khấu trừ (10.5%)", "-${fmt.format(summary.tienBh)}đ", isRedVal = true)
    }
    if (summary.doanPhi > 0.0) {
        drawRow("Phí Công Đoàn Bắt Buộc", "-${fmt.format(summary.doanPhi)}đ", isRedVal = true)
    }

    if (selectedTab == 0 && summary.tienKhauTruNghi > 0.0) {
        val missed = ((if (summary.isCurrentMonth) summary.expectedWorkDays else summary.standardWorkDays) - summary.workingDays).coerceAtLeast(0)
        drawRow("Khấu trừ vắng làm ($missed ngày)", "-${fmt.format(summary.tienKhauTruNghi)}đ", isRedVal = true)
    }

    currentY += 10f
    canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
    currentY += 55f

    // Total actual salary pay
    val paintNetText = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 30f
        isFakeBoldText = true
    }

    val paintNetVal = Paint().apply {
        color = android.graphics.Color.parseColor("#27AE60")
        textSize = 40f
        isFakeBoldText = true
    }

    val netLabelPNG = if (selectedTab == 1) "DỰ KIẾN THỰC NHẬN:" else "THỰC NHẬN:"
    val netValuePNG = if (selectedTab == 1) luongDuKienVal else summary.luongThucNhan

    canvas.drawText(netLabelPNG, 80f, currentY, paintNetText)
    val textVal = "${fmt.format(netValuePNG)}đ"
    val measureNetVal = paintNetVal.measureText(textVal)
    canvas.drawText(textVal, (width - 80) - measureNetVal, currentY, paintNetVal)

    // Append Approval & Founder Info to PNG (Without the barcode box)
    currentY += 60f
    canvas.drawLine(80f, currentY, (width - 80).toFloat(), currentY, paintDivider)
    currentY += 50f

    val paintApproval = Paint().apply {
        color = android.graphics.Color.parseColor("#828282")
        textSize = 15f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("* ĐÃ ĐƯỢC PHÊ DUYỆT BỞI HỆ THỐNG TIMESNAP PRO *", (width / 2f), currentY, paintApproval)
    currentY += 35f

    val paintFounder = Paint().apply {
        color = android.graphics.Color.parseColor("#35A3FF") // Neon blue
        textSize = 17f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("SÁNG LẬP & PHÁT TRIỂN BỞI TRUONGVANKHOA", (width / 2f), currentY, paintFounder)

    // Save Bitmap to MediaStore Gallery output
    try {
        val filename = "TimeSnap_Pro_Payslip_${selectedMonth}_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/TimeSnapPro")
            }
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = contentResolver.openOutputStream(imageUri)
                true
            } else {
                false
            }
        } else {
            // Under Android 9 legacy file writer
            val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES).toString()
            val file = java.io.File(imagesDir, filename)
            fos = java.io.FileOutputStream(file)
            true
        }

        if (fos != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            return true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

