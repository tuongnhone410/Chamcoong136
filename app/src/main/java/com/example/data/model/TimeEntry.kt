package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val date: String, // format "yyyy-MM-dd"
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val isWorking: Boolean = false,
    val dayType: String = "NORMAL", // "NORMAL", "SUNDAY", "HOLIDAY", "PAID_LEAVE", "UNPAID_LEAVE"
    val isHourlyCalculated: Boolean = true,
    val note: String? = null
)
