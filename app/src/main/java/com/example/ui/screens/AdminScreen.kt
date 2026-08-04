package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserConfig
import com.example.data.db.AppDatabase
import com.example.data.AttendanceRecord
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel
import com.example.util.ThousandSeparatorVisualTransformation
import com.example.util.ExportUtils
import com.example.util.toTimeEntry
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val adminViewModel: AdminViewModel = viewModel()
    val employees by adminViewModel.employees.collectAsStateWithLifecycle()
    val isLoading by adminViewModel.isLoading.collectAsStateWithLifecycle()
    val selectedEmployee by adminViewModel.selectedEmployee.collectAsStateWithLifecycle()
    val selectedIds by adminViewModel.selectedEmployeeIds.collectAsStateWithLifecycle()
    val isExportingByVM by adminViewModel.isExporting.collectAsStateWithLifecycle()
    val exportProgressByVM by adminViewModel.exportProgress.collectAsStateWithLifecycle()
    
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showBatchExportDialog by remember { mutableStateOf(false) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showSingleDeleteConfirm by remember { mutableStateOf(false) }
    var showSendNotifDialog by remember { mutableStateOf(false) }
    var showShiftManagementDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var longClickedEmployee by remember { mutableStateOf<UserConfig?>(null) }

    val exportSuccessCount by adminViewModel.exportSuccessCount.collectAsStateWithLifecycle()
    val todayAttendanceMap by adminViewModel.todayAttendanceMap.collectAsStateWithLifecycle()
    val isExportingSingle by adminViewModel.isExportingSingle.collectAsStateWithLifecycle()

    BackHandler {
        if (selectedIds.isNotEmpty()) {
            adminViewModel.clearSelection()
        } else if (selectedEmployee != null) {
            adminViewModel.selectEmployee(null)
        } else {
            onBack()
        }
    }

    LaunchedEffect(isExportingByVM) {
        if (!isExportingByVM && showBatchExportDialog) {
            if (employees.isNotEmpty()) {
                android.widget.Toast.makeText(context, "Đã xuất thành công $exportSuccessCount/${employees.size} phiếu lương vào thư mục Download/TimeSnapPro", android.widget.Toast.LENGTH_LONG).show()
            }
            showBatchExportDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectedIds.isNotEmpty()) {
                        Text("${selectedIds.size}/${employees.size} đã chọn", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else {
                        Text("Quản Trị Hệ Thống", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { adminViewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection", tint = White)
                        }
                    } else if (selectedEmployee != null) {
                        IconButton(onClick = { adminViewModel.selectEmployee(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to list", tint = White)
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                        }
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedIds.size == employees.size) {
                                    adminViewModel.clearSelection()
                                } else {
                                    adminViewModel.selectAllEmployees(employees.map { it.userId })
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedIds.size == employees.size) "Bỏ chọn" else "Chọn tất cả",
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = { showBatchEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Batch Edit", tint = White)
                        }
                        IconButton(onClick = { showBatchDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Batch Delete", tint = AccentOrange)
                        }
                    } else {
                        if (selectedEmployee != null) {
                            IconButton(
                                onClick = {
                                    adminViewModel.exportSingleEmployeePayslip(context, selectedEmployee!!)
                                }
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Xuất phiếu lương cá nhân", tint = NeonBlue)
                            }
                        } else {
                            IconButton(onClick = { showShiftManagementDialog = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Cấu hình ca làm việc", tint = NeonBlue)
                            }
                            IconButton(onClick = { showSendNotifDialog = true }) {
                                Icon(Icons.Default.Campaign, contentDescription = "Gửi thông báo", tint = White)
                            }
                            IconButton(onClick = { showBatchExportDialog = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Xuất hàng loạt", tint = White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (selectedEmployee == null && selectedIds.isEmpty()) {
                FloatingActionButton(
                    onClick = { showAddEmployeeDialog = true },
                    containerColor = NeonBlue,
                    contentColor = White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Employee")
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { adminViewModel.loadEmployees() },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isExportingSingle) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = NeonBlue)
            } else {
                if (selectedEmployee != null) {
                    EmployeeDetailView(
                        employee = selectedEmployee!!,
                        records = adminViewModel.attendanceRecords.collectAsStateWithLifecycle().value,
                        onBack = { adminViewModel.selectEmployee(null) },
                        onClose = { adminViewModel.loadEmployees() },
                        adminViewModel = adminViewModel,
                        onDeleteRequest = { showSingleDeleteConfirm = true }
                    )
                } else {
                    EmployeeListView(
                        employees = employees.filter { it.hoVaTen.contains(searchQuery, ignoreCase = true) || it.maNhanVien.contains(searchQuery, ignoreCase = true) }.sortedWith(compareByDescending<UserConfig> { it.isAdmin }.thenBy { it.hoVaTen }),
                        selectedIds = selectedIds,
                        todayAttendanceMap = todayAttendanceMap,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onEmployeeClick = { emp ->
                            if (selectedIds.isNotEmpty()) {
                                adminViewModel.toggleEmployeeSelection(emp.userId)
                            } else {
                                adminViewModel.selectEmployee(emp)
                            }
                        },
                        onEmployeeLongClick = { emp ->
                            longClickedEmployee = emp
                        }
                    )
                }
            }
        }
    }

    if (longClickedEmployee != null) {
        AlertDialog(
            onDismissRequest = { longClickedEmployee = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = NeonBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tùy chọn nhân viên", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${longClickedEmployee?.hoVaTen} (${longClickedEmployee?.maNhanVien})",
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text("Chọn chế độ chọn hoặc chấm công:", color = Color.Gray, fontSize = 12.sp)

                    // Option 1: Select All Employees
                    Button(
                        onClick = {
                            val allList = employees.map { it.userId }
                            adminViewModel.selectAllEmployees(allList)
                            longClickedEmployee = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkContainer),
                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.SelectAll, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Chọn tất cả nhân viên (${employees.size} NV)", color = White, fontSize = 13.sp)
                        }
                    }

                    // Option 2: Toggle select this individual employee
                    val isThisSelected = selectedIds.contains(longClickedEmployee?.userId)
                    Button(
                        onClick = {
                            longClickedEmployee?.let { adminViewModel.toggleEmployeeSelection(it.userId) }
                            longClickedEmployee = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkContainer),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                if (isThisSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (isThisSelected) AccentOrange else White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isThisSelected) "Bỏ chọn nhân viên này" else "Chọn duy nhất / Thêm NV này",
                                color = White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Option 3: Quick Batch Attendance Dialog
                    Button(
                        onClick = {
                            val emp = longClickedEmployee
                            if (emp != null && !selectedIds.contains(emp.userId)) {
                                adminViewModel.toggleEmployeeSelection(emp.userId)
                            }
                            longClickedEmployee = null
                            showBatchEditDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, NeonBlue)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Chấm công & Sửa hàng loạt ngay", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { longClickedEmployee = null }) {
                    Text("Đóng", color = Color.Gray)
                }
            },
            containerColor = DarkContainer
        )
    }

    if (showBatchEditDialog) {
        var selectedMode by remember { mutableStateOf(0) } // 0: Vào ca / Thêm ca, 1: Ra ca hàng loạt, 2: Sửa Lương

        var batchLcb by remember { mutableStateOf("") }
        var batchPcXangXe by remember { mutableStateOf("") }
        var batchChuyenCan by remember { mutableStateOf("") }

        val todayDdMmYyyy = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
        var batchDateInput by remember { mutableStateOf(todayDdMmYyyy.replace("/", "")) }
        var batchClockIn by remember { mutableStateOf("07:30") }
        var batchClockOut by remember { mutableStateOf("19:30") }
        var batchNote by remember { mutableStateOf("Admin chấm công hàng loạt") }
        var activeShiftPreset by remember { mutableStateOf("7:30-19:30") }

        AlertDialog(
            onDismissRequest = { showBatchEditDialog = false },
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chấm công hàng loạt (${selectedIds.size} NV)", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == 0,
                            onClick = { selectedMode = 0 },
                            label = { Text("Vào / Thêm ca", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue.copy(alpha = 0.3f),
                                selectedLabelColor = NeonBlue
                            )
                        )
                        FilterChip(
                            selected = selectedMode == 1,
                            onClick = { 
                                selectedMode = 1
                                if (batchNote == "Admin chấm công hàng loạt") {
                                    batchNote = "Admin ra ca hàng loạt"
                                }
                            },
                            label = { Text("Ra ca hàng loạt", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentOrange.copy(alpha = 0.3f),
                                selectedLabelColor = AccentOrange
                            )
                        )
                        FilterChip(
                            selected = selectedMode == 2,
                            onClick = { selectedMode = 2 },
                            label = { Text("Sửa Lương", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.3f),
                                selectedLabelColor = Color(0xFF00E676)
                            )
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedMode) {
                        0 -> {
                            // MODE 0: VÀO / THÊM CA HÀNG LOẠT
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Chọn mẫu ca chuẩn:", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilterChip(
                                                selected = activeShiftPreset == "7:30-19:30",
                                                onClick = {
                                                    activeShiftPreset = "7:30-19:30"
                                                    batchClockIn = "07:30"
                                                    batchClockOut = "19:30"
                                                },
                                                label = { Text("7:30 - 19:30 (Ca ngày)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = NeonBlue.copy(alpha = 0.25f),
                                                    selectedLabelColor = NeonBlue
                                                )
                                            )
                                            FilterChip(
                                                selected = activeShiftPreset == "7:30-20:00",
                                                onClick = {
                                                    activeShiftPreset = "7:30-20:00"
                                                    batchClockIn = "07:30"
                                                    batchClockOut = "20:00"
                                                },
                                                label = { Text("7:30 - 20:00 (Ca ngày)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AccentOrange.copy(alpha = 0.25f),
                                                    selectedLabelColor = AccentOrange
                                                )
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilterChip(
                                                selected = activeShiftPreset == "19:30-07:30",
                                                onClick = {
                                                    activeShiftPreset = "19:30-07:30"
                                                    batchClockIn = "19:30"
                                                    batchClockOut = "07:30"
                                                },
                                                label = { Text("19:30 - 07:30 (Ca đêm)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFAB47BC).copy(alpha = 0.25f),
                                                    selectedLabelColor = Color(0xFFAB47BC)
                                                )
                                            )
                                            FilterChip(
                                                selected = activeShiftPreset == "7:30-15:30",
                                                onClick = {
                                                    activeShiftPreset = "7:30-15:30"
                                                    batchClockIn = "07:30"
                                                    batchClockOut = "15:30"
                                                },
                                                label = { Text("7:30 - 15:30 (Hành chính)", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.25f),
                                                    selectedLabelColor = Color(0xFF00E676)
                                                )
                                            )
                                        }
                                        FilterChip(
                                            selected = activeShiftPreset == "Đang làm",
                                            onClick = {
                                                activeShiftPreset = "Đang làm"
                                                batchClockIn = "07:30"
                                                batchClockOut = ""
                                            },
                                            label = { Text("Đang làm (Để trống giờ ra)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFFD54F).copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFFFFD54F)
                                            )
                                        )
                                    }

                                    AdminInputField(
                                        label = "Ngày chấm công (DD/MM/YYYY)",
                                        value = batchDateInput,
                                        onValueChange = { input ->
                                            val clean = input.filter { it.isDigit() }.take(8)
                                            batchDateInput = clean
                                        },
                                        keyboardType = KeyboardType.Number,
                                        visualTransformation = DateVisualTransformation()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            AdminInputField(
                                                label = "Giờ vào ca",
                                                value = batchClockIn,
                                                onValueChange = { batchClockIn = autoFormatTimeInput(it) },
                                                keyboardType = KeyboardType.Number
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            AdminInputField(
                                                label = "Giờ ra ca (Trống = đang làm)",
                                                value = batchClockOut,
                                                onValueChange = { batchClockOut = autoFormatTimeInput(it) },
                                                keyboardType = KeyboardType.Number
                                            )
                                        }
                                    }

                                    if (batchClockOut.isEmpty()) {
                                        Text(
                                            "💡 Để trống giờ ra = Nhân viên đang trong ca làm (chưa ra ca).",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 11.sp
                                        )
                                    }

                                    AdminInputField(
                                        label = "Ghi chú chấm công",
                                        value = batchNote,
                                        onValueChange = { batchNote = it }
                                    )

                                    Text(
                                        "⚡ Tự động nhận diện giờ khi gõ số (VD: gõ 0730 -> 07:30, 1930 -> 19:30, 2000 -> 20:00)",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        1 -> {
                            // MODE 1: RA CA HÀNG LOẠT
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Cập nhật giờ RA CA hàng loạt cho NV đang làm:", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    Text("Mẫu giờ ra ca nhanh:", color = Color.Gray, fontSize = 11.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilterChip(
                                            selected = batchClockOut == "19:30",
                                            onClick = { batchClockOut = "19:30" },
                                            label = { Text("19:30", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                        FilterChip(
                                            selected = batchClockOut == "20:00",
                                            onClick = { batchClockOut = "20:00" },
                                            label = { Text("20:00", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                        FilterChip(
                                            selected = batchClockOut == "07:30",
                                            onClick = { batchClockOut = "07:30" },
                                            label = { Text("07:30", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                        FilterChip(
                                            selected = batchClockOut == "17:00",
                                            onClick = { batchClockOut = "17:00" },
                                            label = { Text("17:00", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.3f), selectedLabelColor = AccentOrange)
                                        )
                                    }

                                    AdminInputField(
                                        label = "Ngày ra ca (DD/MM/YYYY)",
                                        value = batchDateInput,
                                        onValueChange = { input ->
                                            val clean = input.filter { it.isDigit() }.take(8)
                                            batchDateInput = clean
                                        },
                                        keyboardType = KeyboardType.Number,
                                        visualTransformation = DateVisualTransformation()
                                    )

                                    AdminInputField(
                                        label = "Giờ ra ca thực tế",
                                        value = batchClockOut,
                                        onValueChange = { batchClockOut = autoFormatTimeInput(it) },
                                        keyboardType = KeyboardType.Number
                                    )

                                    AdminInputField(
                                        label = "Ghi chú ra ca",
                                        value = batchNote,
                                        onValueChange = { batchNote = it }
                                    )

                                    Text(
                                        "⚡ Sẽ cập nhật giờ ra ca cho tất cả ${selectedIds.size} NV đã chọn trong ngày này.",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        2 -> {
                            // MODE 2: CẤU HÌNH LƯƠNG HÀNG LOẠT
                            Surface(
                                color = Color.White.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Cập nhật lương & Phụ cấp hàng loạt:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    AdminInputField("Lương Cơ Bản mới", batchLcb, onValueChange = { batchLcb = it }, isNumeric = true)
                                    AdminInputField("Phụ cấp xăng xe mới", batchPcXangXe, onValueChange = { batchPcXangXe = it }, isNumeric = true)
                                    AdminInputField("Tiền chuyên cần mới", batchChuyenCan, onValueChange = { batchChuyenCan = it }, isNumeric = true)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formattedDateStr = if (batchDateInput.length == 8) {
                            val d = batchDateInput.substring(0, 2)
                            val m = batchDateInput.substring(2, 4)
                            val y = batchDateInput.substring(4, 8)
                            "$d/$m/$y"
                        } else {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        }

                        when (selectedMode) {
                            0 -> {
                                adminViewModel.batchAddAttendance(formattedDateStr, batchClockIn.trim(), batchClockOut.trim(), batchNote.trim())
                            }
                            1 -> {
                                adminViewModel.batchCheckout(formattedDateStr, batchClockOut.trim(), batchNote.trim())
                            }
                            2 -> {
                                if (batchLcb.isNotEmpty() || batchPcXangXe.isNotEmpty() || batchChuyenCan.isNotEmpty()) {
                                    adminViewModel.batchUpdateSalaryConfig { emp ->
                                        emp.copy(
                                            luongCoBan = batchLcb.toDoubleOrNull() ?: emp.luongCoBan,
                                            pcXangXe = batchPcXangXe.toDoubleOrNull() ?: emp.pcXangXe,
                                            tienChuyenCanGoc = batchChuyenCan.toDoubleOrNull() ?: emp.tienChuyenCanGoc
                                        )
                                    }
                                }
                            }
                        }
                        showBatchEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedMode) {
                            1 -> AccentOrange
                            2 -> Color(0xFF00E676)
                            else -> NeonBlue
                        }
                    )
                ) {
                    Text(
                        text = when (selectedMode) {
                            1 -> "Xác nhận Ra Ca (${selectedIds.size} NV)"
                            2 -> "Cập nhật Lương (${selectedIds.size} NV)"
                            else -> "Chấm Công (${selectedIds.size} NV)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchEditDialog = false }) { Text("Hủy", color = Color.Gray) }
            },
            containerColor = DarkContainer
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Xác nhận xóa", color = White) },
            text = { Text("Bạn có chắc chắn muốn xóa ${selectedIds.size} nhân viên đã chọn? Hành động này không thể hoàn tác.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.batchDeleteEmployees()
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Xóa tất cả")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Hủy") }
            },
            containerColor = DarkContainer
        )
    }

    if (showSingleDeleteConfirm && selectedEmployee != null) {
        AlertDialog(
            onDismissRequest = { showSingleDeleteConfirm = false },
            title = { Text("Xác nhận xóa", color = White) },
            text = { Text("Bạn có chắc chắn muốn xóa nhân viên '${selectedEmployee?.hoVaTen}'? Tất cả dữ liệu chấm công và cấu hình sẽ bị mất.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteEmployee(selectedEmployee!!.userId)
                        showSingleDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDeleteConfirm = false }) { Text("Hủy") }
            },
            containerColor = DarkContainer
        )
    }

    if (showSendNotifDialog) {
        SendAdminNotificationDialog(
            employees = employees,
            onDismiss = { showSendNotifDialog = false },
            onSend = { targetUid, targetName, title, message, type ->
                adminViewModel.sendNotificationToEmployee(
                    targetUid = targetUid,
                    targetName = targetName,
                    title = title,
                    message = message,
                    type = type
                ) { success ->
                    if (success) {
                        android.widget.Toast.makeText(
                            context,
                            "Đã gửi thông báo",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "❌ Lỗi gửi thông báo. Vui lòng kiểm tra kết nối mạng.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showSendNotifDialog = false
                }
            }
        )
    }

    if (showShiftManagementDialog) {
        val db = AppDatabase.getInstance(context)
        val shiftRepo = remember { com.example.data.repository.ShiftRepository(db.shiftDao()) }
        ShiftManagementDialog(
            shiftRepository = shiftRepo,
            companyId = "default_company",
            onDismiss = { showShiftManagementDialog = false }
        )
    }

    if (showAddEmployeeDialog) {
        var newName by remember { mutableStateOf("") }
        var newMsnv by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Thêm Nhân Viên", color = White) },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Họ và Tên") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                    )
                    OutlinedTextField(
                        value = newMsnv,
                        onValueChange = { newMsnv = it },
                        label = { Text("Mã Số Nhân Viên") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                    )
                }
            },
            confirmButton = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                Button(onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    val uid = "offline_" + UUID.randomUUID().toString().take(8)
                    adminViewModel.saveEmployeeConfig(UserConfig(userId = uid, hoVaTen = newName, maNhanVien = newMsnv))
                    showAddEmployeeDialog = false
                }) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) { Text("Hủy") }
            },
            containerColor = DarkContainer
        )
    }

    if (showBatchExportDialog) {
        val defaultMonthStr = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
        var exportInput by remember { mutableStateOf(defaultMonthStr) }

        val normalizedYymm = remember(exportInput) { normalizeMonthYearInput(exportInput) }
        val displayMonthLabel = remember(normalizedYymm) {
            try {
                val parts = normalizedYymm.split("-")
                "Tháng ${parts[1]}/${parts[0]}"
            } catch (e: Exception) {
                "Tháng $normalizedYymm"
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isExportingByVM) showBatchExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xuất Phiếu Lương Hàng Loạt", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current

                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isExportingByVM) {
                        Text("Đang tổng hợp và xuất dữ liệu phiếu lương... ${(exportProgressByVM * 100).toInt()}%", color = White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { exportProgressByVM },
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonBlue,
                            trackColor = DarkBackground
                        )
                        Text("Vui lòng chờ trong giây lát...", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        Text("Nhập tháng và năm cần xuất phiếu lương:", color = White, fontSize = 13.sp)

                        OutlinedTextField(
                            value = exportInput,
                            onValueChange = { exportInput = it },
                            label = { Text("Nhập Tháng/Năm (VD: 08/2026, 8-2026, 082026)") },
                            placeholder = { Text("08/2026") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        // Smart Auto-Detection Indicator Card
                        Surface(
                            color = NeonBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Hệ thống tự động nhận diện:", color = Color.LightGray, fontSize = 11.sp)
                                    Text("$displayMonthLabel (Mã: $normalizedYymm)", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        // Quick selection buttons
                        Text("Chọn nhanh tháng:", color = Color.Gray, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    exportInput = String.format(Locale.US, "%02d/%04d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                                },
                                label = { Text("Tháng này", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonBlue.copy(alpha = 0.2f), selectedLabelColor = NeonBlue)
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, -1)
                                    exportInput = String.format(Locale.US, "%02d/%04d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                                },
                                label = { Text("Tháng trước", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentOrange.copy(alpha = 0.2f), selectedLabelColor = AccentOrange)
                            )
                        }

                        Text("⚡ Bạn có thể nhập số liền nhau (VD: 082026), cách nhau gạch (8-2026), xược (8/2026), hoặc gõ chữ (Tháng 8 năm 2026) hệ thống đều tự động nhận dạng chính xác.", color = Color.Gray, fontSize = 10.sp)

                        Text("Hệ thống sẽ tổng hợp và xuất phiếu lương cho toàn bộ ${employees.size} nhân viên dưới dạng hình ảnh (.PNG) lưu vào thư mục Download/TimeSnapPro.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                if (!isExportingByVM) {
                    Button(
                        onClick = { adminViewModel.performBatchExport(context, normalizedYymm) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Text("Xuất $displayMonthLabel (${employees.size} NV)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isExportingByVM) {
                    TextButton(onClick = { showBatchExportDialog = false }) { Text("Đóng", color = Color.Gray) }
                }
            },
            containerColor = DarkContainer
        )
    }
}

data class ShiftStatusInfo(
    val label: String,
    val color: Color,
    val timeDetail: String = ""
)

fun getEmployeeShiftStatus(rec: AttendanceRecord?): ShiftStatusInfo {
    if (rec == null) {
        return ShiftStatusInfo("Chưa vào ca", Color(0xFF8F9BB3), "")
    }
    val statusLower = rec.status.lowercase()
    if (statusLower.contains("phep") || statusLower.contains("leave")) {
        return ShiftStatusInfo("Nghỉ phép", Color(0xFFFFB74D), "")
    }

    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val inStr = if (rec.clockInTime != 0L) sdf.format(Date(rec.clockInTime)) else ""
    val outStr = rec.clockOutTime?.let { if (it != 0L) sdf.format(Date(it)) else null }

    return if (rec.clockInTime != 0L) {
        if (outStr == null) {
            ShiftStatusInfo(inStr, Color(0xFF00E676), "")
        } else {
            ShiftStatusInfo(outStr, Color(0xFF4C84FF), "")
        }
    } else {
        ShiftStatusInfo("Chưa vào ca", Color(0xFF8F9BB3), "")
    }
}

@Composable
fun EmployeeListView(
    employees: List<UserConfig>,
    selectedIds: Set<String> = emptySet(),
    todayAttendanceMap: Map<String, AttendanceRecord> = emptyMap(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmployeeClick: (UserConfig) -> Unit,
    onEmployeeLongClick: (UserConfig) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Tìm tên hoặc mã nhân viên...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonBlue) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = DarkContainer,
                focusedContainerColor = DarkContainer,
                unfocusedContainerColor = DarkContainer
            )
        )

        if (employees.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (searchQuery.isEmpty()) "Chưa có nhân viên nào" else "Không tìm thấy kết quả", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val inShiftCount = employees.count { 
                        val status = getEmployeeShiftStatus(todayAttendanceMap[it.userId])
                        status.color == Color(0xFF00E676)
                    }
                    val outShiftCount = employees.count { 
                        val status = getEmployeeShiftStatus(todayAttendanceMap[it.userId])
                        status.color == Color(0xFF4C84FF)
                    }
                    val notInCount = employees.size - inShiftCount - outShiftCount

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tổng: ${employees.size} NV",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🟢 Vào ca: $inShiftCount", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("🔵 Ra ca: $outShiftCount", color = Color(0xFF4C84FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("⚪ Chưa: $notInCount", color = Color(0xFF8F9BB3), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                items(employees) { employee ->
                    EmployeeCard(
                        employee = employee, 
                        isSelected = selectedIds.contains(employee.userId),
                        todayAttendanceRecord = todayAttendanceMap[employee.userId],
                        onClick = { onEmployeeClick(employee) },
                        onLongClick = { onEmployeeLongClick(employee) }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EmployeeCard(
    employee: UserConfig, 
    isSelected: Boolean,
    todayAttendanceRecord: AttendanceRecord? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shiftStatus = remember(todayAttendanceRecord) { getEmployeeShiftStatus(todayAttendanceRecord) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) NeonBlue.copy(alpha = 0.15f) else DarkContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) NeonBlue else Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isSelected) NeonBlue else NeonBlue.copy(alpha = 0.12f), 
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = employee.hoVaTen.take(1).uppercase(),
                        color = NeonBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Line 1: Employee Name + ADMIN Badge + ID
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = employee.hoVaTen, 
                            color = White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        if (employee.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AccentOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "ADMIN", 
                                    color = AccentOrange, 
                                    fontSize = 8.sp, 
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ID: ${employee.maNhanVien}", 
                        color = Color.Gray, 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Line 2: Shift Status Tag + Time Details + Department
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = shiftStatus.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, shiftStatus.color.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(shiftStatus.color, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = shiftStatus.label,
                                color = shiftStatus.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (shiftStatus.timeDetail.isNotEmpty()) {
                        Text(
                            text = shiftStatus.timeDetail,
                            color = shiftStatus.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (employee.boPhan.isNotEmpty()) {
                        Text(text = "•", color = Color.DarkGray, fontSize = 10.sp)
                        Text(
                            text = employee.boPhan,
                            color = NeonBlue.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

data class AttendanceStats(
    val workDays: Int,
    val leaveDays: Int,
    val totalHours: Double,
    val lateArrivals: Int
)

@Composable
fun EmployeeDetailView(
    employee: UserConfig,
    records: List<AttendanceRecord>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    adminViewModel: AdminViewModel,
    onDeleteRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val sdfYm = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val calCurrent = remember { Calendar.getInstance() }
    val currentMonthYm = remember { sdfYm.format(calCurrent.time) }

    var selectedMonthYm by remember { mutableStateOf(currentMonthYm) }
    var isAllMonths by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    fun isRecordInMonth(dateStr: String, monthYmd: String): Boolean {
        return com.example.util.ExportUtils.isRecordInMonth(dateStr, monthYmd)
    }

    val filteredRecords = remember(records, selectedMonthYm, isAllMonths) {
        if (isAllMonths) {
            records
        } else {
            records.filter { isRecordInMonth(it.dateString, selectedMonthYm) }
        }
    }

    val monthStats = remember(filteredRecords, employee) {
        val workDays = filteredRecords.count { it.clockOutTime != null }
        val lateCount = filteredRecords.count { r ->
            val isLeave = com.example.data.SalaryCalculator.isLeaveType(r.status)
            if (!isLeave && r.clockInTime > 0) {
                val tempEntry = r.toTimeEntry()
                com.example.data.SalaryCalculator.calculateSingleEntry(tempEntry, employee).lateMinutes > 0
            } else {
                false
            }
        }
        val totalHrs = filteredRecords.sumOf { r ->
            if (r.clockOutTime != null) (r.clockOutTime - r.clockInTime) / 3600000.0 else 0.0
        }
        val leaves = filteredRecords.count { com.example.data.SalaryCalculator.isLeaveType(it.status) }
        AttendanceStats(workDays, leaves, totalHrs, lateCount)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val cal = Calendar.getInstance()
        val todayDmy = String.format(Locale.US, "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        val todayRec = records.find { r ->
            com.example.data.SalaryCalculator.normalizeDateToDmy(r.dateString) == todayDmy
        }
        val shiftStatus = remember(todayRec) { getEmployeeShiftStatus(todayRec) }

        // Employee Info Card (Large, High-End Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkContainer),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Square rounded avatar
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = employee.hoVaTen.take(1).uppercase(),
                                color = PrimaryBlue,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = employee.hoVaTen.uppercase(),
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ID: ${employee.maNhanVien}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (employee.emailDangKy.isNotEmpty()) {
                                Text("•", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    text = employee.emailDangKy,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }

                            Surface(
                                color = if (employee.isAdmin) Color(0xFFF59E0B).copy(alpha = 0.18f) else PrimaryBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (employee.isAdmin) Color(0xFFF59E0B).copy(alpha = 0.4f) else PrimaryBlue.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    if (employee.isAdmin) "ADMIN" else "NV",
                                    color = if (employee.isAdmin) Color(0xFFF59E0B) else PrimaryBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { /* Detail action */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Chi tiết",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Shift Status Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(shiftStatus.color, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hôm nay: ${shiftStatus.label} ${if (shiftStatus.timeDetail.isNotEmpty()) "(${shiftStatus.timeDetail})" else ""}",
                            color = shiftStatus.color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (todayRec == null || todayRec.clockInTime == 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId,
                                            dateString = todayDmy,
                                            clockInTime = now,
                                            status = "NORMAL"
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text("Vào ca", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.HOUR_OF_DAY, 8)
                                    cal.set(Calendar.MINUTE, 0)
                                    val leaveTime = cal.timeInMillis
                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId,
                                            dateString = todayDmy,
                                            clockInTime = leaveTime,
                                            status = "PAID_LEAVE",
                                            notes = "Nghỉ phép có lương"
                                        )
                                    )
                                    if (employee.phepNamConLai > 0) {
                                        adminViewModel.saveEmployeeConfig(employee.copy(phepNamConLai = (employee.phepNamConLai - 1).coerceAtLeast(0)))
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2C94C))
                            ) {
                                Text("Nghỉ phép", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.HOUR_OF_DAY, 8)
                                    cal.set(Calendar.MINUTE, 0)
                                    val leaveTime = cal.timeInMillis
                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId,
                                            dateString = todayDmy,
                                            clockInTime = leaveTime,
                                            status = "UNAUTHORIZED_LEAVE",
                                            notes = "Nghỉ không phép"
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB5757))
                            ) {
                                Text("Không phép", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (todayRec.clockOutTime == null || todayRec.clockOutTime == 0L) {
                        // Admin muốn ra ca thì xoá ngày công này và thêm giờ vào, giờ ra thủ công, chứ không được sửa giờ ra trực tiếp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF312E81).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "💡 Nhân viên đang trong ca làm việc. Để ra ca, vui lòng xóa ngày công này và bấm Thêm công mới.",
                                color = Color(0xFFC7D2FE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = DarkBackground,
            contentColor = NeonBlue,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = NeonBlue,
                    height = 3.dp
                )
            },
            divider = {
                HorizontalDivider(color = DarkContainer, thickness = 1.dp)
            }
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Cấu Hình", fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Chấm Công", fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                text = { Text("Phiếu Lương", fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        // MONTH NAVIGATION BAR FOR CHẤM CÔNG AND PHIẾU LƯƠNG
        if (pagerState.currentPage == 1 || pagerState.currentPage == 2) {
            Surface(
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkContainer, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, NeonBlue.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isAllMonths = false
                            try {
                                val d = sdfYm.parse(selectedMonthYm) ?: Date()
                                val c = Calendar.getInstance().apply { time = d }
                                c.add(Calendar.MONTH, -1)
                                selectedMonthYm = sdfYm.format(c.time)
                            } catch (e: Exception) {
                                selectedMonthYm = currentMonthYm
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Tháng trước",
                            tint = NeonBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    val displayMonthStr = remember(selectedMonthYm, isAllMonths) {
                        if (isAllMonths) "Tất cả các tháng"
                        else {
                            try {
                                val d = sdfYm.parse(selectedMonthYm) ?: Date()
                                val fmt = SimpleDateFormat("'Tháng' MM/yyyy", Locale("vi", "VN"))
                                fmt.format(d)
                            } catch (e: Exception) {
                                "Tháng $selectedMonthYm"
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showCalendarDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Xem lịch chấm công",
                            tint = NeonBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = displayMonthStr,
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            isAllMonths = false
                            try {
                                val d = sdfYm.parse(selectedMonthYm) ?: Date()
                                val c = Calendar.getInstance().apply { time = d }
                                c.add(Calendar.MONTH, 1)
                                selectedMonthYm = sdfYm.format(c.time)
                            } catch (e: Exception) {
                                selectedMonthYm = currentMonthYm
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Tháng sau",
                            tint = NeonBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (showCalendarDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showCalendarDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF121212)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Lịch chấm công - ${employee.hoVaTen}",
                                    color = White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Chấm công & lịch sử theo ngày",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { showCalendarDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = White)
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Box(modifier = Modifier.weight(1f)) {
                            TabHistoryContent(
                                userId = employee.userId,
                                userConfig = employee,
                                attendanceLogs = records,
                                onRecordsChanged = {
                                    adminViewModel.loadTodayAttendance()
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page == 0) {
                EmployeeConfigEdit(
                    employee = employee, 
                    onSave = { adminViewModel.saveEmployeeConfig(it) },
                    onDelete = onDeleteRequest
                )
            } else if (page == 1) {
                var showAddAttendanceDialog by remember { mutableStateOf(false) }
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    item {
                        // Attendance Summary Board
                        AttendanceSummaryBoard(
                            stats = monthStats,
                            totalRecordCount = filteredRecords.size,
                            filteredRecords = filteredRecords,
                            employee = employee,
                            onShowCalendar = { showCalendarDialog = true }
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dữ liệu chấm công", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(
                                onClick = { showAddAttendanceDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = NeonBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thêm công", fontSize = 14.sp)
                            }
                        }
                    }
                    
                    if (filteredRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Không có dữ liệu chấm công cho khoảng thời gian này",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredRecords.sortedByDescending { it.dateString }) { record ->
                            AttendanceRecordItem(record = record, employee = employee, onDelete = { adminViewModel.deleteAttendanceRecord(employee.userId, record.dateString) })
                        }
                    }
                }
                
                if (showAddAttendanceDialog) {
                    val todayCal = remember { Calendar.getInstance() }
                    var dayStr by remember { mutableStateOf(TextFieldValue(String.format("%02d", todayCal.get(Calendar.DAY_OF_MONTH)))) }
                    var monthStr by remember { mutableStateOf(TextFieldValue(String.format("%02d", todayCal.get(Calendar.MONTH) + 1))) }
                    var yearStr by remember { mutableStateOf(TextFieldValue(String.format("%04d", todayCal.get(Calendar.YEAR)))) }

                    var recordType by remember { mutableStateOf("NORMAL") } // "NORMAL", "PAID_LEAVE", "UNPAID_LEAVE", "HOLIDAY_LEAVE"
                    var noteText by remember { mutableStateOf("") }

                    var checkInHour by remember { mutableStateOf(TextFieldValue("08")) }
                    var checkInMin by remember { mutableStateOf(TextFieldValue("00")) }
                    var checkOutHour by remember { mutableStateOf(TextFieldValue("17")) }
                    var checkOutMin by remember { mutableStateOf(TextFieldValue("00")) }
                    
                    val focusRequesters = remember { List(7) { FocusRequester() } }

                    val dVal = dayStr.text.trim().padStart(2, '0')
                    val mVal = monthStr.text.trim().padStart(2, '0')
                    val yVal = yearStr.text.trim()
                    val dialogDateStr = "$dVal/$mVal/$yVal"
                    val isDayHoliday = com.example.data.SalaryCalculator.isHoliday(dialogDateStr)

                    LaunchedEffect(isDayHoliday) {
                        if (isDayHoliday) {
                            recordType = "HOLIDAY_LEAVE"
                        } else {
                            if (recordType == "HOLIDAY_LEAVE") {
                                recordType = "NORMAL"
                            }
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showAddAttendanceDialog = false },
                        title = { Text("Thêm / Nhập ngày công", color = White) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                @Composable
                                fun AutoJumpField(
                                    value: TextFieldValue,
                                    onValueChange: (TextFieldValue) -> Unit,
                                    focusRequester: FocusRequester,
                                    nextFocusRequester: FocusRequester? = null,
                                    maxLength: Int = 2,
                                    modifier: Modifier = Modifier
                                ) {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isFocused by interactionSource.collectIsFocusedAsState()
                                    
                                    LaunchedEffect(isFocused) {
                                        if (isFocused) {
                                            delay(50)
                                            onValueChange(value.copy(selection = TextRange(0, value.text.length)))
                                        }
                                    }
                                    
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { 
                                            val filtered = it.text.filter { char -> char.isDigit() }
                                            if (filtered.length <= maxLength) {
                                                val newVal = it.copy(text = filtered)
                                                val textChanged = newVal.text != value.text
                                                onValueChange(newVal)
                                                if (textChanged && newVal.text.length == maxLength && nextFocusRequester != null) {
                                                    nextFocusRequester.requestFocus()
                                                }
                                            }
                                        },
                                        modifier = modifier.focusRequester(focusRequester),
                                        interactionSource = interactionSource,
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = LocalTextStyle.current.copy(
                                            textAlign = TextAlign.Center, 
                                            fontSize = 15.sp,
                                            color = White
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = White,
                                            unfocusedTextColor = White
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number, 
                                            imeAction = if (nextFocusRequester != null) ImeAction.Next else ImeAction.Done
                                        )
                                    )
                                }

                                // Record Type Selector
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Loại ngày:", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val types = remember(isDayHoliday) {
                                            val list = mutableListOf(
                                                "NORMAL" to "Đi làm",
                                                "PAID_LEAVE" to "Phép năm",
                                                "UNPAID_LEAVE" to "Nghỉ KL",
                                                "UNAUTHORIZED_LEAVE" to "Nghỉ KP"
                                            )
                                            if (isDayHoliday) {
                                                list.add("HOLIDAY_LEAVE" to "Nghỉ lễ")
                                            }
                                            list
                                        }
                                        types.forEach { (type, label) ->
                                            val isSelected = recordType == type
                                            val chipBg = when {
                                                !isSelected -> DarkContainer
                                                type == "PAID_LEAVE" -> Color(0xFFF2C94C)
                                                type == "UNPAID_LEAVE" -> Color(0xFFFF9800)
                                                type == "UNAUTHORIZED_LEAVE" -> Color(0xFFEB5757)
                                                type == "HOLIDAY_LEAVE" -> Color(0xFFBB86FC)
                                                else -> NeonBlue
                                            }
                                            val chipTextCol = if (isSelected && type == "PAID_LEAVE") Color.Black else White

                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { recordType = type },
                                                color = chipBg,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (isSelected) chipBg else Color.Gray.copy(alpha = 0.5f))
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = chipTextCol,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Date input: Ngày (dd/MM/yyyy)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Ngày (dd/MM/yyyy):", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AutoJumpField(
                                            value = dayStr,
                                            onValueChange = { newVal ->
                                                val text = newVal.text
                                                val num = text.toIntOrNull()
                                                if (num == null || (num in 1..31)) {
                                                    dayStr = newVal
                                                }
                                            },
                                            focusRequester = focusRequesters[0],
                                            nextFocusRequester = focusRequesters[1],
                                            maxLength = 2,
                                            modifier = Modifier.width(55.dp)
                                        )
                                        Text("/", color = White, fontWeight = FontWeight.Bold)
                                        AutoJumpField(
                                            value = monthStr,
                                            onValueChange = { newVal ->
                                                val text = newVal.text
                                                val num = text.toIntOrNull()
                                                if (num == null || (num in 1..12)) {
                                                    monthStr = newVal
                                                }
                                            },
                                            focusRequester = focusRequesters[1],
                                            nextFocusRequester = focusRequesters[2],
                                            maxLength = 2,
                                            modifier = Modifier.width(55.dp)
                                        )
                                        Text("/", color = White, fontWeight = FontWeight.Bold)
                                        AutoJumpField(
                                            value = yearStr,
                                            onValueChange = { yearStr = it },
                                            focusRequester = focusRequesters[2],
                                            nextFocusRequester = if (recordType == "NORMAL") focusRequesters[3] else null,
                                            maxLength = 4,
                                            modifier = Modifier.width(75.dp)
                                        )
                                    }
                                }

                                // Hour input: Giờ vào & Giờ ra (ONLY IF NORMAL)
                                if (recordType == "NORMAL") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Giờ vào (HH:mm)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Giờ vào (HH:mm):", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                AutoJumpField(
                                                    value = checkInHour,
                                                    onValueChange = { newVal ->
                                                        val text = newVal.text
                                                        val num = text.toIntOrNull()
                                                        if (num == null || (num in 0..23)) {
                                                            checkInHour = newVal
                                                        }
                                                    },
                                                    focusRequester = focusRequesters[3],
                                                    nextFocusRequester = focusRequesters[4],
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(":", color = White, fontWeight = FontWeight.Bold)
                                                AutoJumpField(
                                                    value = checkInMin,
                                                    onValueChange = { newVal ->
                                                        val text = newVal.text
                                                        val num = text.toIntOrNull()
                                                        if (num == null || (num in 0..59)) {
                                                            checkInMin = newVal
                                                        }
                                                    },
                                                    focusRequester = focusRequesters[4],
                                                    nextFocusRequester = focusRequesters[5],
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        // Giờ ra (HH:mm)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Giờ ra (HH:mm):", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                AutoJumpField(
                                                    value = checkOutHour,
                                                    onValueChange = { newVal ->
                                                        val text = newVal.text
                                                        val num = text.toIntOrNull()
                                                        if (num == null || (num in 0..23)) {
                                                            checkOutHour = newVal
                                                        }
                                                    },
                                                    focusRequester = focusRequesters[5],
                                                    nextFocusRequester = focusRequesters[6],
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(":", color = White, fontWeight = FontWeight.Bold)
                                                AutoJumpField(
                                                    value = checkOutMin,
                                                    onValueChange = { newVal ->
                                                        val text = newVal.text
                                                        val num = text.toIntOrNull()
                                                        if (num == null || (num in 0..59)) {
                                                            checkOutMin = newVal
                                                        }
                                                    },
                                                    focusRequester = focusRequesters[6],
                                                    nextFocusRequester = null,
                                                    maxLength = 2,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Note for Leave
                                    OutlinedTextField(
                                        value = noteText,
                                        onValueChange = { noteText = it },
                                        label = { Text("Ghi chú nghỉ phép", fontSize = 12.sp) },
                                        placeholder = { Text(if (recordType == "PAID_LEAVE") "Nghỉ phép có lương" else "Lý do nghỉ...", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = White,
                                            unfocusedTextColor = White,
                                            focusedBorderColor = NeonBlue,
                                            unfocusedBorderColor = Color.Gray
                                        )
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                try {
                                    val d = dayStr.text.trim().padStart(2, '0')
                                    val m = monthStr.text.trim().padStart(2, '0')
                                    val y = yearStr.text.trim().padStart(4, '0')
                                    val dbDateStr = "$d/$m/$y"

                                    val inH = checkInHour.text.trim().padStart(2, '0')
                                    val inM = checkInMin.text.trim().padStart(2, '0')
                                    val outHStr = checkOutHour.text.trim()
                                    val outMStr = checkOutMin.text.trim()

                                    val isLeave = recordType == "PAID_LEAVE" || 
                                                  recordType == "UNPAID_LEAVE" || 
                                                  recordType == "UNAUTHORIZED_LEAVE" || 
                                                  recordType == "HOLIDAY_LEAVE"

                                    val fullIn = if (isLeave) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr 08:00")?.time ?: 0L
                                    } else if (recordType == "NORMAL" && inH.isNotBlank() && inM.isNotBlank()) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr $inH:$inM")?.time ?: 0L
                                    } else {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr 08:00")?.time ?: System.currentTimeMillis()
                                    }
                                    
                                    var fullOut = if (isLeave) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr 17:00")?.time
                                    } else if (recordType == "NORMAL" && outHStr.isNotEmpty() && outMStr.isNotEmpty()) {
                                        val outH = outHStr.padStart(2, '0')
                                        val outM = outMStr.padStart(2, '0')
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr $outH:$outM")?.time
                                    } else {
                                        null
                                    }
                                    if (fullOut != null && fullOut <= fullIn) {
                                        fullOut += 24 * 3600 * 1000L
                                    }

                                    val defaultNote = when (recordType) {
                                        "PAID_LEAVE" -> "Nghỉ phép có lương"
                                        "UNPAID_LEAVE" -> "Nghỉ không lương"
                                        "UNAUTHORIZED_LEAVE" -> "Nghỉ không phép"
                                        "HOLIDAY_LEAVE" -> "Nghỉ lễ"
                                        else -> ""
                                    }
                                    val finalNote = if (noteText.isNotBlank()) noteText.trim() else defaultNote

                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId, 
                                            dateString = dbDateStr, 
                                            clockInTime = fullIn, 
                                            clockOutTime = fullOut,
                                            status = recordType,
                                            notes = finalNote
                                        )
                                    )

                                    if (recordType == "PAID_LEAVE" && employee.phepNamConLai > 0) {
                                        adminViewModel.saveEmployeeConfig(employee.copy(phepNamConLai = (employee.phepNamConLai - 1).coerceAtLeast(0)))
                                    }
                                } catch (e: Exception) {}
                                showAddAttendanceDialog = false
                            }) { Text("Lưu") }
                        },
                        dismissButton = { TextButton(onClick = { showAddAttendanceDialog = false }) { Text("Hủy") } },
                        containerColor = DarkContainer
                    )
                }
            } else {
                EmployeePayslipView(
                    employee = employee,
                    records = records,
                    selectedMonthYm = selectedMonthYm,
                    isAllMonths = isAllMonths
                )
            }
        }
    }
}

@Composable
fun AttendanceSummaryBoard(
    stats: AttendanceStats,
    totalRecordCount: Int = 0,
    filteredRecords: List<AttendanceRecord> = emptyList(),
    employee: UserConfig,
    onShowCalendar: () -> Unit
) {
    var showLeavesDetailDialog by remember { mutableStateOf(false) }
    var showHoursChartDialog by remember { mutableStateOf(false) }
    var showLateDetailsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TỔNG QUAN THÁNG",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "$totalRecordCount ngày ghi nhận",
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 4 Key Metrics Cards Grid (2x2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryItem(
                    label = "Ngày công",
                    value = "${stats.workDays}",
                    unit = "ngày",
                    icon = Icons.Default.WorkHistory,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onShowCalendar
                )
                SummaryItem(
                    label = "Tổng giờ",
                    value = String.format(Locale.US, "%.1f", stats.totalHours),
                    unit = "giờ",
                    icon = Icons.Default.Timer,
                    accentColor = PrimaryBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { showHoursChartDialog = true }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryItem(
                    label = "Đi trễ",
                    value = "${stats.lateArrivals}",
                    unit = "lần",
                    icon = Icons.Default.Schedule,
                    accentColor = if (stats.lateArrivals > 0) Color(0xFFF59E0B) else TextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { showLateDetailsDialog = true }
                )
                SummaryItem(
                    label = "Nghỉ phép",
                    value = "${stats.leaveDays}",
                    unit = "ngày",
                    icon = Icons.Default.EventBusy,
                    accentColor = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    onClick = { showLeavesDetailDialog = true }
                )
            }

            // Monthly Progress Indicator (Standard 160h)
            val progress = (stats.totalHours / 160.0).coerceIn(0.0, 1.0).toFloat()
            Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tiến độ giờ chuẩn (160h/tháng)", color = TextSecondary, fontSize = 12.sp)
                    Text("${(progress * 100).toInt()}%", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = PrimaryBlue,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }

    // --- LEAVES DETAIL DIALOG ---
    if (showLeavesDetailDialog) {
        val leaveRecords = remember(filteredRecords) {
            filteredRecords.filter { com.example.data.SalaryCalculator.isLeaveType(it.status) }
                .sortedByDescending { 
                    try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateString)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
        }

        AlertDialog(
            onDismissRequest = { showLeavesDetailDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFA855F7))
                    Text("Danh Sách Ngày Nghỉ Phép", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (leaveRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SentimentSatisfied, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Không có ngày nghỉ phép nào trong tháng.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(leaveRecords) { r ->
                            val statusUpper = r.status.uppercase(Locale.ROOT)
                            val typeLabel = when {
                                statusUpper == "PAID_LEAVE" || statusUpper == "PAID" || statusUpper == "NP" || statusUpper == "PHEP" -> "Phép năm"
                                statusUpper == "UNAUTHORIZED_LEAVE" || statusUpper == "KP" -> "Nghỉ không phép"
                                statusUpper == "UNPAID_LEAVE" || statusUpper == "UNPAID" -> "Nghỉ không lương"
                                statusUpper == "HOLIDAY_LEAVE" || statusUpper == "HOLIDAY" || statusUpper.contains("LỄ") -> "Nghỉ lễ"
                                else -> "Nghỉ phép"
                            }
                            val badgeColor = when {
                                typeLabel == "Phép năm" -> Color(0xFFF2C94C)
                                typeLabel == "Nghỉ không phép" -> Color(0xFFEB5757)
                                typeLabel == "Nghỉ không lương" -> Color(0xFFFF9800)
                                typeLabel == "Nghỉ lễ" -> Color(0xFFBB86FC)
                                else -> Color(0xFFA855F7)
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = r.dateString,
                                            color = White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (!r.notes.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Lý do: ${r.notes}",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = typeLabel,
                                            color = badgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeavesDetailDialog = false }) {
                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkContainer
        )
    }

    // --- HOURS WORKED CHART DIALOG ---
    if (showHoursChartDialog) {
        AlertDialog(
            onDismissRequest = { showHoursChartDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryBlue)
                    Text("Biểu Đồ Giờ Làm Trong Tháng", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                MonthlyWorkHoursChart(records = filteredRecords)
            },
            confirmButton = {
                TextButton(onClick = { showHoursChartDialog = false }) {
                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkContainer
        )
    }

    // --- LATE DETAILS DIALOG ---
    if (showLateDetailsDialog) {
        val lateRecords = remember(filteredRecords, employee) {
            filteredRecords.filter { r ->
                val isLeave = com.example.data.SalaryCalculator.isLeaveType(r.status)
                if (!isLeave && r.clockInTime > 0) {
                    com.example.data.SalaryCalculator.calculateSingleEntry(r.toTimeEntry(), employee).lateMinutes > 0
                } else {
                    false
                }
            }.sortedByDescending { 
                try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateString)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showLateDetailsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text("Chi Tiết Đi Trễ", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (lateRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tuyệt vời! Nhân viên không đi trễ lần nào.", color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lateRecords) { r ->
                            val calc = com.example.data.SalaryCalculator.calculateSingleEntry(r.toTimeEntry(), employee)
                            val lateMins = calc.lateMinutes
                            val inTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(r.clockInTime))

                            val formattedLate = if (lateMins < 60) {
                                "$lateMins phút"
                            } else {
                                val h = lateMins / 60
                                val m = lateMins % 60
                                if (m == 0) "$h giờ" else "$h giờ $m phút"
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = r.dateString,
                                            color = White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Giờ vào: $inTimeStr",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "Trễ $formattedLate",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLateDetailsDialog = false }) {
                    Text("Đóng", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkContainer
        )
    }
}

@Composable
fun MonthlyWorkHoursChart(records: List<AttendanceRecord>) {
    val workedRecords = remember(records) {
        records.filter { it.clockInTime > 0 && it.clockOutTime != null }
            .sortedBy { 
                try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.dateString)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
    }

    if (workedRecords.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Không có dữ liệu giờ làm trong tháng.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            val hoursList = workedRecords.map { r ->
                (r.clockOutTime!! - r.clockInTime) / 3600000.0
            }
            val maxHours = (hoursList.maxOrNull() ?: 8.0).coerceAtLeast(1.0)

            Text("Giờ làm hàng ngày (Vuốt để xem toàn bộ):", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                workedRecords.forEach { record ->
                    val hrs = (record.clockOutTime!! - record.clockInTime) / 3600000.0
                    val barHeightFraction = (hrs / maxHours).coerceIn(0.01, 1.0).toFloat()
                    val isNight = com.example.data.SalaryCalculator.isNightShift(record.clockInTime, record.clockOutTime)
                    
                    val dayNum = try {
                        val parts = record.dateString.split("/")
                        parts.firstOrNull() ?: record.dateString
                    } catch (e: Exception) {
                        record.dateString
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", hrs),
                            color = if (isNight) NightPurple else White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .weight(1f, fill = false)
                                .fillMaxHeight(barHeightFraction)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = if (isNight) {
                                            listOf(NightPurple, NightPurple.copy(alpha = 0.2f))
                                        } else if (hrs > 8.0) {
                                            listOf(AccentGreen, AccentGreen.copy(alpha = 0.2f))
                                        } else {
                                            listOf(NeonBlue, NeonBlue.copy(alpha = 0.2f))
                                        }
                                    ),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayNum,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))
            
            val dayHours = workedRecords.filter { record ->
                !com.example.data.SalaryCalculator.isNightShift(record.clockInTime, record.clockOutTime)
            }.sumOf { (it.clockOutTime!! - it.clockInTime) / 3600000.0 }

            val nightHours = workedRecords.filter { record ->
                com.example.data.SalaryCalculator.isNightShift(record.clockInTime, record.clockOutTime)
            }.sumOf { (it.clockOutTime!! - it.clockInTime) / 3600000.0 }

            val total = dayHours + nightHours
            val avg = if (workedRecords.isNotEmpty()) total / workedRecords.size else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Trung bình/ngày", color = Color.Gray, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%.1f giờ", avg), color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tổng số giờ làm", color = Color.Gray, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (dayHours > 0.0) {
                            Text(String.format(Locale.US, "Ca ngày: %.1fh", dayHours), color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (nightHours > 0.0) {
                            Text(String.format(Locale.US, "Ca đêm: %.1fh", nightHours), color = NightPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (dayHours == 0.0 && nightHours == 0.0) {
                            Text("0.0 giờ", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(unit, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 1.dp))
                }
            }
        }
    }
}

@Composable
fun ConfigSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkContainer.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun EmployeeConfigEdit(
    employee: UserConfig, 
    onSave: (UserConfig) -> Unit,
    onDelete: () -> Unit
) {
    // Basic Info
    var name by remember { mutableStateOf(employee.hoVaTen) }
    var msnv by remember { mutableStateOf(employee.maNhanVien) }
    var email by remember { mutableStateOf(employee.emailDangKy) }
    var phone by remember { mutableStateOf(employee.soDienThoai) }
    var dept by remember { mutableStateOf(employee.boPhan) }
    var schedule by remember { mutableStateOf(employee.lichTrinh) }
    var ngayVaoLam by remember { mutableStateOf(employee.ngayVaoLam) }
    var ngayVaoLamInput by remember { mutableStateOf(convertYyyyMmDdToDdMmYyyy(employee.ngayVaoLam)) }

    // Salary & Insurance
    var lcb by remember { mutableStateOf(formatCurrency(employee.luongCoBan)) }
    var lbh by remember { mutableStateOf(formatCurrency(employee.luongDongBaoHiem)) }
    var tiLeBh by remember { mutableStateOf(employee.tiLeDongBaoHiem.toString()) }
    var dpcd by remember { mutableStateOf(employee.doanPhiCongDoan.toString()) }

    // OT Coefficients
    var hsOtThuong by remember { mutableStateOf(employee.heSoOtNgayThuong.toString()) }
    var hsOtChuNhat by remember { mutableStateOf(employee.heSoOtChuNhat.toString()) }
    var hsOtLe by remember { mutableStateOf(employee.heSoOtNgayLe.toString()) }
    var hsOtDem by remember { mutableStateOf(employee.heSoOtDem.toString()) }

    // Allowances (Phụ cấp)
    var pcKyThuat by remember { mutableStateOf(formatCurrency(employee.pcKyThuat)) }
    var pcTrachNhiem by remember { mutableStateOf(formatCurrency(employee.pcTrachNhiem)) }
    var pcChucVu by remember { mutableStateOf(formatCurrency(employee.pcChucVu)) }
    var pcHieuSuat by remember { mutableStateOf(formatCurrency(employee.pcHieuSuat)) }
    var pcSanPham by remember { mutableStateOf(formatCurrency(employee.pcSanPham)) }
    var pcComCa by remember { mutableStateOf(formatCurrency(employee.pcComCa)) }
    var pcComOt by remember { mutableStateOf(formatCurrency(employee.pcComOt)) }
    var pcNhaO by remember { mutableStateOf(formatCurrency(employee.pcNhaO)) }
    var pcDocHai by remember { mutableStateOf(formatCurrency(employee.pcDocHai)) }
    var pcXangXe by remember { mutableStateOf(formatCurrency(employee.pcXangXe)) }
    var pcThamNien by remember { mutableStateOf(formatCurrency(employee.pcThamNien)) }
    var pcDtDoanhThu by remember { mutableStateOf(formatCurrency(employee.pcDtDoanhThu)) }
    var pcCaDem by remember { mutableStateOf(formatCurrency(employee.pcCaDem)) }
    var pcKhac1 by remember { mutableStateOf(formatCurrency(employee.pcKhac1)) }

    // Others
    var chuyenCan by remember { mutableStateOf(formatCurrency(employee.tienChuyenCanGoc)) }
    var phepNam by remember { mutableStateOf(employee.soNgayPhepNam.toString()) }
    var phepNamConLai by remember { mutableStateOf(employee.phepNamConLai.toString()) }
    var breakHours by remember { mutableStateOf(employee.soGioNghiGiaiLao.toString()) }
    var tinhKhauTru by remember { mutableStateOf(employee.tinhKhauTruNghi) }
    var isAdmin by remember { mutableStateOf(employee.isAdmin) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            ConfigSection(title = "Thông tin nhân sự", icon = Icons.Default.Badge) {
                AdminInputField("Họ và Tên", name, onValueChange = { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Mã Nhân Viên", msnv, onValueChange = { msnv = it })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Bộ phận", dept, onValueChange = { dept = it })
                    }
                }
                AdminInputField("Số điện thoại", phone, onValueChange = { phone = it }, isNumeric = true)
                AdminInputField("Email Đăng Ký", email, onValueChange = { email = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Lịch trình", schedule, onValueChange = { schedule = it }, keyboardType = KeyboardType.Phone)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField(
                            label = "Ngày Vào Làm", 
                            value = ngayVaoLamInput, 
                            onValueChange = { input ->
                                var clean = input.filter { it.isDigit() }.take(8)
                                if (clean.length == 8) {
                                    val isoDate = convertDdMmYyyyToYyyyMmDd(clean)
                                    ngayVaoLam = isoDate
                                } else if (clean.isEmpty()) {
                                    ngayVaoLam = ""
                                }
                                ngayVaoLamInput = clean
                            },
                            keyboardType = KeyboardType.Number,
                            visualTransformation = DateVisualTransformation()
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                    )
                    Text("Quyền Quản Trị (Admin)", color = White, fontSize = 14.sp)
                }
            }
        }
        
        item {
            ConfigSection(title = "Cấu hình lương & Bảo hiểm", icon = Icons.Default.AccountBalanceWallet) {
                AdminInputField("Lương Cơ Bản", lcb, onValueChange = { lcb = it }, isNumeric = true)
                AdminInputField("Lương Đóng BH", lbh, onValueChange = { lbh = it }, isNumeric = true)
                AdminInputField("Tỉ lệ đóng BH (%)", tiLeBh, onValueChange = { tiLeBh = it }, isNumeric = true)
                AdminInputField("Đoàn phí công đoàn", dpcd, onValueChange = { dpcd = it }, isNumeric = true)
            }
        }

        item {
            ConfigSection(title = "Hệ số tăng ca", icon = Icons.Default.TrendingUp) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Hệ số ngày thường", hsOtThuong, onValueChange = { hsOtThuong = it }, isNumeric = true, keyboardType = KeyboardType.Decimal)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Hệ số Chủ nhật", hsOtChuNhat, onValueChange = { hsOtChuNhat = it }, isNumeric = true, keyboardType = KeyboardType.Decimal)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Hệ số ngày lễ", hsOtLe, onValueChange = { hsOtLe = it }, isNumeric = true, keyboardType = KeyboardType.Decimal)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Hệ số OT đêm", hsOtDem, onValueChange = { hsOtDem = it }, isNumeric = true, keyboardType = KeyboardType.Decimal)
                    }
                }
            }
        }

        item {
            ConfigSection(title = "Các khoản phụ cấp", icon = Icons.Default.LocalActivity) {
                AdminInputField("Phụ cấp kỹ thuật", pcKyThuat, onValueChange = { pcKyThuat = it }, isNumeric = true)
                AdminInputField("Phụ cấp trách nhiệm", pcTrachNhiem, onValueChange = { pcTrachNhiem = it }, isNumeric = true)
                AdminInputField("Phụ cấp chức vụ", pcChucVu, onValueChange = { pcChucVu = it }, isNumeric = true)
                AdminInputField("Phụ cấp hiệu suất", pcHieuSuat, onValueChange = { pcHieuSuat = it }, isNumeric = true)
                AdminInputField("Phụ cấp sản phẩm", pcSanPham, onValueChange = { pcSanPham = it }, isNumeric = true)
                AdminInputField("Phụ cấp cơm ca", pcComCa, onValueChange = { pcComCa = it }, isNumeric = true)
                AdminInputField("Phụ cấp cơm OT", pcComOt, onValueChange = { pcComOt = it }, isNumeric = true)
                AdminInputField("Phụ cấp thâm niên", pcThamNien, onValueChange = { pcThamNien = it }, isNumeric = true)
                AdminInputField("Phụ cấp nhà ở", pcNhaO, onValueChange = { pcNhaO = it }, isNumeric = true)
                AdminInputField("Phụ cấp độc hại", pcDocHai, onValueChange = { pcDocHai = it }, isNumeric = true)
                AdminInputField("Phụ cấp doanh thu", pcDtDoanhThu, onValueChange = { pcDtDoanhThu = it }, isNumeric = true)
                AdminInputField("Phụ cấp xăng xe", pcXangXe, onValueChange = { pcXangXe = it }, isNumeric = true)
                AdminInputField("Phụ cấp ca đêm", pcCaDem, onValueChange = { pcCaDem = it }, isNumeric = true)
                AdminInputField("Phụ cấp khác", pcKhac1, onValueChange = { pcKhac1 = it }, isNumeric = true)
            }
        }

        item {
            ConfigSection(title = "Cài đặt & Chế độ khác", icon = Icons.Default.AutoFixHigh) {
                AdminInputField("Tiền chuyên cần", chuyenCan, onValueChange = { chuyenCan = it }, isNumeric = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Số ngày phép năm (Tổng)", phepNam, onValueChange = { phepNam = it }, isNumeric = true)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Số ngày phép còn lại", phepNamConLai, onValueChange = { phepNamConLai = it }, isNumeric = true)
                    }
                }
                AdminInputField("Số giờ nghỉ giải lao", breakHours, onValueChange = { breakHours = it }, isNumeric = true)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(
                        checked = tinhKhauTru,
                        onCheckedChange = { tinhKhauTru = it },
                        colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                    )
                    Text("Khấu trừ giờ nghỉ vào OT", color = White, fontSize = 14.sp)
                }
            }
        }

        item {
            Button(
                onClick = {
                    onSave(employee.copy(
                        hoVaTen = name,
                        maNhanVien = msnv,
                        emailDangKy = email,
                        soDienThoai = phone,
                        boPhan = dept,
                        lichTrinh = schedule,
                        ngayVaoLam = ngayVaoLam,
                        luongCoBan = lcb.replace(".", "").toDoubleOrNull() ?: 0.0,
                        luongDongBaoHiem = lbh.replace(".", "").toDoubleOrNull() ?: 0.0,
                        tiLeDongBaoHiem = tiLeBh.toDoubleOrNull() ?: 0.0,
                        doanPhiCongDoan = dpcd.toDoubleOrNull() ?: 0.0,
                        heSoOtNgayThuong = hsOtThuong.toDoubleOrNull() ?: 0.0,
                        heSoOtChuNhat = hsOtChuNhat.toDoubleOrNull() ?: 0.0,
                        heSoOtNgayLe = hsOtLe.toDoubleOrNull() ?: 0.0,
                        heSoOtDem = hsOtDem.toDoubleOrNull() ?: 0.0,
                        pcKyThuat = pcKyThuat.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcTrachNhiem = pcTrachNhiem.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcChucVu = pcChucVu.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcHieuSuat = pcHieuSuat.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcSanPham = pcSanPham.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcComCa = pcComCa.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcComOt = pcComOt.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcNhaO = pcNhaO.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcDocHai = pcDocHai.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcThamNien = pcThamNien.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcDtDoanhThu = pcDtDoanhThu.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcXangXe = pcXangXe.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcCaDem = pcCaDem.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcKhac1 = pcKhac1.replace(".", "").toDoubleOrNull() ?: 0.0,
                        tienChuyenCanGoc = chuyenCan.replace(".", "").toDoubleOrNull() ?: 0.0,
                        soNgayPhepNam = phepNam.toIntOrNull() ?: 12,
                        phepNamConLai = phepNamConLai.toIntOrNull() ?: 0,
                        thuong = 0.0,
                        soGioNghiGiaiLao = breakHours.toDoubleOrNull() ?: 1.5,
                        tinhKhauTruNghi = tinhKhauTru,
                        isAdmin = isAdmin
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("LƯU CẤU HÌNH", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }

        item {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentOrange)
            ) {
                Icon(Icons.Default.PersonRemove, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("XÓA NHÂN VIÊN", fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

fun formatCurrency(value: Double): String {
    return String.format("%,d", value.toLong()).replace(",", ".")
}

fun convertYyyyMmDdToDdMmYyyy(input: String): String {
    if (input.isBlank()) return ""
    val parts = input.split("-")
    if (parts.size == 3) {
        return "${parts[2]}${parts[1]}${parts[0]}"
    }
    return input
}

fun convertDdMmYyyyToYyyyMmDd(input: String): String {
    val digits = input.filter { it.isDigit() }
    if (digits.length == 8) {
        val d = digits.substring(0, 2)
        val m = digits.substring(2, 4)
        val y = digits.substring(4, 8)
        return "$y-$m-$d"
    }
    if (input.contains("/")) {
        val parts = input.split("/")
        if (parts.size == 3) {
            val d = parts[0].padStart(2, '0')
            val m = parts[1].padStart(2, '0')
            val y = parts[2]
            return "$y-$m-$d"
        }
    }
    return input
}

fun autoFormatTimeInput(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when (digits.length) {
        0 -> ""
        1, 2 -> digits
        3 -> "${digits.substring(0, 1).padStart(2, '0')}:${digits.substring(1)}"
        4 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
        else -> input
    }
}

fun normalizeMonthYearInput(input: String): String {
    val currentCal = Calendar.getInstance()
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMonth = currentCal.get(Calendar.MONTH) + 1

    val clean = input.trim().lowercase(Locale.getDefault())
    if (clean.isEmpty()) {
        return String.format(Locale.US, "%04d-%02d", currentYear, currentMonth)
    }

    val numberTokens = Regex("\\d+").findAll(clean).map { it.value }.toList()
    if (numberTokens.isEmpty()) {
        return String.format(Locale.US, "%04d-%02d", currentYear, currentMonth)
    }

    var year = currentYear
    var month = currentMonth

    if (numberTokens.size == 1) {
        val numStr = numberTokens[0]
        when (numStr.length) {
            1, 2 -> {
                month = numStr.toIntOrNull() ?: currentMonth
            }
            3 -> {
                month = numStr.substring(0, 1).toIntOrNull() ?: currentMonth
                val yrDigit = numStr.substring(1).toIntOrNull() ?: 26
                year = if (yrDigit < 100) 2000 + yrDigit else yrDigit
            }
            4 -> {
                val valInt = numStr.toIntOrNull() ?: 0
                if (valInt in 2020..2035) {
                    year = valInt
                } else {
                    val m = numStr.substring(0, 2).toIntOrNull() ?: currentMonth
                    val y = numStr.substring(2, 4).toIntOrNull() ?: (currentYear % 100)
                    month = m
                    year = if (y < 100) 2000 + y else y
                }
            }
            6 -> {
                val first4 = numStr.substring(0, 4).toIntOrNull() ?: 0
                if (first4 in 2020..2035) {
                    year = first4
                    month = numStr.substring(4, 6).toIntOrNull() ?: currentMonth
                } else {
                    month = numStr.substring(0, 2).toIntOrNull() ?: currentMonth
                    year = numStr.substring(2, 6).toIntOrNull() ?: currentYear
                }
            }
            else -> {
                month = numStr.take(2).toIntOrNull() ?: currentMonth
            }
        }
    } else if (numberTokens.size >= 2) {
        val token1 = numberTokens[0].toIntOrNull() ?: 0
        val token2 = numberTokens[1].toIntOrNull() ?: 0

        if (token1 > 100) {
            year = token1
            month = token2
        } else if (token2 > 100) {
            month = token1
            year = token2
        } else {
            month = token1
            year = if (token2 < 100) 2000 + token2 else token2
        }
    }

    val finalMonth = month.coerceIn(1, 12)
    val finalYear = if (year in 2000..2099) year else currentYear

    return String.format(Locale.US, "%04d-%02d", finalYear, finalMonth)
}

@Composable
fun AdminInputField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false,
    keyboardType: KeyboardType? = null,
    visualTransformation: VisualTransformation? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val isDecimal = keyboardType == KeyboardType.Decimal || 
            label.contains("Hệ số") || label.contains("hệ số") || 
            label.contains("%") || label.contains("giờ") || label.contains("Giờ") || 
            label.contains("Tỉ lệ") || label.contains("Tỷ lệ")
    
    val isScheduleOrTimeOrDate = label.contains("Lịch") || label.contains("lịch") ||
            label.contains("Ca") || label.contains("ca") ||
            label.contains("Giờ") || label.contains("giờ") ||
            label.contains("Ngày") || label.contains("ngày") ||
            label.contains("Tháng") || label.contains("tháng") ||
            label.contains("Thời gian") || label.contains("thời gian") ||
            label.contains("Số điện thoại") || label.contains("SĐT")

    val effectiveKeyboardType = keyboardType ?: if (isNumeric) {
        if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
    } else {
        if (label.contains("Email")) {
            KeyboardType.Email
        } else if (isScheduleOrTimeOrDate) {
            KeyboardType.Phone
        } else {
            KeyboardType.Text
        }
    }

    val effectiveVisualTransformation = visualTransformation ?: if (isNumeric && !isDecimal && effectiveKeyboardType != KeyboardType.Decimal && effectiveKeyboardType != KeyboardType.Phone) {
        ThousandSeparatorVisualTransformation()
    } else {
        VisualTransformation.None
    }

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (isNumeric || effectiveKeyboardType == KeyboardType.Decimal || effectiveKeyboardType == KeyboardType.Number) {
                val filtered = if (isDecimal || effectiveKeyboardType == KeyboardType.Decimal) {
                    input.replace(",", ".").filter { it.isDigit() || it == '.' }
                } else if (effectiveKeyboardType == KeyboardType.Number || isNumeric) {
                    input.filter { it.isDigit() }
                } else {
                    input
                }
                onValueChange(filtered)
            } else {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = effectiveKeyboardType,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        ),
        visualTransformation = effectiveVisualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedLabelColor = NeonBlue,
            unfocusedLabelColor = Color.Gray,
            focusedBorderColor = NeonBlue,
            unfocusedBorderColor = DarkContainer,
            focusedContainerColor = DarkContainer.copy(alpha = 0.3f),
            unfocusedContainerColor = DarkContainer.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun EmployeeAttendanceEdit(
    employee: UserConfig,
    records: List<AttendanceRecord>,
    onSaveRecord: (AttendanceRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Không có dữ liệu chấm công cho khoảng thời gian này",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), 
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(records.sortedByDescending { it.dateString }) { record ->
                AttendanceRecordItem(record = record, employee = employee, onDelete = { onDeleteRecord(record.dateString) })
            }
        }
    }
}

@Composable
fun AttendanceRecordItem(record: AttendanceRecord, employee: UserConfig, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    val timeSdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val inTime = if (record.clockInTime != 0L) timeSdf.format(Date(record.clockInTime)) else "--:--"
    val outTime = record.clockOutTime?.let { timeSdf.format(Date(it)) } ?: "--:--"

    val isInShift = record.clockInTime != 0L && (record.clockOutTime == null || record.clockOutTime == 0L)

    val tempEntry = record.toTimeEntry()
    val calculatedEntry = remember(record.clockInTime, record.clockOutTime, record.status, employee) {
        if (record.clockInTime == 0L && !com.example.data.SalaryCalculator.isLeaveType(record.status)) null
        else {
            com.example.data.SalaryCalculator.calculateSingleEntry(tempEntry, employee)
        }
    }

    val isLeave = com.example.data.SalaryCalculator.isLeaveType(record.status) || com.example.data.SalaryCalculator.isLeaveType(calculatedEntry?.dayType)
    val isLate = if (isLeave) false else (calculatedEntry?.let { it.lateMinutes > 0 } ?: false)
    val otHours = calculatedEntry?.otHours ?: 0.0
    val workDay = calculatedEntry?.workDay ?: 0.0
    val isNightShift = calculatedEntry?.shiftType == "NIGHT"

    // Calculate duration in hours
    val hrsDouble = remember(record.clockInTime, record.clockOutTime) {
        if (record.clockInTime > 0L && record.clockOutTime != null) {
            var outMs = record.clockOutTime
            if (outMs <= record.clockInTime) {
                outMs += 24 * 3600 * 1000L
            }
            (outMs - record.clockInTime) / 3600000.0
        } else 0.0
    }

    val durationBadgeText = remember(hrsDouble, isInShift) {
        if (isInShift) "Đang làm"
        else if (hrsDouble > 0) String.format(Locale.US, "%.1f giờ", hrsDouble)
        else "--"
    }

    // Parse date into DayOfWeek, DayNumber, MonthYear
    val dateParts = remember(record.dateString) {
        try {
            val parser = if (record.dateString.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            }
            val parsedDate = parser.parse(record.dateString)
            if (parsedDate != null) {
                val cal = Calendar.getInstance().apply { time = parsedDate }
                val dayOfWeekStr = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Thứ 2"
                    Calendar.TUESDAY -> "Thứ 3"
                    Calendar.WEDNESDAY -> "Thứ 4"
                    Calendar.THURSDAY -> "Thứ 5"
                    Calendar.FRIDAY -> "Thứ 6"
                    Calendar.SATURDAY -> "Thứ 7"
                    Calendar.SUNDAY -> "Chủ Nhật"
                    else -> ""
                }
                val dayNum = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
                val monthYear = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(parsedDate)
                Triple(dayOfWeekStr, dayNum, monthYear)
            } else {
                Triple("", record.dateString, "")
            }
        } catch (e: Exception) {
            Triple("", record.dateString, "")
        }
    }

    val statusUpper = record.status.uppercase(Locale.ROOT)
    val leaveBadge = when {
        statusUpper.contains("PAID") || statusUpper == "NP" || statusUpper == "PHEP" || statusUpper.contains("PHÉP") -> "🟡 Phép năm" to Color(0xFFF2C94C)
        statusUpper.contains("UNAUTHORIZED") || statusUpper == "KP" || statusUpper.contains("KHONGPHEP") || statusUpper.contains("KHÔNG PHÉP") -> "🔴 Nghỉ không phép" to Color(0xFFEB5757)
        statusUpper.contains("UNPAID") || statusUpper.contains("KHÔNG LƯƠNG") || statusUpper.contains("KHONG LUONG") -> "🟠 Nghỉ không lương" to Color(0xFFFF9800)
        statusUpper == "HOLIDAY_LEAVE" || statusUpper == "HOLIDAY LEAVE" || statusUpper.contains("NGHỈ LỄ") || statusUpper.contains("NGHI LE") -> "🟣 Nghỉ lễ" to Color(0xFFBB86FC)
        else -> null
    }

    val accentColor = when {
        leaveBadge != null -> leaveBadge.second
        isInShift -> PrimaryBlue
        isLate -> Color(0xFFF59E0B)
        record.clockOutTime != null -> SuccessGreen
        else -> TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT COLUMN: Vertical bar accent + Date info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(46.dp)
                        .background(accentColor, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.width(60.dp)
                ) {
                    Text(
                        text = dateParts.first,
                        color = PrimaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = dateParts.second,
                        color = White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 18.sp,
                        maxLines = 1
                    )
                    Text(
                        text = dateParts.third,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // MIDDLE COLUMN: Check In / Check Out timestamps
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(SuccessGreen, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vào", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(inTime, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(PrimaryBlue, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ra", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(outTime, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }

            // RIGHT COLUMN: Hours badge, work count subtext, 3-dots menu
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (leaveBadge != null) {
                        Surface(
                            color = leaveBadge.second.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, leaveBadge.second.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = leaveBadge.first,
                                color = leaveBadge.second,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                maxLines = 1
                            )
                        }
                    } else {
                        val shiftLabel = if (isNightShift) "Ca đêm" else "Ca ngày"
                        val shiftBg = if (isNightShift) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                        val shiftTextCol = if (isNightShift) Color(0xFFA5B4FC) else Color(0xFFFCD34D)

                        Surface(
                            color = shiftBg,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, shiftTextCol.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = shiftLabel,
                                color = shiftTextCol,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = durationBadgeText,
                            color = if (isInShift) PrimaryBlue else if (isLate) Color(0xFFF59E0B) else SuccessGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (leaveBadge != null) leaveBadge.first else if (isInShift) "Trong ca" else if (isLate) "Đi trễ" else if (workDay >= 1.0) "1 công" else if (workDay > 0) "${workDay} công" else "Thường",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (otHours > 0.0) {
                        Text(
                            text = " • OT ${String.format(Locale.US, "%.1fh", otHours)}",
                            color = PrimaryBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Overflow 3-dot menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkContainer)
                ) {
                    DropdownMenuItem(
                        text = { Text("Xóa công", color = Color(0xFFEF4444)) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444))
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeePayslipView(
    employee: UserConfig,
    records: List<AttendanceRecord>,
    selectedMonthYm: String,
    isAllMonths: Boolean = false
) {
    val targetMonthYm = if (isAllMonths) {
        val cal = Calendar.getInstance()
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    } else {
        selectedMonthYm
    }

    val monthLabel = remember(selectedMonthYm, isAllMonths) {
        if (isAllMonths) "Tất cả các tháng"
        else {
            try {
                val sdfYm = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val d = sdfYm.parse(selectedMonthYm) ?: Date()
                val fmt = SimpleDateFormat("MM/yyyy", Locale("vi", "VN"))
                fmt.format(d)
            } catch (e: Exception) {
                selectedMonthYm
            }
        }
    }

    val monthEntries = remember(records, targetMonthYm, isAllMonths) {
        if (isAllMonths) {
            records.map { it.toTimeEntry() }
        } else {
            records.filter { record ->
                ExportUtils.isRecordInMonth(record.dateString, targetMonthYm)
            }.map { it.toTimeEntry() }
        }
    }

    val s = remember(monthEntries, employee, targetMonthYm) {
        ExportUtils.calculateSalarySummary(monthEntries, employee, targetMonthYm)
    }

    val fmt = remember { DecimalFormat("#,###") }
    val df = remember { DecimalFormat("#.#") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkContainer),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header (Receipt Style)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TIMESNAP PRO",
                        color = NeonBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PHIẾU LƯƠNG ĐIỆN TỬ CHI TIẾT",
                        color = White,
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

                    if (s.isCurrentMonth) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ TẠM TÍNH (THÁNG HIỆN TẠI)",
                            color = AccentOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFF2C2C2C),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                // Profile Information
                PayslipProfileRow(label = "Nhân viên:", value = employee.hoVaTen)
                PayslipProfileRow(label = "Mã nhân viên (UID):", value = employee.maNhanVien, isMono = true)
                PayslipProfileRow(label = "Mức lương cơ bản:", value = "${fmt.format(employee.luongCoBan)}đ")
                PayslipProfileRow(
                    label = "Số ngày công:", 
                    value = "${s.workingDays} / ${if (s.isCurrentMonth) s.expectedWorkDays else s.standardWorkDays} ngày"
                )

                HorizontalDivider(
                    color = Color(0xFF2C2C2C),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Additions Header
                Text(
                    text = "KHOẢN CỘNG LƯƠNG (+)",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PayslipMoneyRow(label = "LCB thực nhận (${s.workingDays} / ${s.standardWorkDays})", value = s.baseBasicSalary, isAddition = true)
                
                if (employee.tienChuyenCanGoc > 0.0 && s.phuCapChuyenCan > 0.0) {
                    PayslipMoneyRow(label = "Chuyên cần", value = s.phuCapChuyenCan, isAddition = true)
                }
                if (employee.pcTrachNhiem > 0.0 && s.pcTrachNhiemVal > 0.0) {
                    PayslipMoneyRow(label = "Trách nhiệm", value = s.pcTrachNhiemVal, isAddition = true)
                }
                if (employee.pcKyThuat > 0.0 && s.pcKyThuatVal > 0.0) {
                    PayslipMoneyRow(label = "Kỹ thuật", value = s.pcKyThuatVal, isAddition = true)
                }
                if (employee.pcHieuSuat > 0.0 && s.pcHieuSuatVal > 0.0) {
                    PayslipMoneyRow(label = "Hiệu suất", value = s.pcHieuSuatVal, isAddition = true)
                }
                if (employee.pcSanPham > 0.0 && s.pcSanPhamVal > 0.0) {
                    PayslipMoneyRow(label = "Sản phẩm", value = s.pcSanPhamVal, isAddition = true)
                }
                if (employee.pcChucVu > 0.0 && s.pcChucVuVal > 0.0) {
                    PayslipMoneyRow(label = "Chức vụ", value = s.pcChucVuVal, isAddition = true)
                }
                if (employee.pcDocHai > 0.0 && s.pcDocHaiVal > 0.0) {
                    PayslipMoneyRow(label = "Độc hại", value = s.pcDocHaiVal, isAddition = true)
                }
                if (employee.pcDtDoanhThu > 0.0 && s.pcDtDoanhThuVal > 0.0) {
                    PayslipMoneyRow(label = "Doanh thu", value = s.pcDtDoanhThuVal, isAddition = true)
                }
                if (employee.pcThamNien > 0.0 && s.pcThamNienVal > 0.0) {
                    PayslipMoneyRow(label = "Thâm niên", value = s.pcThamNienVal, isAddition = true)
                }
                if (s.pcComCaVal > 0.0) {
                    PayslipMoneyRow(label = "Cơm/ ca", value = s.pcComCaVal, isAddition = true)
                }
                if (s.pcComOtVal > 0.0) {
                    PayslipMoneyRow(label = "Cơm OT", value = s.pcComOtVal, isAddition = true)
                }
                if (s.tienOtNgay > 0.0) {
                    PayslipMoneyRow(label = "OT 1.5 (${df.format(s.otDayHours)}h)", value = s.tienOtNgay, isAddition = true, isAccent = true)
                }
                if (s.tienChuNhat > 0.0) {
                    PayslipMoneyRow(label = "OT 2.0 (${df.format(s.chuNhatHours)}h)", value = s.tienChuNhat, isAddition = true, isAccent = true)
                }
                if (s.tienOtLe > 0.0) {
                    PayslipMoneyRow(label = "OT 3.0 (${df.format(s.otLeHours)}h)", value = s.tienOtLe, isAddition = true, isAccent = true)
                }
                if (s.tienOtDem > 0.0) {
                    PayslipMoneyRow(label = "OTĐ 1.5 (${df.format(s.otNightHours)}h)", value = s.tienOtDem, isAddition = true, isAccent = true)
                }
                if (s.pcCaDemVal > 0.0) {
                    PayslipMoneyRow(label = "Phụ cấp ca đêm (${s.caDemCount} ca)", value = s.pcCaDemVal, isAddition = true)
                }
                if (employee.pcXangXe > 0.0 && s.pcXangXeVal > 0.0) {
                    PayslipMoneyRow(label = "Xăng xe", value = s.pcXangXeVal, isAddition = true)
                }
                if (employee.pcNhaO > 0.0 && s.pcNhaOVal > 0.0) {
                    PayslipMoneyRow(label = "Nhà ở", value = s.pcNhaOVal, isAddition = true)
                }
                if (employee.pcKhac1 > 0.0 && s.pcKhac1Val > 0.0) {
                    PayslipMoneyRow(label = "Phụ cấp khác", value = s.pcKhac1Val, isAddition = true)
                }

                HorizontalDivider(
                    color = Color(0xFF2C2C2C),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Deductions Header
                Text(
                    text = "KHOẢN KHẤU TRỪ (-)",
                    color = AccentOrange,
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
                if (s.tienKhauTruNghi > 0.0) {
                    PayslipMoneyRow(label = "Khấu trừ vắng mặt", value = s.tienKhauTruNghi, isAddition = false)
                }

                HorizontalDivider(
                    color = Color(0xFF2C2C2C),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Total
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THỰC NHẬN:",
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${fmt.format(s.luongThucNhan)}đ",
                        color = NeonBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun SendAdminNotificationDialog(
    employees: List<UserConfig>,
    onDismiss: () -> Unit,
    onSend: (targetUid: String, targetName: String, title: String, message: String, type: String) -> Unit
) {
    var selectedTargetUid by remember { mutableStateOf("ALL") }
    var selectedTargetName by remember { mutableStateOf("Tất cả nhân viên") }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var notifType by remember { mutableStateOf("SHIFT_REMINDER") }
    var isExpandedEmpDropdown by remember { mutableStateOf(false) }
    var isExpandedTypeDropdown by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val typeOptions = listOf(
        "SHIFT_REMINDER" to "⏰ Nhắc nhở ca làm việc",
        "SHIFT_CHANGE" to "🔄 Xác nhận thay đổi ca làm việc",
        "AUTO_TIME_APPROVED" to "✅ Giờ tự động đã phê duyệt",
        "GENERAL" to "📢 Thông báo chung"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkContainer,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gửi thông báo đến nhân viên", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Thông báo sẽ tự động phát tới thiết bị nhân viên ngay khi thiết bị kết nối Mạng / Wi-Fi mà không cần mở ứng dụng.",
                    color = LightGray,
                    fontSize = 12.sp
                )

                Text("Người nhận:", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Box {
                    OutlinedButton(
                        onClick = { isExpandedEmpDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
                    ) {
                        Text(selectedTargetName, color = White, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = isExpandedEmpDropdown,
                        onDismissRequest = { isExpandedEmpDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🌐 Tất cả nhân viên (Gửi hàng loạt)") },
                            onClick = {
                                selectedTargetUid = "ALL"
                                selectedTargetName = "Tất cả nhân viên"
                                isExpandedEmpDropdown = false
                            }
                        )
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.hoVaTen} (${emp.maNhanVien})") },
                                onClick = {
                                    selectedTargetUid = emp.userId
                                    selectedTargetName = emp.hoVaTen
                                    isExpandedEmpDropdown = false
                                }
                            )
                        }
                    }
                }

                Text("Loại thông báo:", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Box {
                    OutlinedButton(
                        onClick = { isExpandedTypeDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
                        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
                    ) {
                        val currentLabel = typeOptions.find { it.first == notifType }?.second ?: "Thông báo chung"
                        Text(currentLabel, color = White)
                    }
                    DropdownMenu(
                        expanded = isExpandedTypeDropdown,
                        onDismissRequest = { isExpandedTypeDropdown = false }
                    ) {
                        typeOptions.forEach { (typeKey, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    notifType = typeKey
                                    if (title.isBlank()) {
                                        title = when (typeKey) {
                                            "SHIFT_REMINDER" -> "Nhắc nhở ca làm việc"
                                            "SHIFT_CHANGE" -> "Xác nhận thay đổi ca làm việc"
                                            "AUTO_TIME_APPROVED" -> "Xác nhận giờ tự động được duyệt"
                                            else -> "Thông báo từ Ban Quản Lý"
                                        }
                                    }
                                    isExpandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                Text("Tiêu đề thông báo:", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Nhập tiêu đề...", color = LightGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                )

                Text("Nội dung thông báo:", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Nhập nội dung chi tiết...", color = LightGray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                )
            }
        },
        confirmButton = {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val finalTitle = title.ifBlank { "📢 Thông báo từ Admin" }
                    if (message.isNotBlank()) {
                        onSend(selectedTargetUid, selectedTargetName, finalTitle, message, notifType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                enabled = message.isNotBlank()
            ) {
                Text("Gửi Ngay", color = White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LightGray)
            }
        }
    )
}
