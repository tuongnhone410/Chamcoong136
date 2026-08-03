package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_shifts")
data class CompanyShift(
    @PrimaryKey val shift_code: String,
    val company_id: String = "DEFAULT",
    val shift_name: String,
    val start_time: String, // HH:mm
    val end_time: String,   // HH:mm
    val checkin_start_windowTime: String, // HH:mm
    val checkin_end_windowTime: String,   // HH:mm
    val ot_start_buffer_minutes: Int = 30,
    val is_active: Boolean = true
)
