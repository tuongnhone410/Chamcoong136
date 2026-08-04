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
import androidx.compose.material.icons.filled.Schedule
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
import com.example.data.model.OvertimeRule
import com.example.data.repository.OvertimeRuleRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeRuleManagementDialog(
    overtimeRuleRepository: OvertimeRuleRepository,
    companyId: String = "default_company",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var rules by remember { mutableStateOf<List<OvertimeRule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<OvertimeRule?>(null) }

    fun refreshRules() {
        coroutineScope.launch {
            isLoading = true
            rules = overtimeRuleRepository.getOvertimeRulesList(companyId)
            if (rules.isEmpty()) {
                val defaultR = OvertimeRule.createDefault(companyId)
                overtimeRuleRepository.saveOvertimeRule(defaultR)
                rules = overtimeRuleRepository.getOvertimeRulesList(companyId)
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
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quy tắc tăng ca (OvertimeRule)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                text = "Cấu hình hệ số OT theo phiên bản công ty",
                                fontSize = 12.sp,
                                color = LightGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_overtime_rule_dialog_button")
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
                        editingRule = OvertimeRule(
                            id = "ot_rule_${companyId}_v${newVersion}_$now",
                            companyId = companyId,
                            name = "Quy tắc tăng ca v$newVersion",
                            version = newVersion,
                            normalDayMultiplier = rules.firstOrNull()?.normalDayMultiplier ?: 1.5,
                            weeklyOffMultiplier = rules.firstOrNull()?.weeklyOffMultiplier ?: 2.0,
                            holidayMultiplier = rules.firstOrNull()?.holidayMultiplier ?: 3.0,
                            minimumOvertimeMinutes = rules.firstOrNull()?.minimumOvertimeMinutes ?: 30,
                            roundingMinutes = rules.firstOrNull()?.roundingMinutes ?: 15,
                            createdAt = now,
                            updatedAt = now
                        )
                        showEditDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_overtime_rule_button"),
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
                        text = "Tạo phiên bản OT mới (New Version)",
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
                            text = "Chưa có quy tắc tăng ca nào.",
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
                            OvertimeRuleItemCard(
                                rule = rule,
                                onEdit = {
                                    editingRule = rule
                                    showEditDialog = true
                                },
                                onDelete = {
                                    if (rules.size <= 1) {
                                        Toast.makeText(context, "Phải giữ lại ít nhất 1 phiên bản quy tắc OT", Toast.LENGTH_SHORT).show()
                                        return@OvertimeRuleItemCard
                                    }
                                    coroutineScope.launch {
                                        overtimeRuleRepository.deleteOvertimeRule(rule.id)
                                        refreshRules()
                                        Toast.makeText(context, "Đã xóa quy tắc OT v${rule.version}", Toast.LENGTH_SHORT).show()
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
        OvertimeRuleEditFormDialog(
            ruleToEdit = editingRule!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedRule ->
                coroutineScope.launch {
                    try {
                        overtimeRuleRepository.saveOvertimeRule(updatedRule)
                        showEditDialog = false
                        refreshRules()
                        Toast.makeText(context, "Đã lưu quy tắc OT v${updatedRule.version} thành công", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Lỗi khi lưu quy tắc OT", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun OvertimeRuleItemCard(
    rule: OvertimeRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overtime_rule_item_${rule.id}"),
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
                        modifier = Modifier.testTag("edit_ot_rule_${rule.id}")
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
                        modifier = Modifier.testTag("delete_ot_rule_${rule.id}")
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
                text = "Ngày thường: ${rule.normalDayMultiplier}x | Ngày nghỉ: ${rule.weeklyOffMultiplier}x | Ngày lễ: ${rule.holidayMultiplier}x",
                fontSize = 13.sp,
                color = LightGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "OT tối thiểu: ${rule.minimumOvertimeMinutes} phút | Làm tròn: ${rule.roundingMinutes} phút (${rule.roundingMode})",
                fontSize = 12.sp,
                color = LightGray
            )
        }
    }
}

@Composable
fun OvertimeRuleEditFormDialog(
    ruleToEdit: OvertimeRule,
    onDismiss: () -> Unit,
    onSave: (OvertimeRule) -> Unit
) {
    var name by remember { mutableStateOf(ruleToEdit.name) }
    var normalMultiplierStr by remember { mutableStateOf(ruleToEdit.normalDayMultiplier.toString()) }
    var weeklyOffMultiplierStr by remember { mutableStateOf(ruleToEdit.weeklyOffMultiplier.toString()) }
    var holidayMultiplierStr by remember { mutableStateOf(ruleToEdit.holidayMultiplier.toString()) }
    var minOtMinutesStr by remember { mutableStateOf(ruleToEdit.minimumOvertimeMinutes.toString()) }
    var roundingMinutesStr by remember { mutableStateOf(ruleToEdit.roundingMinutes.toString()) }

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
                    text = "Chỉnh sửa Quy tắc tăng ca (Version ${ruleToEdit.version})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên quy tắc OT", color = LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ot_rule_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = MediumGray,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Multipliers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = normalMultiplierStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val firstDotIndex = filtered.indexOf('.')
                            normalMultiplierStr = if (firstDotIndex != -1) {
                                filtered.substring(0, firstDotIndex + 1) + filtered.substring(firstDotIndex + 1).replace(".", "")
                            } else {
                                filtered
                            }
                        },
                        label = { Text("Ngày thường", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ot_normal_multiplier_input"),
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
                        value = weeklyOffMultiplierStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val firstDotIndex = filtered.indexOf('.')
                            weeklyOffMultiplierStr = if (firstDotIndex != -1) {
                                filtered.substring(0, firstDotIndex + 1) + filtered.substring(firstDotIndex + 1).replace(".", "")
                            } else {
                                filtered
                            }
                        },
                        label = { Text("Ngày nghỉ", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ot_weekly_off_multiplier_input"),
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
                        value = holidayMultiplierStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val firstDotIndex = filtered.indexOf('.')
                            holidayMultiplierStr = if (firstDotIndex != -1) {
                                filtered.substring(0, firstDotIndex + 1) + filtered.substring(firstDotIndex + 1).replace(".", "")
                            } else {
                                filtered
                            }
                        },
                        label = { Text("Ngày lễ", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ot_holiday_multiplier_input"),
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

                // Minimum minutes & Rounding minutes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minOtMinutesStr,
                        onValueChange = { input ->
                            minOtMinutesStr = input.filter { it.isDigit() }
                        },
                        label = { Text("OT tối thiểu (phút)", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ot_min_minutes_input"),
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
                        value = roundingMinutesStr,
                        onValueChange = { input ->
                            roundingMinutesStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Làm tròn (phút)", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ot_rounding_minutes_input"),
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
                            val normalMult = normalMultiplierStr.toDoubleOrNull() ?: 1.5
                            val offMult = weeklyOffMultiplierStr.toDoubleOrNull() ?: 2.0
                            val holMult = holidayMultiplierStr.toDoubleOrNull() ?: 3.0
                            val minMins = minOtMinutesStr.toIntOrNull() ?: 30
                            val rndMins = roundingMinutesStr.toIntOrNull() ?: 15

                            val err = OvertimeRule.validateOvertimeRule(
                                name = name,
                                normalDayMultiplier = normalMult,
                                weeklyOffMultiplier = offMult,
                                holidayMultiplier = holMult,
                                minimumOvertimeMinutes = minMins,
                                roundingMinutes = rndMins
                            )

                            if (err != null) {
                                errorMessage = err
                                return@Button
                            }

                            val updated = ruleToEdit.copy(
                                name = name.trim(),
                                normalDayMultiplier = normalMult,
                                weeklyOffMultiplier = offMult,
                                holidayMultiplier = holMult,
                                minimumOvertimeMinutes = minMins,
                                roundingMinutes = rndMins,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_overtime_rule_button")
                    ) {
                        Text("Lưu quy tắc OT", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
