package com.example.data.repository

import com.example.data.db.TimeEntryDao
import com.example.data.db.UserConfigDao
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import kotlinx.coroutines.flow.Flow

class TimeRepository(
    private val timeEntryDao: TimeEntryDao,
    private val userConfigDao: UserConfigDao
) {
    fun getAllTimeEntries(userId: String): Flow<List<TimeEntry>> {
        return timeEntryDao.getAllTimeEntries(userId)
    }

    fun getTimeEntriesByMonth(userId: String, monthPrefix: String): Flow<List<TimeEntry>> {
        return timeEntryDao.getTimeEntriesByMonth(userId, monthPrefix)
    }

    suspend fun getTimeEntryByDate(userId: String, date: String): TimeEntry? {
        return timeEntryDao.getTimeEntryByDate(userId, date)
    }

    suspend fun insertTimeEntry(entry: TimeEntry): Long {
        return timeEntryDao.insertTimeEntry(entry)
    }

    suspend fun deleteTimeEntryById(id: Int) {
        timeEntryDao.deleteTimeEntryById(id)
    }

    suspend fun deleteTimeEntryByDate(userId: String, date: String) {
        timeEntryDao.deleteTimeEntryByDate(userId, date)
    }

    fun getUserConfig(userId: String): Flow<UserConfig?> {
        return userConfigDao.getUserConfig(userId)
    }

    suspend fun getUserConfigOnce(userId: String): UserConfig? {
        return userConfigDao.getUserConfigOnce(userId)
    }

    suspend fun saveUserConfig(config: UserConfig) {
        userConfigDao.insertUserConfig(config)
    }
}
