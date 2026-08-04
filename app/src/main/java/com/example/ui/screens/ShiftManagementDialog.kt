package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ShiftEntity
import com.example.data.repository.ShiftRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementDialog(
    shiftRepository: ShiftRepository,
    companyId: String = "default_company",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var shifts by remember { mutableStateOf<List<ShiftEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }

    fun refreshShifts() {
        coroutineScope.launch {
            isLoading = true
            shifts = shiftRepository.getShifts(companyId)
            isLoading = false
        }
    }

    LaunchedEffect(companyId) {
        refreshShifts()
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
                                text = "Cấu hình ca làm việc",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                text = "Quản lý danh sách ca cho công ty",
                                fontSize = 12.sp,
                                color = LightGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_shift_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Add Shift
                Button(
                    onClick = {
                        editingShift = null
                        showEditDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_shift_button"),
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
                        text = "Thêm ca làm việc mới",
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
                } else if (shifts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có ca làm việc nào. Hãy thêm ca mới!",
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
                        items(shifts, key = { it.id }) { shift ->
                            ShiftItemCard(
                                shift = shift,
                                onToggleEnabled = { enabled ->
                                    coroutineScope.launch {
                                        shiftRepository.saveShift(shift.copy(enabled = enabled))
                                        refreshShifts()
                                    }
                                },
                                onEdit = {
                                    editingShift = shift
                                    showEditDialog = true
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        shiftRepository.deleteShift(shift.id)
                                        refreshShifts()
                                        Toast.makeText(context, "Đã xóa ca: ${shift.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        ShiftEditFormDialog(
            shiftToEdit = editingShift,
            companyId = companyId,
            onDismiss = { showEditDialog = false },
            onSave = { newShift ->
                coroutineScope.launch {
                    try {
                        shiftRepository.saveShift(newShift)
                        showEditDialog = false
                        refreshShifts()
                        Toast.makeText(context, "Đã lưu ca làm việc thành công", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Lỗi khi lưu ca", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ShiftItemCard(
    shift: ShiftEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val durationHrs = ShiftEntity.calculateDurationHours(
        startTime = shift.startTime,
        endTime = shift.endTime,
        crossesMidnight = shift.crossesMidnight,
        breakMinutes = shift.breakMinutes
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shift_item_${shift.id}"),
        colors = CardDefaults.cardColors(
            containerColor = DarkBackground
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (shift.enabled) NeonBlue.copy(alpha = 0.3f) else MediumGray)
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
                    Text(
                        text = shift.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (shift.enabled) White else LightGray
                    )

                    if (shift.crossesMidnight) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = AccentOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nightlight,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Qua đêm",
                                    fontSize = 10.sp,
                                    color = AccentOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Switch(
                    checked = shift.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = AccentGreen,
                        uncheckedThumbColor = LightGray,
                        uncheckedTrackColor = MediumGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${shift.startTime} → ${shift.endTime}${if (shift.crossesMidnight) " (+1 ngày)" else ""}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nghỉ: ${shift.breakMinutes} ph | Tiêu chuẩn: ${shift.standardHours}h | Tổng: ${String.format("%.1fh", durationHrs)}",
                        fontSize = 12.sp,
                        color = LightGray
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_shift_${shift.id}")
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
                        modifier = Modifier.testTag("delete_shift_${shift.id}")
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
        }
    }
}

@Composable
fun ShiftEditFormDialog(
    shiftToEdit: ShiftEntity?,
    companyId: String,
    onDismiss: () -> Unit,
    onSave: (ShiftEntity) -> Unit
) {
    var name by remember { mutableStateOf(shiftToEdit?.name ?: "") }
    var startTimeRaw by remember { mutableStateOf(shiftToEdit?.startTime?.replace(":", "") ?: "0730") }
    var endTimeRaw by remember { mutableStateOf(shiftToEdit?.endTime?.replace(":", "") ?: "1630") }
    var breakMinutesStr by remember { mutableStateOf(shiftToEdit?.breakMinutes?.toString() ?: "60") }
    var standardHoursStr by remember { mutableStateOf(shiftToEdit?.standardHours?.toString() ?: "8.0") }
    var crossesMidnight by remember { mutableStateOf(shiftToEdit?.crossesMidnight ?: false) }
    var enabled by remember { mutableStateOf(shiftToEdit?.enabled ?: true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun formatRawToHhMm(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        val padded = digits.padEnd(4, '0').take(4)
        return "${padded.substring(0, 2)}:${padded.substring(2, 4)}"
    }

    val startTimeFormatted = formatRawToHhMm(startTimeRaw)
    val endTimeFormatted = formatRawToHhMm(endTimeRaw)

    // Auto-detect overnight shift
    LaunchedEffect(startTimeRaw, endTimeRaw) {
        val autoNight = ShiftEntity.isOvernight(startTimeFormatted, endTimeFormatted)
        if (autoNight) {
            crossesMidnight = true
        }
    }

    val durationHours = ShiftEntity.calculateDurationHours(
        startTime = startTimeFormatted,
        endTime = endTimeFormatted,
        crossesMidnight = crossesMidnight,
        breakMinutes = breakMinutesStr.toIntOrNull() ?: 0
    )

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
                    .padding(20.dp)
            ) {
                Text(
                    text = if (shiftToEdit == null) "Thêm ca làm việc mới" else "Chỉnh sửa ca làm việc",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên ca (ví dụ: Ca sáng, Ca đêm)", color = LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shift_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = MediumGray,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Start time & End time in row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startTimeRaw,
                        onValueChange = { input ->
                            // Strictly allow digits only, max 4 characters
                            val digitsOnly = input.filter { it.isDigit() }.take(4)
                            startTimeRaw = digitsOnly
                        },
                        label = { Text("Giờ bắt đầu (HH:mm)", color = LightGray) },
                        placeholder = { Text("0730", color = MediumGray) },
                        supportingText = { Text("Hiển thị: $startTimeFormatted", color = NeonBlue, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shift_start_time_input"),
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
                        value = endTimeRaw,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }.take(4)
                            endTimeRaw = digitsOnly
                        },
                        label = { Text("Giờ kết thúc (HH:mm)", color = LightGray) },
                        placeholder = { Text("1630", color = MediumGray) },
                        supportingText = { Text("Hiển thị: $endTimeFormatted", color = NeonBlue, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shift_end_time_input"),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Break minutes & Standard hours
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = breakMinutesStr,
                        onValueChange = { input ->
                            breakMinutesStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Nghỉ (phút)", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shift_break_minutes_input"),
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
                        label = { Text("Giờ công chuẩn", color = LightGray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shift_standard_hours_input"),
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

                // Crosses Midnight & Enabled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ca qua đêm (+1 ngày)",
                        color = White,
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = crossesMidnight,
                        onCheckedChange = { crossesMidnight = it },
                        modifier = Modifier.testTag("shift_crosses_midnight_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Duration Calculation Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Thời gian thực tế: ${String.format("%.1f", durationHours)} giờ (trừ ${breakMinutesStr.ifEmpty { "0" }} phút nghỉ)",
                        color = AccentGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
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
                            val breakMin = breakMinutesStr.toIntOrNull() ?: 0
                            val stdHrs = standardHoursStr.toDoubleOrNull() ?: 8.0

                            val validationErr = ShiftEntity.validateShift(
                                name = name,
                                startTime = startTimeFormatted,
                                endTime = endTimeFormatted,
                                breakMinutes = breakMin,
                                standardHours = stdHrs
                            )

                            if (validationErr != null) {
                                errorMessage = validationErr
                                return@Button
                            }

                            val shiftId = shiftToEdit?.id ?: "shift_${System.currentTimeMillis()}"
                            val newShift = ShiftEntity(
                                id = shiftId,
                                companyId = companyId,
                                name = name.trim(),
                                startTime = startTimeFormatted,
                                endTime = endTimeFormatted,
                                breakMinutes = breakMin,
                                standardHours = stdHrs,
                                crossesMidnight = crossesMidnight,
                                enabled = enabled
                            )
                            onSave(newShift)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_shift_button")
                    ) {
                        Text("Lưu ca", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
