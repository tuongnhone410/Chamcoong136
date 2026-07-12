package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val date: String, // yyyy-MM-dd
    val checkInTime: Long? = null, // millis
    val checkOutTime: Long? = null, // millis
    val isWorking: Boolean = true,
    val dayType: String = "NORMAL", // NORMAL, PAID_LEAVE, UNPAID_LEAVE, HOLIDAY, SUNDAY
    val isHourlyCalculated: Boolean = false,
    val note: String? = null
)
