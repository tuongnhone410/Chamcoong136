package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TimeEntry
import com.example.viewmodel.TimeSnapViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: TimeSnapViewModel) {
    val entries by viewModel.monthTimeEntries.collectAsState()
    val sdf = SimpleDateFormat("HH:mm", Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch Sử Điểm Danh") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Không có dữ liệu trong tháng này.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(entries.sortedByDescending { it.date }, key = { it.id }) { entry ->
                    HistoryItemCard(entry, onDelete = { viewModel.deleteTimeEntry(entry.id) })
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(entry: TimeEntry, onDelete: () -> Unit) {
    val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
    val checkInStr = entry.checkInTime?.let { sdfTime.format(Date(it)) } ?: "--:--"
    val checkOutStr = entry.checkOutTime?.let { sdfTime.format(Date(it)) } ?: "--:--"

    val totalHours = if (entry.checkInTime != null && entry.checkOutTime != null) {
        val duration = entry.checkOutTime - entry.checkInTime
        String.format(Locale.US, "%.1f", duration.toDouble() / (1000.0 * 3600.0))
    } else {
        null
    }

    val typeLabel = when (entry.dayType) {
        "NORMAL" -> "Ngày Thường"
        "PAID_LEAVE" -> "Phép Năm"
        "UNPAID_LEAVE" -> "Nghỉ Không Lương"
        "HOLIDAY" -> "Ngày Lễ"
        "SUNDAY" -> "Chủ Nhật"
        else -> entry.dayType
    }

    val typeColor = when (entry.dayType) {
        "PAID_LEAVE" -> MaterialTheme.colorScheme.secondary
        "UNPAID_LEAVE" -> MaterialTheme.colorScheme.error
        "HOLIDAY" -> MaterialTheme.colorScheme.tertiary
        "SUNDAY" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_card_${entry.date}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(typeLabel, style = MaterialTheme.typography.bodySmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = typeColor)
                    )

                    if (totalHours != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("$totalHours Giờ làm", style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                if (entry.checkInTime != null || entry.checkOutTime != null) {
                    Text(
                        text = "Vào: $checkInStr  -  Ra: $checkOutStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                entry.note?.let {
                    Text(
                        text = "Ghi chú: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_entry_${entry.date}")) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
