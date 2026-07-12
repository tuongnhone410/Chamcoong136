package com.example.data.db

import androidx.room.*
import com.example.data.model.TimeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeEntryDao {
    @Query("SELECT * FROM time_entries WHERE userId = :userId ORDER BY date ASC")
    fun getAllTimeEntries(userId: String): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun getTimeEntriesByMonth(userId: String, monthPrefix: String): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getTimeEntryByDate(userId: String, date: String): TimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeEntry(entry: TimeEntry): Long

    @Query("DELETE FROM time_entries WHERE id = :id")
    suspend fun deleteTimeEntryById(id: Int)

    @Query("DELETE FROM time_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteTimeEntryByDate(userId: String, date: String)
}
