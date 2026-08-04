package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserConfig
import com.example.data.repository.OvertimeRuleRepository
import com.example.data.repository.ShiftRepository
import com.example.data.repository.WorkRuleRepository
import com.example.ui.theme.*
import com.example.viewmodel.TimeSnapViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyRulesHubDialog(
    viewModel: TimeSnapViewModel,
    companyId: String = "default_company",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf<HubSection?>(null) }
    val userConfig by viewModel.userConfig.collectAsStateWithLifecycle()
    val defaultConfig = userConfig ?: UserConfig(userId = "default_user")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkContainer,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Trung tâm Cấu hình Công ty & Quy tắc",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                text = "Quản lý toàn diện theo 7 chuyên mục cấu hình",
                                fontSize = 12.sp,
                                color = LightGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_hub_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng", tint = LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedSection == null) {
                    // List of 7 Hub Categories
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            HubCategoryCard(
                                title = "1. Thông tin công ty",
                                subtitle = "Tên công ty, mã, liên hệ, lương cơ bản & bảo hiểm",
                                icon = Icons.Default.Work,
                                testTag = "hub_sec_company",
                                onClick = { selectedSection = HubSection.COMPANY_INFO }
                            )
                        }
                        item {
                            HubCategoryCard(
                                title = "2. Ca làm việc",
                                subtitle = "Cấu hình ca sáng, chiều, đêm, thời gian bắt đầu/kết thúc",
                                icon = Icons.Default.Schedule,
                                testTag = "hub_sec_shift",
                                onClick = { selectedSection = HubSection.SHIFTS }
                            )
                        }
                        item {
                            HubCategoryCard(
                                title = "3. Quy tắc giờ công (WorkRule)",
                                subtitle = "Số giờ chuẩn/ngày, quy tắc làm tròn, ngưỡng bắt đầu OT",
                                icon = Icons.Default.Settings,
                                testTag = "hub_sec_workrule",
                                onClick = { selectedSection = HubSection.WORK_RULES }
                            )
                        }
                        item {
                            HubCategoryCard(
                                title = "4. Quy tắc OT (OvertimeRule)",
                                subtitle = "Hệ số tăng ca ngày thường, ngày nghỉ, ngày lễ",
                                icon = Icons.Default.Percent,
                                testTag = "hub_sec_otrule",
                                onClick = { selectedSection = HubSection.OVERTIME_RULES }
                            )
                        }
                        item {
                            HubCategoryCard(
                                title = "5. Ngày nghỉ / Lễ",
                                subtitle = "Cấu hình danh sách ngày lễ trong năm và ngày nghỉ hàng tuần",
                                icon = Icons.Default.DateRange,
                                testTag = "hub_sec_holidays",
                                onClick = { selectedSection = HubSection.HOLIDAYS }
                            )
                        }
                        item {
                            HubCategoryCard(
                                title = "6. Phụ cấp",
                                subtitle = "14 khoản phụ cấp & cấu hình cách tính (Pro-rated, Flat, Per Day)",
                                icon = Icons.Default.Payments,
                                testTag = "hub_sec_allowances",
                                onClick = { selectedSection = HubSection.ALLOWANCES }
                            )
                        }
                        item {
                            HubCategoryCard(
                                title = "7. Nâng cao",
                                subtitle = "Đoàn phí công đoàn, phép năm, trừ giờ nghỉ giải lao, tính lại lịch sử",
                                icon = Icons.Default.Tune,
                                testTag = "hub_sec_advanced",
                                onClick = { selectedSection = HubSection.ADVANCED }
                            )
                        }
                    }
                }
                when (selectedSection) {
                    HubSection.COMPANY_INFO -> CompanyInfoSection(defaultConfig, viewModel) { selectedSection = null }
                    HubSection.SHIFTS -> ShiftManagementDialogContent(viewModel.shiftRepository, companyId) { selectedSection = null }
                    HubSection.WORK_RULES -> WorkRuleManagementDialogContent(viewModel.workRuleRepository, companyId) { selectedSection = null }
                    HubSection.OVERTIME_RULES -> OvertimeRuleManagementDialogContent(viewModel.overtimeRuleRepository, companyId) { selectedSection = null }
                    HubSection.HOLIDAYS -> HolidaysSection(companyId) { selectedSection = null }
                    HubSection.ALLOWANCES -> AllowancesSection(defaultConfig, viewModel) { selectedSection = null }
                    HubSection.ADVANCED -> AdvancedSection(defaultConfig, viewModel) { selectedSection = null }
                    null -> {}
                }
            }
        }
    }
}

