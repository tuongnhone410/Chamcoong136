import re

content = open("app/src/main/java/com/example/ui/screens/ShiftManagementDialog.kt").read()

match = re.search(r"fun ShiftEditFormDialog\(\s*shiftToEdit: ShiftEntity\?,\s*companyId: String,\s*onDismiss: \(\) -> Unit,\s*onSave: \(ShiftEntity\) -> Unit\s*\) \{.*?\n    Dialog\(onDismissRequest = onDismiss\) \{", content, re.DOTALL)

if match:
    old_func_start = match.group(0)
    
    replacement = r"""fun ShiftEditFormDialog(
    shiftToEdit: ShiftEntity?,
    companyId: String,
    onDismiss: () -> Unit,
    onSave: (ShiftEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(shiftToEdit?.name ?: "") }
    var startTime by remember { mutableStateOf(shiftToEdit?.startTime ?: "08:00") }
    var endTime by remember { mutableStateOf(shiftToEdit?.endTime ?: "17:00") }
    var isOvernight by remember { mutableStateOf(shiftToEdit?.isOvernight ?: false) }
    var countAs by remember { mutableStateOf(shiftToEdit?.countAs?.toString() ?: "1.0") }
    var colorHex by remember { mutableStateOf(shiftToEdit?.colorHex ?: "#44B1FF") }
    var description by remember { mutableStateOf(shiftToEdit?.description ?: "") }

    var showColorPicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var countAsError by remember { mutableStateOf<String?>(null) }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = name != (shiftToEdit?.name ?: "") ||
        startTime != (shiftToEdit?.startTime ?: "08:00") ||
        endTime != (shiftToEdit?.endTime ?: "17:00") ||
        isOvernight != (shiftToEdit?.isOvernight ?: false) ||
        countAs != (shiftToEdit?.countAs?.toString() ?: "1.0") ||
        colorHex != (shiftToEdit?.colorHex ?: "#44B1FF") ||
        description != (shiftToEdit?.description ?: "")

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
        var isValid = true
        if (name.isBlank()) {
            nameError = "Vui lòng nhập tên ca"
            isValid = false
        } else {
            nameError = null
        }
        if (!startTime.matches(Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")) || !endTime.matches(Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$"))) {
            timeError = "Giờ không hợp lệ (HH:mm)"
            isValid = false
        } else {
            timeError = null
        }
        val count = countAs.toDoubleOrNull()
        if (count == null || count < 0) {
            countAsError = "Số công phải >= 0"
            isValid = false
        } else {
            countAsError = null
        }
        return isValid
    }

    Dialog(onDismissRequest = handleBack) {"""
    
    new_content = content.replace(old_func_start, replacement)
    
    # Also fix the inner dialog close button
    # Wait, the inner dialog close button should use `handleBack`
    # Let's find: `TextButton(onClick = onDismiss)` and replace with `handleBack`
    # Or `IconButton(onClick = onDismiss)` inside `ShiftEditFormDialog`?
    # Wait, in the dialog, there is a `Button` to Save, and a `TextButton` to Cancel.
    # Cancel button should just call `handleBack()`.
    # Let's search and replace `TextButton(onClick = onDismiss)` with `TextButton(onClick = handleBack)` in ShiftManagementDialog.kt
    
    # Oh wait, there are multiple `onDismiss` usages.
    open("app/src/main/java/com/example/ui/screens/ShiftManagementDialog.kt", "w").write(new_content)
    print("SUCCESS1")
else:
    print("FAILED1")
