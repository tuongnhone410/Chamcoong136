package com.example.domain.calculation

import com.example.data.model.ShiftEntity
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import org.junit.Assert.*
import org.junit.Test

class ShiftConfigurationTest {

    @Test
    fun testValidTimeFormat_HHmm() {
        // Valid HH:mm formats
        assertTrue(ShiftEntity.isValidTimeFormat("07:30"))
        assertTrue(ShiftEntity.isValidTimeFormat("08:00"))
        assertTrue(ShiftEntity.isValidTimeFormat("19:30"))
        assertTrue(ShiftEntity.isValidTimeFormat("22:00"))
        assertTrue(ShiftEntity.isValidTimeFormat("00:00"))
        assertTrue(ShiftEntity.isValidTimeFormat("23:59"))

        // Invalid formats rejected according to Step 5 rules
        assertFalse(ShiftEntity.isValidTimeFormat("7:3"))
        assertFalse(ShiftEntity.isValidTimeFormat("7h30"))
        assertFalse(ShiftEntity.isValidTimeFormat("730"))
        assertFalse(ShiftEntity.isValidTimeFormat("24:00"))
        assertFalse(ShiftEntity.isValidTimeFormat("12:60"))
        assertFalse(ShiftEntity.isValidTimeFormat("abc"))
        assertFalse(ShiftEntity.isValidTimeFormat(""))
    }

    @Test
    fun testShiftValidation() {
        // Valid Shift
        assertNull(ShiftEntity.validateShift("Ca Sáng", "07:30", "16:30", 60, 8.0))

        // Invalid Name
        assertNotNull(ShiftEntity.validateShift("", "07:30", "16:30", 60, 8.0))

        // Invalid Start Time
        assertNotNull(ShiftEntity.validateShift("Ca 1", "7:30", "16:30", 60, 8.0))

        // Invalid End Time
        assertNotNull(ShiftEntity.validateShift("Ca 1", "07:30", "16:300", 60, 8.0))

        // Negative break minutes
        assertNotNull(ShiftEntity.validateShift("Ca 1", "07:30", "16:30", -10, 8.0))

        // Negative standard hours
        assertNotNull(ShiftEntity.validateShift("Ca 1", "07:30", "16:30", 60, -1.0))
    }

    @Test
    fun testOvernightShift_DurationCalculation() {
        // Ca sáng: 07:30 -> 16:30, 60m break => 8.0 hours
        val hrsSang = ShiftEntity.calculateDurationHours("07:30", "16:30", false, 60)
        assertEquals(8.0, hrsSang, 0.01)

        // Ca hành chính: 08:00 -> 17:00, 60m break => 8.0 hours
        val hrsHanhChinh = ShiftEntity.calculateDurationHours("08:00", "17:00", false, 60)
        assertEquals(8.0, hrsHanhChinh, 0.01)

        // Ca chiều: 13:00 -> 22:00, 60m break => 8.0 hours
        val hrsChieu = ShiftEntity.calculateDurationHours("13:00", "22:00", false, 60)
        assertEquals(8.0, hrsChieu, 0.01)

        // Ca đêm: 20:00 -> 08:00 (hôm sau), 0m break => 12.0 hours
        val isOvernightDem = ShiftEntity.isOvernight("20:00", "08:00")
        assertTrue("20:00 to 08:00 must be auto-detected as overnight", isOvernightDem)

        val hrsDem = ShiftEntity.calculateDurationHours("20:00", "08:00", crossesMidnight = true, breakMinutes = 0)
        assertEquals(12.0, hrsDem, 0.01)

        // Ca 12 tiếng: 07:00 -> 19:00, 60m break => 11.0 hours
        val hrs12h = ShiftEntity.calculateDurationHours("07:00", "19:00", false, 60)
        assertEquals(11.0, hrs12h, 0.01)
    }

    @Test
    fun testLegacyCalculationEngine_RemainsUnaffected() {
        val legacyEngine = LegacyCalculationEngine()
        val config = UserConfig(
            userId = "legacy_user",
            luongCoBan = 12000000.0,
            luongDongBaoHiem = 6000000.0,
            soGioNghiGiaiLao = 1.5
        )

        val calIn = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.AUGUST, 3, 7, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val calOut = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.AUGUST, 3, 19, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val entry = TimeEntry(
            id = 99,
            userId = "legacy_user",
            date = "03/08/2026",
            shiftId = "ca1",
            checkInTime = calIn.timeInMillis,
            checkOutTime = calOut.timeInMillis
        )

        val result = legacyEngine.calculateSingleEntry(entry, config)

        // Verifies legacy engine calculations are preserved without changes
        assertNotNull(result)
        assertEquals("ca1", result.shiftId)
        assertEquals(1.0, result.workDay, 0.01)
    }
}
