package com.example.ui.screens

import androidx.compose.foundation.background
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
        containerColor = DarkBackground
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
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
            ShiftStatusInfo(inStr, Color(0xFF00E676), "")
        } else {
            ShiftStatusInfo("Đã ra ca", Color(0xFF4C84FF), "Vào: $inStr - Ra: $outStr")
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
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
    var selectedMonthFilter by remember { mutableStateOf("CURRENT") } // "CURRENT", "PREVIOUS", "ALL"

    val calCurrent = remember { Calendar.getInstance() }
    val currentMonthYm = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calCurrent.time) }
    val currentMonthDisplay = remember { SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calCurrent.time) }

    val calPrev = remember { Calendar.getInstance().apply { add(Calendar.MONTH, -1) } }
    val prevMonthYm = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calPrev.time) }
    val prevMonthDisplay = remember { SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calPrev.time) }

    fun isRecordInMonth(dateStr: String, monthYmd: String): Boolean {
        if (monthYmd.isEmpty()) return true
        val parts = monthYmd.split("-")
        if (parts.size < 2) return dateStr.startsWith(monthYmd)
        val year = parts[0]
        val month = parts[1]
        return dateStr.startsWith(monthYmd) || 
               dateStr.endsWith("$month/$year") || 
               dateStr.contains("/$month/$year") || 
               dateStr.contains("-$month-$year")
    }

    val targetMonthYm = when (selectedMonthFilter) {
        "PREVIOUS" -> prevMonthYm
        "ALL" -> ""
        else -> currentMonthYm
    }

    val filteredRecords = remember(records, selectedMonthFilter, currentMonthYm, prevMonthYm) {
        if (selectedMonthFilter == "ALL") {
            records
        } else {
            records.filter { isRecordInMonth(it.dateString, targetMonthYm) }
        }
    }

    val monthStats = remember(filteredRecords) {
        val workDays = filteredRecords.count { it.clockOutTime != null }
        val lateCount = filteredRecords.count { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.clockInTime }
            // Threshold 08:05 for late
            cal.get(Calendar.HOUR_OF_DAY) > 8 || (cal.get(Calendar.HOUR_OF_DAY) == 8 && cal.get(Calendar.MINUTE) > 5)
        }
        val totalHrs = filteredRecords.sumOf { r ->
            if (r.clockOutTime != null) (r.clockOutTime - r.clockInTime) / 3600000.0 else 0.0
        }
        val leaves = filteredRecords.count { it.status.uppercase() == "PHEP" || it.status.uppercase().contains("LEAVE") }
        AttendanceStats(workDays, leaves, totalHrs, lateCount)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Profile Summary Header (Compact & Professional)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = NeonBlue.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = employee.hoVaTen.take(1).uppercase(),
                                color = NeonBlue,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = employee.hoVaTen,
                                color = White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Surface(
                                color = if (employee.isAdmin) AccentOrange.copy(alpha = 0.15f) else NeonBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (employee.isAdmin) "ADMIN" else "NV",
                                    color = if (employee.isAdmin) AccentOrange else NeonBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ID: ${employee.maNhanVien}",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (employee.boPhan.isNotEmpty()) {
                                Text("•", color = Color.DarkGray, fontSize = 10.sp)
                                Text(
                                    text = employee.boPhan,
                                    color = NeonBlue.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (employee.emailDangKy.isNotEmpty()) {
                                Text("•", color = Color.DarkGray, fontSize = 10.sp)
                                Text(
                                    text = employee.emailDangKy, 
                                    color = Color.Gray, 
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Shift Status Banner (Compact & Clean - Removed redundant export button)
                val cal = Calendar.getInstance()
                val todayYmd = String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                val todayDmy = String.format(Locale.US, "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                val todayRec = records.find { r ->
                    r.dateString == todayYmd || r.dateString == todayDmy || r.dateString.endsWith(todayYmd)
                }
                val shiftStatus = remember(todayRec) { getEmployeeShiftStatus(todayRec) }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = shiftStatus.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, shiftStatus.color.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(shiftStatus.color, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hôm nay: ${shiftStatus.label} ${if (shiftStatus.timeDetail.isNotEmpty()) "(${shiftStatus.timeDetail})" else ""}",
                            color = shiftStatus.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            } else {
                var showAddAttendanceDialog by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxSize()) {
                    // Month Selection Filter Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedMonthFilter == "CURRENT",
                            onClick = { selectedMonthFilter = "CURRENT" },
                            label = { Text("Tháng này ($currentMonthDisplay)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = if (selectedMonthFilter == "CURRENT") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue,
                                selectedLabelColor = White,
                                selectedLeadingIconColor = White,
                                containerColor = DarkContainer,
                                labelColor = Color.LightGray
                            )
                        )
                        FilterChip(
                            selected = selectedMonthFilter == "PREVIOUS",
                            onClick = { selectedMonthFilter = "PREVIOUS" },
                            label = { Text("Tháng trước ($prevMonthDisplay)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = if (selectedMonthFilter == "PREVIOUS") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue,
                                selectedLabelColor = White,
                                selectedLeadingIconColor = White,
                                containerColor = DarkContainer,
                                labelColor = Color.LightGray
                            )
                        )
                        FilterChip(
                            selected = selectedMonthFilter == "ALL",
                            onClick = { selectedMonthFilter = "ALL" },
                            label = { Text("Tất cả", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = if (selectedMonthFilter == "ALL") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue,
                                selectedLabelColor = White,
                                selectedLeadingIconColor = White,
                                containerColor = DarkContainer,
                                labelColor = Color.LightGray
                            )
                        )
                    }

                    // Attendance Summary Board
                    AttendanceSummaryBoard(stats = monthStats, totalRecordCount = filteredRecords.size)
                    
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

                    EmployeeAttendanceEdit(
                        employee = employee,
                        records = filteredRecords,
                        onSaveRecord = { adminViewModel.saveAttendanceRecord(it) },
                        onDeleteRecord = { date -> adminViewModel.deleteAttendanceRecord(employee.userId, date) }
                    )
                }

                if (showAddAttendanceDialog) {
                    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
                    var checkIn by remember { mutableStateOf("08:00") }
                    var checkOut by remember { mutableStateOf("17:00") }

                    AlertDialog(
                        onDismissRequest = { showAddAttendanceDialog = false },
                        title = { Text("Thêm công thủ công", color = White) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdminInputField("Ngày (yyyy-MM-dd)", dateStr, onValueChange = { dateStr = it }, keyboardType = KeyboardType.Phone)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AdminInputField("Giờ vào (HH:mm)", checkIn, onValueChange = { checkIn = it }, keyboardType = KeyboardType.Phone)
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        AdminInputField("Giờ ra (HH:mm)", checkOut, onValueChange = { checkOut = it }, keyboardType = KeyboardType.Phone)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                try {
                                    val fullIn = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse("$dateStr $checkIn")?.time ?: 0L
                                    val fullOut = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse("$dateStr $checkOut")?.time
                                    adminViewModel.saveAttendanceRecord(AttendanceRecord(uid = employee.userId, dateString = dateStr, clockInTime = fullIn, clockOutTime = fullOut))
                                } catch (e: Exception) {}
                                showAddAttendanceDialog = false
                            }) { Text("Lưu") }
                        },
                        dismissButton = { TextButton(onClick = { showAddAttendanceDialog = false }) { Text("Hủy") } },
                        containerColor = DarkContainer
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceSummaryBoard(stats: AttendanceStats, totalRecordCount: Int = 0) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .background(NeonBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TỔNG QUAN THÁNG",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = NeonBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "$totalRecordCount ngày ghi nhận",
                        color = NeonBlue,
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
                    accentColor = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Tổng giờ làm",
                    value = String.format("%.1f", stats.totalHours),
                    unit = "giờ",
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFF4C84FF),
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
                    accentColor = if (stats.lateArrivals > 0) AccentOrange else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                SummaryItem(
                    label = "Nghỉ phép",
                    value = "${stats.leaveDays}",
                    unit = "ngày",
                    icon = Icons.Default.EventBusy,
                    accentColor = Color(0xFFB388FF),
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
                    Text("Tiến độ giờ chuẩn (160h/tháng)", color = Color.Gray, fontSize = 11.sp)
                    Text("${(progress * 100).toInt()}%", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = NeonBlue,
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
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, color = White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(unit, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 1.dp))
                }
                Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
    var pcKhac by remember { mutableStateOf(formatCurrency(employee.pcKhac)) }

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
                AdminInputField("Phụ cấp khác", pcKhac, onValueChange = { pcKhac = it }, isNumeric = true)
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
                        pcKhac = pcKhac.replace(".", "").toDoubleOrNull() ?: 0.0,
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
    onDeleteRecord: (String) -> Unit
) {
    if (records.isEmpty()) {
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), 
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(records.sortedByDescending { it.dateString }) { record ->
                AttendanceRecordItem(record = record, onDelete = { onDeleteRecord(record.dateString) })
            }
        }
    }
}

@Composable
fun AttendanceRecordItem(record: AttendanceRecord, onDelete: () -> Unit) {
    val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val inTime = if (record.clockInTime != 0L) timeSdf.format(Date(record.clockInTime)) else "--:--"
    val outTime = record.clockOutTime?.let { timeSdf.format(Date(it)) } ?: "--:--"
    
    val isLate = remember(record.clockInTime) {
        if (record.clockInTime == 0L) false
        else {
            val cal = Calendar.getInstance().apply { timeInMillis = record.clockInTime }
            cal.get(Calendar.HOUR_OF_DAY) > 8 || (cal.get(Calendar.HOUR_OF_DAY) == 8 && cal.get(Calendar.MINUTE) > 5)
        }
    }

    val isInShift = record.clockInTime != 0L && record.clockOutTime == null

    // Calculate duration
    val durationText = remember(record.clockInTime, record.clockOutTime) {
        if (record.clockInTime > 0L && record.clockOutTime != null && record.clockOutTime > record.clockInTime) {
            val diffMs = record.clockOutTime - record.clockInTime
            val hrs = diffMs / 3600000
            val mins = (diffMs % 3600000) / 60000
            if (hrs > 0) "${hrs}h ${mins}p" else "${mins}p"
        } else if (isInShift) {
            "Đang trong ca"
        } else {
            "--"
        }
    }

    // Format full date with day of week (e.g. "Thứ 2, 24/07/2026")
    val formattedDateInfo = remember(record.dateString) {
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
                val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                Pair(dayOfWeekStr, displaySdf.format(parsedDate))
            } else {
                Pair("", record.dateString)
            }
        } catch (e: Exception) {
            Pair("", record.dateString)
        }
    }

    val statusColor = when {
        isInShift -> Color(0xFF00E676)
        isLate -> AccentOrange
        record.clockOutTime != null -> Color(0xFF4C84FF)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status Pillar Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(statusColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header Row: Day of week + Date + Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (formattedDateInfo.first.isNotEmpty()) {
                            Text(
                                text = formattedDateInfo.first,
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(text = " • ", color = Color.Gray, fontSize = 13.sp)
                        }
                        Text(
                            text = formattedDateInfo.second,
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Status Pill Tags
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isInShift) {
                            Surface(
                                color = Color(0xFF00E676).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    inTime,
                                    color = Color(0xFF00E676),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isLate) {
                            Surface(
                                color = AccentOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "ĐI TRỄ",
                                    color = AccentOrange,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (durationText != "--" && !isInShift) {
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    durationText,
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time Logs Details Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "Vào: ", color = Color.Gray, fontSize = 11.sp)
                        Text(text = inTime, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFF4C84FF), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(text = "Ra: ", color = Color.Gray, fontSize = 11.sp)
                        Text(text = outTime, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
