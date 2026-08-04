import re

content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

pattern = r"""@Composable
fun AllowancesSection\(userConfig: UserConfig, viewModel: TimeSnapViewModel\) \{
(.*?)
    LazyColumn\(
        modifier = Modifier\.fillMaxSize\(\)\.padding\(vertical = 8\.dp\),
        verticalArrangement = Arrangement\.spacedBy\(12\.dp\)
    \) \{
(.*?)
        item \{
            Spacer\(modifier = Modifier\.height\(8\.dp\)\)
            Button\(
                onClick = \{
                    val updated = userConfig\.copy\(
                        pcXangXe = pcXangXe\.toDoubleOrNull\(\) \?: userConfig\.pcXangXe,
                        pcDtDoanhThu = pcDienThoai\.toDoubleOrNull\(\) \?: userConfig\.pcDtDoanhThu,
                        pcNhaO = pcNhaO\.toDoubleOrNull\(\) \?: userConfig\.pcNhaO,
                        tienChuyenCanGoc = pcChuyenCan\.toDoubleOrNull\(\) \?: userConfig\.tienChuyenCanGoc,
                        pcTrachNhiem = pcTrachNhiem\.toDoubleOrNull\(\) \?: userConfig\.pcTrachNhiem,
                        pcKyThuat = pcKyThuat\.toDoubleOrNull\(\) \?: userConfig\.pcKyThuat,
                        pcHieuSuat = pcHieuSuat\.toDoubleOrNull\(\) \?: userConfig\.pcHieuSuat,
                        pcCaDem = pcCaDem\.toDoubleOrNull\(\) \?: userConfig\.pcCaDem,
                        pcComCa = pcComCa\.toDoubleOrNull\(\) \?: userConfig\.pcComCa,
                        pcComOt = pcComOt\.toDoubleOrNull\(\) \?: userConfig\.pcComOt
                    \)
                    viewModel\.updateSalaryConfig\(updated\)
                    Toast\.makeText\(context, "Đã lưu phụ cấp thành công!", Toast\.LENGTH_SHORT\)\.show\(\)
                \},
                modifier = Modifier\.fillMaxWidth\(\)\.height\(50\.dp\)\.testTag\("save_allowances_button"\),
                colors = ButtonDefaults\.buttonColors\(containerColor = NeonBlue\),
                shape = RoundedCornerShape\(12\.dp\)
            \) \{
                Text\("Lưu phụ cấp", color = White, fontWeight = FontWeight\.Bold\)
            \}
            Spacer\(modifier = Modifier\.height\(24\.dp\)\)
        \}
    \}
\}"""

replacement = r"""@Composable
fun AllowancesSection(userConfig: UserConfig, viewModel: TimeSnapViewModel, onDismiss: () -> Unit) {
\1
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = pcXangXe != userConfig.pcXangXe.toString() ||
        pcDienThoai != userConfig.pcDtDoanhThu.toString() ||
        pcNhaO != userConfig.pcNhaO.toString() ||
        pcChuyenCan != userConfig.tienChuyenCanGoc.toString() ||
        pcTrachNhiem != userConfig.pcTrachNhiem.toString() ||
        pcKyThuat != userConfig.pcKyThuat.toString() ||
        pcHieuSuat != userConfig.pcHieuSuat.toString() ||
        pcCaDem != userConfig.pcCaDem.toString() ||
        pcComCa != userConfig.pcComCa.toString() ||
        pcComOt != userConfig.pcComOt.toString()

    val handleBack = {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Bạn có thay đổi chưa lưu", color = White) },
            text = { Text("Bạn có chắc chắn muốn thoát? Các thay đổi sẽ bị mất.", color = LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onDismiss()
                }) {
                    Text("Thoát", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Ở lại", color = NeonBlue)
                }
            },
            containerColor = DarkContainer
        )
    }

    Dialog(onDismissRequest = handleBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)),
            color = DarkContainer,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = handleBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = NeonBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Phụ cấp & Tiền cơm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Phụ cấp cố định, cơm ca", fontSize = 12.sp, color = LightGray)
                        }
                    }
                    TextButton(onClick = {
                        val updated = userConfig.copy(
                            pcXangXe = pcXangXe.toDoubleOrNull() ?: userConfig.pcXangXe,
                            pcDtDoanhThu = pcDienThoai.toDoubleOrNull() ?: userConfig.pcDtDoanhThu,
                            pcNhaO = pcNhaO.toDoubleOrNull() ?: userConfig.pcNhaO,
                            tienChuyenCanGoc = pcChuyenCan.toDoubleOrNull() ?: userConfig.tienChuyenCanGoc,
                            pcTrachNhiem = pcTrachNhiem.toDoubleOrNull() ?: userConfig.pcTrachNhiem,
                            pcKyThuat = pcKyThuat.toDoubleOrNull() ?: userConfig.pcKyThuat,
                            pcHieuSuat = pcHieuSuat.toDoubleOrNull() ?: userConfig.pcHieuSuat,
                            pcCaDem = pcCaDem.toDoubleOrNull() ?: userConfig.pcCaDem,
                            pcComCa = pcComCa.toDoubleOrNull() ?: userConfig.pcComCa,
                            pcComOt = pcComOt.toDoubleOrNull() ?: userConfig.pcComOt
                        )
                        viewModel.updateSalaryConfig(updated)
                        Toast.makeText(context, "Đã lưu phụ cấp thành công!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }) {
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
\2
                }
            }
        }
    }
}"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(new_content)
