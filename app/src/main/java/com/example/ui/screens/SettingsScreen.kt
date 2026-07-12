package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserConfig
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkContainer
import com.example.ui.theme.LightGray
import com.example.ui.theme.MediumGray
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import com.example.ui.theme.White
import com.example.viewmodel.TimeSnapViewModel
import java.text.DecimalFormat

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.AnnotatedString

class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = StringBuilder()
        val originalLen = originalText.length
        val originalToTransformed = IntArray(originalLen + 1)

        var dotCount = 0
        for (i in 0 until originalLen) {
            val distFromEnd = originalLen - i
            if (i > 0 && distFromEnd % 3 == 0) {
                formatted.append('.')
                dotCount++
            }
            originalToTransformed[i] = i + dotCount
            formatted.append(originalText[i])
        }
        originalToTransformed[originalLen] = originalLen + dotCount

        val transformedText = formatted.toString()
        val transformedLen = transformedText.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, originalLen)
                return originalToTransformed[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformedLen)
                for (i in 0..originalLen) {
                    if (originalToTransformed[i] >= clamped) {
                        return i
                    }
                }
                return originalLen
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}

fun formatNumberWithDots(rawInput: String): String {
    val clean = rawInput.replace(".", "").filter { it.isDigit() }
    if (clean.isEmpty()) return ""
    return try {
        val number = clean.toDouble()
        DecimalFormat("#,###").format(number).replace(",", ".")
    } catch (e: Exception) {
        clean
    }
}

