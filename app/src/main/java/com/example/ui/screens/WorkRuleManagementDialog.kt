package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WorkRule
import com.example.data.repository.WorkRuleRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkRuleManagementDialog(
    workRuleRepository: WorkRuleRepository,
    companyId: String = "default_company",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<WorkRule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<WorkRule?>(null) }

    fun refreshRules() {
        coroutineScope.launch {
            isLoading = true
            rules = workRuleRepository.getWorkRulesList(companyId)
            if (rules.isEmpty()) {
                // Create default rule v1 if empty
                val defaultR = WorkRule.createDefault(companyId)
                workRuleRepository.saveWorkRule(defaultR)
                rules = workRuleRepository.getWorkRulesList(companyId)
            }
            isLoading = false
        }
    }

    LaunchedEffect(companyId) {
        refreshRules()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quy tắc giờ công & Phiên bản",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                text = "Quản lý WorkRule theo phiên bản công ty",
                                fontSize = 12.sp,
                                color = LightGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_work_rule_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Add New Version Rule
                Button(
                    onClick = {
                        val maxVersion = rules.maxOfOrNull { it.version } ?: 0
                        val newVersion = maxVersion + 1
                        val now = System.currentTimeMillis()
                        editingRule = WorkRule(
                            id = "rule_${companyId}_v${newVersion}_$now",
                            companyId = companyId,
                            name = "Quy tắc phiên bản v$newVersion",
                            version = newVersion,
                            standardHoursPerDay = rules.firstOrNull()?.standardHoursPerDay ?: 8.0,
                            overtimeStartAfterHours = rules.firstOrNull()?.overtimeStartAfterHours ?: 8.0,
                            roundingMinutes = rules.firstOrNull()?.roundingMinutes ?: 15,
                            lateToleranceMinutes = rules.firstOrNull()?.lateToleranceMinutes ?: 5,
                            earlyLeaveToleranceMinutes = rules.firstOrNull()?.earlyLeaveToleranceMinutes ?: 5,
                            createdAt = now,
                            updatedAt = now
                        )
                        showEditDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_work_rule_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tạo phiên bản quy tắc mới (New Version)",
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonBlue)
                    }
                } else if (rules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có quy tắc giờ công nào.",
                            color = LightGray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rules, key = { it.id }) { rule ->
                            WorkRuleItemCard(
                                rule = rule,
                                onEdit = {
                                    editingRule = rule
                                    showEditDialog = true
                                },
                                onDelete = {
                                    if (rules.size <= 1) {
                                        Toast.makeText(context, "Phải giữ lại ít nhất 1 phiên bản quy tắc", Toast.LENGTH_SHORT).show()
                                        return@WorkRuleItemCard
                                    }
                                    coroutineScope.launch {
                                        workRuleRepository.deleteWorkRule(rule.id)
                                        refreshRules()
                                        Toast.makeText(context, "Đã xóa quy tắc v${rule.version}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && editingRule != null) {
        WorkRuleEditFormDialog(
            ruleToEdit = editingRule!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedRule ->
                coroutineScope.launch {
                    try {
                        workRuleRepository.saveWorkRule(updatedRule)
                        showEditDialog = false
                        refreshRules()
                        Toast.makeText(context, "Đã lưu quy tắc phiên bản v${updatedRule.version} thành công", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Lỗi khi lưu quy tắc", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun WorkRuleItemCard(
    rule: WorkRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("work_rule_item_${rule.id}"),
        colors = CardDefaults.cardColors(
            containerColor = DarkBackground
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (rule.enabled) NeonBlue.copy(alpha = 0.3f) else MediumGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NeonBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "v${rule.version}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = NeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = rule.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_work_rule_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Sửa",
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_work_rule_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Giờ chuẩn/ngày: ${rule.standardHoursPerDay}h | OT bắt đầu từ: ${rule.overtimeStartAfterHours}h | Làm tròn: ${rule.roundingMinutes}p (${rule.roundingMode})",
                fontSize = 13.sp,
                color = LightGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dung sai trễ: ${rule.lateToleranceMinutes}p | Dung sai về sớm: ${rule.earlyLeaveToleranceMinutes}p | Tính nghỉ: ${rule.breakCalculationMode}",
                fontSize = 12.sp,
                color = LightGray
            )
        }
    }
}

@Composable
fun WorkRuleEditFormDialog(
    ruleToEdit: WorkRule,
    onDismiss: () -> Unit,
    onSave: (WorkRule) -> Unit
) {
    var name by remember { mutableStateOf(ruleToEdit.name) }
    var standardHoursStr by remember { mutableStateOf(ruleToEdit.standardHoursPerDay.toString()) }
    var overtimeHoursStr by remember { mutableStateOf(ruleToEdit.overtimeStartAfterHours.toString()) }
    var roundingMinutesStr by remember { mutableStateOf(ruleToEdit.roundingMinutes.toString()) }
    var lateToleranceStr by remember { mutableStateOf(ruleToEdit.lateToleranceMinutes.toString()) }
    var earlyLeaveToleranceStr by remember { mutableStateOf(ruleToEdit.earlyLeaveToleranceMinutes.toString()) }
    var breakMode by remember { mutableStateOf(ruleToEdit.breakCalculationMode) }
    var roundingMode by remember { mutableStateOf(ruleToEdit.roundingMode) }
    var enabled by remember { mutableStateOf(ruleToEdit.enabled) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = DarkContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Chỉnh sửa Quy tắc giờ công (Version ${ruleToEdit.version})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên quy tắc", color = LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = MediumGray,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Standard Hours & OT Start
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = standardHoursStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val firstDotIndex = filtered.indexOf('.')
                            standardHoursStr = if (firstDotIndex != -1) {
                                filtered.substring(0, firstDotIndex + 1) + filtered.substring(firstDotIndex + 1).replace(".", "")
                            } else {
                                filtered
                            }
                        },
                        label = { Text("Giờ chuẩn/ngày", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_standard_hours_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = MediumGray,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = overtimeHoursStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val firstDotIndex = filtered.indexOf('.')
                            overtimeHoursStr = if (firstDotIndex != -1) {
                                filtered.substring(0, firstDotIndex + 1) + filtered.substring(firstDotIndex + 1).replace(".", "")
                            } else {
                                filtered
                            }
                        },
                        label = { Text("OT sau (giờ)", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_overtime_hours_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = MediumGray,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rounding minutes & Late tolerance & Early leave tolerance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = roundingMinutesStr,
                        onValueChange = { input ->
                            roundingMinutesStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Làm tròn (ph)", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_rounding_minutes_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = MediumGray,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lateToleranceStr,
                        onValueChange = { input ->
                            lateToleranceStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Dung sai trễ(p)", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_late_tolerance_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = MediumGray,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = earlyLeaveToleranceStr,
                        onValueChange = { input ->
                            earlyLeaveToleranceStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Về sớm(p)", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_early_leave_tolerance_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = MediumGray,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy", color = LightGray)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val stdHrs = standardHoursStr.toDoubleOrNull() ?: 8.0
                            val otHrs = overtimeHoursStr.toDoubleOrNull() ?: 8.0
                            val rndMins = roundingMinutesStr.toIntOrNull() ?: 15
                            val lateTol = lateToleranceStr.toIntOrNull() ?: 5
                            val earlyTol = earlyLeaveToleranceStr.toIntOrNull() ?: 5

                            val err = WorkRule.validateWorkRule(
                                name = name,
                                standardHoursPerDay = stdHrs,
                                overtimeStartAfterHours = otHrs,
                                roundingMinutes = rndMins,
                                lateToleranceMinutes = lateTol,
                                earlyLeaveToleranceMinutes = earlyTol
                            )

                            if (err != null) {
                                errorMessage = err
                                return@Button
                            }

                            val updated = ruleToEdit.copy(
                                name = name.trim(),
                                standardHoursPerDay = stdHrs,
                                overtimeStartAfterHours = otHrs,
                                roundingMinutes = rndMins,
                                lateToleranceMinutes = lateTol,
                                earlyLeaveToleranceMinutes = earlyTol,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_work_rule_button")
                    ) {
                        Text("Lưu quy tắc", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
