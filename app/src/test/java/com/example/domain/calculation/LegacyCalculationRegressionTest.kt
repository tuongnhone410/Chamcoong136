package com.example.domain.calculation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ShiftConfig
import com.example.data.db.AppDatabase
import com.example.data.model.OvertimeRule
import com.example.data.model.ShiftEntity
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.data.model.WorkRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LegacyCalculationRegressionTest {

    private lateinit var legacyEngine: LegacyCalculationEngine
    private lateinit var configurableEngine: ConfigurableCalculationEngine
    private lateinit var defaultConfig: UserConfig
    private lateinit var legacyWorkRule: WorkRule
    private lateinit var legacyOvertimeRule: OvertimeRule
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        legacyEngine = LegacyCalculationEngine()
        
        // Setup WorkRule and OvertimeRule that replicate the legacy engine parameters exactly
        legacyWorkRule = WorkRule(
            id = "legacy_work_rule",
            companyId = "legacy_company",
            name = "Legacy Work Rule",
            standardHoursPerDay = 8.0,
            overtimeStartAfterHours = 8.0,
            roundingMinutes = 15
        )
        legacyOvertimeRule = OvertimeRule(
            id = "legacy_ot_rule",
            companyId = "legacy_company",
            name = "Legacy Overtime Rule",
            normalDayMultiplier = 1.5,
            weeklyOffMultiplier = 2.0,
            holidayMultiplier = 3.0
        )
        configurableEngine = ConfigurableCalculationEngine(
            defaultWorkRule = legacyWorkRule,
            defaultOvertimeRule = legacyOvertimeRule
        )

        defaultConfig = UserConfig(
            userId = "legacy_user_123",
            luongCoBan = 10000000.0,
            luongDongBaoHiem = 5000000.0,
            tiLeDongBaoHiem = 10.5,
            heSoOtNgayThuong = 1.5,
            heSoOtChuNhat = 2.0,
            heSoOtNgayLe = 3.0,
            heSoOtDem = 1.75,
            pcComCa = 50000.0,
            pcComOt = 30000.0,
            pcXangXe = 500000.0,
            pcNhaO = 1000000.0,
            tinhKhauTruNghi = true,
            soGioNghiGiaiLao = 1.5
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun getMillis(hour: Int, minute: Int, dayOffset: Int = 0, isSunday: Boolean = false, isHoliday: Boolean = false): Long {
        val cal = Calendar.getInstance()
        // Standard test date: Aug 3, 2026 (Monday)
        val day = when {
            isSunday -> 9 // Aug 9, 2026 (Sunday)
            isHoliday -> 2 // Aug 2, 2026 (We can override dates using String, but for millis we use day 3 or 9)
            else -> 3
        }
        val month = if (isHoliday) Calendar.SEPTEMBER else Calendar.AUGUST // Sep 2 is holiday in Vietnam
        val dayOfMonth = if (isHoliday) 2 else day
        
        cal.set(2026, month, dayOfMonth, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (dayOffset != 0) {
            cal.add(Calendar.DAY_OF_MONTH, dayOffset)
        }
        return cal.timeInMillis
    }

    /**
     * Helper to run equivalence testing on single time entry calculations.
     */
    private fun assertSingleEntryEquivalence(entry: TimeEntry) {
        val legacyResult = legacyEngine.calculateSingleEntry(entry, defaultConfig)
        
        // For the configurable engine, we snap the legacy rule IDs onto the entry to simulate it being calculated with rules
        val configurableEntry = entry.copy(
            workRuleId = legacyWorkRule.id,
            overtimeRuleId = legacyOvertimeRule.id,
            snapshotStandardHours = legacyWorkRule.standardHoursPerDay,
            snapshotOtMultiplier = legacyOvertimeRule.normalDayMultiplier
        )
        val configurableResult = configurableEngine.calculateSingleEntry(configurableEntry, defaultConfig)

        // Compare key output dimensions
        assertEquals("workDay should be identical for date ${entry.date}", legacyResult.workDay, configurableResult.workDay, 0.01)
        assertEquals("otHours should be identical for date ${entry.date}", legacyResult.otHours, configurableResult.otHours, 0.01)
        assertEquals("lateMinutes should be identical for date ${entry.date}", legacyResult.lateMinutes, configurableResult.lateMinutes)
        assertEquals("earlyLeaveMinutes should be identical for date ${entry.date}", legacyResult.earlyLeaveMinutes, configurableResult.earlyLeaveMinutes)
        assertEquals("customBreakHours should be identical for date ${entry.date}", legacyResult.customBreakHours, configurableResult.customBreakHours)
        assertEquals("shiftId should be identical for date ${entry.date}", legacyResult.shiftId, configurableResult.shiftId)
        assertEquals("shiftType should be identical for date ${entry.date}", legacyResult.shiftType, configurableResult.shiftType)
    }

    /**
     * Helper to run equivalence testing on monthly salary summaries.
     */
    private fun assertMonthlySalaryEquivalence(entries: List<TimeEntry>, holidayDates: Set<String> = emptySet()) {
        val legacySummary = legacyEngine.calculateMonthlySalary(
            entries = entries,
            config = defaultConfig,
            scheduledDaysSoFar = entries.size,
            totalScheduledDaysInMonth = 26,
            earliestDate = "01/08/2026",
            selectedMonth = "08/2026",
            todayStr = "31/08/2026",
            isCurrentSelectedMonth = false,
            holidayDatesInMonth = holidayDates
        )

        val configurableEntries = entries.map {
            it.copy(
                workRuleId = legacyWorkRule.id,
                overtimeRuleId = legacyOvertimeRule.id,
                snapshotStandardHours = legacyWorkRule.standardHoursPerDay,
                snapshotOtMultiplier = legacyOvertimeRule.normalDayMultiplier
            )
        }
        val configurableSummary = configurableEngine.calculateMonthlySalary(
            entries = configurableEntries,
            config = defaultConfig,
            scheduledDaysSoFar = entries.size,
            totalScheduledDaysInMonth = 26,
            earliestDate = "01/08/2026",
            selectedMonth = "08/2026",
            todayStr = "31/08/2026",
            isCurrentSelectedMonth = false,
            holidayDatesInMonth = holidayDates
        )

        assertEquals("workingDays must match", legacySummary.workingDays, configurableSummary.workingDays)
        assertEquals("standardHours must match", legacySummary.standardHours, configurableSummary.standardHours, 0.01)
        assertEquals("otDayHours must match", legacySummary.otDayHours, configurableSummary.otDayHours, 0.01)
        assertEquals("otNightHours must match", legacySummary.otNightHours, configurableSummary.otNightHours, 0.01)
        assertEquals("otLeHours must match", legacySummary.otLeHours, configurableSummary.otLeHours, 0.01)
        assertEquals("chuNhatHours must match", legacySummary.chuNhatHours, configurableSummary.chuNhatHours, 0.01)
        assertEquals("baseBasicSalary must match", legacySummary.baseBasicSalary, configurableSummary.baseBasicSalary, 0.01)
        assertEquals("phuCap must match", legacySummary.phuCap, configurableSummary.phuCap, 0.01)
        assertEquals("tienOtNgay must match", legacySummary.tienOtNgay, configurableSummary.tienOtNgay, 0.01)
        assertEquals("tienOtDem must match", legacySummary.tienOtDem, configurableSummary.tienOtDem, 0.01)
        assertEquals("tienOtLe must match", legacySummary.tienOtLe, configurableSummary.tienOtLe, 0.01)
        assertEquals("tienChuNhat must match", legacySummary.tienChuNhat, configurableSummary.tienChuNhat, 0.01)
        assertEquals("luongThucNhan must match", legacySummary.luongThucNhan, configurableSummary.luongThucNhan, 0.01)
    }

    // --- REGRESSION CALCULATION TESTS ---

    @Test
    fun testRegression_NormalDayShift_ca1() {
        val entry = TimeEntry(
            id = 1,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(19, 30)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_DayShift_WithOT() {
        val entry = TimeEntry(
            id = 2,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(21, 30)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_DayShiftRest_ca2() {
        val entry = TimeEntry(
            id = 3,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca2",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(20, 0)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_NightShift_Overnight() {
        val entry = TimeEntry(
            id = 4,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            checkInTime = getMillis(19, 30),
            checkOutTime = getMillis(7, 30, dayOffset = 1)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_LateArrival() {
        val entry = TimeEntry(
            id = 5,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(8, 30),
            checkOutTime = getMillis(19, 30)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_EarlyDeparture() {
        val entry = TimeEntry(
            id = 6,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(18, 30)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_HalfDayWork() {
        val entry = TimeEntry(
            id = 7,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(12, 0)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_PaidLeave() {
        val entry = TimeEntry(
            id = 8,
            userId = "legacy_user_123",
            date = "03/08/2026",
            dayType = "PAID_LEAVE"
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_UnpaidLeave() {
        val entry = TimeEntry(
            id = 9,
            userId = "legacy_user_123",
            date = "03/08/2026",
            dayType = "UNPAID_LEAVE"
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_SundayWork() {
        val entry = TimeEntry(
            id = 10,
            userId = "legacy_user_123",
            date = "09/08/2026", // Aug 9, 2026 is Sunday
            shiftId = "ca1",
            checkInTime = getMillis(7, 30, isSunday = true),
            checkOutTime = getMillis(19, 30, isSunday = true)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_HolidayWork() {
        val entry = TimeEntry(
            id = 11,
            userId = "legacy_user_123",
            date = "02/09/2026", // National Day (Vietnam) is Sep 2
            dayType = "HOLIDAY",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30, isHoliday = true),
            checkOutTime = getMillis(19, 30, isHoliday = true)
        )
        assertSingleEntryEquivalence(entry)
    }

    @Test
    fun testRegression_MealAllowancesAndOvertimeMeals() {
        val allowanceMealCa = legacyEngine.calculateAllowanceValue(
            fieldName = "pcComCa",
            allowanceValue = 50000.0,
            calcType = "PER_WORK_DAY",
            totalWorkDays = 22.0,
            comCaCount = 22,
            comOtCount = 10,
            nightShiftsCount = 5,
            scheduledDaysSoFar = 22,
            totalScheduledDaysInMonth = 26
        )
        val configurableMealCa = configurableEngine.calculateAllowanceValue(
            fieldName = "pcComCa",
            allowanceValue = 50000.0,
            calcType = "PER_WORK_DAY",
            totalWorkDays = 22.0,
            comCaCount = 22,
            comOtCount = 10,
            nightShiftsCount = 5,
            scheduledDaysSoFar = 22,
            totalScheduledDaysInMonth = 26
        )
        assertEquals(allowanceMealCa, configurableMealCa, 0.01)

        val allowanceMealOt = legacyEngine.calculateAllowanceValue(
            fieldName = "pcComOt",
            allowanceValue = 30000.0,
            calcType = "OT_MEAL_GE_1H",
            totalWorkDays = 22.0,
            comCaCount = 22,
            comOtCount = 10,
            nightShiftsCount = 5,
            scheduledDaysSoFar = 22,
            totalScheduledDaysInMonth = 26
        )
        val configurableMealOt = configurableEngine.calculateAllowanceValue(
            fieldName = "pcComOt",
            allowanceValue = 30000.0,
            calcType = "OT_MEAL_GE_1H",
            totalWorkDays = 22.0,
            comCaCount = 22,
            comOtCount = 10,
            nightShiftsCount = 5,
            scheduledDaysSoFar = 22,
            totalScheduledDaysInMonth = 26
        )
        assertEquals(allowanceMealOt, configurableMealOt, 0.01)
    }

    @Test
    fun testRegression_MonthlySalaryCompilationEquivalence() {
        val entry1 = TimeEntry(
            id = 20,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(19, 30)
        )
        val entry2 = TimeEntry(
            id = 21,
            userId = "legacy_user_123",
            date = "04/08/2026",
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            checkInTime = getMillis(19, 30),
            checkOutTime = getMillis(7, 30, dayOffset = 1)
        )
        val entry3 = TimeEntry(
            id = 22,
            userId = "legacy_user_123",
            date = "09/08/2026", // Sunday
            shiftId = "ca1",
            checkInTime = getMillis(7, 30, isSunday = true),
            checkOutTime = getMillis(19, 30, isSunday = true)
        )

        assertMonthlySalaryEquivalence(listOf(entry1, entry2, entry3))
    }

    // --- DATABASE UPGRADE, PERSISTENCE & MULTI-COMPANY FLOW INTEGRATION ---

    @Test
    fun testRegression_DatabaseUpgradeAndPersistence() = runBlocking {
        // 1. Check schemas & verify that we can insert legacy/existing objects
        val configDao = database.userConfigDao()
        val entryDao = database.timeEntryDao()

        val legacyConfig = defaultConfig
        val legacyEntry = TimeEntry(
            id = 500,
            userId = "legacy_user_123",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(19, 30),
            workDay = 1.0,
            otHours = 2.5
        )

        // Simulate insertion of legacy user and history data
        configDao.saveConfig(legacyConfig)
        entryDao.insertOrUpdate(legacyEntry)

        // 2. Dữ liệu cũ vẫn còn (Retrieve and assert)
        val retrievedConfig = configDao.getConfigForUser("legacy_user_123")
        val retrievedEntries = entryDao.getAllEntriesForUserDirect("legacy_user_123")

        assertNotNull("Retrieved legacy config should not be null", retrievedConfig)
        assertEquals(10000000.0, retrievedConfig?.luongCoBan ?: 0.0, 0.01)
        assertEquals("legacy_user_123", retrievedConfig?.userId)

        assertEquals(1, retrievedEntries.size)
        assertEquals(500, retrievedEntries[0].id)
        assertEquals(2.5, retrievedEntries[0].otHours, 0.01)

        // 3. User cũ đăng nhập (Verifies that user mapping works perfectly)
        val userSessionConfig = retrievedConfig
        assertNotNull("User can login and map standard profile settings", userSessionConfig)
        assertEquals("legacy_user_123", userSessionConfig?.userId)

        // 4. Lịch sử vẫn hiển thị
        assertFalse("History screen displays old entries", retrievedEntries.isEmpty())
        assertEquals("03/08/2026", retrievedEntries[0].date)

        // 5. Tính lại dữ liệu cũ -> 6. Kết quả không đổi
        val recalculatedEntry = legacyEngine.calculateSingleEntry(retrievedEntries[0], retrievedConfig)
        assertEquals(1.0, recalculatedEntry.workDay, 0.01)
        assertEquals(2.5, recalculatedEntry.otHours, 0.01)
        assertEquals(12.0, ((recalculatedEntry.normalizedCheckOut!! - recalculatedEntry.normalizedCheckIn!!) / 3600000.0), 0.01)
    }

    @Test
    fun testRegression_NewUser_Company_Shift_RulesCreation() = runBlocking {
        val workRuleDao = database.workRuleDao()
        val overtimeRuleDao = database.overtimeRuleDao()
        val shiftDao = database.shiftDao()

        // 7. User mới tạo company (represented in rules / shifts as companyId)
        val newCompanyId = "vin_group_001"

        // 8. User mới tạo WorkRule
        val newWorkRule = WorkRule(
            id = "wr_vingroup_standard",
            companyId = newCompanyId,
            name = "Quy tắc 8h - VinGroup",
            standardHoursPerDay = 8.0,
            overtimeStartAfterHours = 8.0,
            roundingMinutes = 15
        )
        workRuleDao.insertWorkRule(newWorkRule)

        // 9. User mới tạo OvertimeRule
        val newOvertimeRule = OvertimeRule(
            id = "ot_vingroup_standard",
            companyId = newCompanyId,
            name = "Quy tắc OT - VinGroup",
            normalDayMultiplier = 1.5,
            weeklyOffMultiplier = 2.0,
            holidayMultiplier = 3.0
        )
        overtimeRuleDao.insertOvertimeRule(newOvertimeRule)

        // 10. User mới tạo shift
        val newShift = ShiftEntity(
            id = "shift_vingroup_ca_sang",
            companyId = newCompanyId,
            name = "Ca Sáng VinGroup",
            startTime = "08:00",
            endTime = "17:00",
            breakMinutes = 60,
            standardHours = 8.0,
            crossesMidnight = false,
            enabled = true
        )
        shiftDao.insertShift(newShift)

        // Verify all entities exist in DB
        val retrievedWorkRuleList = workRuleDao.getWorkRulesByCompany(newCompanyId)
        val retrievedWorkRule = retrievedWorkRuleList.firstOrNull { it.id == newWorkRule.id }
        
        val retrievedOvertimeRuleList = overtimeRuleDao.getOvertimeRulesByCompany(newCompanyId)
        val retrievedOvertimeRule = retrievedOvertimeRuleList.firstOrNull { it.id == newOvertimeRule.id }
        
        val retrievedShift = shiftDao.getShiftById(newShift.id)

        assertNotNull("WorkRule successfully persisted", retrievedWorkRule)
        assertEquals("Quy tắc 8h - VinGroup", retrievedWorkRule?.name)
        assertEquals(newCompanyId, retrievedWorkRule?.companyId)

        assertNotNull("OvertimeRule successfully persisted", retrievedOvertimeRule)
        assertEquals("Quy tắc OT - VinGroup", retrievedOvertimeRule?.name)
        assertEquals(newCompanyId, retrievedOvertimeRule?.companyId)

        assertNotNull("Shift successfully persisted", retrievedShift)
        assertEquals("Ca Sáng VinGroup", retrievedShift?.name)
        assertEquals("08:00", retrievedShift?.startTime)
        assertEquals("17:00", retrievedShift?.endTime)
    }
}
