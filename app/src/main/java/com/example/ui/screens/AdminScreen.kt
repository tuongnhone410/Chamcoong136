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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserConfig
import com.example.data.AttendanceRecord
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel
import com.example.util.ThousandSeparatorVisualTransformation
import com.example.util.ExportUtils
import com.example.util.toTimeEntry
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var searchQuery by remember { mutableStateOf("") }

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
                        Text("${selectedIds.size} đã chọn", color = White, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Quản Lý Nhân Viên", color = White, fontWeight = FontWeight.Bold)
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
                        employees = employees.filter { it.hoVaTen.contains(searchQuery, ignoreCase = true) || it.maNhanVien.contains(searchQuery, ignoreCase = true) },
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
                            adminViewModel.toggleEmployeeSelection(emp.userId)
                        }
                    )
                }
            }
        }
    }

    if (showBatchEditDialog) {
        var batchLcb by remember { mutableStateOf("") }
        var batchPcXangXe by remember { mutableStateOf("") }
        var batchChuyenCan by remember { mutableStateOf("") }
        var addBatchAttendance by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBatchEditDialog = false },
            title = { Text("Sửa hàng loạt (${selectedIds.size} NV)", color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chỉ nhập vào ô muốn thay đổi cho tất cả đã chọn:", color = Color.Gray, fontSize = 12.sp)
                    AdminInputField("Lương Cơ Bản mới", batchLcb, onValueChange = { batchLcb = it }, isNumeric = true)
                    AdminInputField("Phụ cấp xăng xe mới", batchPcXangXe, onValueChange = { batchPcXangXe = it }, isNumeric = true)
                    AdminInputField("Tiền chuyên cần mới", batchChuyenCan, onValueChange = { batchChuyenCan = it }, isNumeric = true)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Thêm công hàng loạt cho ngày hiện tại:", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = addBatchAttendance, onCheckedChange = { addBatchAttendance = it }, colors = CheckboxDefaults.colors(checkedColor = NeonBlue))
                        Text("Thêm công (08:00 - 17:00)", color = White, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (batchLcb.isNotEmpty() || batchPcXangXe.isNotEmpty() || batchChuyenCan.isNotEmpty()) {
                        adminViewModel.batchUpdateSalaryConfig { emp ->
                            emp.copy(
                                luongCoBan = batchLcb.toDoubleOrNull() ?: emp.luongCoBan,
                                pcXangXe = batchPcXangXe.toDoubleOrNull() ?: emp.pcXangXe,
                                tienChuyenCanGoc = batchChuyenCan.toDoubleOrNull() ?: emp.tienChuyenCanGoc
                            )
                        }
                    }
                    if (addBatchAttendance) {
                        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        adminViewModel.batchAddAttendance(today, "08:00", "17:00")
                    }
                    showBatchEditDialog = false
                }) {
                    Text("Áp dụng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchEditDialog = false }) { Text("Hủy") }
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

    if (showAddEmployeeDialog) {
        var newName by remember { mutableStateOf("") }
        var newMsnv by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Thêm Nhân Viên", color = White) },
            text = {
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
                Button(onClick = {
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
        var exportMonth by remember { mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        
        AlertDialog(
            onDismissRequest = { if (!isExportingByVM) showBatchExportDialog = false },
            title = { Text("Xuất Phiếu Lương Hàng Loạt", color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isExportingByVM) {
                        Text("Đang tổng hợp và xuất dữ liệu... ${(exportProgressByVM * 100).toInt()}%", color = White)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { exportProgressByVM },
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonBlue,
                            trackColor = DarkBackground
                        )
                    } else {
                        Text("Chọn tháng muốn xuất phiếu lương:", color = White, fontSize = 14.sp)
                        OutlinedTextField(
                            value = exportMonth,
                            onValueChange = { exportMonth = it },
                            label = { Text("Tháng (yyyy-MM)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                        )
                        Text("Hệ thống sẽ tổng hợp và xuất phiếu lương cho toàn bộ ${employees.size} nhân viên dưới dạng hình ảnh (.PNG).", color = Color.Gray, fontSize = 12.sp)
                        Text("Lưu ý: Dữ liệu được lấy từ cloud. Nếu nhân viên chưa đồng bộ, dữ liệu có thể bị thiếu.", color = AccentOrange.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                if (!isExportingByVM) {
                    Button(onClick = { adminViewModel.performBatchExport(context, exportMonth) }) {
                        Text("Bắt đầu xuất")
                    }
                }
            },
            dismissButton = {
                if (!isExportingByVM) {
                    TextButton(onClick = { showBatchExportDialog = false }) { Text("Đóng") }
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
            ShiftStatusInfo("+$inStr", Color(0xFF00E676), "")
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
                        status.label == "Đang trong ca"
                    }
                    val outShiftCount = employees.count { 
                        val status = getEmployeeShiftStatus(todayAttendanceMap[it.userId])
                        status.label == "Đã ra ca"
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

    fun isRecordInMonth(dateStr: String, monthYmd: String): Boolean {
        if (monthYmd.isEmpty()) return true
        val parts = monthYmd.split("-")
        if (parts.size < 2) return dateStr.contains(monthYmd)
        val year = parts[0]
        val month = parts[1]
        val slashPattern = "/$month/$year"
        val dashPattern = "-$month-$year"
        val altDashPattern = "$year-$month"
        return dateStr.startsWith(altDashPattern) || 
               dateStr.contains(slashPattern) || 
               dateStr.contains(dashPattern) ||
               dateStr.endsWith(slashPattern)
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
            if (r.clockInTime > 0) {
                val tempEntry = com.example.data.model.TimeEntry(
                    userId = employee.userId,
                    date = r.dateString,
                    checkInTime = r.clockInTime,
                    checkOutTime = r.clockOutTime
                )
                com.example.data.SalaryCalculator.calculateSingleEntry(tempEntry, employee).lateMinutes > 0
            } else {
                false
            }
        }
        val totalHrs = filteredRecords.sumOf { r ->
            if (r.clockOutTime != null) (r.clockOutTime - r.clockInTime) / 3600000.0 else 0.0
        }
        val leaves = filteredRecords.count { it.status.uppercase() == "PHEP" || it.status.uppercase().contains("LEAVE") }
        AttendanceStats(workDays, leaves, totalHrs, lateCount)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val cal = Calendar.getInstance()
        val todayYmd = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val todayDmy = String.format(Locale.US, "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        val todayShortDmy = String.format(Locale.US, "%d/%d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        val todayShortYmd = String.format(Locale.US, "%04d-%d-%d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val todayRec = records.find { r ->
            val ds = r.dateString.trim()
            ds == todayYmd || ds == todayDmy || ds == todayShortDmy || ds == todayShortYmd || ds.endsWith(todayYmd)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                adminViewModel.saveAttendanceRecord(
                                    AttendanceRecord(
                                        uid = employee.userId,
                                        dateString = todayYmd,
                                        clockInTime = now
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Vào ca", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (todayRec.clockOutTime == null || todayRec.clockOutTime == 0L) {
                        Button(
                            onClick = {
                                val now = System.currentTimeMillis()
                                adminViewModel.saveAttendanceRecord(
                                    todayRec.copy(clockOutTime = now)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Ra ca", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        modifier = Modifier.clickable {
                            if (isAllMonths) {
                                isAllMonths = false
                                selectedMonthYm = currentMonthYm
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = displayMonthStr,
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = isAllMonths,
                            onClick = { isAllMonths = !isAllMonths },
                            label = { Text("Tất cả", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue,
                                selectedLabelColor = White,
                                containerColor = Color.Transparent,
                                labelColor = Color.Gray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllMonths,
                                borderColor = if (isAllMonths) NeonBlue else Color.Gray.copy(alpha = 0.3f),
                                selectedBorderColor = NeonBlue
                            ),
                            modifier = Modifier.height(28.dp)
                        )

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
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        // Attendance Summary Board
                        AttendanceSummaryBoard(stats = monthStats, totalRecordCount = filteredRecords.size)
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
                        }
                    } else {
                        items(filteredRecords.sortedByDescending { it.dateString }) { record ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                AttendanceRecordItem(record = record, employee = employee, onDelete = { adminViewModel.deleteAttendanceRecord(employee.userId, record.dateString) })
                            }
                        }
                    }
                }
                
                if (showAddAttendanceDialog) {
                    val todayCal = remember { Calendar.getInstance() }
                    var dayStr by remember { mutableStateOf(TextFieldValue(String.format("%02d", todayCal.get(Calendar.DAY_OF_MONTH)))) }
                    var monthStr by remember { mutableStateOf(TextFieldValue(String.format("%02d", todayCal.get(Calendar.MONTH) + 1))) }
                    var yearStr by remember { mutableStateOf(TextFieldValue(String.format("%04d", todayCal.get(Calendar.YEAR)))) }

                    var checkInHour by remember { mutableStateOf(TextFieldValue("08")) }
                    var checkInMin by remember { mutableStateOf(TextFieldValue("00")) }
                    var checkOutHour by remember { mutableStateOf(TextFieldValue("17")) }
                    var checkOutMin by remember { mutableStateOf(TextFieldValue("00")) }
                    
                    val focusRequesters = remember { List(7) { FocusRequester() } }

                    AlertDialog(
                        onDismissRequest = { showAddAttendanceDialog = false },
                        title = { Text("Thêm công thủ công", color = White) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
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

                                // Date input: Ngày (dd/MM/yyyy)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Ngày (dd/MM/yyyy):", color = LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AutoJumpField(
                                            value = dayStr,
                                            onValueChange = { dayStr = it },
                                            focusRequester = focusRequesters[0],
                                            nextFocusRequester = focusRequesters[1],
                                            maxLength = 2,
                                            modifier = Modifier.width(55.dp)
                                        )
                                        Text("/", color = White, fontWeight = FontWeight.Bold)
                                        AutoJumpField(
                                            value = monthStr,
                                            onValueChange = { monthStr = it },
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
                                            nextFocusRequester = focusRequesters[3],
                                            maxLength = 4,
                                            modifier = Modifier.width(75.dp)
                                        )
                                    }
                                }

                                // Hour input: Giờ vào & Giờ ra
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
                                                onValueChange = { checkInHour = it },
                                                focusRequester = focusRequesters[3],
                                                nextFocusRequester = focusRequesters[4],
                                                maxLength = 2,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(":", color = White, fontWeight = FontWeight.Bold)
                                            AutoJumpField(
                                                value = checkInMin,
                                                onValueChange = { checkInMin = it },
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
                                                onValueChange = { checkOutHour = it },
                                                focusRequester = focusRequesters[5],
                                                nextFocusRequester = focusRequesters[6],
                                                maxLength = 2,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(":", color = White, fontWeight = FontWeight.Bold)
                                            AutoJumpField(
                                                value = checkOutMin,
                                                onValueChange = { checkOutMin = it },
                                                focusRequester = focusRequesters[6],
                                                nextFocusRequester = null,
                                                maxLength = 2,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                try {
                                    val d = dayStr.text.trim().padStart(2, '0')
                                    val m = monthStr.text.trim().padStart(2, '0')
                                    val y = yearStr.text.trim().padStart(4, '0')
                                    
                                    val inH = checkInHour.text.trim().padStart(2, '0')
                                    val inM = checkInMin.text.trim().padStart(2, '0')
                                    val outHStr = checkOutHour.text.trim()
                                    val outMStr = checkOutMin.text.trim()

                                    // Target stored format is dd/MM/yyyy
                                    val dbDateStr = "$d/$m/$y"
                                    
                                    val fullIn = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr $inH:$inM")?.time ?: 0L
                                    var fullOut = if (outHStr.isNotEmpty() && outMStr.isNotEmpty()) {
                                        val outH = outHStr.padStart(2, '0')
                                        val outM = outMStr.padStart(2, '0')
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("$dbDateStr $outH:$outM")?.time
                                    } else {
                                        null
                                    }
                                    if (fullOut != null && fullOut <= fullIn) {
                                        fullOut += 24 * 3600 * 1000L
                                    }
                                    adminViewModel.saveAttendanceRecord(
                                        AttendanceRecord(
                                            uid = employee.userId, 
                                            dateString = dbDateStr, 
                                            clockInTime = fullIn, 
                                            clockOutTime = fullOut
                                        )
                                    )
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
fun AttendanceSummaryBoard(stats: AttendanceStats, totalRecordCount: Int = 0) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TỔNG QUAN THÁNG",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }

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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Tổng giờ",
                    value = String.format(Locale.US, "%.1f", stats.totalHours),
                    unit = "giờ",
                    icon = Icons.Default.Timer,
                    accentColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
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
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Nghỉ phép",
                    value = "${stats.leaveDays}",
                    unit = "ngày",
                    icon = Icons.Default.EventBusy,
                    accentColor = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f)
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
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
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
    var pcCaDem by remember { mutableStateOf(formatCurrency(employee.pcKhac)) }
    var pcKhac1 by remember { mutableStateOf(formatCurrency(employee.pcKhac1)) }

    // Others
    var chuyenCan by remember { mutableStateOf(formatCurrency(employee.tienChuyenCanGoc)) }
    var phepNam by remember { mutableStateOf(employee.soNgayPhepNam.toString()) }
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
                        AdminInputField("Lịch trình", schedule, onValueChange = { schedule = it })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        AdminInputField("Ngày Vào Làm", ngayVaoLam, onValueChange = { ngayVaoLam = it })
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
                AdminInputField("Phụ cấp điện thoại", pcDtDoanhThu, onValueChange = { pcDtDoanhThu = it }, isNumeric = true)
                AdminInputField("Phụ cấp xăng xe", pcXangXe, onValueChange = { pcXangXe = it }, isNumeric = true)
                AdminInputField("Phụ cấp ca đêm", pcCaDem, onValueChange = { pcCaDem = it }, isNumeric = true)
                AdminInputField("Phụ cấp khác", pcKhac1, onValueChange = { pcKhac1 = it }, isNumeric = true)
            }
        }

        item {
            ConfigSection(title = "Cài đặt & Chế độ khác", icon = Icons.Default.AutoFixHigh) {
                AdminInputField("Tiền chuyên cần", chuyenCan, onValueChange = { chuyenCan = it }, isNumeric = true)
                AdminInputField("Số ngày phép năm", phepNam, onValueChange = { phepNam = it }, isNumeric = true)
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
                        pcKhac = pcCaDem.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcKhac1 = pcKhac1.replace(".", "").toDoubleOrNull() ?: 0.0,
                        tienChuyenCanGoc = chuyenCan.replace(".", "").toDoubleOrNull() ?: 0.0,
                        soNgayPhepNam = phepNam.toIntOrNull() ?: 12,
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

@Composable
fun AdminInputField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false,
    keyboardType: KeyboardType? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val isDecimal = keyboardType == KeyboardType.Decimal || 
            label.contains("Hệ số") || label.contains("hệ số") || 
            label.contains("%") || label.contains("giờ") || label.contains("Giờ") || 
            label.contains("Tỉ lệ") || label.contains("Tỷ lệ")
    
    val effectiveKeyboardType = keyboardType ?: if (isNumeric) {
        if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
    } else {
        if (label.contains("Email")) {
            KeyboardType.Email
        } else if (label.contains("Số điện thoại") || label.contains("SĐT") || 
                   label.contains("Ngày") || label.contains("Giờ") || 
                   label.contains("Tháng")) {
            KeyboardType.Phone
        } else {
            KeyboardType.Text
        }
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
        visualTransformation = if (isNumeric && !isDecimal && effectiveKeyboardType != KeyboardType.Decimal && effectiveKeyboardType != KeyboardType.Phone) {
            ThousandSeparatorVisualTransformation()
        } else {
            VisualTransformation.None
        },
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

    val calculatedEntry = remember(record.clockInTime, record.clockOutTime, employee) {
        if (record.clockInTime == 0L) null
        else {
            var outMs = record.clockOutTime
            if (outMs != null && outMs <= record.clockInTime) {
                outMs += 24 * 3600 * 1000L
            }
            val tempEntry = com.example.data.model.TimeEntry(
                userId = employee.userId,
                date = record.dateString,
                checkInTime = record.clockInTime,
                checkOutTime = outMs
            )
            com.example.data.SalaryCalculator.calculateSingleEntry(tempEntry, employee)
        }
    }

    val isLate = calculatedEntry?.let { it.lateMinutes > 0 } ?: false
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

    val accentColor = when {
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
                    val shiftLabel = if (isNightShift) "🌙 Ca đêm" else "☀️ Ca ngày"
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
                        text = if (isInShift) "Trong ca" else if (isLate) "Đi trễ" else if (workDay >= 1.0) "1 công" else if (workDay > 0) "${workDay} công" else "Thường",
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
                val ds = record.dateString
                val matchYmd = ds.startsWith(targetMonthYm)
                val matchDmy = if (targetMonthYm.contains("-")) {
                    val parts = targetMonthYm.split("-")
                    ds.contains("/${parts[1]}/${parts[0]}") || ds.endsWith("/${parts[1]}/${parts[0]}")
                } else false
                matchYmd || matchDmy
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
                    PayslipMoneyRow(label = "Khác 1", value = s.pcKhac1Val, isAddition = true)
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
