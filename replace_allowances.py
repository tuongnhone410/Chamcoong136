import re

content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

pattern = r"""@Composable
fun AllowancesSection\(userConfig: UserConfig, viewModel: TimeSnapViewModel\) \{.*?\}\s*\}
\}"""

# find the exact content and replace
match = re.search(r"@Composable\nfun AllowancesSection\(userConfig: UserConfig, viewModel: TimeSnapViewModel\).*?\n\s+\}\n\}", content, re.DOTALL)
if match:
    old_func = match.group(0)
    
    # Extract the items
    items_match = re.search(r"Spacer\(modifier = Modifier\.height\(4\.dp\)\)\n\s*\}(.*?)        item \{\n            Spacer\(modifier = Modifier\.height\(8\.dp\)\)", old_func, re.DOTALL)
    if items_match:
        items = items_match.group(1)
    else:
        items = "FAILED TO MATCH ITEMS"
        
    replacement = f"""@Composable
fun AllowancesSection(userConfig: UserConfig, viewModel: TimeSnapViewModel, onDismiss: () -> Unit) {{
    val context = LocalContext.current
    var pcXangXe by remember {{ mutableStateOf(userConfig.pcXangXe.toString()) }}
    var pcDienThoai by remember {{ mutableStateOf(userConfig.pcDtDoanhThu.toString()) }}
    var pcNhaO by remember {{ mutableStateOf(userConfig.pcNhaO.toString()) }}
    var pcChuyenCan by remember {{ mutableStateOf(userConfig.tienChuyenCanGoc.toString()) }}
    var pcTrachNhiem by remember {{ mutableStateOf(userConfig.pcTrachNhiem.toString()) }}
    var pcKyThuat by remember {{ mutableStateOf(userConfig.pcKyThuat.toString()) }}
    var pcHieuSuat by remember {{ mutableStateOf(userConfig.pcHieuSuat.toString()) }}
    var pcCaDem by remember {{ mutableStateOf(userConfig.pcCaDem.toString()) }}
    var pcComCa by remember {{ mutableStateOf(userConfig.pcComCa.toString()) }}
    var pcComOt by remember {{ mutableStateOf(userConfig.pcComOt.toString()) }}

    var showUnsavedDialog by remember {{ mutableStateOf(false) }}

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
                            Text("Phụ cấp & Tiền cơm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Phụ cấp cố định, cơm ca", fontSize = 12.sp, color = LightGray)
                        }}
                    }}
                    TextButton(onClick = {{
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
    }}
}}"""
    
    new_content = content.replace(old_func, replacement)
    open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(new_content)
    print("SUCCESS")
else:
    print("FAILED TO FIND")
