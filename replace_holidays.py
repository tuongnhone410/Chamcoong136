import re

content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

match = re.search(r"@Composable\nfun HolidaysSection\(companyId: String\).*?\n    \}\n\}", content, re.DOTALL)
if match:
    old_func = match.group(0)
    
    replacement = f"""@Composable
fun HolidaysSection(companyId: String, onDismiss: () -> Unit) {{
    var holidayListStr by remember {{ mutableStateOf("01/01 (Tết Dương lịch), 30/04 (Giải phóng miền Nam), 01/05 (Quốc tế Lao động), 02/09 (Quốc khánh)") }}
    val context = LocalContext.current
    var showUnsavedDialog by remember {{ mutableStateOf(false) }}

    val hasUnsavedChanges = holidayListStr != "01/01 (Tết Dương lịch), 30/04 (Giải phóng miền Nam), 01/05 (Quốc tế Lao động), 02/09 (Quốc khánh)"

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
                            Text("Ngày nghỉ / Ngày lễ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Danh sách ngày lễ chính thức", fontSize = 12.sp, color = LightGray)
                        }}
                    }}
                    TextButton(onClick = {{
                        Toast.makeText(context, "Đã cập nhật danh sách ngày lễ thành công!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }}) {{
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }}
                }}

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {{
                    item {{
                        OutlinedTextField(
                            value = holidayListStr,
                            onValueChange = {{ holidayListStr = it }},
                            label = {{ Text("Danh sách ngày lễ (dd/MM - Mô tả)") }},
                            modifier = Modifier.fillMaxWidth().testTag("input_holidays_list"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonBlue, unfocusedBorderColor = LightGray),
                            minLines = 3
                        )
                    }}
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
