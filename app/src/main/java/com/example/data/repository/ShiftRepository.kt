package com.example.data.repository

import com.example.data.db.ShiftDao
import com.example.data.model.ShiftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ShiftRepository(private val shiftDao: ShiftDao) {

    fun getShiftsFlow(companyId: String = "default_company"): Flow<List<ShiftEntity>> {
        return shiftDao.getShiftsByCompanyFlow(companyId)
    }

    suspend fun getShifts(companyId: String = "default_company"): List<ShiftEntity> = withContext(Dispatchers.IO) {
        val list = shiftDao.getShiftsByCompany(companyId)
        if (list.isEmpty()) {
            seedDefaultShifts(companyId)
            shiftDao.getShiftsByCompany(companyId)
        } else {
            list
        }
    }

    suspend fun getEnabledShifts(companyId: String = "default_company"): List<ShiftEntity> = withContext(Dispatchers.IO) {
        val list = shiftDao.getEnabledShifts(companyId)
        if (list.isEmpty()) {
            seedDefaultShifts(companyId)
            shiftDao.getEnabledShifts(companyId)
        } else {
            list
        }
    }

    suspend fun getShiftById(id: String): ShiftEntity? = withContext(Dispatchers.IO) {
        shiftDao.getShiftById(id)
    }

    suspend fun saveShift(shift: ShiftEntity) = withContext(Dispatchers.IO) {
        val validationError = ShiftEntity.validateShift(
            name = shift.name,
            startTime = shift.startTime,
            endTime = shift.endTime,
            breakMinutes = shift.breakMinutes,
            standardHours = shift.standardHours
        )
        if (validationError != null) {
            throw IllegalArgumentException(validationError)
        }
        val isOvernight = ShiftEntity.isOvernight(shift.startTime, shift.endTime)
        val shiftToSave = shift.copy(
            crossesMidnight = isOvernight || shift.crossesMidnight
        )
        shiftDao.insertShift(shiftToSave)
    }

    suspend fun deleteShift(id: String) = withContext(Dispatchers.IO) {
        shiftDao.deleteShiftById(id)
    }

    suspend fun seedDefaultShifts(companyId: String = "default_company") = withContext(Dispatchers.IO) {
        val defaults = listOf(
            ShiftEntity(
                id = "shift_sang_$companyId",
                companyId = companyId,
                name = "Ca sáng",
                startTime = "07:30",
                endTime = "16:30",
                breakMinutes = 60,
                standardHours = 8.0,
                crossesMidnight = false,
                enabled = true
            ),
            ShiftEntity(
                id = "shift_hanh_chinh_$companyId",
                companyId = companyId,
                name = "Ca hành chính",
                startTime = "08:00",
                endTime = "17:00",
                breakMinutes = 60,
                standardHours = 8.0,
                crossesMidnight = false,
                enabled = true
            ),
            ShiftEntity(
                id = "shift_chieu_$companyId",
                companyId = companyId,
                name = "Ca chiều",
                startTime = "13:00",
                endTime = "22:00",
                breakMinutes = 60,
                standardHours = 8.0,
                crossesMidnight = false,
                enabled = true
            ),
            ShiftEntity(
                id = "shift_dem_$companyId",
                companyId = companyId,
                name = "Ca đêm",
                startTime = "20:00",
                endTime = "08:00",
                breakMinutes = 0,
                standardHours = 8.0,
                crossesMidnight = true,
                enabled = true
            ),
            ShiftEntity(
                id = "shift_12h_$companyId",
                companyId = companyId,
                name = "Ca 12 tiếng",
                startTime = "07:00",
                endTime = "19:00",
                breakMinutes = 60,
                standardHours = 8.0,
                crossesMidnight = false,
                enabled = true
            )
        )
        shiftDao.insertShifts(defaults)
    }
}
