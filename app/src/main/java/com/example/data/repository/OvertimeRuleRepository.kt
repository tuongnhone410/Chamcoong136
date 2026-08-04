package com.example.data.repository

import com.example.data.db.OvertimeRuleDao
import com.example.data.model.OvertimeRule
import kotlinx.coroutines.flow.Flow

class OvertimeRuleRepository(private val overtimeRuleDao: OvertimeRuleDao) {
    fun getOvertimeRules(companyId: String = "default_company"): Flow<List<OvertimeRule>> {
        return overtimeRuleDao.getOvertimeRulesByCompanyFlow(companyId)
    }

    suspend fun getOvertimeRulesList(companyId: String = "default_company"): List<OvertimeRule> {
        return overtimeRuleDao.getOvertimeRulesByCompany(companyId)
    }

    suspend fun getActiveOvertimeRule(companyId: String = "default_company"): OvertimeRule? {
        val active = overtimeRuleDao.getActiveOvertimeRule(companyId)
        if (active != null) return active

        val defaultRule = OvertimeRule.createDefault(companyId)
        overtimeRuleDao.insertOvertimeRule(defaultRule)
        return defaultRule
    }

    suspend fun saveOvertimeRule(rule: OvertimeRule) {
        overtimeRuleDao.insertOvertimeRule(rule)
    }

    suspend fun deleteOvertimeRule(id: String) {
        overtimeRuleDao.deleteOvertimeRuleById(id)
    }
}
