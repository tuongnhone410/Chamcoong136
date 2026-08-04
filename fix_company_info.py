import re

content = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

pattern = r"""@Composable
fun CompanyInfoSection\(userConfig: UserConfig, viewModel: TimeSnapViewModel\) \{
(.*?)
    fun validate\(\): Boolean \{
(.*?)
        return isValid
    \}

    LazyColumn\(
        modifier = Modifier
            \.fillMaxSize\(\)
            \.padding\(vertical = 8\.dp\),
        verticalArrangement = Arrangement\.spacedBy\(12\.dp\)
    \) \{
        item \{
            Text\("1\. Thông tin công ty & Hợp đồng", fontSize = 16\.sp, fontWeight = FontWeight\.Bold, color = White\)
            Text\("Cấu hình tên, thông tin liên hệ và lương cơ bản của nhân viên/công ty\.", fontSize = 12\.sp, color = LightGray\)
            Spacer\(modifier = Modifier\.height\(4\.dp\)\)
        \}
(.*?)
        item \{
            Spacer\(modifier = Modifier\.height\(8\.dp\)\)
            Button\(
                onClick = \{
                    if \(validate\(\)\) \{
                        val updated = userConfig\.copy\(
                            hoVaTen = companyName,
                            maNhanVien = companyCode,
                            soDienThoai = phone,
                            emailDangKy = email,
                            luongCoBan = baseSalary\.toDoubleOrNull\(\) \?: userConfig\.luongCoBan,
                            luongDongBaoHiem = insSalary\.toDoubleOrNull\(\) \?: userConfig\.luongDongBaoHiem,
                            tiLeDongBaoHiem = insRate\.toDoubleOrNull\(\) \?: userConfig\.tiLeDongBaoHiem,
                            ngayChotLuong = cutoffDay\.toIntOrNull\(\) \?: userConfig\.ngayChotLuong
                        \)
                        viewModel\.updateSalaryConfig\(updated\)
                        Toast\.makeText\(context, "Đã lưu thông tin công ty thành công!", Toast\.LENGTH_SHORT\)\.show\(\)
                    \}
                \},
                modifier = Modifier\.fillMaxWidth\(\)\.height\(50\.dp\)\.testTag\("save_company_info_button"\),
                colors = ButtonDefaults\.buttonColors\(containerColor = NeonBlue\),
                shape = RoundedCornerShape\(12\.dp\)
            \) \{
                Text\("Lưu thông tin công ty", color = White, fontWeight = FontWeight\.Bold\)
            \}
            Spacer\(modifier = Modifier\.height\(24\.dp\)\)
        \}
    \}
\}"""

replacement = r"""@Composable
fun CompanyInfoSection(userConfig: UserConfig, viewModel: TimeSnapViewModel, onDismiss: () -> Unit) {
\1
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = companyName != userConfig.hoVaTen ||
        companyCode != userConfig.maNhanVien ||
        phone != userConfig.soDienThoai ||
        email != userConfig.emailDangKy ||
        baseSalary != userConfig.luongCoBan.toString() ||
        insSalary != userConfig.luongDongBaoHiem.toString() ||
        insRate != userConfig.tiLeDongBaoHiem.toString() ||
        cutoffDay != userConfig.ngayChotLuong.toString()

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

    fun validate(): Boolean {
\2
        return isValid
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
                            Text("Thông tin công ty", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                            Text("Hợp đồng & liên hệ", fontSize = 12.sp, color = LightGray)
                        }
                    }
                    TextButton(onClick = {
                        if (validate()) {
                            val updated = userConfig.copy(
                                hoVaTen = companyName,
                                maNhanVien = companyCode,
                                soDienThoai = phone,
                                emailDangKy = email,
                                luongCoBan = baseSalary.toDoubleOrNull() ?: userConfig.luongCoBan,
                                luongDongBaoHiem = insSalary.toDoubleOrNull() ?: userConfig.luongDongBaoHiem,
                                tiLeDongBaoHiem = insRate.toDoubleOrNull() ?: userConfig.tiLeDongBaoHiem,
                                ngayChotLuong = cutoffDay.toIntOrNull() ?: userConfig.ngayChotLuong
                            )
                            viewModel.updateSalaryConfig(updated)
                            Toast.makeText(context, "Đã lưu thành công!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }) {
                        Text("Lưu", color = NeonBlue, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
\3
                }
            }
        }
    }
}"""

# Use re.DOTALL to match across newlines
new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(new_content)
