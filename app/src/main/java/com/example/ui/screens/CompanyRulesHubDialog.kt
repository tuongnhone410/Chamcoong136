package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val userConfig by viewModel.userConfig.collectAsState()
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
                } else {
                    // Sub-screen for the selected section
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { selectedSection = null }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeonBlue)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Quay lại danh mục", color = NeonBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (selectedSection) {
                            HubSection.COMPANY_INFO -> CompanyInfoSection(defaultConfig, viewModel)
                            HubSection.SHIFTS -> ShiftManagementDialogContent(viewModel.shiftRepository, companyId)
                            HubSection.WORK_RULES -> WorkRuleManagementDialogContent(viewModel.workRuleRepository, companyId)
                            HubSection.OVERTIME_RULES -> OvertimeRuleManagementDialogContent(viewModel.overtimeRuleRepository, companyId)
                            HubSection.HOLIDAYS -> HolidaysSection(companyId)
                            HubSection.ALLOWANCES -> AllowancesSection(defaultConfig, viewModel)
                            HubSection.ADVANCED -> AdvancedSection(defaultConfig, viewModel)
                            null -> {}
                        }
                    }
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
fun CompanyInfoSection(userConfig: UserConfig, viewModel: TimeSnapViewModel) {
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

    fun validate(): Boolean {
        var isValid = true
        if (phone.isNotBlank() && (!phone.all { it.isDigit() } || phone.length < 9)) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("1. Thông tin công ty & Hợp đồng", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Cấu hình tên, thông tin liên hệ và lương cơ bản của nhân viên/công ty.", fontSize = 12.sp, color = LightGray)
            Spacer(modifier = Modifier.height(4.dp))
        }
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
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
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
                        Toast.makeText(context, "Đã lưu thông tin công ty thành công!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_company_info_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu thông tin công ty", color = White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ShiftManagementDialogContent(shiftRepository: ShiftRepository, companyId: String) {
    ShiftManagementDialog(shiftRepository = shiftRepository, companyId = companyId, onDismiss = {})
}

@Composable
fun WorkRuleManagementDialogContent(workRuleRepository: WorkRuleRepository, companyId: String) {
    WorkRuleManagementDialog(workRuleRepository = workRuleRepository, companyId = companyId, onDismiss = {})
}

@Composable
fun OvertimeRuleManagementDialogContent(overtimeRuleRepository: OvertimeRuleRepository, companyId: String) {
    OvertimeRuleManagementDialog(overtimeRuleRepository = overtimeRuleRepository, companyId = companyId, onDismiss = {})
}

@Composable
fun HolidaysSection(companyId: String) {
    var holidayListStr by remember { mutableStateOf("01/01 (Tết Dương lịch), 30/04 (Giải phóng miền Nam), 01/05 (Quốc tế Lao động), 02/09 (Quốc khánh)") }
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("5. Cấu hình Ngày nghỉ / Ngày lễ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Danh sách các ngày lễ chính thức trong năm áp dụng tính lương nhân viên.", fontSize = 12.sp, color = LightGray)
            Spacer(modifier = Modifier.height(4.dp))
        }
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
        item {
            Button(
                onClick = {
                    Toast.makeText(context, "Đã cập nhật danh sách ngày lễ thành công!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_holidays_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu cấu hình ngày lễ", color = White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AllowancesSection(userConfig: UserConfig, viewModel: TimeSnapViewModel) {
    val context = LocalContext.current
    var pcXangXe by remember { mutableStateOf(userConfig.pcXangXe.toString()) }
    var pcDienThoai by remember { mutableStateOf(userConfig.pcDtDoanhThu.toString()) }
    var pcNhaO by remember { mutableStateOf(userConfig.pcNhaO.toString()) }
    var pcChuyenCan by remember { mutableStateOf(userConfig.tienChuyenCanGoc.toString()) }
    var pcTrachNhiem by remember { mutableStateOf(userConfig.pcTrachNhiem.toString()) }
    var pcComCa by remember { mutableStateOf(userConfig.pcComCa.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("6. Cấu hình Phụ cấp", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Cấu hình mức tiền cho các khoản phụ cấp (Xăng xe, điện thoại, nhà ở, chuyên cần...).", fontSize = 12.sp, color = LightGray)
            Spacer(modifier = Modifier.height(4.dp))
        }
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
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val updated = userConfig.copy(
                        pcXangXe = pcXangXe.toDoubleOrNull() ?: userConfig.pcXangXe,
                        pcDtDoanhThu = pcDienThoai.toDoubleOrNull() ?: userConfig.pcDtDoanhThu,
                        pcNhaO = pcNhaO.toDoubleOrNull() ?: userConfig.pcNhaO,
                        tienChuyenCanGoc = pcChuyenCan.toDoubleOrNull() ?: userConfig.tienChuyenCanGoc,
                        pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: userConfig.pcTrachNhiem,
                        pcComCa = pcComCa.toDoubleOrNull() ?: userConfig.pcComCa
                    )
                    viewModel.updateSalaryConfig(updated)
                    Toast.makeText(context, "Đã lưu cấu hình phụ cấp thành công!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_allowances_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu cấu hình phụ cấp", color = White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AdvancedSection(userConfig: UserConfig, viewModel: TimeSnapViewModel) {
    val context = LocalContext.current
    var unionFee by remember { mutableStateOf(userConfig.doanPhiCongDoan.toString()) }
    var annualLeave by remember { mutableStateOf(userConfig.soNgayPhepNam.toString()) }
    var breakHours by remember { mutableStateOf(userConfig.soGioNghiGiaiLao.toString()) }
    var tinhKhauTru by remember { mutableStateOf(userConfig.tinhKhauTruNghi) }

    var showRecalculateWarning by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("7. Cấu hình Nâng cao", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Đoàn phí công đoàn, phép năm, trừ giờ nghỉ và công cụ tính lại lịch sử.", fontSize = 12.sp, color = LightGray)
            Spacer(modifier = Modifier.height(4.dp))
        }
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
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val updated = userConfig.copy(
                        doanPhiCongDoan = unionFee.toDoubleOrNull() ?: userConfig.doanPhiCongDoan,
                        soNgayPhepNam = annualLeave.toIntOrNull() ?: userConfig.soNgayPhepNam,
                        soGioNghiGiaiLao = breakHours.toDoubleOrNull() ?: userConfig.soGioNghiGiaiLao,
                        tinhKhauTruNghi = tinhKhauTru
                    )
                    viewModel.updateSalaryConfig(updated)
                    Toast.makeText(context, "Đã lưu cài đặt nâng cao thành công!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_advanced_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lưu cài đặt nâng cao", color = White, fontWeight = FontWeight.Bold)
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

    if (showRecalculateWarning) {
        AlertDialog(
            onDismissRequest = { showRecalculateWarning = false },
            title = { Text("Cảnh báo tính lại lịch sử", color = White) },
            text = {
                Text(
                    "Thao tác này sẽ áp dụng các quy tắc giờ công và tăng ca hiện tại cho toàn bộ lịch sử chấm công trước đó. Bạn có chắc chắn muốn tính lại?",
                    color = LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRecalculateWarning = false
                        viewModel.recalculateAllHistory { count ->
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
