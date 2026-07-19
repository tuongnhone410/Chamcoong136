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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
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

    val context = androidx.compose.ui.platform.LocalContext.current

    val exportSuccessCount by adminViewModel.exportSuccessCount.collectAsStateWithLifecycle()

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
                        IconButton(onClick = { showBatchExportDialog = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Batch Export", tint = White)
                        }
                        IconButton(onClick = { adminViewModel.loadEmployees() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = White)
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
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
                    AdminInputField("Lương Cơ Bản mới", batchLcb) { batchLcb = it }
                    AdminInputField("Phụ cấp xăng xe mới", batchPcXangXe) { batchPcXangXe = it }
                    AdminInputField("Tiền chuyên cần mới", batchChuyenCan) { batchChuyenCan = it }
                    
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
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Thêm Nhân Viên", color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Họ và Tên") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White)
                    )
                    OutlinedTextField(
                        value = newMsnv,
                        onValueChange = { newMsnv = it },
                        label = { Text("Mã Số Nhân Viên") },
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
        AlertDialog(
            onDismissRequest = { if (!isExportingByVM) showBatchExportDialog = false },
            title = { Text("Xuất Phiếu Lương Hàng Loạt", color = White) },
            text = {
                Column {
                    if (isExportingByVM) {
                        Text("Đang tổng hợp và xuất dữ liệu... ${(exportProgressByVM * 100).toInt()}%", color = White)
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { exportProgressByVM },
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonBlue,
                            trackColor = DarkBackground
                        )
                    } else {
                        Text("Tính năng này sẽ tổng hợp và xuất phiếu lương cho toàn bộ ${employees.size} nhân viên trong tháng hiện tại dưới dạng hình ảnh (.PNG).", color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                if (!isExportingByVM) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(onClick = { adminViewModel.performBatchExport(context) }) {
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

@Composable
fun EmployeeListView(
    employees: List<UserConfig>,
    selectedIds: Set<String> = emptySet(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmployeeClick: (UserConfig) -> Unit,
    onEmployeeLongClick: (UserConfig) -> Unit = {}
) {
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Tổng cộng: ${employees.size} nhân viên",
                        color = NeonBlue,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(employees) { employee ->
                    EmployeeCard(
                        employee = employee, 
                        isSelected = selectedIds.contains(employee.userId),
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
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) NeonBlue.copy(alpha = 0.15f) else DarkContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (isSelected) NeonBlue else NeonBlue.copy(alpha = 0.1f), 
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = White)
                } else {
                    Text(
                        text = employee.hoVaTen.take(1).uppercase(),
                        color = NeonBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.hoVaTen, 
                    color = White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 17.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContactPage, 
                        contentDescription = null, 
                        tint = NeonBlue.copy(alpha = 0.7f), 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = employee.maNhanVien, color = Color.Gray, fontSize = 13.sp)
                }
            }
            if (employee.isAdmin) {
                Surface(
                    color = AccentOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Admin", 
                        color = AccentOrange, 
                        fontSize = 10.sp, 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (!isSelected) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun EmployeeDetailView(
    employee: UserConfig,
    records: List<AttendanceRecord>,
    onBack: () -> Unit,
    onClose: () -> Unit,
    adminViewModel: AdminViewModel,
    onDeleteRequest: () -> Unit
) {
    var selectedDetailTab by remember { mutableStateOf(0) } // 0: Config, 1: Attendance

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedDetailTab,
            containerColor = DarkBackground,
            contentColor = NeonBlue,
            divider = {}
        ) {
            Tab(
                selected = selectedDetailTab == 0,
                onClick = { selectedDetailTab = 0 },
                text = { Text("Cấu Hình") }
            )
            Tab(
                selected = selectedDetailTab == 1,
                onClick = { selectedDetailTab = 1 },
                text = { Text("Chấm Công") }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (selectedDetailTab == 0) {
                EmployeeConfigEdit(
                    employee = employee, 
                    onSave = { adminViewModel.saveEmployeeConfig(it) },
                    onDelete = onDeleteRequest
                )
            } else {
                var showAddAttendanceDialog by remember { mutableStateOf(false) }
                Column {
                    Button(
                        onClick = { showAddAttendanceDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thêm ngày công")
                    }
                    EmployeeAttendanceEdit(
                        employee = employee,
                        records = records,
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
                                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Ngày (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White))
                                OutlinedTextField(value = checkIn, onValueChange = { checkIn = it }, label = { Text("Giờ vào (HH:mm)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White))
                                OutlinedTextField(value = checkOut, onValueChange = { checkOut = it }, label = { Text("Giờ ra (HH:mm)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White))
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
        
        Button(
            onClick = { adminViewModel.selectEmployee(null) }, // Back to list
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Quay lại danh sách")
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
    var pcKhac by remember { mutableStateOf(formatCurrency(employee.pcKhac)) }

    // Others
    var chuyenCan by remember { mutableStateOf(formatCurrency(employee.tienChuyenCanGoc)) }
    var phepNam by remember { mutableStateOf(employee.soNgayPhepNam.toString()) }
    var thuong by remember { mutableStateOf(formatCurrency(employee.thuong)) }
    var isAdmin by remember { mutableStateOf(employee.isAdmin) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConfigSection(title = "Thông tin cơ bản", icon = Icons.Default.Person) {
                AdminInputField("Họ và Tên", name) { name = it }
                AdminInputField("Mã Nhân Viên", msnv) { msnv = it }
                AdminInputField("Email Đăng Ký", email) { email = it }
                AdminInputField("Ngày Vào Làm", ngayVaoLam) { ngayVaoLam = it }
            }
        }

        item {
            ConfigSection(title = "Lương & Bảo Hiểm", icon = Icons.Default.Payments) {
                AdminInputField("Lương Cơ Bản", lcb) { lcb = it }
                AdminInputField("Lương Đóng BH", lbh) { lbh = it }
                AdminInputField("Tỉ lệ đóng BH (%)", tiLeBh) { tiLeBh = it }
                AdminInputField("Đoàn phí công đoàn (%)", dpcd) { dpcd = it }
            }
        }

        item {
            ConfigSection(title = "Hệ Số Tăng Ca", icon = Icons.Default.History) {
                AdminInputField("Ngày thường", hsOtThuong) { hsOtThuong = it }
                AdminInputField("Chủ nhật", hsOtChuNhat) { hsOtChuNhat = it }
                AdminInputField("Ngày lễ", hsOtLe) { hsOtLe = it }
                AdminInputField("OT đêm", hsOtDem) { hsOtDem = it }
            }
        }

        item {
            ConfigSection(title = "Phụ Cấp", icon = Icons.Default.CardGiftcard) {
                AdminInputField("Phụ cấp kỹ thuật", pcKyThuat) { pcKyThuat = it }
                AdminInputField("Phụ cấp trách nhiệm", pcTrachNhiem) { pcTrachNhiem = it }
                AdminInputField("Phụ cấp chức vụ", pcChucVu) { pcChucVu = it }
                AdminInputField("Phụ cấp hiệu suất", pcHieuSuat) { pcHieuSuat = it }
                AdminInputField("Phụ cấp sản phẩm", pcSanPham) { pcSanPham = it }
                AdminInputField("Phụ cấp cơm ca", pcComCa) { pcComCa = it }
                AdminInputField("Phụ cấp cơm OT", pcComOt) { pcComOt = it }
                AdminInputField("Phụ cấp nhà ở", pcNhaO) { pcNhaO = it }
                AdminInputField("Phụ cấp độc hại", pcDocHai) { pcDocHai = it }
                AdminInputField("Phụ cấp xăng xe", pcXangXe) { pcXangXe = it }
                AdminInputField("Phụ cấp khác", pcKhac) { pcKhac = it }
            }
        }

        item {
            ConfigSection(title = "Cài đặt khác", icon = Icons.Default.Settings) {
                AdminInputField("Tiền chuyên cần", chuyenCan) { chuyenCan = it }
                AdminInputField("Số ngày phép năm", phepNam) { phepNam = it }
                AdminInputField("Tiền thưởng", thuong) { thuong = it }
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
            Button(
                onClick = {
                    onSave(employee.copy(
                        hoVaTen = name,
                        maNhanVien = msnv,
                        emailDangKy = email,
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
                        pcXangXe = pcXangXe.replace(".", "").toDoubleOrNull() ?: 0.0,
                        pcKhac = pcKhac.replace(".", "").toDoubleOrNull() ?: 0.0,
                        tienChuyenCanGoc = chuyenCan.replace(".", "").toDoubleOrNull() ?: 0.0,
                        soNgayPhepNam = phepNam.toIntOrNull() ?: 12,
                        thuong = thuong.replace(".", "").toDoubleOrNull() ?: 0.0,
                        isAdmin = isAdmin
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu tất cả thay đổi", fontWeight = FontWeight.Bold)
            }
        }

        item {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xóa nhân viên này")
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

fun formatCurrency(value: Double): String {
    return String.format("%,d", value.toLong()).replace(",", ".")
}

@Composable
fun AdminInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedLabelColor = NeonBlue,
            unfocusedLabelColor = Color.Gray,
            focusedBorderColor = NeonBlue
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
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(records) { record ->
            AttendanceRecordItem(record = record, onDelete = { onDeleteRecord(record.dateString) })
        }
    }
}

@Composable
fun AttendanceRecordItem(record: AttendanceRecord, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val inTime = if (record.clockInTime != 0L) sdf.format(Date(record.clockInTime)) else "--:--"
    val outTime = record.clockOutTime?.let { sdf.format(Date(it)) } ?: "--:--"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.dateString, color = White, fontWeight = FontWeight.Bold)
                Text(text = "Vào: $inTime - Ra: $outTime", color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentOrange)
            }
        }
    }
}
