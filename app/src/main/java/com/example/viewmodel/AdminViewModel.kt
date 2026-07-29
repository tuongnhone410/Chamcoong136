package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreService
import com.example.data.model.UserConfig
import com.example.data.AttendanceRecord
import com.example.util.ExportUtils
import com.example.util.toTimeEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val _employees = MutableStateFlow<List<UserConfig>>(emptyList())
    val employees: StateFlow<List<UserConfig>> = _employees

    private val _selectedEmployeeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedEmployeeIds: StateFlow<Set<String>> = _selectedEmployeeIds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedEmployee = MutableStateFlow<UserConfig?>(null)
    val selectedEmployee: StateFlow<UserConfig?> = _selectedEmployee

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportSuccessCount = MutableStateFlow(0)
    val exportSuccessCount: StateFlow<Int> = _exportSuccessCount

    private val _todayAttendanceMap = MutableStateFlow<Map<String, AttendanceRecord>>(emptyMap())
    val todayAttendanceMap: StateFlow<Map<String, AttendanceRecord>> = _todayAttendanceMap

    private val _isExportingSingle = MutableStateFlow(false)
    val isExportingSingle: StateFlow<Boolean> = _isExportingSingle

    init {
        loadEmployees()
        observeTodayAttendanceRealtime()
    }

    private fun observeTodayAttendanceRealtime() {
        viewModelScope.launch {
            FirestoreService.getTodayAttendanceLogsFlow().collect { map ->
                _todayAttendanceMap.value = map
            }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = FirestoreService.getAllUserConfigs()
            _employees.value = list.sortedBy { it.hoVaTen }
            _isLoading.value = false
            loadTodayAttendance()
        }
    }

    fun loadTodayAttendance() {
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val day = cal.get(Calendar.DAY_OF_MONTH)

                val monthStr = String.format(Locale.US, "%04d-%02d", year, month)
                val todayYmd = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                val todayDmy = String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
                val todayShortDmy = String.format(Locale.US, "%d/%d/%04d", day, month, year)
                val todayShortYmd = String.format(Locale.US, "%04d-%d-%d", year, month, day)

                val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
                val yYear = calYesterday.get(Calendar.YEAR)
                val yMonth = calYesterday.get(Calendar.MONTH) + 1
                val yDay = calYesterday.get(Calendar.DAY_OF_MONTH)

                val yesterdayYmd = String.format(Locale.US, "%04d-%02d-%02d", yYear, yMonth, yDay)
                val yesterdayDmy = String.format(Locale.US, "%02d/%02d/%04d", yDay, yMonth, yYear)
                val yesterdayShortDmy = String.format(Locale.US, "%d/%d/%04d", yDay, yMonth, yYear)
                val yesterdayShortYmd = String.format(Locale.US, "%04d-%d-%d", yYear, yMonth, yDay)

                val yesterdayMonthStr = String.format(Locale.US, "%04d-%02d", yYear, yMonth)
                val monthRecords = if (yesterdayMonthStr != monthStr) {
                    FirestoreService.getAllAttendanceLogsInMonth(monthStr) + FirestoreService.getAllAttendanceLogsInMonth(yesterdayMonthStr)
                } else {
                    FirestoreService.getAllAttendanceLogsInMonth(monthStr)
                }

                val todayMap = mutableMapOf<String, AttendanceRecord>()
                val recordIsToday = mutableMapOf<String, Boolean>()
                for (r in monthRecords) {
                    val ds = r.dateString.trim()
                    val isToday = ds == todayYmd || ds == todayDmy || ds == todayShortDmy || ds == todayShortYmd || ds.endsWith(todayYmd)
                    val isYesterday = ds == yesterdayYmd || ds == yesterdayDmy || ds == yesterdayShortDmy || ds == yesterdayShortYmd || ds.endsWith(yesterdayYmd)
                    
                    val uid = r.uid
                    if (isToday) {
                        val existing = todayMap[uid]
                        val existingIsToday = recordIsToday[uid] == true
                        if (existing == null || !existingIsToday || (existing.clockOutTime != null && r.clockOutTime == null) || r.clockInTime > existing.clockInTime) {
                            todayMap[uid] = r
                            recordIsToday[uid] = true
                        }
                    } else if (isYesterday) {
                        val isCurrentlyActiveNightShift = r.clockInTime > 0 && (r.clockOutTime == null || r.clockOutTime == 0L)
                        if (isCurrentlyActiveNightShift) {
                            val existing = todayMap[uid]
                            val existingIsToday = recordIsToday[uid] == true
                            if (existing == null || (!existingIsToday && r.clockInTime > existing.clockInTime)) {
                                todayMap[uid] = r
                                recordIsToday[uid] = false
                            }
                        }
                    }
                }

                _todayAttendanceMap.value = todayMap
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Error loading today attendance: ${e.message}")
            }
        }
    }

    private var attendanceJob: kotlinx.coroutines.Job? = null

    fun selectEmployee(employee: UserConfig?) {
        _selectedEmployee.value = employee
        attendanceJob?.cancel()
        if (employee != null) {
            _selectedEmployeeIds.value = emptySet() // Clear multi-selection when viewing details
            attendanceJob = viewModelScope.launch {
                FirestoreService.getAttendanceLogsFlow(employee.userId).collect { logs ->
                    _attendanceRecords.value = logs
                }
            }
        } else {
            _attendanceRecords.value = emptyList()
        }
    }

    fun toggleEmployeeSelection(uid: String) {
        val current = _selectedEmployeeIds.value
        if (current.contains(uid)) {
            _selectedEmployeeIds.value = current - uid
        } else {
            _selectedEmployeeIds.value = current + uid
        }
    }

    fun clearSelection() {
        _selectedEmployeeIds.value = emptySet()
    }

    fun batchUpdateSalaryConfig(updates: (UserConfig) -> UserConfig) {
        viewModelScope.launch {
            val ids = _selectedEmployeeIds.value
            val affected = _employees.value.filter { it.userId in ids }
            affected.forEach { emp ->
                val updated = updates(emp)
                FirestoreService.saveUserSalaryConfigToFirestore(updated)
            }
            loadEmployees()
            _selectedEmployeeIds.value = emptySet()
        }
    }

    fun batchAddAttendance(dateString: String, clockIn: String, clockOut: String) {
        viewModelScope.launch {
            val ids = _selectedEmployeeIds.value
            val formatPattern = if (dateString.contains("/")) "dd/MM/yyyy HH:mm" else "yyyy-MM-dd HH:mm"
            val sdf = SimpleDateFormat(formatPattern, Locale.getDefault())
            ids.forEach { uid ->
                try {
                    val fullIn = sdf.parse("$dateString $clockIn")?.time ?: 0L
                    val fullOut = sdf.parse("$dateString $clockOut")?.time
                    FirestoreService.saveAttendanceRecord(AttendanceRecord(uid = uid, dateString = dateString, clockInTime = fullIn, clockOutTime = fullOut))
                } catch (e: Exception) {}
            }
            _selectedEmployeeIds.value = emptySet()
        }
    }

    fun deleteEmployee(userId: String) {
        viewModelScope.launch {
            FirestoreService.deleteUserFully(userId)
            loadEmployees()
            if (_selectedEmployee.value?.userId == userId) {
                _selectedEmployee.value = null
            }
        }
    }

    fun batchDeleteEmployees() {
        viewModelScope.launch {
            val ids = _selectedEmployeeIds.value
            ids.forEach { userId ->
                FirestoreService.deleteUserFully(userId)
            }
            loadEmployees()
            _selectedEmployeeIds.value = emptySet()
        }
    }

    fun saveEmployeeConfig(config: UserConfig) {
        viewModelScope.launch {
            FirestoreService.saveUserSalaryConfigToFirestore(config)
            loadEmployees() // refresh list
        }
    }

    fun saveAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            FirestoreService.saveAttendanceRecord(record)
        }
    }

    fun deleteAttendanceRecord(uid: String, dateString: String) {
        viewModelScope.launch {
            FirestoreService.deleteAttendanceRecord(uid, dateString)
        }
    }

    fun performBatchExport(context: Context, targetMonthStr: String? = null) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            _exportSuccessCount.value = 0
            
            val allEmployees = _employees.value
            if (allEmployees.isEmpty()) {
                _isExporting.value = false
                return@launch
            }

            // Target month logic
            val cal = Calendar.getInstance()
            val finalMonthStr = targetMonthStr ?: String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            
            val monthLabel = if (finalMonthStr.contains("-")) {
                val parts = finalMonthStr.split("-")
                "${parts[1]}/${parts[0]}"
            } else {
                finalMonthStr
            }

            var successCount = 0
            allEmployees.forEachIndexed { index, employee ->
                try {
                    // 1. Fetch records for this employee for the month
                    val records = FirestoreService.getAttendanceLogsForUser(employee.userId)
                    
                    // Filter records for the target month (handles yyyy-MM-dd and dd/MM/yyyy)
                    val monthEntries = records.filter { record ->
                        ExportUtils.isRecordInMonth(record.dateString, finalMonthStr)
                    }.map { it.toTimeEntry() }
                    
                    // 2. Calculate summary
                    val summary = ExportUtils.calculateSalarySummary(monthEntries, employee, finalMonthStr)
                    
                    // 3. Generate and save PNG
                    val saved = ExportUtils.savePayslipAsPngImage(
                        context = context,
                        summary = summary,
                        config = employee,
                        userSession = null,
                        monthLabel = monthLabel,
                        selectedMonth = finalMonthStr
                    )
                    
                    if (saved) successCount++
                } catch (e: Exception) {
                    android.util.Log.e("AdminViewModel", "Export failed for ${employee.hoVaTen}: ${e.message}")
                }
                _exportProgress.value = (index + 1).toFloat() / allEmployees.size
            }

            _exportSuccessCount.value = successCount
            _isExporting.value = false
            _exportProgress.value = 0f
        }
    }

    fun exportSingleEmployeePayslip(context: Context, employee: UserConfig, targetMonthStr: String? = null) {
        viewModelScope.launch {
            _isExportingSingle.value = true
            try {
                val cal = Calendar.getInstance()
                val finalMonthStr = targetMonthStr ?: String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                
                val monthLabel = if (finalMonthStr.contains("-")) {
                    val parts = finalMonthStr.split("-")
                    "${parts[1]}/${parts[0]}"
                } else finalMonthStr

                val records = FirestoreService.getAttendanceLogsForUser(employee.userId)
                val monthEntries = records.filter { record ->
                    ExportUtils.isRecordInMonth(record.dateString, finalMonthStr)
                }.map { it.toTimeEntry() }

                val summary = ExportUtils.calculateSalarySummary(monthEntries, employee, finalMonthStr)
                val saved = ExportUtils.savePayslipAsPngImage(
                    context = context,
                    summary = summary,
                    config = employee,
                    userSession = null,
                    monthLabel = monthLabel,
                    selectedMonth = finalMonthStr
                )

                if (saved) {
                    android.widget.Toast.makeText(
                        context, 
                        "Đã xuất phiếu lương cho ${employee.hoVaTen} (Mã NV: ${employee.maNhanVien}) vào thư mục Download/TimeSnapPro", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context, 
                        "Không thể xuất phiếu lương cho ${employee.hoVaTen}", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminViewModel", "Export single failed: ${e.message}")
                android.widget.Toast.makeText(context, "Lỗi khi xuất phiếu lương: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                _isExportingSingle.value = false
            }
        }
    }
}
