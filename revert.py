# I need to fix ShiftManagementDialog.kt and CompanyRulesHubDialog.kt

# For ShiftManagementDialog.kt, I will just rewrite ShiftEditFormDialog completely, using the correct fields:
content = open("app/src/main/java/com/example/ui/screens/ShiftManagementDialog.kt").read()
import re
match = re.search(r"fun ShiftEditFormDialog\(.*?Dialog\(onDismissRequest = handleBack\) \{", content, re.DOTALL)
if match:
    replacement = r"""fun ShiftEditFormDialog(
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
    
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = name != (shiftToEdit?.name ?: "") ||
        startTimeRaw != (shiftToEdit?.startTime?.replace(":", "") ?: "0730") ||
        endTimeRaw != (shiftToEdit?.endTime?.replace(":", "") ?: "1630") ||
        breakMinutesStr != (shiftToEdit?.breakMinutes?.toString() ?: "60") ||
        standardHoursStr != (shiftToEdit?.standardHours?.toString() ?: "8.0") ||
        crossesMidnight != (shiftToEdit?.crossesMidnight ?: false) ||
        enabled != (shiftToEdit?.enabled ?: true)

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

    Dialog(onDismissRequest = handleBack) {"""
    
    new_content = content.replace(match.group(0), replacement)
    open("app/src/main/java/com/example/ui/screens/ShiftManagementDialog.kt", "w").write(new_content)
    print("Shift fixed")

# Now check CompanyRulesHubDialog.kt
content_hub = open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt").read()

# find trailing brackets or duplicate enum
match = re.search(r"enum class HubSection.*?\}", content_hub, re.DOTALL)
if match:
    # See if there is anything after it?
    parts = content_hub.split("enum class HubSection")
    if len(parts) > 2:
        print("Duplicate HubSection!")
        # keep only up to the first enum class
        first_part = parts[0] + "enum class HubSection" + parts[1]
        open("app/src/main/java/com/example/ui/screens/CompanyRulesHubDialog.kt", "w").write(first_part)
        print("Hub fixed duplicate")
    else:
        # maybe extra brackets at the end
        if content_hub.endswith("}"):
            # let's just use regex to remove extra braces at EOF
            # wait, the error is Expecting a top level declaration.
            pass
