package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.UserConfig
import com.example.viewmodel.SalarySummary
import com.example.viewmodel.TimeSnapViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipScreen(viewModel: TimeSnapViewModel) {
    val summary by viewModel.salarySummary.collectAsState()
    val config by viewModel.userConfig.collectAsState()
    val currentMonthStr by viewModel.currentMonth.collectAsState()

    val fmt = DecimalFormat("#,###")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phiếu Lương Chi Tiết") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Personal Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = config.hoVaTen.ifBlank { "Nhân Viên" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (config.maNhanVien.isNotBlank()) {
                            Text(
                                text = "Mã NV: ${config.maNhanVien}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Tháng $currentMonthStr",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // 2. Proportional items calculation card (The 4 core items calculated via the requested formula)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1. Các Mục Tính Theo Ngày Công (Ngày công thực tế: ${summary.workingDays} ngày)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Item 1: Lương
                    PayslipRow(
                        label = "Lương",
                        value = "${fmt.format(summary.baseSalaryReceived)} đ"
                    )

                    // Item 2: Trách nhiệm
                    PayslipRow(
                        label = "Trách nhiệm",
                        value = "${fmt.format(summary.responsibilityPayReceived)} đ"
                    )

                    // Item 3: Kỹ thuật
                    PayslipRow(
                        label = "Kỹ thuật",
                        value = "${fmt.format(summary.technicalAllowanceReceived)} đ"
                    )

                    // Item 4: Chuyên cần
                    PayslipRow(
                        label = "Chuyên cần",
                        value = "${fmt.format(summary.diligenceAllowanceReceived)} đ"
                    )
                }
            }

            // 3. Overtime Pay Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "2. Tiền Tăng Ca (Overtime)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Weekday OT
                    if (summary.overtimeHoursWeekday > 0) {
                        PayslipRow(
                            label = "OT Ngày Thường (${summary.overtimeHoursWeekday} giờ)",
                            value = "${fmt.format(summary.overtimePayWeekday)} đ"
                        )
                    }

                    // Sunday OT
                    if (summary.overtimeHoursSunday > 0) {
                        PayslipRow(
                            label = "OT Chủ Nhật (${summary.overtimeHoursSunday} giờ)",
                            value = "${fmt.format(summary.overtimePaySunday)} đ"
                        )
                    }

                    // Holiday OT
                    if (summary.overtimeHoursHoliday > 0) {
                        PayslipRow(
                            label = "OT Ngày Lễ (${summary.overtimeHoursHoliday} giờ)",
                            value = "${fmt.format(summary.overtimePayHoliday)} đ"
                        )
                    }

                    if (summary.totalOvertimePay == 0.0) {
                        Text(
                            "Không có số giờ tăng ca trong tháng này.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Divider()
                        PayslipRow(
                            label = "Tổng tiền OT",
                            value = "${fmt.format(summary.totalOvertimePay)} đ",
                            isBold = true,
                            textColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 4. Flat Allowances & Other Incomes
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "3. Phụ Cấp Khác & Tiền Thưởng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (summary.tienComNhan > 0) {
                        PayslipRow(
                            label = "Tiền Cơm (${fmt.format(config.tienComMoiNgay)}đ * ${summary.workingDays} công)",
                            value = "${fmt.format(summary.tienComNhan)} đ"
                        )
                    }

                    if (config.pcChucVu > 0) PayslipRow(label = "Phụ Cấp Chức Vụ", value = "${fmt.format(config.pcChucVu)} đ")
                    if (config.pcHieuSuat > 0) PayslipRow(label = "Phụ Cấp Hiệu Suất", value = "${fmt.format(config.pcHieuSuat)} đ")
                    if (config.pcNhaO > 0) PayslipRow(label = "Phụ Cấp Nhà Ở", value = "${fmt.format(config.pcNhaO)} đ")
                    if (config.pcXangXe > 0) PayslipRow(label = "Phụ Cấp Xăng Xe", value = "${fmt.format(config.pcXangXe)} đ")
                    if (config.pcKhac > 0) PayslipRow(label = "Phụ Cấp Khác", value = "${fmt.format(config.pcKhac)} đ")
                    if (config.thuong > 0) PayslipRow(label = "Thưởng Thêm", value = "${fmt.format(config.thuong)} đ")

                    val hasOtherIncomes = summary.tienComNhan > 0 || config.pcChucVu > 0 || config.pcHieuSuat > 0 ||
                            config.pcNhaO > 0 || config.pcXangXe > 0 || config.pcKhac > 0 || config.thuong > 0

                    if (!hasOtherIncomes) {
                        Text(
                            "Không có phụ cấp cố định hoặc tiền thưởng.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 5. Deductions (BHXH & Union Fee)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "4. Khoản Khấu Trừ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    PayslipRow(
                        label = "Bảo Hiểm Xã Hội (${config.tiLeDongBaoHiem}%)",
                        value = "-${fmt.format(summary.bhxhDeduction)} đ"
                    )

                    if (config.doanPhiCongDoan > 0) {
                        PayslipRow(
                            label = "Đoàn Phí Công Đoàn",
                            value = "-${fmt.format(summary.unionFeeDeduction)} đ"
                        )
                    }

                    Divider()

                    PayslipRow(
                        label = "Tổng khấu trừ",
                        value = "-${fmt.format(summary.totalDeductions)} đ",
                        isBold = true,
                        textColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 6. Net Take Home (Thực Lĩnh) Highlight Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "THỰC LĨNH CUỐI CÙNG",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )

                    Text(
                        text = "${fmt.format(summary.netSalary)} VNĐ",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )

                    Text(
                        text = "Đã tính đầy đủ các khoản trợ cấp, tăng ca và khấu trừ bảo hiểm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun PayslipRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    textColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold && textColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else textColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PayslipRowWithFormula(
    label: String,
    normVal: Double,
    workingDays: Double,
    receivedVal: Double,
    formatter: DecimalFormat
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatter.format(receivedVal)} đ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = "Tính theo công thức: (${formatter.format(normVal)} / 26) * $workingDays",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
