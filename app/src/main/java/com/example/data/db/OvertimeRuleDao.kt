package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OvertimeRule
import kotlinx.coroutines.flow.Flow

@Dao
interface OvertimeRuleDao {
    @Query("SELECT * FROM overtime_rules WHERE companyId = :companyId ORDER BY version DESC")
    fun getOvertimeRulesByCompanyFlow(companyId: String = "default_company"): Flow<List<OvertimeRule>>

    @Query("SELECT * FROM overtime_rules WHERE companyId = :companyId ORDER BY version DESC")
    suspend fun getOvertimeRulesByCompany(companyId: String = "default_company"): List<OvertimeRule>

    @Query("SELECT * FROM overtime_rules WHERE companyId = :companyId AND enabled = 1 ORDER BY version DESC LIMIT 1")
    suspend fun getActiveOvertimeRule(companyId: String = "default_company"): OvertimeRule?

    @Query("SELECT * FROM overtime_rules WHERE companyId = :companyId AND version = :version LIMIT 1")
    suspend fun getOvertimeRuleByVersion(companyId: String = "default_company", version: Int): OvertimeRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOvertimeRule(rule: OvertimeRule)

    @Update
    suspend fun updateOvertimeRule(rule: OvertimeRule)

    @Query("DELETE FROM overtime_rules WHERE id = :id")
    suspend fun deleteOvertimeRuleById(id: String)
}
