package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.UserConfig
import com.example.viewmodel.TimeSnapViewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TimeSnapViewModel) {
    val config by viewModel.userConfig.collectAsState()
    val context = LocalContext.current

    // Local mutable state for editing fields
    var hoVaTen by remember { mutableStateOf("") }
    var maNhanVien by remember { mutableStateOf("") }
    var emailDangKy by remember { mutableStateOf("") }

    var luongCoBan by remember { mutableStateOf("") }
    var luongDongBaoHiem by remember { mutableStateOf("") }
    var tiLeDongBaoHiem by remember { mutableStateOf("") }
    var doanPhiCongDoan by remember { mutableStateOf("") }

    var tienChuyenCanGoc by remember { mutableStateOf("") }
    var pcTrachNhiem by remember { mutableStateOf("") }
    var pcKyThuat by remember { mutableStateOf("") }

    var tienComMoiNgay by remember { mutableStateOf("") }
    var pcChucVu by remember { mutableStateOf("") }
    var pcHieuSuat by remember { mutableStateOf("") }
    var pcNhaO by remember { mutableStateOf("") }
    var pcXangXe by remember { mutableStateOf("") }
    var pcKhac by remember { mutableStateOf("") }

    var heSoOtNgayThuong by remember { mutableStateOf("") }
    var heSoOtChuNhat by remember { mutableStateOf("") }
    var heSoOtNgayLe by remember { mutableStateOf("") }

    // Sync state when config updates from database
    LaunchedEffect(config) {
        hoVaTen = config.hoVaTen
        maNhanVien = config.maNhanVien
        emailDangKy = config.emailDangKy

        luongCoBan = config.luongCoBan.toLong().toString()
        luongDongBaoHiem = config.luongDongBaoHiem.toLong().toString()
        tiLeDongBaoHiem = config.tiLeDongBaoHiem.toString()
        doanPhiCongDoan = config.doanPhiCongDoan.toLong().toString()

        tienChuyenCanGoc = config.tienChuyenCanGoc.toLong().toString()
        pcTrachNhiem = config.pcTrachNhiem.toLong().toString()
        pcKyThuat = config.pcKyThuat.toLong().toString()

        tienComMoiNgay = config.tienComMoiNgay.toLong().toString()
        pcChucVu = config.pcChucVu.toLong().toString()
        pcHieuSuat = config.pcHieuSuat.toLong().toString()
        pcNhaO = config.pcNhaO.toLong().toString()
        pcXangXe = config.pcXangXe.toLong().toString()
        pcKhac = config.pcKhac.toLong().toString()

        heSoOtNgayThuong = config.heSoOtNgayThuong.toString()
        heSoOtChuNhat = config.heSoOtChuNhat.toString()
        heSoOtNgayLe = config.heSoOtNgayLe.toString()
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài Đặt Cấu Hình Lương") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val updated = config.copy(
                        hoVaTen = hoVaTen,
                        maNhanVien = maNhanVien,
                        emailDangKy = emailDangKy,
                        luongCoBan = luongCoBan.toDoubleOrNull() ?: 0.0,
                        luongDongBaoHiem = luongDongBaoHiem.toDoubleOrNull() ?: 0.0,
                        tiLeDongBaoHiem = tiLeDongBaoHiem.toDoubleOrNull() ?: 10.5,
                        doanPhiCongDoan = doanPhiCongDoan.toDoubleOrNull() ?: 0.0,
                        tienChuyenCanGoc = tienChuyenCanGoc.toDoubleOrNull() ?: 0.0,
                        pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: 0.0,
                        pcKyThuat = pcKyThuat.toDoubleOrNull() ?: 0.0,
                        tienComMoiNgay = tienComMoiNgay.toDoubleOrNull() ?: 0.0,
                        pcChucVu = pcChucVu.toDoubleOrNull() ?: 0.0,
                        pcHieuSuat = pcHieuSuat.toDoubleOrNull() ?: 0.0,
                        pcNhaO = pcNhaO.toDoubleOrNull() ?: 0.0,
                        pcXangXe = pcXangXe.toDoubleOrNull() ?: 0.0,
                        pcKhac = pcKhac.toDoubleOrNull() ?: 0.0,
                        heSoOtNgayThuong = heSoOtNgayThuong.toDoubleOrNull() ?: 1.5,
                        heSoOtChuNhat = heSoOtChuNhat.toDoubleOrNull() ?: 2.0,
                        heSoOtNgayLe = heSoOtNgayLe.toDoubleOrNull() ?: 3.0
                    )
                    viewModel.saveConfig(updated)
                    Toast.makeText(context, "Đã lưu cấu hình thành công!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("save_settings_button"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save Settings")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // SECTION 1: Personal Info
            SettingsSectionCard(title = "Thông Tin Cá Nhân") {
                SettingsTextField(label = "Họ và Tên", value = hoVaTen, onValueChange = { hoVaTen = it })
                SettingsTextField(label = "Mã Nhân Viên", value = maNhanVien, onValueChange = { maNhanVien = it })
                SettingsTextField(label = "Email Đăng Ký", value = emailDangKy, onValueChange = { emailDangKy = it })
            }

            // SECTION 2: Proportional Limits (4 items)
            SettingsSectionCard(title = "Các Mục Tính Theo Ngày Công (Công thức tỷ lệ / 26)") {
                SettingsTextField(
                    label = "Lương Tháng (Định mức cơ bản)",
                    value = luongCoBan,
                    onValueChange = { luongCoBan = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Phụ Cấp Chuyên Cần (Định mức tối đa)",
                    value = tienChuyenCanGoc,
                    onValueChange = { tienChuyenCanGoc = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Trợ Cấp Kỹ Thuật (Định mức tối đa)",
                    value = pcKyThuat,
                    onValueChange = { pcKyThuat = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Tiền Trách Nhiệm (Định mức tối đa)",
                    value = pcTrachNhiem,
                    onValueChange = { pcTrachNhiem = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
            }

            // SECTION 3: Insurance & Union
            SettingsSectionCard(title = "Bảo Hiểm & Công Đoàn") {
                SettingsTextField(
                    label = "Mức Lương Đóng Bảo Hiểm",
                    value = luongDongBaoHiem,
                    onValueChange = { luongDongBaoHiem = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Tỉ Lệ Đóng Bảo Hiểm (%)",
                    value = tiLeDongBaoHiem,
                    onValueChange = { tiLeDongBaoHiem = it },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Phí Đoàn Phí Công Đoàn",
                    value = doanPhiCongDoan,
                    onValueChange = { doanPhiCongDoan = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
            }

            // SECTION 4: Flat Allowances
            SettingsSectionCard(title = "Phụ Cấp Cố Định & Khác") {
                SettingsTextField(
                    label = "Tiền Cơm Mỗi Ngày Công",
                    value = tienComMoiNgay,
                    onValueChange = { tienComMoiNgay = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Phụ Cấp Chức Vụ",
                    value = pcChucVu,
                    onValueChange = { pcChucVu = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Phụ Cấp Hiệu Suất",
                    value = pcHieuSuat,
                    onValueChange = { pcHieuSuat = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Phụ Cấp Nhà Ở",
                    value = pcNhaO,
                    onValueChange = { pcNhaO = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Phụ Cấp Xăng Xe",
                    value = pcXangXe,
                    onValueChange = { pcXangXe = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
                SettingsTextField(
                    label = "Phụ Cấp Khác",
                    value = pcKhac,
                    onValueChange = { pcKhac = it.filter { c -> c.isDigit() } },
                    isNumeric = true
                )
            }

            // SECTION 5: Overtime Coefficients
            SettingsSectionCard(title = "Hệ Số Tăng Ca (OT)") {
                SettingsTextField(
                    label = "Hệ Số OT Ngày Thường",
                    value = heSoOtNgayThuong,
                    onValueChange = { heSoOtNgayThuong = it }
                )
                SettingsTextField(
                    label = "Hệ Số OT Chủ Nhật",
                    value = heSoOtChuNhat,
                    onValueChange = { heSoOtChuNhat = it }
                )
                SettingsTextField(
                    label = "Hệ Số OT Ngày Lễ",
                    value = heSoOtNgayLe,
                    onValueChange = { heSoOtNgayLe = it }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isNumeric) KeyboardType.Number else KeyboardType.Text
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
