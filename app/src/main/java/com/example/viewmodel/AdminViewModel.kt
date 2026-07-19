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

    init {
        loadEmployees()
    }

    fun loadEmployees() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = FirestoreService.getAllUserConfigs()
            _employees.value = list.sortedBy { it.hoVaTen }
            _isLoading.value = false
        }
    }

    fun selectEmployee(employee: UserConfig?) {
        _selectedEmployee.value = employee
        if (employee != null) {
            _selectedEmployeeIds.value = emptySet() // Clear multi-selection when viewing details
            loadAttendanceForEmployee(employee.userId)
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
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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

    private fun loadAttendanceForEmployee(uid: String) {
        viewModelScope.launch {
            val logs = FirestoreService.getAttendanceLogsForUser(uid)
            _attendanceRecords.value = logs
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
            _selectedEmployee.value?.let { loadAttendanceForEmployee(it.userId) }
        }
    }

    fun deleteAttendanceRecord(uid: String, dateString: String) {
        viewModelScope.launch {
            FirestoreService.deleteAttendanceRecord(uid, dateString)
            _selectedEmployee.value?.let { loadAttendanceForEmployee(it.userId) }
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
                        val ds = record.dateString
                        // Match yyyy-MM (e.g. 2026-07-19 starts with 2026-07)
                        val matchYmd = ds.startsWith(finalMonthStr)
                        // Match dd/MM/yyyy (e.g. 19/07/2026 matches /07/2026)
                        val matchDmy = if (finalMonthStr.contains("-")) {
                            val parts = finalMonthStr.split("-")
                            ds.contains("/${parts[1]}/${parts[0]}")
                        } else false
                        
                        matchYmd || matchDmy
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
}