enum class HubSection {
    COMPANY_INFO, SHIFTS, WORK_RULES, OVERTIME_RULES, HOLIDAYS, ALLOWANCES, ADVANCED
}

@Composable
fun HubCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = LightGray)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = LightGray)
        }
    }
}

@Composable
fun CompanyInfoSection(userConfig: UserConfig, viewModel: TimeSnapViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var companyName by remember { mutableStateOf(userConfig.hoVaTen) }
    var companyCode by remember { mutableStateOf(userConfig.maNhanVien) }
    var phone by remember { mutableStateOf(userConfig.soDienThoai) }
    var email by remember { mutableStateOf(userConfig.emailDangKy) }
    var baseSalary by remember { mutableStateOf(userConfig.luongCoBan.toString()) }
    var insSalary by remember { mutableStateOf(userConfig.luongDongBaoHiem.toString()) }
    var insRate by remember { mutableStateOf(userConfig.tiLeDongBaoHiem.toString()) }
    var cutoffDay by remember { mutableStateOf(userConfig.ngayChotLuong.toString()) }

    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var baseSalaryError by remember { mutableStateOf<String?>(null) }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = companyName != userConfig.hoVaTen ||
        companyCode != userConfig.maNhanVien ||
        phone != userConfig.soDienThoai ||
        email != userConfig.emailDangKy ||
        baseSalary != userConfig.luongCoBan.toString() ||
        insSalary != userConfig.luongDongBaoHiem.toString() ||
        insRate != userConfig.tiLeDongBaoHiem.toString() ||
        cutoffDay != userConfig.ngayChotLuong.toString()

    val handleBack = {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Bạn có thay đổi chưa lưu", color = White) },
            text = { Text("Bạn có chắc chắn muốn thoát? Các thay đổi sẽ bị mất.", color = LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onDismiss()
                }) {
                    Text("Thoát", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Ở lại", color = NeonBlue)
                }
            },
            containerColor = DarkContainer
        )
    }

    fun validate(): Boolean {
        var isValid = true
        if (phone.isNotBlank() && (!phone.all { it.isDigit() || it == '+' || it == ' ' } || phone.filter { it.isDigit() }.length < 9)) {
            phoneError = "Số điện thoại không hợp lệ"
            isValid = false
        } else {
            phoneError = null
        }

        if (email.isNotBlank() && (!email.contains("@") || !email.contains("."))) {
            emailError = "Email không hợp lệ"
            isValid = false
        } else {
            emailError = null
        }

        val bSalary = baseSalary.toDoubleOrNull()
        if (bSalary == null || bSalary < 0) {
            baseSalaryError = "Lương cơ bản phải là số >= 0"
            isValid = false
        } else {
            baseSalaryError = null
        }
        return isValid
    }

    Dialog(onDismissRequest = handleBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)),
            color = DarkContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = handleBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = NeonBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Thông tin công ty", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Hợp đồng & liên hệ", fontSize = 12.sp, color = LightGray)
                        }
                    }
                    TextButton(onClick = {
                        if (validate()) {
                            val updated = userConfig.copy(
                                hoVaTen = companyName,
                                maNhanVien = companyCode,
                                soDienThoai = phone,
                                emailDangKy = email,
                                luongCoBan = baseSalary.toDoubleOrNull() ?: userConfig.luongCoBan,
                                luongDongBaoHiem = insSalary.toDoubleOrNull() ?: userConfig.luongDongBaoHiem,
                                tiLeDongBaoHiem = insRate.toDoubleOrNull() ?: userConfig.tiLeDongBaoHiem,
                                ngayChotLuong = cutoffDay.toIntOrNull() ?: userConfig.ngayChotLuong
                            )
                            viewModel.updateSalaryConfig(updated)
                            Toast.makeText(context, "Đã lưu thành công!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }) {
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
        item {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Tên công ty / Nhân viên") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth().testTag("input_company_name"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray, focusedLabelColor = NeonBlue, cursorColor = NeonBlue),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = companyCode,
                onValueChange = { companyCode = it },
                label = { Text("Mã nhân viên / Mã công ty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth().testTag("input_company_code"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    phone = input.filter { it.isDigit() || it == '+' || it == ' ' }
                    phoneError = null
                },
                label = { Text("Số điện thoại liên hệ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                modifier = Modifier.fillMaxWidth().testTag("input_company_phone"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
            phoneError?.let { Text(it, color = AccentRed, fontSize = 11.sp) }
        }
        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = { Text("Email liên hệ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError != null,
                modifier = Modifier.fillMaxWidth().testTag("input_company_email"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
            emailError?.let { Text(it, color = AccentRed, fontSize = 11.sp) }
        }
        item {
            OutlinedTextField(
                value = baseSalary,
                onValueChange = { input ->
                    baseSalary = input.filter { it.isDigit() }
                    baseSalaryError = null
                },
                label = { Text("Lương cơ bản (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = baseSalaryError != null,
                modifier = Modifier.fillMaxWidth().testTag("input_base_salary"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
            baseSalaryError?.let { Text(it, color = AccentRed, fontSize = 11.sp) }
        }
        item {
            OutlinedTextField(
                value = insSalary,
                onValueChange = { input ->
                    insSalary = input.filter { it.isDigit() }
                },
                label = { Text("Lương đóng bảo hiểm (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_ins_salary"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = insRate,
                onValueChange = { input ->
                    insRate = input.filter { it.isDigit() || it == '.' }
                },
                label = { Text("Tỷ lệ đóng bảo hiểm (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("input_ins_rate"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = cutoffDay,
                onValueChange = { input ->
                    cutoffDay = input.filter { it.isDigit() }
                },
                label = { Text("Ngày chốt lương trong tháng (1-31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_cutoff_day"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
                }
            }
        }
    }
}

@Composable
fun ShiftManagementDialogContent(shiftRepository: ShiftRepository, companyId: String, onDismiss: () -> Unit) {
    ShiftManagementDialog(shiftRepository = shiftRepository, companyId = companyId, onDismiss = onDismiss)
}

@Composable
fun WorkRuleManagementDialogContent(workRuleRepository: WorkRuleRepository, companyId: String, onDismiss: () -> Unit) {
    WorkRuleManagementDialog(workRuleRepository = workRuleRepository, companyId = companyId, onDismiss = onDismiss)
}

@Composable
fun OvertimeRuleManagementDialogContent(overtimeRuleRepository: OvertimeRuleRepository, companyId: String, onDismiss: () -> Unit) {
    OvertimeRuleManagementDialog(overtimeRuleRepository = overtimeRuleRepository, companyId = companyId, onDismiss = onDismiss)
}

@Composable
fun HolidaysSection(companyId: String, onDismiss: () -> Unit) {
    var holidayListStr by remember { mutableStateOf("01/01 (Tết Dương lịch), 30/04 (Giải phóng miền Nam), 01/05 (Quốc tế Lao động), 02/09 (Quốc khánh)") }
    val context = LocalContext.current
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = holidayListStr != "01/01 (Tết Dương lịch), 30/04 (Giải phóng miền Nam), 01/05 (Quốc tế Lao động), 02/09 (Quốc khánh)"

    val handleBack = {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Bạn có thay đổi chưa lưu", color = White) },
            text = { Text("Bạn có chắc chắn muốn thoát? Các thay đổi sẽ bị mất.", color = LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onDismiss()
                }) {
                    Text("Thoát", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Ở lại", color = NeonBlue)
                }
            },
            containerColor = DarkContainer
        )
    }

    Dialog(onDismissRequest = handleBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)),
            color = DarkContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = handleBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = NeonBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Ngày nghỉ / Ngày lễ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Danh sách ngày lễ chính thức", fontSize = 12.sp, color = LightGray)
                        }
                    }
                    TextButton(onClick = {
                        Toast.makeText(context, "Đã cập nhật danh sách ngày lễ thành công!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }) {
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = holidayListStr,
                            onValueChange = { holidayListStr = it },
                            label = { Text("Danh sách ngày lễ (dd/MM - Mô tả)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_holidays_list"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                            minLines = 3
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AllowancesSection(userConfig: UserConfig, viewModel: TimeSnapViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pcXangXe by remember { mutableStateOf(userConfig.pcXangXe.toString()) }
    var pcDienThoai by remember { mutableStateOf(userConfig.pcDtDoanhThu.toString()) }
    var pcNhaO by remember { mutableStateOf(userConfig.pcNhaO.toString()) }
    var pcChuyenCan by remember { mutableStateOf(userConfig.tienChuyenCanGoc.toString()) }
    var pcTrachNhiem by remember { mutableStateOf(userConfig.pcTrachNhiem.toString()) }
    var pcKyThuat by remember { mutableStateOf(userConfig.pcKyThuat.toString()) }
    var pcHieuSuat by remember { mutableStateOf(userConfig.pcHieuSuat.toString()) }
    var pcCaDem by remember { mutableStateOf(userConfig.pcCaDem.toString()) }
    var pcComCa by remember { mutableStateOf(userConfig.pcComCa.toString()) }
    var pcComOt by remember { mutableStateOf(userConfig.pcComOt.toString()) }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = pcXangXe != userConfig.pcXangXe.toString() ||
        pcDienThoai != userConfig.pcDtDoanhThu.toString() ||
        pcNhaO != userConfig.pcNhaO.toString() ||
        pcChuyenCan != userConfig.tienChuyenCanGoc.toString() ||
        pcTrachNhiem != userConfig.pcTrachNhiem.toString() ||
        pcKyThuat != userConfig.pcKyThuat.toString() ||
        pcHieuSuat != userConfig.pcHieuSuat.toString() ||
        pcCaDem != userConfig.pcCaDem.toString() ||
        pcComCa != userConfig.pcComCa.toString() ||
        pcComOt != userConfig.pcComOt.toString()

    val handleBack = {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Bạn có thay đổi chưa lưu", color = White) },
            text = { Text("Bạn có chắc chắn muốn thoát? Các thay đổi sẽ bị mất.", color = LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onDismiss()
                }) {
                    Text("Thoát", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Ở lại", color = NeonBlue)
                }
            },
            containerColor = DarkContainer
        )
    }

    Dialog(onDismissRequest = handleBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)),
            color = DarkContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = handleBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = NeonBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Phụ cấp & Tiền cơm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Phụ cấp cố định, cơm ca", fontSize = 12.sp, color = LightGray)
                        }
                    }
                    TextButton(onClick = {
                        val updated = userConfig.copy(
                            pcXangXe = pcXangXe.toDoubleOrNull() ?: userConfig.pcXangXe,
                            pcDtDoanhThu = pcDienThoai.toDoubleOrNull() ?: userConfig.pcDtDoanhThu,
                            pcNhaO = pcNhaO.toDoubleOrNull() ?: userConfig.pcNhaO,
                            tienChuyenCanGoc = pcChuyenCan.toDoubleOrNull() ?: userConfig.tienChuyenCanGoc,
                            pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: userConfig.pcTrachNhiem,
                            pcKyThuat = pcKyThuat.toDoubleOrNull() ?: userConfig.pcKyThuat,
                            pcHieuSuat = pcHieuSuat.toDoubleOrNull() ?: userConfig.pcHieuSuat,
                            pcCaDem = pcCaDem.toDoubleOrNull() ?: userConfig.pcCaDem,
                            pcComCa = pcComCa.toDoubleOrNull() ?: userConfig.pcComCa,
                            pcComOt = pcComOt.toDoubleOrNull() ?: userConfig.pcComOt
                        )
                        viewModel.updateSalaryConfig(updated)
                        Toast.makeText(context, "Đã lưu phụ cấp thành công!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }) {
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

        item {
            OutlinedTextField(
                value = pcXangXe,
                onValueChange = { pcXangXe = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Xăng xe (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_xang_xe"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcDienThoai,
                onValueChange = { pcDienThoai = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Điện thoại / Doanh thu (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_dienthoai"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcNhaO,
                onValueChange = { pcNhaO = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Nhà ở (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_nhao"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcChuyenCan,
                onValueChange = { pcChuyenCan = it.filter { c -> c.isDigit() } },
                label = { Text("Tiền chuyên cần gốc (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_chuyencan"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcTrachNhiem,
                onValueChange = { pcTrachNhiem = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Trách nhiệm (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_trachnhiem"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcKyThuat,
                onValueChange = { pcKyThuat = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Kỹ thuật (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_kythuat"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcHieuSuat,
                onValueChange = { pcHieuSuat = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Hiệu suất (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_hieusuat"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcCaDem,
                onValueChange = { pcCaDem = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Ca đêm (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_cadem"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcComCa,
                onValueChange = { pcComCa = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Cơm ca (mỗi ngày công) (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_comca"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = pcComOt,
                onValueChange = { pcComOt = it.filter { c -> c.isDigit() } },
                label = { Text("Phụ cấp Cơm OT (mỗi ngày công tăng ca >= 10h) (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_pc_comot"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }

                }
            }
        }
    }
}

@Composable
fun AdvancedSection(userConfig: UserConfig, viewModel: TimeSnapViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var unionFee by remember { mutableStateOf(userConfig.doanPhiCongDoan.toString()) }
    var annualLeave by remember { mutableStateOf(userConfig.soNgayPhepNam.toString()) }
    var breakHours by remember { mutableStateOf(userConfig.soGioNghiGiaiLao.toString()) }
    var tinhKhauTru by remember { mutableStateOf(userConfig.tinhKhauTruNghi) }

    var showRecalculateWarning by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = unionFee != userConfig.doanPhiCongDoan.toString() ||
        annualLeave != userConfig.soNgayPhepNam.toString() ||
        breakHours != userConfig.soGioNghiGiaiLao.toString() ||
        tinhKhauTru != userConfig.tinhKhauTruNghi

    val handleBack = {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Bạn có thay đổi chưa lưu", color = White) },
            text = { Text("Bạn có chắc chắn muốn thoát? Các thay đổi sẽ bị mất.", color = LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onDismiss()
                }) {
                    Text("Thoát", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Ở lại", color = NeonBlue)
                }
            },
            containerColor = DarkContainer
        )
    }

    Dialog(onDismissRequest = handleBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)),
            color = DarkContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = handleBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = NeonBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Cấu hình nâng cao", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Đoàn phí, phép năm, tính lại", fontSize = 12.sp, color = LightGray)
                        }
                    }
                    TextButton(onClick = {
                        val updated = userConfig.copy(
                            doanPhiCongDoan = unionFee.toDoubleOrNull() ?: userConfig.doanPhiCongDoan,
                            soNgayPhepNam = annualLeave.toIntOrNull() ?: userConfig.soNgayPhepNam,
                            soGioNghiGiaiLao = breakHours.toDoubleOrNull() ?: userConfig.soGioNghiGiaiLao,
                            tinhKhauTruNghi = tinhKhauTru
                        )
                        viewModel.updateSalaryConfig(updated)
                        Toast.makeText(context, "Đã lưu cài đặt nâng cao thành công!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }) {
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

        item {
            OutlinedTextField(
                value = unionFee,
                onValueChange = { unionFee = it.filter { c -> c.isDigit() } },
                label = { Text("Đoàn phí công đoàn (VND)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_union_fee"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = annualLeave,
                onValueChange = { annualLeave = it.filter { c -> c.isDigit() } },
                label = { Text("Số ngày phép năm được cấp") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("input_annual_leave"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = breakHours,
                onValueChange = { breakHours = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Số giờ nghỉ giải lao mỗi ca (giờ)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("input_break_hours"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                singleLine = true
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bật tính khấu trừ giờ nghỉ giải lao", color = White, fontSize = 14.sp)
                Switch(
                    checked = tinhKhauTru,
                    onCheckedChange = { tinhKhauTru = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonBlue)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showRecalculateWarning = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("open_recalculate_history_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tính lại toàn bộ lịch sử chấm công", color = White, fontWeight = FontWeight.Bold)
            }
        }
    }

    var recalcMode by remember { mutableStateOf("ALL") } // "ALL", "SINGLE_DAY", "MONTH", "RANGE"
    var selectedSingleDay by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedMonthYear by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedStartDate by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var selectedEndDate by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }

    if (showRecalculateWarning) {
        AlertDialog(
            onDismissRequest = { showRecalculateWarning = false },
            title = { Text("Tính lại lịch sử chấm công", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Chọn phạm vi tính lại lịch sử. Thao tác này sẽ áp dụng các quy tắc giờ công và tăng ca hiện tại cho các bản ghi được chọn.",
                        color = LightGray,
                        fontSize = 13.sp
                    )
                    
                    val modes = listOf(
                        "ALL" to "Tất cả lịch sử",
                        "SINGLE_DAY" to "Một ngày cụ thể",
                        "MONTH" to "Một tháng cụ thể",
                        "RANGE" to "Khoảng ngày"
                    )
                    
                    modes.forEach { (m, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { recalcMode = m }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (recalcMode == m),
                                onClick = { recalcMode = m },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = White, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (recalcMode == "SINGLE_DAY") {
                        Text("Chọn ngày để tính lại:", color = LightGray, fontSize = 12.sp)
                        OutlinedButton(
                            onClick = {
                                val parts = selectedSingleDay.split("-")
                                val curCal = java.util.Calendar.getInstance().apply {
                                    if (parts.size == 3) {
                                        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                    }
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, yr, mo, dy ->
                                        selectedSingleDay = String.format("%04d-%02d-%02d", yr, mo + 1, dy)
                                    },
                                    curCal.get(java.util.Calendar.YEAR),
                                    curCal.get(java.util.Calendar.MONTH),
                                    curCal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, NeonBlue)
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = NeonBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ngày: $selectedSingleDay", color = NeonBlue)
                        }
                    }

                    if (recalcMode == "MONTH") {
                        Text("Chọn tháng để tính lại:", color = LightGray, fontSize = 12.sp)
                        OutlinedButton(
                            onClick = {
                                val parts = selectedMonthYear.split("-")
                                val curCal = java.util.Calendar.getInstance().apply {
                                    if (parts.size == 2) {
                                        set(parts[0].toInt(), parts[1].toInt() - 1, 1)
                                    }
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, yr, mo, _ ->
                                        selectedMonthYear = String.format("%04d-%02d", yr, mo + 1)
                                    },
                                    curCal.get(java.util.Calendar.YEAR),
                                    curCal.get(java.util.Calendar.MONTH),
                                    1
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, NeonBlue)
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = NeonBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tháng: $selectedMonthYear", color = NeonBlue)
                        }
                    }

                    if (recalcMode == "RANGE") {
                        Text("Chọn khoảng thời gian:", color = LightGray, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val parts = selectedStartDate.split("-")
                                    val curCal = java.util.Calendar.getInstance().apply {
                                        if (parts.size == 3) {
                                            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                        }
                                    }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, yr, mo, dy ->
                                            selectedStartDate = String.format("%04d-%02d-%02d", yr, mo + 1, dy)
                                        },
                                        curCal.get(java.util.Calendar.YEAR),
                                        curCal.get(java.util.Calendar.MONTH),
                                        curCal.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, NeonBlue),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("Từ: $selectedStartDate", color = NeonBlue, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val parts = selectedEndDate.split("-")
                                    val curCal = java.util.Calendar.getInstance().apply {
                                        if (parts.size == 3) {
                                            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                        }
                                    }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, yr, mo, dy ->
                                            selectedEndDate = String.format("%04d-%02d-%02d", yr, mo + 1, dy)
                                        },
                                        curCal.get(java.util.Calendar.YEAR),
                                        curCal.get(java.util.Calendar.MONTH),
                                        curCal.get(java.util.Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, NeonBlue),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("Đến: $selectedEndDate", color = NeonBlue, fontSize = 11.sp)
                            }
                        }
                    }

                    if (recalcMode == "ALL") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, AccentRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AccentRed)
                                Text(
                                    "Cảnh báo: Thao tác này sẽ ghi đè toàn bộ dữ liệu công, tăng ca lịch sử bằng quy tắc hiện tại.",
                                    color = White,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRecalculateWarning = false
                        viewModel.recalculateAllHistory(
                            mode = recalcMode,
                            singleDay = if (recalcMode == "SINGLE_DAY") selectedSingleDay else null,
                            month = if (recalcMode == "MONTH") selectedMonthYear else null,
                            startDate = if (recalcMode == "RANGE") selectedStartDate else null,
                            endDate = if (recalcMode == "RANGE") selectedEndDate else null
                        ) { count ->
                            Toast.makeText(context, "Đã tính lại thành công $count bản ghi lịch sử!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    modifier = Modifier.testTag("confirm_recalc_btn")
                ) {
                    Text("Đồng ý", color = White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecalculateWarning = false }) {
                    Text("Hủy", color = LightGray)
                }
            },
            containerColor = DarkContainer
        )
    }
}
            }
        }
    }
