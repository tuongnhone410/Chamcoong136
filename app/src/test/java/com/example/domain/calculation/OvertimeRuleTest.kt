package com.example.domain.calculation

import com.example.data.model.OvertimeRule
import org.junit.Assert.*
import org.junit.Test

class OvertimeRuleTest {

    @Test
    fun testOvertimeRuleValidation() {
        // Valid OvertimeRule
        assertNull(OvertimeRule.validateOvertimeRule("OT Chuẩn", 1.5, 2.0, 3.0, 30, 15))

        // Invalid Name
        assertNotNull(OvertimeRule.validateOvertimeRule("", 1.5, 2.0, 3.0, 30, 15))

        // Invalid Multiplier (< 0 or > 10)
        assertNotNull(OvertimeRule.validateOvertimeRule("Test", -0.5, 2.0, 3.0, 30, 15))
        assertNotNull(OvertimeRule.validateOvertimeRule("Test", 1.5, 12.0, 3.0, 30, 15))

        // Invalid Minimum Minutes
        assertNotNull(OvertimeRule.validateOvertimeRule("Test", 1.5, 2.0, 3.0, 300, 15))
    }

    @Test
    fun testOvertimeRuleVersioning() {
        val ruleV1 = OvertimeRule.createDefault("company_A", "OT v1")
        assertEquals(1, ruleV1.version)
        assertEquals(1.5, ruleV1.normalDayMultiplier, 0.01)
        assertEquals(2.0, ruleV1.weeklyOffMultiplier, 0.01)
        assertEquals(3.0, ruleV1.holidayMultiplier, 0.01)

        val ruleV2 = ruleV1.copy(
            id = "ot_company_A_v2",
            version = 2,
            normalDayMultiplier = 2.0,
            weeklyOffMultiplier = 3.0,
            holidayMultiplier = 4.0,
            name = "OT v2 Custom"
        )
        assertEquals(2, ruleV2.version)
        assertEquals(2.0, ruleV2.normalDayMultiplier, 0.01)
        assertEquals(3.0, ruleV2.weeklyOffMultiplier, 0.01)
        assertEquals(4.0, ruleV2.holidayMultiplier, 0.01)
        assertNotEquals(ruleV1.version, ruleV2.version)
    }
}