fun interpretBreakHours(input: String, fallback: Double = 1.5): Triple<String, String, Double> {
    val clean = input.trim()
    if (clean.isEmpty()) return Triple("", "giờ", fallback)
    
    val normalized = clean.replace(",", ".")
    val parsedDouble = normalized.toDoubleOrNull()
    
    // Check if contains dot
    if (clean.contains(".") || clean.contains(",")) {
        val d = parsedDouble ?: fallback
        return Triple(clean, "giờ", d)
    }
    
    val parsedInt = clean.toIntOrNull()
    if (parsedInt != null) {
        return if (parsedInt <= 8) {
            Triple(clean, "giờ", parsedInt.toDouble())
        } else {
            Triple(clean, "phút", parsedInt.toDouble() / 60.0)
        }
    }
    
    val d = parsedDouble ?: fallback
    return Triple(clean, "giờ", d)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TimeSnapViewModel
) {
    val context = LocalContext.current
    val configState by viewModel.userConfig.collectAsStateWithLifecycle()
    val syncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val sessionState by viewModel.currentUserSession.collectAsStateWithLifecycle()

    var latestVersionText by remember { mutableStateOf("") }
    var downloadUrlText by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    var publishedVersionCode by remember { mutableStateOf(0L) }
    var publishedVersionName by remember { mutableStateOf("") }
    var publishedDownloadUrl by remember { mutableStateOf("") }
    var loadErrorDetail by remember { mutableStateOf("") }

    val currentAppVersionCode = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (e: Exception) {
        1L
    }

    LaunchedEffect(Unit) {
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.FirestoreService.fetchPublishedVersion()
        }
        publishedVersionCode = result.versionCode
        publishedVersionName = result.versionName
        publishedDownloadUrl = result.downloadUrl
        if (result.isSuccess) {
            if (latestVersionText.isEmpty()) {
                latestVersionText = if (result.versionName.isNotEmpty()) result.versionName else (result.versionCode + 1).toString()
            }
            if (downloadUrlText.isEmpty()) {
                downloadUrlText = result.downloadUrl
            }
        } else {
            loadErrorDetail = result.errorMessage
        }
    }

    var isInitialized by remember { mutableStateOf(false) }

    val currentAppVersionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.4"
        } catch (e: Exception) {
            "1.4"
        }
    }
    var checkingUpdate by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<com.example.data.AppVersionControl?>(null) }

    var hoVaTen by remember { mutableStateOf("") }
    var maNhanVien by remember { mutableStateOf("") }
    var emailDangKy by remember { mutableStateOf("") }

    var luongCoBan by remember { mutableStateOf("") }
    var luongDongBaoHiem by remember { mutableStateOf("") }
    var tiLeDongBaoHiem by remember { mutableStateOf("") }
    var ngayChotLuong by remember { mutableStateOf("") }
    var doanPhiCongDoan by remember { mutableStateOf("") }

    var heSoOtNgayThuong by remember { mutableStateOf("") }
    var heSoOtChuNhat by remember { mutableStateOf("") }
    var heSoOtNgayLe by remember { mutableStateOf("") }

    var tienChuyenCanGoc by remember { mutableStateOf("") }
    var soNgayPhepNam by remember { mutableStateOf("") }

    // 12 Allowances
    var pcKyThuat by remember { mutableStateOf("") }
    var pcTrachNhiem by remember { mutableStateOf("") }
    var pcChucVu by remember { mutableStateOf("") }
    var pcHieuSuat by remember { mutableStateOf("") }
    var pcSanPham by remember { mutableStateOf("") }
    var pcComCa by remember { mutableStateOf("") }
    var pcComOt by remember { mutableStateOf("") }
    var pcNhaO by remember { mutableStateOf("") }
    var pcDocHai by remember { mutableStateOf("") }
    var pcDtDoanhThu by remember { mutableStateOf("") }
    var pcXangXe by remember { mutableStateOf("") }
    var pcKhac by remember { mutableStateOf("") }
    var pcKhac1 by remember { mutableStateOf("") }
    var pcThamNien by remember { mutableStateOf("") }
    var tinhKhauTruNghi by remember { mutableStateOf(false) }
    var soGioNghiGiaiLao by remember { mutableStateOf("") }

    // Sync database configuration state once initialized
    LaunchedEffect(configState) {
        if (configState != null && !isInitialized) {
            val c = configState!!
            hoVaTen = c.hoVaTen
            maNhanVien = c.maNhanVien
            emailDangKy = c.emailDangKy

            luongCoBan = c.luongCoBan.toLong().toString()
            luongDongBaoHiem = c.luongDongBaoHiem.toLong().toString()
            tiLeDongBaoHiem = c.tiLeDongBaoHiem.toString()
            ngayChotLuong = c.ngayChotLuong.toString()
            doanPhiCongDoan = c.doanPhiCongDoan.toLong().toString()

            heSoOtNgayThuong = c.heSoOtNgayThuong.toString()
            heSoOtChuNhat = c.heSoOtChuNhat.toString()
            heSoOtNgayLe = c.heSoOtNgayLe.toString()

            tienChuyenCanGoc = c.tienChuyenCanGoc.toLong().toString()
            soNgayPhepNam = c.soNgayPhepNam.toString()

            pcKyThuat = c.pcKyThuat.toLong().toString()
            pcTrachNhiem = c.pcTrachNhiem.toLong().toString()
            pcChucVu = c.pcChucVu.toLong().toString()
            pcHieuSuat = c.pcHieuSuat.toLong().toString()
            pcSanPham = c.pcSanPham.toLong().toString()
            pcComCa = c.pcComCa.toLong().toString()
            pcComOt = c.pcComOt.toLong().toString()
            pcNhaO = c.pcNhaO.toLong().toString()
            pcDocHai = c.pcDocHai.toLong().toString()
            pcDtDoanhThu = c.pcDtDoanhThu.toLong().toString()
            pcXangXe = c.pcXangXe.toLong().toString()
            pcKhac = c.pcKhac.toLong().toString()
            pcKhac1 = c.pcKhac1.toLong().toString()
            pcThamNien = c.pcThamNien.toLong().toString()
            tinhKhauTruNghi = c.tinhKhauTruNghi
            soGioNghiGiaiLao = c.soGioNghiGiaiLao.toString()

            isInitialized = true
        }
    }

    // Live calculations formatting
    val lcbVal = luongCoBan.replace(".", "").toDoubleOrNull() ?: 0.0
    val hourlyPrice = lcbVal / 26.0 / 8.0
    val fmtPrice = DecimalFormat("#,###").format(hourlyPrice)

    val otThuongCoeff = heSoOtNgayThuong.toDoubleOrNull() ?: 1.5
    val otCnCoeff = heSoOtChuNhat.toDoubleOrNull() ?: 2.0
    val otLeCoeff = heSoOtNgayLe.toDoubleOrNull() ?: 3.0

    val otThuongPrice = hourlyPrice * otThuongCoeff
    val otCnPrice = hourlyPrice * otCnCoeff
    val otLePrice = hourlyPrice * otLeCoeff

    // Direct save trigger function
    val saveChanges = {
        val current = configState
        if (current != null) {
            val sLcb = luongCoBan.replace(".", "")
            val sLbh = luongDongBaoHiem.replace(".", "")
            val sDp = doanPhiCongDoan.replace(".", "")
            val sCc = tienChuyenCanGoc.replace(".", "")

            if (current.maNhanVien != maNhanVien && maNhanVien.isNotBlank()) {
                val sharedPrefs = context.getSharedPreferences("timesnap_auth", android.content.Context.MODE_PRIVATE)
                val email = current.emailDangKy.ifEmpty { viewModel.authController.currentUserFlow.value?.email ?: "" }
                if (email.isNotEmpty()) {
                    sharedPrefs.edit()
                        .remove("email_of_employee_${current.maNhanVien}")
                        .putString("email_of_employee_$maNhanVien", email)
                        .putString("maNhanVien_of_email_$email", maNhanVien)
                        .apply()
                }
            }

            val updated = current.copy(
                hoVaTen = hoVaTen,
                maNhanVien = maNhanVien,
                emailDangKy = emailDangKy,
                luongCoBan = sLcb.toDoubleOrNull() ?: current.luongCoBan,
                luongDongBaoHiem = sLbh.toDoubleOrNull() ?: current.luongDongBaoHiem,
                tiLeDongBaoHiem = tiLeDongBaoHiem.toDoubleOrNull() ?: current.tiLeDongBaoHiem,
                ngayChotLuong = ngayChotLuong.toIntOrNull() ?: current.ngayChotLuong,
                doanPhiCongDoan = sDp.toDoubleOrNull() ?: current.doanPhiCongDoan,
                heSoOtNgayThuong = heSoOtNgayThuong.toDoubleOrNull() ?: current.heSoOtNgayThuong,
                heSoOtChuNhat = heSoOtChuNhat.toDoubleOrNull() ?: current.heSoOtChuNhat,
                heSoOtNgayLe = heSoOtNgayLe.toDoubleOrNull() ?: current.heSoOtNgayLe,
                tienChuyenCanGoc = sCc.toDoubleOrNull() ?: current.tienChuyenCanGoc,
                soNgayPhepNam = soNgayPhepNam.toIntOrNull() ?: current.soNgayPhepNam,
                pcKyThuat = pcKyThuat.replace(".", "").toDoubleOrNull() ?: current.pcKyThuat,
                pcTrachNhiem = pcTrachNhiem.replace(".", "").toDoubleOrNull() ?: current.pcTrachNhiem,
                pcChucVu = pcChucVu.replace(".", "").toDoubleOrNull() ?: current.pcChucVu,
                pcHieuSuat = pcHieuSuat.replace(".", "").toDoubleOrNull() ?: current.pcHieuSuat,
                pcSanPham = pcSanPham.replace(".", "").toDoubleOrNull() ?: current.pcSanPham,
                pcComCa = pcComCa.replace(".", "").toDoubleOrNull() ?: current.pcComCa,
                pcComOt = pcComOt.replace(".", "").toDoubleOrNull() ?: current.pcComOt,
                pcNhaO = pcNhaO.replace(".", "").toDoubleOrNull() ?: current.pcNhaO,
                pcDocHai = pcDocHai.replace(".", "").toDoubleOrNull() ?: current.pcDocHai,
                pcDtDoanhThu = pcDtDoanhThu.replace(".", "").toDoubleOrNull() ?: current.pcDtDoanhThu,
                pcXangXe = pcXangXe.replace(".", "").toDoubleOrNull() ?: current.pcXangXe,
                pcKhac = pcKhac.replace(".", "").toDoubleOrNull() ?: current.pcKhac,
                pcKhac1 = pcKhac1.replace(".", "").toDoubleOrNull() ?: current.pcKhac1,
                pcThamNien = pcThamNien.replace(".", "").toDoubleOrNull() ?: current.pcThamNien,
                tinhKhauTruNghi = tinhKhauTruNghi,
                soGioNghiGiaiLao = interpretBreakHours(soGioNghiGiaiLao, current.soGioNghiGiaiLao).third
            )
            viewModel.updateSalaryConfig(updated)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HỆ SỐ LƯƠNG THEO HỢP ĐỒNG", fontWeight = FontWeight.Bold, color = White, fontSize = 16.sp) },
                actions = {
                    // Hidden as requested: "ở trên có chữ đã đồng bộ m cũng xoá luôn thông báo đó đi"
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // CATEGORY 0: HỒ SƠ NHÂN VIÊN
            CategoryLayout(title = "HỒ SƠ CÁ NHÂN NHÂN VIÊN", icon = Icons.Default.VerifiedUser) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigInputField(
                        label = "Họ và tên",
                        value = hoVaTen,
                        onValueChange = { hoVaTen = it; saveChanges() },
                        keyboardType = KeyboardType.Text
                    )
                    ConfigInputField(
                        label = "Mã nhân viên công ty (UID)",
                        value = maNhanVien,
                        onValueChange = { maNhanVien = it; saveChanges() },
                        keyboardType = KeyboardType.Text
                    )
                    ConfigInputField(
                        label = "Email đăng ký tài khoản (Gmail - Không thể sửa)",
                        value = emailDangKy.ifEmpty { viewModel.authController.currentUserFlow.value?.email ?: "Chưa có thiết lập" },
                        onValueChange = {},
                        keyboardType = KeyboardType.Email,
                        enabled = false
                    )
                }
            }

            // CATEGORY 1: HỢP ĐỒNG LƯƠNG & BẢO HIỂM
            CategoryLayout(title = "HỢP ĐỒNG LƯƠNG & BẢO HIỂM", icon = Icons.Default.Payments) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigInputField(
                        label = "Mức lương cơ bản hàng tháng (LCB)",
                        value = luongCoBan,
                        onValueChange = { luongCoBan = it.filter { c -> c.isDigit() }; saveChanges() },
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )

                    ConfigInputField(
                        label = "Lương đóng bảo hiểm xã hội (LBH)",
                        value = luongDongBaoHiem,
                        onValueChange = { luongDongBaoHiem = it.filter { c -> c.isDigit() }; saveChanges() },
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ConfigInputField(
                            label = "Tỉ lệ đóng BH (%)",
                            value = tiLeDongBaoHiem,
                            onValueChange = { tiLeDongBaoHiem = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "Ngày chốt lương",
                            value = ngayChotLuong,
                            onValueChange = { ngayChotLuong = it.filter { c -> c.isDigit() }; saveChanges() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ConfigInputField(
                        label = "Đoàn phí công đoàn",
                        value = doanPhiCongDoan,
                        onValueChange = { doanPhiCongDoan = it.filter { c -> c.isDigit() }; saveChanges() },
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )

                    // Dynamic calculated cost
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Lương mỗi giờ = LCB / 26 / 8 = $fmtPrice đ/giờ\nDùng làm căn cứ tính tăng ca chính xác.",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // CATEGORY 2: HỆ SỐ TĂNG CA (OT)
            CategoryLayout(title = "HỆ SỐ TĂNG CA (OT)", icon = Icons.Default.Percent) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ConfigInputField(
                            label = "Ngày thường",
                            value = heSoOtNgayThuong,
                            onValueChange = { heSoOtNgayThuong = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "Chủ nhật",
                            value = heSoOtChuNhat,
                            onValueChange = { heSoOtChuNhat = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "Ngày Lễ",
                            value = heSoOtNgayLe,
                            onValueChange = { heSoOtNgayLe = it; saveChanges() },
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Simulated live estimate
                    val dec = DecimalFormat("#,###")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• Tiền OT Ngày thường (${heSoOtNgayThuong}x): ${dec.format(otThuongPrice)} đ/giờ",
                            color = LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Tiền OT Chủ nhật (${heSoOtChuNhat}x): ${dec.format(otCnPrice)} đ/giờ",
                            color = AccentRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Tiền OT Ngày Lễ (${heSoOtNgayLe}x): ${dec.format(otLePrice)} đ/giờ",
                            color = AccentOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CATEGORY 2.5: KHẤU TRỪ THỜI GIAN NGHỈ TRONG CA
            CategoryLayout(title = "KHẤU TRỪ THỜI GIAN NGHỈ TRONG CA", icon = Icons.Default.Settings) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bật khấu trừ thời gian nghỉ",
                                color = White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tự động trừ thời gian nghỉ ra khỏi ca làm việc. Hãy tắt đi nếu bạn làm xuyên suốt không nghỉ trưa/chiều (ví dụ làm thông ca) để được tính đủ công và tăng ca.",
                                color = LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = tinhKhauTruNghi,
                            onCheckedChange = { 
                                tinhKhauTruNghi = it
                                saveChanges()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = NeonBlue,
                                uncheckedThumbColor = MediumGray,
                                uncheckedTrackColor = Color(0xFF1E1E1E)
                            )
                        )
                    }

                    if (tinhKhauTruNghi) {
                        ConfigInputField(
                            label = "Khấu trừ thời gian nghỉ (nhập sút phút vd: 30, hoặc số giờ vd: 1.5)",
                            value = soGioNghiGiaiLao,
                            onValueChange = { 
                                soGioNghiGiaiLao = it
                                saveChanges()
                            },
                            keyboardType = KeyboardType.Decimal
                        )

                        val interpretation = interpretBreakHours(soGioNghiGiaiLao)
                        val badgeText = if (interpretation.first.isNotEmpty() && interpretation.second != "không hợp lệ") {
                            "👉 Nhận diện thông minh: ${interpretation.first} được hiểu là ${interpretation.first} ${interpretation.second} = ${interpretation.third} giờ"
                        } else {
                            "👉 Vui lòng nhập số giờ (vd: 1.5) hoặc số phút nghỉ (vd: 30, 45, 60)..."
                        }

                        Text(
                            text = badgeText,
                            color = AccentOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeonBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(1.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            val computedHrs = interpretation.third
                            Text(
                                text = "Ví dụ: Nếu chấm công lúc 7h30 sáng:\n" +
                                       "• Nghỉ trưa từ 11h30 - 12h30 (1 tiếng)\n" +
                                       "• Nghỉ chiều từ 16h30 - 17h00 (30 phút)\n" +
                                       "• Tổng cộng nghỉ: $computedHrs giờ.\n" +
                                       "Hệ thống sẽ tự động trừ $computedHrs giờ ra khỏi thời gian làm việc và tự động tính đẩy giờ về (Ví dụ: đến 17h00 mới đủ 8 tiếng).",
                                color = NeonBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // CATEGORY 3: CHUYÊN CẦN & PHÉP NĂM
            CategoryLayout(title = "CHUYÊN CẦN & PHÉP NĂM", icon = Icons.Default.DateRange) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConfigInputField(
                        label = "Tiền chuyên cần gốc",
                        value = tienChuyenCanGoc,
                        onValueChange = { tienChuyenCanGoc = it.filter { c -> c.isDigit() }; saveChanges() },
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )
                    ConfigInputField(
                        label = "Số ngày phép cho phép/năm",
                        value = soNgayPhepNam,
                        onValueChange = { soNgayPhepNam = it.filter { c -> c.isDigit() }; saveChanges() }
                    )
                }
            }

            // CATEGORY 4: CÁC KHOẢN PHỤ CẤP (14 KHOẢN KHÁC NHAU)
            CategoryLayout(title = "CÁC KHOẢN PHỤ CẤP (14 KHOẢN KHÁC NHAU)", icon = Icons.Default.Star) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigInputField(
                            label = "1. Kỹ thuật",
                            value = pcKyThuat,
                            onValueChange = { pcKyThuat = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "2. Trách nhiệm",
                            value = pcTrachNhiem,
                            onValueChange = { pcTrachNhiem = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigInputField(
                            label = "3. Chức vụ",
                            value = pcChucVu,
                            onValueChange = { pcChucVu = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "4. Hiệu suất",
                            value = pcHieuSuat,
                            onValueChange = { pcHieuSuat = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigInputField(
                            label = "5. Sản phẩm",
                            value = pcSanPham,
                            onValueChange = { pcSanPham = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "6. Cơm/CA",
                            value = pcComCa,
                            onValueChange = { pcComCa = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigInputField(
                            label = "7. Cơm OT",
                            value = pcComOt,
                            onValueChange = { pcComOt = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "8. Nhà ở",
                            value = pcNhaO,
                            onValueChange = { pcNhaO = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigInputField(
                            label = "9. Độc hại",
                            value = pcDocHai,
                            onValueChange = { pcDocHai = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "10. Doanh thu",
                            value = pcDtDoanhThu,
                            onValueChange = { pcDtDoanhThu = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigInputField(
                            label = "11. Xăng xe",
                            value = pcXangXe,
                            onValueChange = { pcXangXe = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "12. Khác",
                            value = pcKhac,
                            onValueChange = { pcKhac = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConfigInputField(
                            label = "13. Khác 1",
                            value = pcKhac1,
                            onValueChange = { pcKhac1 = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                        ConfigInputField(
                            label = "14. Thâm niên",
                            value = pcThamNien,
                            onValueChange = { pcThamNien = it.filter { c -> c.isDigit() }; saveChanges() },
                            visualTransformation = ThousandSeparatorVisualTransformation(),
                            labelColor = NeonBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            CategoryLayout(title = "THÔNG TIN PHIÊN BẢN", icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Phiên bản hiện tại", color = LightGray, fontSize = 12.sp)
                            Text("v$currentAppVersionName (Build $currentAppVersionCode)", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                if (!checkingUpdate) {
                                    checkingUpdate = true
                                    coroutineScope.launch {
                                        try {
                                            val info = com.example.data.FirestoreService.checkAppVersion(context)
                                            checkingUpdate = false
                                            if (info != null && info.isNewVersionAvailable) {
                                                manualUpdateInfo = info
                                            } else {
                                                Toast.makeText(context, "Ứng dụng của bạn đã là phiên bản mới nhất!", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Throwable) {
                                            checkingUpdate = false
                                            Toast.makeText(context, "Không thể kiểm tra lúc này: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (checkingUpdate) MediumGray else NeonBlue,
                                contentColor = White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).testTag("check_update_button")
                        ) {
                            if (checkingUpdate) {
                                CircularProgressIndicator(color = White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Kiểm tra cập nhật", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (manualUpdateInfo != null) {
                val info = manualUpdateInfo!!
                AlertDialog(
                    onDismissRequest = { manualUpdateInfo = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "New Version Icon",
                                tint = AccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Bản Cập Nhật Mới!",
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "Đã có phiên bản mới v${info.latestVersionName}, vui lòng cập nhật để sử dụng tính năng mới nhất!",
                            color = White.copy(alpha = 0.85f),
                            fontSize = 15.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(info.downloadUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // fallback
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonBlue,
                                contentColor = White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cập nhật ngay", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { manualUpdateInfo = null }
                        ) {
                            Text("Để sau", color = White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = Color(0xFF1E1E1E),
                    tonalElevation = 6.dp
                )
            }

            val userEmail = sessionState?.email ?: ""
            val isDeveloper = userEmail.trim().lowercase() == "khoatubexxx@gmail.com" || userEmail.contains("admin")

            if (isDeveloper) {
                Spacer(modifier = Modifier.height(16.dp))
                CategoryLayout(title = "CÔNG CỤ PHÁT HÀNH CẬP NHẬT (CHỈ QUẢN TRỊ VIÊN)", icon = Icons.Default.VerifiedUser) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Hệ thống tự động đồng nhất phiên bản APK. Khi bạn cập nhật ở đây, tất cả người dùng khác khi mở app sẽ ngay lập tức được yêu cầu cập nhật lên phiên bản này.",
                            color = LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Bản hiện tại trên máy", color = LightGray, fontSize = 10.sp)
                                    Text("v$currentAppVersionCode", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Bản phát hành trên Cloud", color = LightGray, fontSize = 10.sp)
                                    Text(if (publishedVersionName.isNotEmpty()) publishedVersionName else if (publishedVersionCode > 0) "v$publishedVersionCode" else "Đang tải...", color = NeonBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (loadErrorDetail.isNotEmpty()) {
                            var showTroubleshootGuide by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
                                border = BorderStroke(1.dp, Color(0xFFE57373))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Chi tiết lỗi Firebase: ${loadErrorDetail.take(35)}...",
                                            color = Color(0xFFFFD2D2),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { showTroubleshootGuide = !showTroubleshootGuide },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(if (showTroubleshootGuide) "Thu gọn 🔼" else "Cách sửa 🛠️", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    if (showTroubleshootGuide) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Để sửa triệt để lỗi này, bạn cần vào Firebase Console chỉnh lại Rules (Luật bảo mật) của Firestore Database như sau:\n\n" +
                                                    "1️⃣ Truy cập console.firebase.google.com -> Vào dự án của bạn.\n" +
                                                    "2️⃣ Chọn Firestore Database ở menu bên trái -> Vào tab Rules.\n" +
                                                    "3️⃣ Thay thế toàn bộ Rules hiện có bằng đoạn mã sau:\n\n" +
                                                    "rules_version = '2';\n" +
                                                    "service cloud.firestore {\n" +
                                                    "  match /databases/{database}/documents {\n" +
                                                    "    match /app_config/version_control {\n" +
                                                    "      allow read, write: if true;\n" +
                                                    "    }\n" +
                                                    "    match /users/{userId}/{document=**} {\n" +
                                                    "      allow read, write: if request.auth != null && request.auth.uid == userId;\n" +
                                                    "    }\n" +
                                                    "  }\n" +
                                                    "}\n\n" +
                                                    "4️⃣ Nhấn nút 'Publish' (Xuất bản) để lưu. Sau đó, tắt ứng dụng đi bật lại sẽ kết nối thành công!",
                                            color = Color(0xFFECEFF1),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        ConfigInputField(
                            label = "Mã Phiên Bản Phát Hành Mới (Ví dụ: 1.3 hoặc 5)",
                            value = latestVersionText,
                            onValueChange = { latestVersionText = it.filter { c -> c.isDigit() || c == '.' } },
                            keyboardType = KeyboardType.Text,
                            labelColor = AccentOrange
                        )

                        ConfigInputField(
                            label = "Đường dẫn tải APK trực tiếp (Google Drive, v.v)",
                            value = downloadUrlText,
                            onValueChange = { downloadUrlText = it },
                            keyboardType = KeyboardType.Uri,
                            labelColor = AccentOrange
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                val inputStr = latestVersionText.trim()
                                if (inputStr.isBlank()) {
                                    Toast.makeText(context, "Mã phiên bản không được để trống!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val code = if (inputStr.contains(".")) {
                                    com.example.data.FirestoreService.parseVersionToCode(inputStr)
                                } else {
                                    inputStr.toLongOrNull() ?: 0L
                                }
                                if (code <= 0L) {
                                    Toast.makeText(context, "Mã phiên bản không hợp lệ!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (downloadUrlText.isBlank()) {
                                    Toast.makeText(context, "Đường dẫn APK không được để trống!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isPublishing = true
                                coroutineScope.launch {
                                    val success = com.example.data.FirestoreService.publishNewAppVersion(code, downloadUrlText.trim(), inputStr)
                                    isPublishing = false
                                    if (success) {
                                        publishedVersionCode = code
                                        publishedVersionName = inputStr
                                        publishedDownloadUrl = downloadUrlText.trim()
                                        Toast.makeText(context, "Đã phát hành bản cập nhật mớii: $inputStr thành công!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Lỗi xảy ra khi phát hành cập nhật!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            enabled = !isPublishing
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(color = White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Phát Hành Bản Mới Ngay 🚀", color = White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Giải pháp tốt nhất cho người lao động",
                color = LightGray.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CategoryLayout(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = title, tint = NeonBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            content()
        }
    }
}

@Composable
fun ConfigInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    labelColor: Color = LightGray,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = labelColor) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = labelColor,
            focusedTextColor = White,
            unfocusedTextColor = White,
            unfocusedBorderColor = Color(0xFF2C2C2C),
            focusedLabelColor = labelColor,
            disabledTextColor = White.copy(alpha = 0.6f),
            disabledBorderColor = Color(0xFF2C2C2C).copy(alpha = 0.5f),
            disabledLabelColor = labelColor.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    )
}
