package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TimeEntry
import com.example.viewmodel.TimeSnapViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TimeSnapViewModel) {
    val currentMonthStr by viewModel.currentMonth.collectAsState()
    val entries by viewModel.monthTimeEntries.collectAsState()
    val summary by viewModel.salarySummary.collectAsState()
    val config by viewModel.userConfig.collectAsState()
    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US)
    val parsedDate = try { sdfMonth.parse(currentMonthStr) ?: Date() } catch (e: Exception) { Date() }
    calendar.time = parsedDate

    val targetYear = calendar.get(Calendar.YEAR)
    val targetMonth = calendar.get(Calendar.MONTH) + 1 // 1-indexed

    // States for logging days
    var selectedDayToEdit by remember { mutableStateOf<Int?>(null) }
    var editCheckInTime by remember { mutableStateOf<String?>(null) }
    var editCheckOutTime by remember { mutableStateOf<String?>(null) }
    var editDayType by remember { mutableStateOf("NORMAL") }
    var editNote by remember { mutableStateOf("") }
    var editIsWorking by remember { mutableStateOf(true) }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Calculate days offset for the first day of month
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    // Calendar.SUNDAY is 1. If we want Monday as first column, we offset. Let's make Sunday first column for simplicity.
    val leadingEmptyCells = firstDayOfWeek - 1

    val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Quick check-in/out logic
    val todayStr = sdfFull.format(Date())
    val todayEntry = entries.find { it.date == todayStr }

    fun navigateMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = parsedDate
        cal.add(Calendar.MONTH, offset)
        viewModel.setMonth(sdfMonth.format(cal.time))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navigateMonth(-1) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Tháng trước", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "Tháng $targetMonth - $targetYear",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { navigateMonth(1) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Tháng sau", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // 2. Today's Check In / Out Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hôm nay: $todayStr",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val updatedEntry = todayEntry?.copy(checkInTime = now, isWorking = true)
                                ?: TimeEntry(userId = "default_user", date = todayStr, checkInTime = now, isWorking = true)
                            viewModel.saveTimeEntry(updatedEntry)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("check_in_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (todayEntry?.checkInTime != null) {
                                val sdf = SimpleDateFormat("HH:mm", Locale.US)
                                "Đã Vào: " + sdf.format(Date(todayEntry.checkInTime))
                            } else "Vào Ca"
                        )
                    }

                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (todayEntry != null) {
                                viewModel.saveTimeEntry(todayEntry.copy(checkOutTime = now))
                            } else {
                                viewModel.saveTimeEntry(
                                    TimeEntry(userId = "default_user", date = todayStr, checkInTime = now - 8 * 3600 * 1000, checkOutTime = now, isWorking = true)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("check_out_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (todayEntry?.checkOutTime != null) {
                                val sdf = SimpleDateFormat("HH:mm", Locale.US)
                                "Đã Ra: " + sdf.format(Date(todayEntry.checkOutTime))
                            } else "Ra Ca"
                        )
                    }
                }
            }
        }

        // 3. Quick Stats Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Số Ngày Công", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${summary.workingDays} / 26",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Thực Lĩnh Dự Kiến", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val fmt = java.text.DecimalFormat("#,###")
                    Text(
                        "${fmt.format(summary.netSalary)} đ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // 4. Calendar Grid
        Text(
            text = "Lịch Chấm Công",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Calendar Week Headers
                val daysOfWeek = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
                Row(modifier = Modifier.fillMaxWidth()) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (day == "CN") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Build Grid list of cells
                val totalCells = leadingEmptyCells + daysInMonth
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - leadingEmptyCells + 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum in 1..daysInMonth) {
                                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, dayNum)
                                    val entry = entries.find { it.date == dateStr }

                                    // Determine background color based on day details
                                    val cellBgColor = when {
                                        entry?.dayType == "PAID_LEAVE" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        entry?.dayType == "UNPAID_LEAVE" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                        entry?.dayType == "HOLIDAY" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                        entry?.dayType == "SUNDAY" -> MaterialTheme.colorScheme.surfaceVariant
                                        entry != null && entry.isWorking -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    }

                                    val isToday = dateStr == todayStr

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellBgColor)
                                            .clickable {
                                                selectedDayToEdit = dayNum
                                                editIsWorking = entry?.isWorking ?: true
                                                editDayType = entry?.dayType ?: "NORMAL"
                                                editNote = entry?.note ?: ""

                                                val sdf = SimpleDateFormat("HH:mm", Locale.US)
                                                editCheckInTime = entry?.checkInTime?.let { sdf.format(Date(it)) }
                                                editCheckOutTime = entry?.checkOutTime?.let { sdf.format(Date(it)) }
                                            }
                                            .then(
                                                if (isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (entry != null && (entry.checkInTime != null || entry.isWorking)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for logging/editing a day's details
        selectedDayToEdit?.let { day ->
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
            Dialog(onDismissRequest = { selectedDayToEdit = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Ngày $dateStr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider()

                        // Day type selector
                        Text("Loại ngày công:", style = MaterialTheme.typography.bodyMedium)
                        val types = listOf(
                            "NORMAL" to "Ngày Thường",
                            "PAID_LEAVE" to "Phép Năm Có Lương",
                            "UNPAID_LEAVE" to "Nghỉ Không Lương",
                            "HOLIDAY" to "Ngày Lễ",
                            "SUNDAY" to "Chủ Nhật"
                        )

                        types.forEach { (typeVal, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editDayType = typeVal }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = editDayType == typeVal, onClick = { editDayType = typeVal })
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Check in / out pickers
                        if (editDayType == "NORMAL" || editDayType == "SUNDAY" || editDayType == "HOLIDAY") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        TimePickerDialog(context, { _, h, m ->
                                            editCheckInTime = String.format(Locale.US, "%02d:%02d", h, m)
                                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(editCheckInTime?.let { "Vào: $it" } ?: "Chọn giờ vào")
                                }

                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        TimePickerDialog(context, { _, h, m ->
                                            editCheckOutTime = String.format(Locale.US, "%02d:%02d", h, m)
                                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(editCheckOutTime?.let { "Ra: $it" } ?: "Chọn giờ ra")
                                }
                            }
                        }

                        // Note field
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            label = { Text("Ghi chú") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    val entry = entries.find { it.date == dateStr }
                                    if (entry != null) {
                                        viewModel.deleteTimeEntry(entry.id)
                                    }
                                    selectedDayToEdit = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Xóa chấm công", color = MaterialTheme.colorScheme.error)
                            }

                            Button(
                                onClick = {
                                    val baseCal = Calendar.getInstance()
                                    val sdfDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

                                    val finalInTime = editCheckInTime?.let {
                                        try { sdfDateTime.parse("$dateStr $it")?.time } catch (e: Exception) { null }
                                    }
                                    val finalOutTime = editCheckOutTime?.let {
                                        try { sdfDateTime.parse("$dateStr $it")?.time } catch (e: Exception) { null }
                                    }

                                    val existing = entries.find { it.date == dateStr }
                                    val updated = TimeEntry(
                                        id = existing?.id ?: 0,
                                        userId = "default_user",
                                        date = dateStr,
                                        checkInTime = finalInTime,
                                        checkOutTime = finalOutTime,
                                        isWorking = editDayType == "NORMAL" || editDayType == "SUNDAY" || editDayType == "HOLIDAY",
                                        dayType = editDayType,
                                        isHourlyCalculated = finalInTime != null,
                                        note = editNote.ifBlank { null }
                                    )
                                    viewModel.saveTimeEntry(updated)
                                    selectedDayToEdit = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Lưu")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
