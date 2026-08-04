package com.example.domain.calculation

import com.example.data.SalaryCalculator
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class LegacyCalculationEngineTest {

    private lateinit var engine: LegacyCalculationEngine
    private lateinit var defaultConfig: UserConfig

    @Before
    fun setUp() {
        engine = LegacyCalculationEngine()
        defaultConfig = UserConfig(
            userId = "test_user",
            luongCoBan = 10000000.0,
            luongDongBaoHiem = 5000000.0,
            tiLeDongBaoHiem = 10.5,
            heSoOtNgayThuong = 1.5,
            heSoOtChuNhat = 2.0,
            heSoOtNgayLe = 3.0,
            heSoOtDem = 1.75,
            pcComCa = 50000.0,
            pcComOt = 30000.0,
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
    fun testDayShift_Normal8Hours() {
        // Standard Day shift (ca1): 07:30 to 19:30
        // Total 12.0h spent - 1.5h break = 10.5h actual worked => 8.0h standard + 2.5h OT
        val inTime = getMillis(7, 30)
        val outTime = getMillis(19, 30)
        val entry = TimeEntry(
            id = 1,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = engine.calculateSingleEntry(entry, defaultConfig)

        assertEquals("ca1", result.shiftId)
        assertEquals(1.0, result.workDay, 0.01)
        assertEquals(2.5, result.otHours, 0.01)
        assertEquals(0, result.lateMinutes)
        assertEquals(0, result.earlyLeaveMinutes)

        // Equivalence test
        val legacyResult = SalaryCalculator.calculateSingleEntry(entry, defaultConfig)
        assertEquals(result, legacyResult)
    }

    @Test
    fun testDayShift_WithOT() {
        // Standard Day shift ca1 extended to 21:30
        // Total 14.0h spent - 1.5h break = 12.5h actual worked => 8.0h standard + 4.5h OT
        val inTime = getMillis(7, 30)
        val outTime = getMillis(21, 30)
        val entry = TimeEntry(
            id = 2,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = engine.calculateSingleEntry(entry, defaultConfig)

        assertEquals("ca1", result.shiftId)
        assertEquals(1.0, result.workDay, 0.01)
        assertEquals(4.5, result.otHours, 0.01)
    }

    @Test
    fun testNightShift_Overnight() {
        // Night shift (ca_dem): 19:30 to 07:30 (next day)
        val inTime = getMillis(19, 30)
        val outTime = getMillis(7, 30, dayOffset = 1)
        val entry = TimeEntry(
            id = 3,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca_dem",
            shiftType = "NIGHT",
            checkInTime = inTime,
            checkOutTime = outTime
        )

        val result = engine.calculateSingleEntry(entry, defaultConfig)

        assertEquals("ca_dem", result.shiftId)
        assertEquals("NIGHT", result.shiftType)
        assertEquals(1.0, result.workDay, 0.01)

        // Equivalence test
        val legacyResult = SalaryCalculator.calculateSingleEntry(entry, defaultConfig)
        assertEquals(result, legacyResult)
    }

    @Test
    fun testBreakHoursDeduction() {
        // Shift ca2 with 1.5h break deduction
        val inTime = getMillis(7, 30)
        val outTime = getMillis(20, 0)
        val entry = TimeEntry(
            id = 4,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca2",
            checkInTime = inTime,
            checkOutTime = outTime,
            customBreakDeduction = true,
            customBreakHours = 1.5
        )

        val result = engine.calculateSingleEntry(entry, defaultConfig)

        assertEquals("ca2", result.shiftId)
        assertEquals(1.5, result.customBreakHours!!, 0.01)
        assertEquals(1.0, result.workDay, 0.01)
    }

    @Test
    fun testMealAllowance_ComCaAndComOt() {
        // Test allowance calculation for meal
        val comCa = engine.calculateAllowanceValue(
            fieldName = "pcComCa",
            allowanceValue = 50000.0,
            calcType = "PER_WORK_DAY",
            totalWorkDays = 20.0,
            comCaCount = 20,
            comOtCount = 5,
            nightShiftsCount = 0,
            scheduledDaysSoFar = 20,
            totalScheduledDaysInMonth = 26
        )

        val comOt = engine.calculateAllowanceValue(
            fieldName = "pcComOt",
            allowanceValue = 30000.0,
            calcType = "OT_MEAL_GE_1H",
            totalWorkDays = 20.0,
            comCaCount = 20,
            comOtCount = 5,
            nightShiftsCount = 0,
            scheduledDaysSoFar = 20,
            totalScheduledDaysInMonth = 26
        )

        assertEquals(1000000.0, comCa, 0.01) // 20 * 50,000
        assertEquals(150000.0, comOt, 0.01)   // 5 * 30,000
    }

    @Test
    fun testSundayAndHolidayDetection() {
        // 02/09/2026 is National Day in Vietnam
        assertTrue(engine.isHoliday("02/09/2026"))
        assertEquals("NGÀY LỄ", engine.getDayTypeLabel("02/09/2026"))

        // 09/08/2026 is Sunday
        assertTrue(engine.isSunday("09/08/2026"))
        assertEquals("CHỦ NHẬT", engine.getDayTypeLabel("09/08/2026"))
    }

    @Test
    fun testMonthlySalary_Equivalence() {
        val entry1 = TimeEntry(
            id = 10,
            userId = "test_user",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = getMillis(7, 30),
            checkOutTime = getMillis(19, 30)
        )

        val entries = listOf(entry1)
        val engineSummary = engine.calculateMonthlySalary(
            entries = entries,
            config = defaultConfig,
            scheduledDaysSoFar = 1,
            totalScheduledDaysInMonth = 26,
            earliestDate = "01/08/2026",
            selectedMonth = "08/2026",
            todayStr = "03/08/2026",
            isCurrentSelectedMonth = true,
            holidayDatesInMonth = emptySet()
        )

        val legacySummary = SalaryCalculator.calculateMonthlySalary(
            entries = entries,
            config = defaultConfig,
            scheduledDaysSoFar = 1,
            totalScheduledDaysInMonth = 26,
            earliestDate = "01/08/2026",
            selectedMonth = "08/2026",
            todayStr = "03/08/2026",
            isCurrentSelectedMonth = true,
            holidayDatesInMonth = emptySet()
        )

        assertNotNull(engineSummary)
        assertEquals(engineSummary.luongThucNhan, legacySummary.luongThucNhan, 0.01)
        assertEquals(engineSummary.workingDays, legacySummary.workingDays)
        assertEquals(engineSummary.phuCap, legacySummary.phuCap, 0.01)
    }
}
