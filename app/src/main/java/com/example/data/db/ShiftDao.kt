package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE companyId = :companyId ORDER BY startTime ASC")
    fun getShiftsByCompanyFlow(companyId: String = "default_company"): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE companyId = :companyId ORDER BY startTime ASC")
    suspend fun getShiftsByCompany(companyId: String = "default_company"): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE companyId = :companyId AND enabled = 1 ORDER BY startTime ASC")
    suspend fun getEnabledShifts(companyId: String = "default_company"): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<ShiftEntity>)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Delete
    suspend fun deleteShift(shift: ShiftEntity)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteShiftById(id: String)
}
