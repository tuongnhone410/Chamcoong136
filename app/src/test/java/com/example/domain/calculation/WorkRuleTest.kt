package com.example.domain.calculation

import com.example.data.model.WorkRule
import org.junit.Assert.*
import org.junit.Test

class WorkRuleTest {

    @Test
    fun testWorkRuleValidation() {
        // Valid WorkRule
        assertNull(WorkRule.validateWorkRule("Quy tắc chuẩn", 8.0, 8.0, 15, 5, 5))

        // Invalid Name
        assertNotNull(WorkRule.validateWorkRule("", 8.0, 8.0, 15, 5, 5))

        // Invalid Standard Hours (< 0 or > 24)
        assertNotNull(WorkRule.validateWorkRule("Test", 0.0, 8.0, 15, 5, 5))
        assertNotNull(WorkRule.validateWorkRule("Test", 25.0, 8.0, 15, 5, 5))

        // Invalid OT hours
        assertNotNull(WorkRule.validateWorkRule("Test", 8.0, -1.0, 15, 5, 5))

        // Invalid Rounding minutes
        assertNotNull(WorkRule.validateWorkRule("Test", 8.0, 8.0, 200, 5, 5))
    }

    @Test
    fun testWorkRuleVersioning() {
        val ruleV1 = WorkRule.createDefault("company_A", "Rule v1")
        assertEquals(1, ruleV1.version)
        assertEquals("company_A", ruleV1.companyId)
        assertEquals(8.0, ruleV1.standardHoursPerDay, 0.01)

        val ruleV2 = ruleV1.copy(
            id = "rule_company_A_v2",
            version = 2,
            standardHoursPerDay = 7.5,
            name = "Rule v2 7.5h"
        )
        assertEquals(2, ruleV2.version)
        assertEquals(7.5, ruleV2.standardHoursPerDay, 0.01)
        // Verifies version isolation
        assertNotEquals(ruleV1.version, ruleV2.version)
    }
}
