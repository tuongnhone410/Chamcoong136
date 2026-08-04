import re

content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

match = re.search(r"@Composable\nfun AdvancedSection\(userConfig: UserConfig, viewModel: TimeSnapViewModel\).*?\n\s+\}\n\}", content, re.DOTALL)
if match:
    old_func = match.group(0)
    
    # Extract the items excluding the save button but INCLUDING the recalculate history button
    items_match = re.search(r"Spacer\(modifier = Modifier\.height\(4\.dp\)\)\n\s*\}(.*?)        item \{\n            Spacer\(modifier = Modifier\.height\(8\.dp\)\)\n            Button\(\n                onClick = \{[^\}]+\n                    Toast\.makeText\(context, \"Đã lưu cài đặt nâng cao thành công!\", Toast\.LENGTH_SHORT\)\.show\(\)\n                \},\n                modifier = Modifier\.fillMaxWidth\(\)\.height\(50\.dp\)\.testTag\(\"save_advanced_button\"\),\n                colors = ButtonDefaults\.buttonColors\(containerColor = NeonBlue\),\n                shape = RoundedCornerShape\(12\.dp\)\n            \) \{\n                Text\(\"Lưu cài đặt nâng cao\", color = White, fontWeight = FontWeight\.Bold\)\n            \}\n        \}(.*)", old_func, re.DOTALL)
    if items_match:
        items1 = items_match.group(1)
        items2 = items_match.group(2)
        items = items1 + items2
    else:
        items = "FAILED TO MATCH ITEMS"
        
    replacement = f"""@Composable
fun AdvancedSection(userConfig: UserConfig, viewModel: TimeSnapViewModel, onDismiss: () -> Unit) {{
    val context = LocalContext.current
    var unionFee by remember {{ mutableStateOf(userConfig.doanPhiCongDoan.toString()) }}
    var annualLeave by remember {{ mutableStateOf(userConfig.soNgayPhepNam.toString()) }}
    var breakHours by remember {{ mutableStateOf(userConfig.soGioNghiGiaiLao.toString()) }}
    var tinhKhauTru by remember {{ mutableStateOf(userConfig.tinhKhauTruNghi) }}

    var showRecalculateWarning by remember {{ mutableStateOf(false) }}
    var showUnsavedDialog by remember {{ mutableStateOf(false) }}

    val hasUnsavedChanges = unionFee != userConfig.doanPhiCongDoan.toString() ||
        annualLeave != userConfig.soNgayPhepNam.toString() ||
        breakHours != userConfig.soGioNghiGiaiLao.toString() ||
        tinhKhauTru != userConfig.tinhKhauTruNghi

    val handleBack = {{
        if (hasUnsavedChanges) {{
            showUnsavedDialog = true
        }} else {{
            onDismiss()
        }}
    }}

    if (showUnsavedDialog) {{
        AlertDialog(
            onDismissRequest = {{ showUnsavedDialog = false }},
            title = {{ Text("Bạn có thay đổi chưa lưu", color = White) }},
            text = {{ Text("Bạn có chắc chắn muốn thoát? Các thay đổi sẽ bị mất.", color = LightGray) }},
            confirmButton = {{
                TextButton(onClick = {{
                    showUnsavedDialog = false
                    onDismiss()
                }}) {{
                    Text("Thoát", color = AccentRed)
                }}
            }},
            dismissButton = {{
                TextButton(onClick = {{ showUnsavedDialog = false }}) {{
                    Text("Ở lại", color = NeonBlue)
                }}
            }},
            containerColor = DarkContainer
        )
    }}

    Dialog(onDismissRequest = handleBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {{
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).clip(RoundedCornerShape(24.dp)),
            color = DarkContainer,
            tonalElevation = 8.dp
        ) {{
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {{
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {{
                    Row(verticalAlignment = Alignment.CenterVertically) {{
                        IconButton(onClick = handleBack) {{
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = NeonBlue)
                        }}
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {{
                            Text("Cấu hình nâng cao", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Đoàn phí, phép năm, tính lại", fontSize = 12.sp, color = LightGray)
                        }}
                    }}
                    TextButton(onClick = {{
                        val updated = userConfig.copy(
                            doanPhiCongDoan = unionFee.toDoubleOrNull() ?: userConfig.doanPhiCongDoan,
                            soNgayPhepNam = annualLeave.toIntOrNull() ?: userConfig.soNgayPhepNam,
                            soGioNghiGiaiLao = breakHours.toDoubleOrNull() ?: userConfig.soGioNghiGiaiLao,
                            tinhKhauTruNghi = tinhKhauTru
                        )
                        viewModel.updateSalaryConfig(updated)
                        Toast.makeText(context, "Đã lưu cài đặt nâng cao thành công!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }}) {{
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }}
                }}

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {{
{items}
            }}
        }}
    }}
}}"""
    
    new_content = content.replace(old_func, replacement)
    open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(new_content)
    print("SUCCESS")
else:
    print("FAILED TO FIND")
