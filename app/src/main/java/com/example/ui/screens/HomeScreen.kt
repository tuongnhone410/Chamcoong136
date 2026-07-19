package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.UserSession
import com.example.data.model.TimeEntry
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.LightGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.White
import com.example.ui.theme.NightPurple
import com.example.viewmodel.TimeSnapViewModel
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TimeSnapViewModel,
    onNavigateToLogin: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val userSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val activeEntry by viewModel.activeWorkingEntry.collectAsStateWithLifecycle()
    val runningTimeText by viewModel.runningDurationText.collectAsStateWithLifecycle()
    val summaryState by viewModel.salarySummaryState.collectAsStateWithLifecycle()
    val recentEntries by viewModel.monthTimeEntries.collectAsStateWithLifecycle()
    val configState by viewModel.userConfig.collectAsStateWithLifecycle()

    var liveTimeString by remember { mutableStateOf("") }
    var liveDateString by remember { mutableStateOf("") }

    // Live clock ticks
    LaunchedEffect(Unit) {
        val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, 'Ngày' dd 'tháng' MM, yyyy", Locale("vi", "VN"))
        while (true) {
            val now = Date()
            liveTimeString = clockFormat.format(now)
            liveDateString = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    // Load active note dynamically so they can type inside Home
    var quickNoteText by remember { mutableStateOf("") }
    LaunchedEffect(activeEntry) {
        quickNoteText = activeEntry?.note ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TIMESNAP PRO",
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonBlue,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Hệ thống quản trị lương quốc dân",
                            color = LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.authController.signOut {
                                onNavigateToLogin()
                            }
                        },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Đăng xuất",
                            tint = AccentRed
                        )
                    }
                },
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
                .padding(horizontal = 16.dp)
        ) {
            
            // Welcome Header Block
            Spacer(modifier = Modifier.height(6.dp))
            val displayName = configState?.hoVaTen ?: userSession?.displayName ?: "Cá nhân"
            Text(
                text = "Xin chào, $displayName",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = liveDateString.ifEmpty { "Đang kết nối..." },
                color = LightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // CARD 1: NET ESTIMATED INCOME CARD (THỰC LĨNH DỰ KIẾN)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "THỰC LĨNH DỰ KIẾN (NET)",
                        color = LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val realSalary = summaryState?.luongThucNhan ?: 0.0
                    val formattedSalary = DecimalFormat("#,###").format(realSalary) + " đ"
                    Text(
                        text = formattedSalary,
                        color = AccentGreen,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFF2C2C2C))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tổng ngày công thực tế", color = MediumGray, fontSize = 11.sp)
                            Text(
                                text = "${summaryState?.workingDays ?: 0} ngày công",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        // Calculate live hourly calculation based on settings
                        val lcb = configState?.luongCoBan ?: 6000000.0
                        val hrRate = lcb / 26.0 / 8.0
                        val coeff = when {
                            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY -> configState?.heSoOtChuNhat ?: 2.0
                            run {
                                val sdfMonthDay = SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                                val md = sdfMonthDay.format(java.util.Date())
                                md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02"
                            } -> configState?.heSoOtNgayLe ?: 3.0
                            else -> 1.0
                        }
                        val coeffLabel = when {
                            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY -> " (Chủ nhật x2)"
                            run {
                                val sdfMonthDay = SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                                val md = sdfMonthDay.format(java.util.Date())
                                md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02"
                            } -> " (Ngày lễ x3)"
                            else -> ""
                        }
                        val currentHrRate = hrRate * coeff
                        val fmtHr = DecimalFormat("#,###").format(currentHrRate)

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Lương mỗi giờ hiện tại", color = MediumGray, fontSize = 11.sp)
                            Text(
                                text = "$fmtHr đ/g$coeffLabel",
                                color = if (coeff > 1.0) AccentGreen else NeonBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // CARD 2: INTERACTIVE ACTION TERMINAL (VÀO CA / RA CA MODULE)
            val isWorking = activeEntry != null
            val buttonColor by animateColorAsState(
                targetValue = if (isWorking) AccentRed else AccentGreen,
                animationSpec = tween(300), label = ""
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val cal = Calendar.getInstance()
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val dayName = when (dayOfWeek) {
                        Calendar.SUNDAY -> "CHỦ NHẬT"
                        Calendar.SATURDAY -> "THỨ BẢY"
                        else -> "THỨ ${dayOfWeek}"
                    }

                    val otNormal = configState?.heSoOtNgayThuong ?: 1.5
                    val otCn = configState?.heSoOtChuNhat ?: 2.0
                    val otLe = configState?.heSoOtNgayLe ?: 3.0

                    Text(
                        text = "HÔM NAY: $dayName",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Big digital clock
                    Text(
                        text = liveTimeString.ifEmpty { "00:00:00" },
                        color = NeonBlue,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    // Active Work Session Title
                    Text(
                        text = if (isWorking) "Đang trong ca làm việc" else "Chưa nhận ca làm việc",
                        color = if (isWorking) AccentOrange else MediumGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Fingerprint rounded fingerprint button
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(buttonColor.copy(alpha = 0.2f), buttonColor.copy(alpha = 0.4f), buttonColor)
                                )
                            )
                            .clickable {
                                viewModel.toggleCheckIn(quickNoteText)
                            }
                            .testTag("in_out_action_button")
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Fingerprint verify",
                                tint = White,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isWorking) "RA CA" else "VÀO CA",
                                color = White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Dinamic Running/Ticker Text inside ca
                    if (isWorking) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (runningTimeText.isNotEmpty()) runningTimeText else "Vừa nhận ca",
                                color = AccentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val active = activeEntry
                    if (isWorking && active != null && active.checkInTime != null) {
                        val lcb = configState?.luongCoBan ?: 6000000.0
                        
                        // Parse selected standard work days to use accurate daily & hourly rate denominator
                        var standardWorkDays = 26
                        try {
                            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                            val currentMonthStr = sdfMonth.format(Date())
                            val parts = currentMonthStr.split("-")
                            val yr = parts[0].toInt()
                            val mo = parts[1].toInt()
                            
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.YEAR, yr)
                            cal.set(Calendar.MONTH, mo - 1)
                            val maxDaysInMo = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            
                            var totalSundays = 0
                            for (day in 1..maxDaysInMo) {
                                cal.set(Calendar.DAY_OF_MONTH, day)
                                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                                    totalSundays++
                                }
                            }
                            standardWorkDays = maxDaysInMo - totalSundays
                            if (maxDaysInMo == 31) {
                                standardWorkDays = 27
                            }
                        } catch (e: Exception) {}

                        val dailyRate = if (standardWorkDays == 27) {
                            lcb / 26.0
                        } else {
                            lcb / standardWorkDays.toDouble()
                        }
                        val hrRate = dailyRate / 8.0
                        
                        val checkInVal = active.checkInTime
                        val elapsedMs = remember(liveTimeString) { System.currentTimeMillis() - checkInVal }
                        val elapsedHours = (elapsedMs.coerceAtLeast(0L)) / 3600000.0
                        
                        val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        var dt = "NORMAL"
                        try {
                            val parsedDate = dateParser.parse(active.date)
                            if (parsedDate != null) {
                                val c = Calendar.getInstance()
                                c.time = parsedDate
                                if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                                    dt = "SUNDAY"
                                } else {
                                    val parts = active.date.split("-")
                                    if (parts.size >= 3) {
                                        val md = "${parts[1]}-${parts[2]}"
                                        if (md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02") {
                                            dt = "HOLIDAY"
                                        }
                                    }
                                }
                            }
                        } catch(e: Exception) {}

                        val multi = when (dt) {
                            "SUNDAY" -> configState?.heSoOtChuNhat ?: 2.0
                            "HOLIDAY" -> configState?.heSoOtNgayLe ?: 3.0
                            else -> configState?.heSoOtNgayThuong ?: 1.5
                        }

                        val breakHours = if (configState?.tinhKhauTruNghi == true) (configState?.soGioNghiGiaiLao ?: 1.5) else 0.0
                        val actualElapsedHours = (elapsedHours - breakHours).coerceAtLeast(0.0)

                        val earnings = if (actualElapsedHours <= 8.0) {
                            actualElapsedHours * hrRate
                        } else {
                            val stdPay = 8.0 * hrRate
                            val otPay = (actualElapsedHours - 8.0) * hrRate * multi
                            stdPay + otPay
                        }

                        val decFmt = DecimalFormat("#,###")
                        val formattedEarned = decFmt.format(earnings) + " đ"

                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "ƯỚC TÍNH SỐ TIỀN ĐÃ LÀM ĐƯỢC CA NÀY",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formattedEarned,
                                    color = AccentGreen,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                                val hoursStr = String.format(Locale.US, "%.2f giờ", actualElapsedHours)
                                val breakLabel = if (configState?.tinhKhauTruNghi == true) " (đã trừ ${configState?.soGioNghiGiaiLao}g nghỉ)" else ""
                                Text(
                                    text = "Thực làm: $hoursStr$breakLabel" + if (actualElapsedHours > 8.0) " (Đang tăng ca OT x$multi)" else "",
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GHI CHÚ NHANH CA LÀM
                    OutlinedTextField(
                        value = quickNoteText,
                        onValueChange = { 
                            quickNoteText = it
                            if (isWorking) {
                                viewModel.updateActiveEntryNote(it)
                            }
                        },
                        label = { Text("Ghi chú nhanh ca làm...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = Color(0xFF2C2C2C)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // CARD 3: 7-DAY BAR CHART
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timeline, "Chart", tint = NeonBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BIỂU ĐỒ HOẠT ĐỘNG 7 NGÀY GẦN NHẤT",
                            color = White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val last7DaysData = remember(recentEntries) {
                        calculateRecent7Days(recentEntries)
                    }

                    TimeSnap7DayBarChart(data = last7DaysData)

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // CARD 4: CHẤM CÔNG GẦN ĐÂY (SHOWING EXACTLY 3 days descending, real-time updates, no deletes)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, "Recent history logs", tint = NeonBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHẤM CÔNG GẦN ĐÂY",
                            color = White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                    val sortedLogs = remember(recentEntries, todayStr) {
                        recentEntries
                            .filter { it.date <= todayStr }
                            .sortedByDescending { it.date }
                            .take(3)
                    }

                    if (sortedLogs.isEmpty()) {
                        Text(
                            text = "Chưa có dữ liệu chấm công gần đây.",
                            color = MediumGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        sortedLogs.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val formattedDate = remember(entry.date) {
                                        try {
                                            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val dateVal = parser.parse(entry.date)
                                            if (dateVal != null) {
                                                val formatter = SimpleDateFormat("EEEE, dd/MM", Locale("vi", "VN"))
                                                formatter.format(dateVal).replaceFirstChar { it.uppercase() }
                                            } else {
                                                entry.date
                                            }
                                        } catch (e: Exception) {
                                            entry.date
                                        }
                                    }

                                    Text(
                                        text = formattedDate,
                                        color = White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    val statusLabel = when (entry.dayType) {
                                        "SUNDAY" -> "Chủ Nhật"
                                        "HOLIDAY" -> "Ngày Lễ"
                                        "PAID_LEAVE" -> "Nghỉ phép (Có lương)"
                                        "UNPAID_LEAVE" -> "Nghỉ không lương"
                                        else -> "Ngày thường"
                                    }
                                    Text(
                                        text = statusLabel + (if (!entry.note.isNullOrEmpty()) " • ${entry.note}" else ""),
                                        color = when(entry.dayType) {
                                            "SUNDAY", "HOLIDAY" -> AccentRed
                                            "PAID_LEAVE" -> NeonBlue
                                            "UNPAID_LEAVE" -> AccentOrange
                                            else -> LightGray
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    val earnings = remember(entry, configState) {
                                        calculateDayEarnings(entry, configState)
                                    }
                                    val formattedEarnings = DecimalFormat("#,###").format(earnings)
                                    val displayDateStr = remember(entry.date) {
                                        try {
                                            val p = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            val d = p.parse(entry.date)
                                            if (d != null) {
                                                SimpleDateFormat("dd/M", Locale.getDefault()).format(d)
                                            } else {
                                                entry.date
                                            }
                                        } catch (e: Exception) {
                                            entry.date
                                        }
                                    }
                                    Text(
                                        text = "Ngày $displayDateStr: $formattedEarnings VNĐ",
                                        color = AccentGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    if (entry.dayType == "PAID_LEAVE" || entry.dayType == "UNPAID_LEAVE") {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if(entry.dayType == "PAID_LEAVE") NeonBlue.copy(alpha = 0.15f) else AccentOrange.copy(alpha = 0.15f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if(entry.dayType == "PAID_LEAVE") "PHÉP" else "VẮNG",
                                                color = if(entry.dayType == "PAID_LEAVE") NeonBlue else AccentOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (entry.isWorking) {
                                        val isNightShift = remember(entry) {
                                            if (entry.checkInTime != null) {
                                                val inCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime }
                                                val inHour = inCal.get(Calendar.HOUR_OF_DAY)
                                                val inMin = inCal.get(Calendar.MINUTE)
                                                val inTotalMin = inHour * 60 + inMin
                                                (inTotalMin in (18 * 60)..(19 * 60 + 30)) || inHour >= 22 || inHour <= 6 || entry.dayType == "NIGHT"
                                            } else entry.dayType == "NIGHT"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(if (isNightShift) NightPurple.copy(alpha = 0.15f) else AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isNightShift) "CA ĐÊM" else "ĐANG LÀM",
                                                color = if (isNightShift) NightPurple else AccentOrange,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else if (entry.checkInTime != null && entry.checkOutTime != null) {
                                        val isNightShift = remember(entry) {
                                            val inCal = Calendar.getInstance().apply { timeInMillis = entry.checkInTime }
                                            val inHour = inCal.get(Calendar.HOUR_OF_DAY)
                                            val inMin = inCal.get(Calendar.MINUTE)
                                            val inTotalMin = inHour * 60 + inMin
                                            (inTotalMin in (18 * 60)..(19 * 60 + 30)) || inHour >= 22 || inHour <= 6 || entry.dayType == "NIGHT"
                                        }
                                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        val inStr = timeFormat.format(Date(entry.checkInTime))
                                        val outStr = timeFormat.format(Date(entry.checkOutTime))

                                        Text(
                                            text = "$inStr - $outStr",
                                            color = if (isNightShift) NightPurple else AccentGreen,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        val processedEntry = remember(entry) {
                                            com.example.data.SalaryCalculator.calculateSingleEntry(entry)
                                        }
                                        val shift = remember(entry) {
                                            com.example.data.SalaryCalculator.getShiftForEntry(entry)
                                        }
                                        val stdHrs = processedEntry.workDay * shift.standardHours
                                        val otHrs = processedEntry.otHours
                                        Text(
                                            text = "${processedEntry.workDay} công • Giờ: ${DecimalFormat("#.#").format(stdHrs)}h" + 
                                                    (if (otHrs > 0) " • OT: ${DecimalFormat("#.#").format(otHrs)}h" else ""),
                                            color = if (isNightShift) NightPurple else LightGray,
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        Text(
                                            text = "Không xác định",
                                            color = MediumGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            if (index < sortedLogs.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFF2C2C2C))
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(115.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subValue,
                    color = MediumGray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class DayChartPoint(
    val label: String,
    val hours: Double,
    val dateStr: String
)

@Composable
fun TimeSnap7DayBarChart(data: List<DayChartPoint>) {
    val barColor = NeonBlue
    val gridLineColor = Color(0xFF2C2C2C)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val bottomPadding = 30f
            val topPadding = 20f
            val leftPadding = 40f
            val rightPadding = 20f

            val graphWidth = canvasWidth - leftPadding - rightPadding
            val graphHeight = canvasHeight - topPadding - bottomPadding

            val maxHours = 12.0

            drawLine(
                color = gridLineColor,
                start = Offset(leftPadding, canvasHeight - bottomPadding),
                end = Offset(canvasWidth - rightPadding, canvasHeight - bottomPadding),
                strokeWidth = 2f
            )

            val middleY1 = canvasHeight - bottomPadding - (graphHeight * 0.5f)
            drawLine(
                color = gridLineColor.copy(alpha = 0.5f),
                start = Offset(leftPadding, middleY1),
                end = Offset(canvasWidth - rightPadding, middleY1),
                strokeWidth = 1f
            )

            val barCount = data.size
            if (barCount > 0) {
                val columnWidth = graphWidth / barCount
                val barMaxWidth = columnWidth * 0.5f

                for (i in 0 until barCount) {
                    val pt = data[i]

                    val rawHours = pt.hours.coerceAtLeast(0.0)
                    val percent = (rawHours / maxHours).coerceAtMost(1.0).toFloat()

                    val barHeight = graphHeight * percent
                    val xCenter = leftPadding + (i * columnWidth) + (columnWidth / 2f)
                    val xLeft = xCenter - (barMaxWidth / 2f)
                    val yTop = canvasHeight - bottomPadding - barHeight

                    if (barHeight > 2f) {
                        drawRoundRect(
                            color = if (pt.hours > 8.0) AccentGreen else barColor,
                            topLeft = Offset(xLeft, yTop),
                            size = Size(barMaxWidth, barHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = android.text.TextPaint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(
                            pt.label,
                            xCenter,
                            canvasHeight - 5f,
                            textPaint
                        )

                        if (pt.hours > 0.0) {
                            val hrsText = if (pt.hours % 1.0 == 0.0) "${pt.hours.toInt()}h" else "${DecimalFormat("#.#").format(pt.hours)}h"
                            val valPaint = android.text.TextPaint().apply {
                                color = if (pt.hours > 8.0) android.graphics.Color.parseColor("#27AE60") else android.graphics.Color.parseColor("#2F80ED")
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText(
                                hrsText,
                                xCenter,
                                yTop - 6f,
                                valPaint
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateRecent7Days(entries: List<TimeEntry>): List<DayChartPoint> {
    val cal = Calendar.getInstance()
    val sdfValue = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDay = SimpleDateFormat("EEE", Locale("vi", "VN"))

    val dates = ArrayList<Pair<String, String>>()
    for (i in 6 downTo 0) {
        val clone = cal.clone() as Calendar
        clone.add(Calendar.DAY_OF_YEAR, -i)
        val dateStr = sdfValue.format(clone.time)
        var label = sdfDay.format(clone.time)
            .replace("Thứ ", "T")
            .replace("Chủ nhật", "CN")
            .replace("Chủ Nhật", "CN")
        dates.add(Pair(label, dateStr))
    }

    return dates.map { (label, dateStr) ->
        val entity = entries.find { it.date == dateStr }
        val hours = if (entity != null && entity.checkInTime != null && entity.checkOutTime != null) {
            val elapsed = entity.checkOutTime - entity.checkInTime
            val actual = elapsed / 3600000.0
            
            if (actual <= 8.5) {
                actual.coerceAtMost(8.0)
            } else {
                actual
            }
        } else {
            0.0
        }
        DayChartPoint(label, hours, dateStr)
    }
}

private fun calculateDayEarnings(entry: TimeEntry, config: com.example.data.model.UserConfig?): Double {
    if (config == null) return 0.0
    val processed = com.example.data.SalaryCalculator.calculateSingleEntry(entry)
    val hourlySalary = config.luongCoBan / 26.0 / 8.0
    
    if (processed.dayType == "PAID_LEAVE" || processed.dayType == "HOLIDAY_LEAVE") {
        return config.luongCoBan / 26.0
    }
    if (processed.dayType == "UNPAID_LEAVE" || processed.checkInTime == null) {
        return 0.0
    }
    
    val inCal = Calendar.getInstance()
    inCal.timeInMillis = processed.checkInTime!!
    val inHour = inCal.get(Calendar.HOUR_OF_DAY)
    val inMin = inCal.get(Calendar.MINUTE)
    val inTotalMin = inHour * 60 + inMin
    val isNightShift = (inTotalMin in (18 * 60)..(19 * 60 + 30)) || 
                       inHour >= 22 || inHour <= 6 || 
                       processed.dayType == "NIGHT"

    val isSunday = processed.dayType == "SUNDAY" || 
                   (run {
                       try {
                           val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                           val dateVal = parser.parse(processed.date)
                           if (dateVal != null) {
                               val cal = Calendar.getInstance()
                               cal.time = dateVal
                               cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                           } else {
                               false
                           }
                       } catch (e: Exception) {
                           false
                       }
                   } && processed.dayType == "NIGHT")

    val isHoliday = com.example.data.SalaryCalculator.isHoliday(processed.date)

    if (processed.checkOutTime == null) {
        if (isSunday) {
            return 8.0 * hourlySalary * config.heSoOtChuNhat
        } else {
            return config.luongCoBan / 26.0
        }
    }

    val finalCheckIn = processed.normalizedCheckIn ?: processed.checkInTime!!
    val finalCheckOut = processed.normalizedCheckOut ?: processed.checkOutTime!!
    val durationMs = (finalCheckOut - finalCheckIn).coerceAtLeast(0L)
    val rawHours = durationMs / 3600000.0
    val breakHours = if (config.tinhKhauTruNghi) config.soGioNghiGiaiLao else 0.0
    val actualHours = (rawHours - breakHours).coerceAtLeast(0.0)

    val finalStandardHours = processed.workDay * 8.0
    val finalOtHours = processed.otHours

    var earned = 0.0
    if (isSunday) {
        earned += actualHours * hourlySalary * config.heSoOtChuNhat
    } else if (isHoliday) {
        earned += actualHours * hourlySalary * config.heSoOtNgayLe
    } else {
        earned += finalStandardHours * hourlySalary
        if (finalOtHours > 0.0) {
            earned += finalOtHours * hourlySalary * config.heSoOtNgayThuong
        }
    }

    if (actualHours >= 4.0) {
        earned += config.pcComCa
    }

    if (finalOtHours >= 2.0) {
        earned += config.pcComCa
    }

    if (isNightShift) {
        earned += 100000.0
    }

    return earned
}
