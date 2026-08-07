package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CompanyConfig
import com.example.data.model.RoleConfig
import com.example.data.model.getRoles
import com.example.data.model.updateRoles
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementDialog(
    company: CompanyConfig,
    onSave: (CompanyConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var roles by remember { mutableStateOf(company.getRoles()) }
    var editingRole by remember { mutableStateOf<RoleConfig?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (editingRole != null || showAddDialog) {
        val initialRole = editingRole ?: RoleConfig()
        RoleEditDialog(
            role = initialRole,
            onSave = { updatedRole ->
                roles = if (showAddDialog) {
                    roles + updatedRole
                } else {
                    roles.map { if (it.roleId == updatedRole.roleId) updatedRole else it }
                }
                showAddDialog = false
                editingRole = null
                onSave(company.updateRoles(roles))
            },
            onDismiss = {
                showAddDialog = false
                editingRole = null
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkContainer,
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quản lý Chức Vụ (${company.companyName})", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = LightGray)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("THÊM CHỨC VỤ MỚI", color = White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (roles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(DarkBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chưa có chức vụ nào. Hãy thêm chức vụ để cấu hình mức lương & phụ cấp đặc thù cho từng vị trí làm việc.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(roles) { role ->
                                RoleCardItem(
                                    role = role,
                                    onEdit = { editingRole = role },
                                    onDelete = {
                                        roles = roles.filter { it.roleId != role.roleId }
                                        onSave(company.updateRoles(roles))
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Đóng", color = LightGray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleEditDialog(
    role: RoleConfig,
    onSave: (RoleConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var roleName by remember { mutableStateOf(role.roleName) }
    var lcb by remember { mutableStateOf(if (role.luongCoBan == 0.0) "" else role.luongCoBan.toLong().toString()) }
    
    // Professional Allowances
    var pcChucVu by remember { mutableStateOf(if (role.pcChucVu == 0.0) "" else role.pcChucVu.toLong().toString()) }
    var pcTrachNhiem by remember { mutableStateOf(if (role.pcTrachNhiem == 0.0) "" else role.pcTrachNhiem.toLong().toString()) }
    var pcKyThuat by remember { mutableStateOf(if (role.pcKyThuat == 0.0) "" else role.pcKyThuat.toLong().toString()) }
    var pcHieuSuat by remember { mutableStateOf(if (role.pcHieuSuat == 0.0) "" else role.pcHieuSuat.toLong().toString()) }
    var pcSanPham by remember { mutableStateOf(if (role.pcSanPham == 0.0) "" else role.pcSanPham.toLong().toString()) }
    
    // Living / Welfare Allowances
    var pcComCa by remember { mutableStateOf(if (role.pcComCa == 0.0) "" else role.pcComCa.toLong().toString()) }
    var pcComOt by remember { mutableStateOf(if (role.pcComOt == 0.0) "" else role.pcComOt.toLong().toString()) }
    var pcNhaO by remember { mutableStateOf(if (role.pcNhaO == 0.0) "" else role.pcNhaO.toLong().toString()) }
    var pcXangXe by remember { mutableStateOf(if (role.pcXangXe == 0.0) "" else role.pcXangXe.toLong().toString()) }
    var pcDocHai by remember { mutableStateOf(if (role.pcDocHai == 0.0) "" else role.pcDocHai.toLong().toString()) }
    var pcCaDem by remember { mutableStateOf(if (role.pcCaDem == 0.0) "" else role.pcCaDem.toLong().toString()) }
    var pcThamNien by remember { mutableStateOf(if (role.pcThamNien == 0.0) "" else role.pcThamNien.toLong().toString()) }
    var pcDtDoanhThu by remember { mutableStateOf(if (role.pcDtDoanhThu == 0.0) "" else role.pcDtDoanhThu.toLong().toString()) }
    var pcKhac by remember { mutableStateOf(if (role.pcKhac1 == 0.0) "" else role.pcKhac1.toLong().toString()) }
    var chuyenCan by remember { mutableStateOf(if (role.tienChuyenCanGoc == 0.0) "" else role.tienChuyenCanGoc.toLong().toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .widthIn(max = 540.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = DarkContainer,
            border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (role.roleName.isEmpty()) "Thêm Chức Vụ Mới" else "Chỉnh Sửa Chức Vụ",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = LightGray)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Group 1: Basic Info & Salary
                    CompanyCardSection(title = "1. Tên chức vụ & Lương cơ bản", icon = Icons.Default.Badge) {
                        OutlinedTextField(
                            value = roleName,
                            onValueChange = { roleName = it },
                            label = { Text("Tên chức vụ (VD: Tổ trưởng, Kỹ sư...)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lcb,
                            onValueChange = { lcb = it.filter { c -> c.isDigit() } },
                            label = { Text("Lương cơ bản mặc định (VNĐ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Group 2: Professional Allowances
                    CompanyCardSection(title = "2. Phụ cấp chuyên môn & Trách nhiệm", icon = Icons.Default.Engineering) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcChucVu,
                                    onValueChange = { pcChucVu = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Chức vụ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcTrachNhiem,
                                    onValueChange = { pcTrachNhiem = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Trách nhiệm") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcKyThuat,
                                    onValueChange = { pcKyThuat = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Kỹ thuật") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcHieuSuat,
                                    onValueChange = { pcHieuSuat = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Hiệu suất") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                        }
                        OutlinedTextField(
                            value = pcSanPham,
                            onValueChange = { pcSanPham = it.filter { c -> c.isDigit() } },
                            label = { Text("PC Sản phẩm") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Group 3: Living & Welfare Allowances
                    CompanyCardSection(title = "3. Phụ cấp đời sống & Phúc lợi", icon = Icons.Default.Restaurant) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcComCa,
                                    onValueChange = { pcComCa = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Cơm ca") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcXangXe,
                                    onValueChange = { pcXangXe = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Xăng xe") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcNhaO,
                                    onValueChange = { pcNhaO = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Nhà ở") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcDocHai,
                                    onValueChange = { pcDocHai = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Độc hại") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = chuyenCan,
                                    onValueChange = { chuyenCan = it.filter { c -> c.isDigit() } },
                                    label = { Text("Tiền Chuyên cần") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = pcKhac,
                                    onValueChange = { pcKhac = it.filter { c -> c.isDigit() } },
                                    label = { Text("PC Khác") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = White, unfocusedTextColor = White, focusedBorderColor = NeonBlue),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Bottom Actions
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LightGray)
                    ) {
                        Text("Hủy", color = LightGray)
                    }

                    Button(
                        onClick = {
                            val updated = role.copy(
                                roleName = roleName.trim(),
                                luongCoBan = lcb.toDoubleOrNull() ?: 0.0,
                                pcChucVu = pcChucVu.toDoubleOrNull() ?: 0.0,
                                pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: 0.0,
                                pcKyThuat = pcKyThuat.toDoubleOrNull() ?: 0.0,
                                pcHieuSuat = pcHieuSuat.toDoubleOrNull() ?: 0.0,
                                pcSanPham = pcSanPham.toDoubleOrNull() ?: 0.0,
                                pcComCa = pcComCa.toDoubleOrNull() ?: 0.0,
                                pcComOt = pcComOt.toDoubleOrNull() ?: 0.0,
                                pcNhaO = pcNhaO.toDoubleOrNull() ?: 0.0,
                                pcXangXe = pcXangXe.toDoubleOrNull() ?: 0.0,
                                pcDocHai = pcDocHai.toDoubleOrNull() ?: 0.0,
                                pcCaDem = pcCaDem.toDoubleOrNull() ?: 0.0,
                                pcThamNien = pcThamNien.toDoubleOrNull() ?: 0.0,
                                pcDtDoanhThu = pcDtDoanhThu.toDoubleOrNull() ?: 0.0,
                                pcKhac1 = pcKhac.toDoubleOrNull() ?: 0.0,
                                tienChuyenCanGoc = chuyenCan.toDoubleOrNull() ?: 0.0
                            )
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Text("LƯU", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
