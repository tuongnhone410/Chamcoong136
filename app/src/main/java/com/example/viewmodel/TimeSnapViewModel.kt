package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthController
import com.example.auth.UserSession
import com.example.data.db.AppDatabase
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.data.repository.TimeRepository
import com.example.data.repository.CloudSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TimeSnapViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = TimeRepository(database.timeEntryDao(), database.userConfigDao())
    val authController = AuthController(application, repository)
    val cloudSyncManager = CloudSyncManager(application)
    private var syncJob: kotlinx.coroutines.Job? = null
    val hasRestoredForSession = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    // Current logged-in UI session state
    val currentUserSession: StateFlow<UserSession?> = authController.currentUserFlow

    // Live sync status text
    private val _cloudSyncStatus = MutableStateFlow("Đang cập nhật")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus

    // Date formatter
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    // UI Selected month (For Calendar View: "yyyy-MM")
    private val _currentSelectedMonth = MutableStateFlow(monthFormatter.format(Date()))
    val currentSelectedMonth: StateFlow<String> = _currentSelectedMonth

    // Custom UI Trigger to refresh calculations or triggers
    private val _triggerRefresh = MutableStateFlow(0)

    // Reactive flow of current user salary config
    val userConfig: StateFlow<UserConfig?> = currentUserSession
        .flatMapLatest { session ->
            if (session != null) {
                repository.getConfig(session.uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Reactive list of active month entries
    val monthTimeEntries: StateFlow<List<TimeEntry>> = combine(
        currentUserSession,
        currentSelectedMonth,
        _triggerRefresh
    ) { session, month, _ ->
        Pair(session, month)
    }.flatMapLatest { (session, month) ->
        if (session != null) {
            repository.getEntriesInMonth(session.uid, "$month-%")
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active working entry of today (checked-in but not checked-out)
    private val _activeWorkingEntry = MutableStateFlow<TimeEntry?>(null)
    val activeWorkingEntry: StateFlow<TimeEntry?> = _activeWorkingEntry

    // Running duration tracker ticker ("Đã làm: X giờ Y phút")
    private val _runningDurationText = MutableStateFlow("")
    val runningDurationText: StateFlow<String> = _runningDurationText

    // All-time/Month summaries list
    private val _salarySummaryState = MutableStateFlow<SalarySummary?>(null)
    val salarySummaryState: StateFlow<SalarySummary?> = _salarySummaryState

    private fun getMonthDifference(startStr: String, endStr: String): Int {
        if (startStr.isEmpty() || endStr.isEmpty()) return 0
        try {
            val parts1 = startStr.split("-")
            val parts2 = endStr.split("-")
            val y1 = parts1[0].toInt()
            val m1 = parts1[1].toInt()
            val y2 = parts2[0].toInt()
            val m2 = parts2[1].toInt()
            return (y2 - y1) * 12 + (m2 - m1)
        } catch (e: Exception) {
            return 0
        }
    }

    init {
        // Automatic monthly phepNamConLai increment (+1 per month) - Runs once at startup
        viewModelScope.launch(Dispatchers.IO) {
            val config = userConfig.filterNotNull().first()
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            if (config.lastAccumulatedMonth.isEmpty()) {
                val updated = config.copy(lastAccumulatedMonth = currentMonthStr)
                repository.saveConfig(updated)
            } else if (config.lastAccumulatedMonth != currentMonthStr) {
                val diff = getMonthDifference(config.lastAccumulatedMonth, currentMonthStr)
                if (diff > 0) {
                    val updated = config.copy(
                        phepNamConLai = config.phepNamConLai + diff,
                        lastAccumulatedMonth = currentMonthStr
                    )
                    repository.saveConfig(updated)
                }
            }
        }

        // Automatic iCloud/Server Data Restore check on Login (survives app re-install)
        viewModelScope.launch(Dispatchers.IO) {
            var lastUid: String? = null
            currentUserSession.collect { session ->
                if (session != null && session.uid != lastUid) {
                    lastUid = session.uid
                    hasRestoredForSession[session.uid] = false
                    _cloudSyncStatus.value = "Đang kiểm tra..."
                    checkAndFetchSalaryConfigFromFirestore(session.uid)
                    try {
                        val currentEntries = repository.getEntries(session.uid).first()
                        if (currentEntries.isEmpty()) {
                            restoreDataFromServer(session.uid)
                        } else {
                            // If user is already active locally, keep the cloud DB updated with any offline changes
                            val config = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
                            cloudSyncManager.uploadToServer(session.uid, currentEntries, config)
                            hasRestoredForSession[session.uid] = true
                            _cloudSyncStatus.value = "Đã đồng bộ"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TimeSnapViewModel", "Auto-restore initialization failed", e)
                        hasRestoredForSession[session.uid] = true
                        _cloudSyncStatus.value = "Lỗi xác thực dữ liệu"
                    }
                } else if (session == null) {
                    lastUid = null
                    _cloudSyncStatus.value = "Yêu cầu đăng nhập"
                }
            }
        }

        // Run reactive worker to fetch active working shift
        viewModelScope.launch(Dispatchers.IO) {
            combine(currentUserSession, _triggerRefresh) { s, r -> s }
                .collectLatest { session ->
                    if (session != null) {
                        val active = repository.getActiveEntry(session.uid)
                        _activeWorkingEntry.value = active
                    } else {
                        _activeWorkingEntry.value = null
                    }
                }
        }

        // Ticker loop for the active working shift text
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val active = _activeWorkingEntry.value
                val config = userConfig.value
                if (active != null && active.checkInTime != null) {
                    val diffMs = System.currentTimeMillis() - active.checkInTime
                    val hours = diffMs / 3600000
                    val minutes = (diffMs % 3600000) / 60000
                    
                    val breakHours = if (config?.tinhKhauTruNghi == true) config.soGioNghiGiaiLao else 0.0
                    val actualWorkedMin = ((diffMs / 60000.0) - (breakHours * 60.0)).coerceAtLeast(0.0)
                    val actHours = (actualWorkedMin / 60).toInt()
                    val actMinutes = (actualWorkedMin % 60).toInt()

                    val targetMs = active.checkInTime + ((8.0 + breakHours) * 3600000).toLong()
                    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val expectedOut = sdfTime.format(Date(targetMs))

                    if (config?.tinhKhauTruNghi == true) {
                        _runningDurationText.value = "Thực làm: ${actHours}g ${actMinutes}p (Đã khấu trừ ${config.soGioNghiGiaiLao}g nghỉ)\nĐủ 8 tiếng lúc: $expectedOut"
                    } else {
                        _runningDurationText.value = "Đã làm: ${hours}g ${minutes}p\nĐủ 8 tiếng lúc: $expectedOut"
                    }
                } else {
                    _runningDurationText.value = ""
                }
                delay(10000) // update every 10 seconds is perfect for resources and text accuracy
            }
        }

        // Calculate pay slip summaries automatically when month entries or config changes
        viewModelScope.launch(Dispatchers.Default) {
            combine(monthTimeEntries, userConfig) { entries, config ->
                Pair(entries, config)
            }.collectLatest { (entries, config) ->
                if (config != null) {
                    val summary = calculateSalarySummary(entries, config)
                    _salarySummaryState.value = summary
                } else {
                    _salarySummaryState.value = null
                }
            }
        }

        // Automatic cloud remote restore on success login removed to prevent parallel sync state conflicts
        // Logic handled robustly by upper collection listener above

    }

    fun selectMonth(yearMonth: String) {
        _currentSelectedMonth.value = yearMonth
    }

    // Toggle Check-In / Check-Out
    fun toggleCheckIn(note: String = "") {
        val session = currentUserSession.value ?: return
        val todayStr = dateFormatter.format(Date())

        viewModelScope.launch(Dispatchers.IO) {
            val active = repository.getActiveEntry(session.uid)
            if (active == null) {
                // Check if an entry already exists for today. If so, overwrite it or create new
                val existing = repository.getEntryByDate(session.uid, todayStr)
                val cal = Calendar.getInstance()
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val isSunday = (dayOfWeek == Calendar.SUNDAY)
                val isHoliday = isHolidayDate(todayStr)
                
                val dayType = when {
                    isHoliday -> "HOLIDAY"
                    isSunday -> "SUNDAY"
                    else -> "NORMAL"
                }

                val newEntry = TimeEntry(
                    id = existing?.id ?: 0,
                    userId = session.uid,
                    date = todayStr,
                    checkInTime = System.currentTimeMillis(),
                    checkOutTime = null,
                    isWorking = true,
                    dayType = dayType,
                    note = note.takeIf { it.isNotBlank() }
                )
                repository.insertOrUpdate(newEntry)
            } else {
                // Perform check-out
                val updated = active.copy(
                    checkOutTime = System.currentTimeMillis(),
                    isWorking = false,
                    note = if (note.isNotBlank()) note else active.note
                )
                repository.insertOrUpdate(updated)
            }
            triggerSync()
        }
    }

    fun updateActiveEntryNote(note: String) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val active = repository.getActiveEntry(session.uid) ?: return@launch
            val updated = active.copy(note = note.takeIf { it.isNotBlank() })
            repository.insertOrUpdate(updated)
            triggerSyncDebounced()
        }
    }

    // Refresh configurations
    fun triggerSync() {
        _triggerRefresh.value += 1
        val session = currentUserSession.value
        if (session != null) {
            if (hasRestoredForSession[session.uid] != true) {
                android.util.Log.d("TimeSnapViewModel", "Tránh ghi đè: Bỏ qua triggerSync vì đang đồng bộ/khôi phục đám mây.")
                return
            }
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val list = repository.getEntries(session.uid).first()
                    val config = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
                    cloudSyncManager.uploadToServer(session.uid, list, config)
                    _cloudSyncStatus.value = "Đã đồng bộ"
                } catch (e: Exception) {
                    _cloudSyncStatus.value = "Lỗi đồng bộ tự động"
                }
            }
        }
    }

    fun triggerSyncDebounced() {
        _triggerRefresh.value += 1
        syncJob?.cancel()
        val session = currentUserSession.value
        if (session != null) {
            if (hasRestoredForSession[session.uid] != true) {
                android.util.Log.d("TimeSnapViewModel", "Tránh ghi đè: Bỏ qua triggerSyncDebounced vì đang đồng bộ/khôi phục đám mây.")
                return
            }
            syncJob = viewModelScope.launch(Dispatchers.IO) {
                delay(1500) // Wait 1.5s of typing silence before syncing to cloud database
                try {
                    val list = repository.getEntries(session.uid).first()
                    val config = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
                    cloudSyncManager.uploadToServer(session.uid, list, config)
                    _cloudSyncStatus.value = "Đã đồng bộ"
                } catch (e: Exception) {
                    _cloudSyncStatus.value = "Lỗi đồng bộ tự động"
                }
            }
        }
    }

    // Clear and pull fully from mock cloud server
    fun restoreDataFromServer(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _cloudSyncStatus.value = "Khôi phục dữ liệu..."
            try {
                val remoteData = cloudSyncManager.downloadFromServer(userId)
                if (remoteData != null) {
                    val (entries, config) = remoteData
                    if (config != null) {
                        repository.saveConfig(config)
                    }
                    database.timeEntryDao().clearAllForUser(userId)
                    for (entry in entries) {
                        repository.insertOrUpdate(entry)
                    }
                    _cloudSyncStatus.value = "Đã đồng bộ"
                    _triggerRefresh.value += 1
                } else {
                    _cloudSyncStatus.value = "Không tìm thấy dữ liệu mây"
                }
            } catch (e: Exception) {
                _cloudSyncStatus.value = "Lỗi khôi phục đám mây"
            } finally {
                hasRestoredForSession[userId] = true
            }
        }
    }

    fun checkAndFetchSalaryConfigFromFirestore(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbConfig = com.example.data.FirestoreService.fetchUserSalaryConfigFromFirestore(userId)
                if (dbConfig != null) {
                    android.util.Log.d("TimeSnapViewModel", "Found user salary config on Firestore for $userId. Applying locally.")
                    repository.saveConfig(dbConfig)
                } else {
                    android.util.Log.d("TimeSnapViewModel", "No user salary config found on Firestore for $userId.")
                }
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Failed to restore salary settings from firestore: ${e.message}")
            }
        }
    }

    // Direct save matching realtime changes
    fun updateSalaryConfig(config: UserConfig) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = config.copy(userId = session.uid)
            repository.saveConfig(updated)
            try {
                com.example.data.FirestoreService.saveUserSalaryConfigToFirestore(updated)
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Failed to sync to Firestore users_salary list: ${e.message}")
            }
            triggerSyncDebounced()
        }
    }

    // Update Salary Settings
    fun updateSalarySettings(
        luongCoBan: Double,
        tienCom: Double,
        phuCap: Double,
        phuCapXangXe: Double,
        phuCapDienThoai: Double,
        phuCapNhaO: Double,
        phuCapChuyenCan: Double,
        thuong: Double,
        heSoOtDem: Double
    ) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getConfigDirect(session.uid) ?: UserConfig(userId = session.uid)
            val updated = current.copy(
                luongCoBan = luongCoBan,
                tienComMoiNgay = tienCom,
                phuCap = phuCap,
                phuCapXangXe = phuCapXangXe,
                phuCapDienThoai = phuCapDienThoai,
                phuCapNhaO = phuCapNhaO,
                phuCapChuyenCan = phuCapChuyenCan,
                thuong = thuong,
                heSoOtDem = heSoOtDem
            )
            repository.saveConfig(updated)
            try {
                com.example.data.FirestoreService.saveUserSalaryConfigToFirestore(updated)
            } catch (e: Exception) {
                android.util.Log.e("TimeSnapViewModel", "Failed to sync to Firestore users_salary list: ${e.message}")
            }
            triggerSync()
        }
    }

    // ADD SINGLE ENTRY (CHẾ ĐỘ 1)
    fun addSingleEntry(
        dateStr: String,
        checkInHour: Int,
        checkInMin: Int,
        checkOutHour: Int?,
        checkOutMin: Int?,
        dayTypeOverride: String? = null,
        noteStr: String? = null
    ) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isFuture = dateStr > todayStr

            // Check if day is Sunday or Holiday
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            var dayType = "NORMAL"
            try {
                val dateVal = parser.parse(dateStr)
                if (dateVal != null) {
                    val cal = Calendar.getInstance()
                    cal.time = dateVal
                    val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    if (isSunday) {
                        dayType = "SUNDAY"
                    } else if (isHolidayDate(dateStr)) {
                        dayType = "HOLIDAY"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Apply override if selected
            if (!dayTypeOverride.isNullOrEmpty()) {
                dayType = dayTypeOverride
            }

            // ENFORCE RULE: Future dates cannot be normal working days. Only leaves allowed.
            if (isFuture) {
                if (dayType != "PAID_LEAVE" && dayType != "UNPAID_LEAVE" && dayType != "HOLIDAY_LEAVE") {
                    val config = repository.getConfigDirect(session.uid)
                    val phepConLai = config?.phepNamConLai ?: 0
                    dayType = if (phepConLai > 0) "PAID_LEAVE" else "UNPAID_LEAVE"
                }
            }

            val baseCal = Calendar.getInstance()
            val parts = dateStr.split("-")
            val yr = parts[0].toInt()
            val mo = parts[1].toInt() - 1
            val dy = parts[2].toInt()

            baseCal.set(yr, mo, dy, checkInHour, checkInMin, 0)
            val checkInMs = baseCal.timeInMillis

            val checkOutMs = if (checkOutHour != null && checkOutMin != null) {
                val outCal = Calendar.getInstance()
                outCal.set(yr, mo, dy, checkOutHour, checkOutMin, 0)
                if (outCal.timeInMillis < checkInMs) {
                    outCal.add(Calendar.DAY_OF_MONTH, 1)
                }
                outCal.timeInMillis
            } else {
                null
            }

            val isWorking = (checkOutMs == null && dayType != "PAID_LEAVE" && dayType != "UNPAID_LEAVE" && dayType != "HOLIDAY_LEAVE")

            val existing = repository.getEntryByDate(session.uid, dateStr)
            
            // Re-evaluate annual leave delta
            val config = repository.getConfigDirect(session.uid)
            var finalDayType = dayType
            
            if (config != null) {
                var phepNamDelta = 0
                if (existing?.dayType == "PAID_LEAVE") {
                    phepNamDelta += 1
                }
                if (finalDayType == "PAID_LEAVE") {
                    val currentRemaining = config.phepNamConLai + phepNamDelta
                    if (currentRemaining <= 0) {
                        // Out of leave quota, coerce to unpaid leave
                        finalDayType = "UNPAID_LEAVE"
                    } else {
                        // Valid leave, consume 1
                        phepNamDelta -= 1
                    }
                }
                if (phepNamDelta != 0) {
                    val updatedConfig = config.copy(
                        phepNamConLai = (config.phepNamConLai + phepNamDelta).coerceAtLeast(0)
                    )
                    repository.saveConfig(updatedConfig)
                }
            }

            val newEntry = TimeEntry(
                id = existing?.id ?: 0,
                userId = session.uid,
                date = dateStr,
                checkInTime = if (finalDayType == "PAID_LEAVE" || finalDayType == "UNPAID_LEAVE" || finalDayType == "HOLIDAY_LEAVE") null else checkInMs,
                checkOutTime = if (finalDayType == "PAID_LEAVE" || finalDayType == "UNPAID_LEAVE" || finalDayType == "HOLIDAY_LEAVE") null else checkOutMs,
                isWorking = isWorking,
                dayType = finalDayType,
                note = noteStr
            )

            repository.insertOrUpdate(newEntry)
            triggerSync()
        }
    }

    // BULK ENTRY (CHẾ ĐỘ 2)
    fun addBulkEntries(
        selectedDates: List<String>,
        checkInHour: Int,
        checkInMin: Int,
        checkOutHour: Int,
        checkOutMin: Int,
        skipSunday: Boolean,
        skipHoliday: Boolean,
        autoRecognizeOt: Boolean,
        isNightShiftOverride: Boolean = false
    ) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            for (dateStr in selectedDates) {
                // Rule check: Block future date work entries in bulk creation
                val isFuture = dateStr > todayStr
                if (isFuture) continue

                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                var dateVal: Date? = null
                var isSunday = false
                var isHoliday = false
                try {
                    dateVal = parser.parse(dateStr)
                    if (dateVal != null) {
                        val cal = Calendar.getInstance()
                        cal.time = dateVal
                        isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        isHoliday = isHolidayDate(dateStr)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (skipSunday && isSunday) continue
                if (skipHoliday && isHoliday) continue

                val dayType = when {
                    isNightShiftOverride -> "NIGHT"
                    autoRecognizeOt && isHoliday -> "HOLIDAY"
                    autoRecognizeOt && isSunday -> "SUNDAY"
                    else -> "NORMAL"
                }

                val parts = dateStr.split("-")
                val yr = parts[0].toInt()
                val mo = parts[1].toInt() - 1
                val dy = parts[2].toInt()

                val inCal = Calendar.getInstance()
                inCal.set(yr, mo, dy, checkInHour, checkInMin, 0)
                val checkInMs = inCal.timeInMillis

                val outCal = Calendar.getInstance()
                outCal.set(yr, mo, dy, checkOutHour, checkOutMin, 0)
                if (outCal.timeInMillis < checkInMs) {
                    outCal.add(Calendar.DAY_OF_MONTH, 1)
                }
                val checkOutMs = outCal.timeInMillis

                val existing = repository.getEntryByDate(session.uid, dateStr)
                
                // If existing was PAID_LEAVE and is being overwritten by work, restore 1 leave
                if (existing?.dayType == "PAID_LEAVE") {
                    val config = repository.getConfigDirect(session.uid)
                    if (config != null) {
                        repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + 1))
                    }
                }

                val entry = TimeEntry(
                    id = existing?.id ?: 0,
                    userId = session.uid,
                    date = dateStr,
                    checkInTime = checkInMs,
                    checkOutTime = checkOutMs,
                    isWorking = false,
                    dayType = dayType
                )
                repository.insertOrUpdate(entry)
            }
            triggerSync()
        }
    }

    fun deleteEntry(entry: TimeEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entry)
            
            // Revert leave quota if deleted entry was PAID_LEAVE
            if (entry.dayType == "PAID_LEAVE") {
                val session = currentUserSession.value
                if (session != null) {
                    val config = repository.getConfigDirect(session.uid)
                    if (config != null) {
                        repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + 1))
                    }
                }
            }
            triggerSync()
        }
    }

    fun deleteBulkEntries(selectedDatesList: List<String>) {
        val session = currentUserSession.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            var restoredLeaves = 0
            for (dateStr in selectedDatesList) {
                val existing = repository.getEntryByDate(session.uid, dateStr)
                if (existing != null) {
                    if (existing.dayType == "PAID_LEAVE") {
                        restoredLeaves++
                    }
                    repository.delete(existing)
                }
            }
            if (restoredLeaves > 0) {
                val config = repository.getConfigDirect(session.uid)
                if (config != null) {
                    repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + restoredLeaves))
                }
            }
            triggerSync()
        }
    }

    fun clearAllEntriesInSelectedMonth() {
        val session = currentUserSession.value ?: return
        val month = currentSelectedMonth.value
        viewModelScope.launch(Dispatchers.IO) {
            // Restore any paid leaves being cleared in this month
            val allEntries = repository.getEntriesInMonthDirect(session.uid, "$month-%")
            val phepToRestore = allEntries.count { it.dayType == "PAID_LEAVE" }
            
            repository.deleteEntriesInMonth(session.uid, "$month-%")
            
            if (phepToRestore > 0) {
                val config = repository.getConfigDirect(session.uid)
                if (config != null) {
                    repository.saveConfig(config.copy(phepNamConLai = config.phepNamConLai + phepToRestore))
                }
            }
            triggerSync()
        }
    }

    private fun isHolidayDate(dateStr: String): Boolean {
        val parts = dateStr.split("-")
        if (parts.size >= 3) {
            val md = "${parts[1]}-${parts[2]}"
            return md == "01-01" || md == "04-30" || md == "05-01" || md == "09-02"
        }
        return false
    }

    private fun roundCheckInTime(timeMs: Long): Long {
        // Return exact check-in time without rounding up to ensure continuous shifts (e.g. 7:30 - 15:30)
        // are computed exactly as 8.0 hours and overtime is 100% accurate.
        return timeMs
    }

    private fun calculateSalarySummary(entries: List<TimeEntry>, config: UserConfig): SalarySummary {
        val luongBasic = config.luongCoBan

        val selectedMonth = currentSelectedMonth.value
        var targetYear = 2026
        var targetMonth = 5
        try {
            val parts = selectedMonth.split("-")
            targetYear = parts[0].toInt()
            targetMonth = parts[1].toInt()
        } catch (e: Exception) {}

        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val todayDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

        val isCurrentSelectedMonth = (targetYear == currentYear && targetMonth == currentMonth)
        val todayStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, todayDayOfMonth)

        val expectedWorkDaysCount = 26
        val standardWorkDaysInMonth = 26
        val dailySalary = luongBasic / 26.0
        val hourlySalary = dailySalary / 8.0

        // Find all public holidays in the selected month
        val holidayDatesInMonth = mutableSetOf<String>()
        try {
            val maxDaysInMo = Calendar.getInstance().apply {
                set(Calendar.YEAR, targetYear)
                set(Calendar.MONTH, targetMonth - 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (day in 1..maxDaysInMo) {
                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                if (isHolidayDate(dateStr)) {
                    // Do not include future holiday/leaves if it is currently selected month and date is in future
                    if (!isCurrentSelectedMonth || dateStr <= todayStr) {
                        holidayDatesInMonth.add(dateStr)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Identify which holiday dates have been worked (have check-in logged)
        val workedHolidayDates = entries.filter { e ->
            holidayDatesInMonth.contains(e.date) && e.checkInTime != null
        }.map { it.date }.toSet()

        // Unworked holidays automatically merit full 1-day standard salary as a Holiday Leave
        val unworkedHolidaysCount = (holidayDatesInMonth - workedHolidayDates).size

        var workingDaysCount = unworkedHolidaysCount
        var actualPresenceDaysCount = 0
        var totalStandardHours = unworkedHolidaysCount * 8.0
        var totalOtDayHours = 0.0
        var totalOtNightHours = 0.0

        var otDayPay = 0.0
        var otNightPay = 0.0
        var comOtDaysCount = 0
        var totalSundayHours = 0.0
        var sundayPay = 0.0
        var nightShiftsCount = 0

        for (e in entries) {
            // Do not calculate future days/leaves as they have not happened yet
            if (isCurrentSelectedMonth && e.date > todayStr) {
                continue
            }

            val isHolidayDateVal = holidayDatesInMonth.contains(e.date)
            if (isHolidayDateVal && e.checkInTime == null) {
                // Already counted automatically as unworked holiday, skip processing to avoid duplication
                continue
            }

            if (e.dayType == "PAID_LEAVE" || e.dayType == "HOLIDAY_LEAVE") {
                workingDaysCount++
                totalStandardHours += 8.0
                continue
            }
            if (e.dayType == "UNPAID_LEAVE") {
                continue
            }

            if (e.checkInTime == null) continue

            val inCal = Calendar.getInstance()
            inCal.timeInMillis = e.checkInTime
            val inHour = inCal.get(Calendar.HOUR_OF_DAY)
            val isNightShift = if (e.checkOutTime != null) {
                val outCal = Calendar.getInstance()
                outCal.timeInMillis = e.checkOutTime
                val outHour = outCal.get(Calendar.HOUR_OF_DAY)
                inHour >= 22 || inHour <= 6 || outHour >= 22 || outHour <= 6 || e.dayType == "NIGHT"
            } else {
                inHour >= 22 || inHour <= 6 || e.dayType == "NIGHT"
            }
            if (isNightShift) {
                nightShiftsCount++
            }

            val isSunday = (e.dayType == "SUNDAY")

            if (e.isWorking) {
                if (isSunday) {
                    actualPresenceDaysCount++
                    totalSundayHours += 8.0
                    sundayPay += 8.0 * hourlySalary * config.heSoOtChuNhat
                } else {
                    workingDaysCount++
                    actualPresenceDaysCount++
                    totalStandardHours += 8.0
                }
                continue
            }

            if (e.checkOutTime == null) continue

            val originalCheckIn = e.checkInTime
            val roundedCheckIn = roundCheckInTime(originalCheckIn)
            val finalCheckIn = if (roundedCheckIn < e.checkOutTime) roundedCheckIn else originalCheckIn

            val durationMs = e.checkOutTime - finalCheckIn
            val rawHours = durationMs / 3600000.0
            
            // Subtract unpaid bridge/break hours per company standard contract if enabled
            val breakHours = if (config.tinhKhauTruNghi) config.soGioNghiGiaiLao else 0.0
            val actualHours = (rawHours - breakHours).coerceAtLeast(0.0)

            val finalStandardHours = actualHours.coerceAtMost(8.0)
            val finalOtHours = (actualHours - 8.0).coerceAtLeast(0.0)

            if (isSunday) {
                actualPresenceDaysCount++
                totalSundayHours += actualHours
                val dayPay = actualHours * hourlySalary * config.heSoOtChuNhat
                sundayPay += dayPay
                if (finalOtHours >= 2.0) {
                    comOtDaysCount++
                }
            } else {
                workingDaysCount++
                actualPresenceDaysCount++
                totalStandardHours += finalStandardHours

                if (finalOtHours >= 2.0) {
                    comOtDaysCount++
                }

                if (finalOtHours > 0.0) {
                    totalOtDayHours += finalOtHours
                    val coeff = when (e.dayType) {
                        "HOLIDAY" -> config.heSoOtNgayLe
                        else -> config.heSoOtNgayThuong
                    }
                    otDayPay += finalOtHours * (hourlySalary * coeff)
                }
            }
        }

        val allowanceDivisor = 26.0

        val pcKyThuatPr = Math.round((config.pcKyThuat / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcKyThuat)
        val pcTrachNhiemPr = Math.round((config.pcTrachNhiem / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcTrachNhiem)
        val pcChucVuPr = Math.round((config.pcChucVu / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcChucVu)
        val pcHieuSuatPr = Math.round((config.pcHieuSuat / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcHieuSuat)
        val pcSanPhamPr = Math.round((config.pcSanPham / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcSanPham)
        val pcComCaPr = (actualPresenceDaysCount * config.pcComCa)
        val pcComOtPr = (comOtDaysCount * config.pcComOt) // calculated on exact overtime days where total shift >= 10h
        val pcNhaOPr = Math.round((config.pcNhaO / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcNhaO)
        val pcDocHaiPr = Math.round((config.pcDocHai / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcDocHai)
        val pcDtDoanhThuPr = Math.round((config.pcDtDoanhThu / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcDtDoanhThu)
        val pcXangXePr = Math.round((config.pcXangXe / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcXangXe)
        val pcThamNienPr = config.pcThamNien // 100% full monthly value, never deducted, as requested
        val pcKhac1Pr = Math.round((config.pcKhac1 / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcKhac1)
        val pcKhacPr = Math.round((config.pcKhac / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.pcKhac)

        val pcCaDemPr = nightShiftsCount * 100000.0

        val phuCapTong = pcKyThuatPr + pcTrachNhiemPr + pcChucVuPr + pcHieuSuatPr + 
                pcSanPhamPr + pcComCaPr + pcComOtPr + pcNhaOPr + 
                pcDocHaiPr + pcDtDoanhThuPr + pcXangXePr + pcThamNienPr + 
                pcKhac1Pr + pcKhacPr + pcCaDemPr

        // Chuyen can block calculated per standard work / dynamic standard days * working day contribution
        val chuyenCanValue = Math.round((config.tienChuyenCanGoc / allowanceDivisor) * workingDaysCount).toDouble().coerceAtMost(config.tienChuyenCanGoc)

        // Remaining retro-compatible values for standard slots (set to 0 to prevent double-counting dynamic meal allowances)
        val tongCom = 0.0

        // Deductions
        val tieuBaoHiem = Math.round(config.luongDongBaoHiem * (config.tiLeDongBaoHiem / 100.0)).toDouble()
        val doanPhi = config.doanPhiCongDoan

        // Missed day deduction (Compare with dynamically expected workdays count up to today/full month)
        var missedDays = 0
        if (isCurrentSelectedMonth) {
            try {
                for (day in 1 until todayDayOfMonth) {
                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
                    val cal = Calendar.getInstance()
                    cal.set(currentYear, currentMonth - 1, day)
                    val isSunday = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    val isHoliday = isHolidayDate(dateStr)
                    if (!isSunday && !isHoliday) {
                        // Check if they worked or had a paid leave on this day
                        val entryForDay = entries.find { it.date == dateStr }
                        val workedOrPaid = entryForDay != null && (
                            entryForDay.checkInTime != null || 
                            entryForDay.dayType == "PAID_LEAVE" || 
                            entryForDay.dayType == "HOLIDAY_LEAVE" || 
                            entryForDay.isWorking
                        )
                        if (!workedOrPaid) {
                            missedDays++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            missedDays = (expectedWorkDaysCount - workingDaysCount).coerceAtLeast(0)
        }
        val baseBasicSalary = Math.round((luongBasic / 26.0) * workingDaysCount).toDouble().coerceAtMost(luongBasic)
        val tienKhauTruNghi = 0.0

        val roundedOtDay = Math.round(otDayPay).toDouble()
        val roundedOtNight = Math.round(otNightPay).toDouble()
        val roundedSundayPay = Math.round(sundayPay).toDouble()

        val grossAdditions = baseBasicSalary + roundedOtDay + roundedOtNight + roundedSundayPay + tongCom + phuCapTong + chuyenCanValue
        val totalDeductions = tieuBaoHiem + doanPhi + tienKhauTruNghi
        val luongThucNhan = Math.round(grossAdditions - totalDeductions).coerceAtLeast(0L).toDouble()

        return SalarySummary(
            workingDays = workingDaysCount,
            standardHours = totalStandardHours,
            otDayHours = totalOtDayHours,
            otNightHours = totalOtNightHours,
            tienOtNgay = roundedOtDay,
            tienOtDem = roundedOtNight,
            tongTienCom = tongCom,
            phuCap = phuCapTong,
            phuCapXangXe = pcXangXePr,
            phuCapDienThoai = pcDtDoanhThuPr,
            phuCapNhaO = pcNhaOPr,
            phuCapChuyenCan = chuyenCanValue,
            thuong = 0.0,
            tienBh = tieuBaoHiem,
            doanPhi = doanPhi,
            tienKhauTruNghi = tienKhauTruNghi,
            luongThucNhan = luongThucNhan,
            baseBasicSalary = baseBasicSalary,
            expectedWorkDays = expectedWorkDaysCount,
            standardWorkDays = standardWorkDaysInMonth,
            isCurrentMonth = isCurrentSelectedMonth,
            
            pcKyThuatVal = pcKyThuatPr,
            pcTrachNhiemVal = pcTrachNhiemPr,
            pcChucVuVal = pcChucVuPr,
            pcHieuSuatVal = pcHieuSuatPr,
            pcSanPhamVal = pcSanPhamPr,
            pcComCaVal = pcComCaPr,
            pcComOtVal = pcComOtPr,
            pcNhaOVal = pcNhaOPr,
            pcDocHaiVal = pcDocHaiPr,
            pcDtDoanhThuVal = pcDtDoanhThuPr,
            pcXangXeVal = pcXangXePr,
            pcThamNienVal = pcThamNienPr,
            pcKhac1Val = pcKhac1Pr,
            pcKhacVal = pcKhacPr,
            pcCaDemVal = pcCaDemPr,
            caDemCount = nightShiftsCount,
            
            tienChuNhat = roundedSundayPay,
            chuNhatHours = totalSundayHours
        )
    }

}

// Data wrapper for salary analysis
data class SalarySummary(
    val workingDays: Int,
    val standardHours: Double,
    val otDayHours: Double,
    val otNightHours: Double,
    val tienOtNgay: Double,
    val tienOtDem: Double,
    val tongTienCom: Double,
    val phuCap: Double,
    val phuCapXangXe: Double = 0.0,
    val phuCapDienThoai: Double = 0.0,
    val phuCapNhaO: Double = 0.0,
    val phuCapChuyenCan: Double = 0.0,
    val thuong: Double,
    val tienBh: Double,
    val doanPhi: Double,
    val tienKhauTruNghi: Double,
    val luongThucNhan: Double,
    val baseBasicSalary: Double = 0.0,
    val expectedWorkDays: Int = 26,
    val standardWorkDays: Int = 26,
    val isCurrentMonth: Boolean = false,
    
    val pcKyThuatVal: Double = 0.0,
    val pcTrachNhiemVal: Double = 0.0,
    val pcChucVuVal: Double = 0.0,
    val pcHieuSuatVal: Double = 0.0,
    val pcSanPhamVal: Double = 0.0,
    val pcComCaVal: Double = 0.0,
    val pcComOtVal: Double = 0.0,
    val pcNhaOVal: Double = 0.0,
    val pcDocHaiVal: Double = 0.0,
    val pcDtDoanhThuVal: Double = 0.0,
    val pcXangXeVal: Double = 0.0,
    val pcThamNienVal: Double = 0.0,
    val pcKhac1Val: Double = 0.0,
    val pcKhacVal: Double = 0.0,
    val pcCaDemVal: Double = 0.0,
    val caDemCount: Int = 0,
    val tienChuNhat: Double = 0.0,
    val chuNhatHours: Double = 0.0
)
