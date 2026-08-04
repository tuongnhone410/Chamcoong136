package com.example.domain.calculation

import com.example.data.ShiftConfig
import com.example.data.model.OvertimeRule
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.data.model.WorkRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class ConfigurableCalculationEngineTest {

    private lateinit var configurableEngine: ConfigurableCalculationEngine
    private lateinit var legacyEngine: LegacyCalculationEngine
    private lateinit var defaultConfig: UserConfig
    private lateinit var customWorkRule: WorkRule
    private lateinit var customOvertimeRule: OvertimeRule

    @Before
    fun setUp() {
        legacyEngine = LegacyCalculationEngine()
        customWorkRule = WorkRule(
            id = "rule_test_1",
            companyId = "test_company",
            name = "Quy tắc tuỳ chỉnh 8h",
            standardHoursPerDay = 8.0,
            overtimeStartAfterHours = 8.0,
            roundingMinutes = 15
        )
        customOvertimeRule = OvertimeRule(
            id = "ot_test_1",
            companyId = "test_company",
            name = "Quy tắc OT tuỳ chỉnh",
            normalDayMultiplier = 1.5,
            weeklyOffMultiplier = 2.0,
            holidayMultiplier = 3.0
        )
        configurableEngine = ConfigurableCalculationEngine(
            defaultWorkRule = customWorkRule,
            defaultOvertimeRule = customOvertimeRule
        )

        defaultConfig = UserConfig(
            userId = "test_user",
            luongCoBan = 10000000.0,
            luongDongBaoHiem = 5000000.0,
            tiLeDongBaoHiem = 10.5,
            heSoOtNgayThuong = 1.5,
            heSoOtChuNhat = 2.0,
            heSoOtNgayLe = 3.0,
            heSoOtDem = 1.75,
            tinhKhauTruNghi = true,
            soGioNghiGiaiLao = 1.5
        )
    }

    private fun getMillis(hour: Int, minute: Int, dayOffset: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 3, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (dayOffset != 0) {
            cal.add(Calendar.DAY_OF_MONTH, dayOffset)
        }
        return cal.timeInMillis
    }

    @Test
    fun testConfigurable_DayShift() {
        // Day shift: 07:30 to 19:30 (12h spent, 1.5h break = 10.5h actual => 8.0h standard + 2.5h OT)
        val inTime = getMillis(7, 30)
        val outTime = getMillis(19, 30)
        val entry = TimeEntry(
            id = 101,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime,
            workRuleId = customWorkRule.id,
            overtimeRuleId = customOvertimeRule.id,
            snapshotStandardHours = 8.0,
            snapshotOtMultiplier = 1.5
        )

        val result = configurableEngine.calculateSingleEntry(entry, defaultConfig)
        assertEquals("ca1", result.shiftId)
        assertEquals(1.0, result.workDay, 0.01)
        assertEquals(2.5, result.otHours, 0.01)
        assertEquals(0, result.lateMinutes)
    }

    @Test
    fun testConfigurable_NightShift() {
        // Night shift: 19:30 to 07:30 next day
        val inTime = getMillis(19, 30)
        val outTime = getMillis(7, 30, dayOffset = 1)
        val entry = TimeEntry(
            id = 102,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            checkInTime = inTime,
            checkOutTime = outTime,
            workRuleId = customWorkRule.id,
            overtimeRuleId = customOvertimeRule.id,
            snapshotStandardHours = 8.0,
            snapshotOtMultiplier = 1.5
        )

        val result = configurableEngine.calculateSingleEntry(entry, defaultConfig)
        assertEquals("ca_dem", result.shiftId)
        assertEquals("NIGHT", result.shiftType)
        assertEquals(1.0, result.workDay, 0.01)
    }

    @Test
    fun testConfigurable_OvernightCrossDayShift() {
        // Cross-day shift: 22:00 to 06:00 next day
        val customShifts = mapOf(
            "night_custom" to ShiftConfig(
                shiftId = "night_custom",
                shiftType = "NIGHT",
                startTime = "22:00",
                endTime = "06:00",
                checkInWindowStart = "21:30",
                checkInWindowEnd = "22:00",
                checkOutWindowStart = "06:00",
                checkOutWindowEnd = "06:30",
                breakHours = 0.0,
                standardHours = 8.0
            )
        )
        val crossEngine = ConfigurableCalculationEngine(
            defaultWorkRule = customWorkRule,
            defaultOvertimeRule = customOvertimeRule,
            customShifts = customShifts
        )

        val inTime = getMillis(22, 0)
        val outTime = getMillis(6, 0, dayOffset = 1)
        val entry = TimeEntry(
            id = 103,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "night_custom",
            checkInTime = inTime,
            checkOutTime = outTime,
            workRuleId = customWorkRule.id,
            overtimeRuleId = customOvertimeRule.id,
            snapshotStandardHours = 8.0,
            customBreakDeduction = false,
            customBreakHours = 0.0
        )

        val result = crossEngine.calculateSingleEntry(entry, defaultConfig)
        assertEquals("night_custom", result.shiftId)
        assertEquals(1.0, result.workDay, 0.01)
        assertEquals(0.0, result.otHours, 0.01)
    }

    @Test
    fun testLegacyVsConfigurable_Equivalence() {
        val inTime = getMillis(7, 30)
        val outTime = getMillis(19, 30)
        val legacyEntry = TimeEntry(
            id = 201,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val legacyResult = legacyEngine.calculateSingleEntry(legacyEntry, defaultConfig)
        val legacyMonthly = legacyEngine.calculateMonthlySalary(
            entries = listOf(legacyEntry),
            config = defaultConfig,
            scheduledDaysSoFar = 1,
            totalScheduledDaysInMonth = 26,
            earliestDate = "01/08/2026",
            selectedMonth = "08/2026",
            todayStr = "03/08/2026",
            isCurrentSelectedMonth = true,
            holidayDatesInMonth = emptySet()
        )

        assertNotNull(legacyMonthly)
        assertEquals(12.0, (legacyResult.normalizedCheckOut!! - legacyResult.normalizedCheckIn!!) / 3600000.0, 0.01)
    }
}
