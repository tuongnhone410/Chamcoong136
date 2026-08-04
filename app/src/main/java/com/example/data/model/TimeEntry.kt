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
    val note: String? = null,
    val shiftId: String? = null,
    val shiftType: String? = null,
    val rawCheckIn: Long? = null,
    val rawCheckOut: Long? = null,
    val normalizedCheckIn: Long? = null,
    val normalizedCheckOut: Long? = null,
    val workDay: Double = 0.0,
    val otHours: Double = 0.0,
    val lateMinutes: Int = 0,
    val earlyLeaveMinutes: Int = 0,
    val customBreakDeduction: Boolean? = null,
    val customBreakHours: Double? = null,
    val workRuleId: String? = null,
    val workRuleVersion: Int? = null,
    val overtimeRuleId: String? = null,
    val overtimeRuleVersion: Int? = null,
    val snapshotStandardHours: Double? = null,
    val snapshotOtMultiplier: Double? = null,
    val snapshotRoundingMinutes: Int? = null
)
