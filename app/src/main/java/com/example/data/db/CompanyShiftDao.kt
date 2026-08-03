package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CompanyShift
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyShiftDao {
    @Query("SELECT * FROM company_shifts WHERE company_id = :companyId ORDER BY shift_code ASC")
    fun getShiftsByCompany(companyId: String): Flow<List<CompanyShift>>

    @Query("SELECT * FROM company_shifts WHERE company_id = :companyId AND is_active = 1")
    suspend fun getActiveShiftsByCompany(companyId: String): List<CompanyShift>

    @Query("SELECT * FROM company_shifts WHERE shift_code = :shiftCode LIMIT 1")
    suspend fun getShiftByCode(shiftCode: String): CompanyShift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateShift(shift: CompanyShift)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<CompanyShift>)

    @Query("DELETE FROM company_shifts WHERE shift_code = :shiftCode")
    suspend fun deleteShiftByCode(shiftCode: String)
}
