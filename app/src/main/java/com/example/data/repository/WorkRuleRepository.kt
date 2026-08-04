package com.example.data.repository

import com.example.data.db.WorkRuleDao
import com.example.data.model.WorkRule
import kotlinx.coroutines.flow.Flow

class WorkRuleRepository(private val workRuleDao: WorkRuleDao) {
    fun getWorkRules(companyId: String = "default_company"): Flow<List<WorkRule>> {
        return workRuleDao.getWorkRulesByCompanyFlow(companyId)
    }

    suspend fun getWorkRulesList(companyId: String = "default_company"): List<WorkRule> {
        return workRuleDao.getWorkRulesByCompany(companyId)
    }

    suspend fun getActiveWorkRule(companyId: String = "default_company"): WorkRule? {
        val active = workRuleDao.getActiveWorkRule(companyId)
        if (active != null) return active

        // If none exists, create default version 1
        val defaultRule = WorkRule.createDefault(companyId)
        workRuleDao.insertWorkRule(defaultRule)
        return defaultRule
    }

    suspend fun saveWorkRule(rule: WorkRule) {
        workRuleDao.insertWorkRule(rule)
    }

    suspend fun deleteWorkRule(id: String) {
        workRuleDao.deleteWorkRuleById(id)
    }
}
