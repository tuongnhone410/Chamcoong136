package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkRule
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkRuleDao {
    @Query("SELECT * FROM work_rules WHERE companyId = :companyId ORDER BY version DESC")
    fun getWorkRulesByCompanyFlow(companyId: String = "default_company"): Flow<List<WorkRule>>

    @Query("SELECT * FROM work_rules WHERE companyId = :companyId ORDER BY version DESC")
    suspend fun getWorkRulesByCompany(companyId: String = "default_company"): List<WorkRule>

    @Query("SELECT * FROM work_rules WHERE companyId = :companyId AND enabled = 1 ORDER BY version DESC LIMIT 1")
    suspend fun getActiveWorkRule(companyId: String = "default_company"): WorkRule?

    @Query("SELECT * FROM work_rules WHERE companyId = :companyId AND version = :version LIMIT 1")
    suspend fun getWorkRuleByVersion(companyId: String = "default_company", version: Int): WorkRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkRule(rule: WorkRule)

    @Update
    suspend fun updateWorkRule(rule: WorkRule)

    @Delete
    suspend fun deleteWorkRule(rule: WorkRule)

    @Query("DELETE FROM work_rules WHERE id = :id")
    suspend fun deleteWorkRuleById(id: String)
}
