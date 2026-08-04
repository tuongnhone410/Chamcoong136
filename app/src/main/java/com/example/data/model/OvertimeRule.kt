package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "overtime_rules")
data class OvertimeRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String = "default_company",
    val name: String = "Quy tắc tăng ca tiêu chuẩn",
    val normalDayMultiplier: Double = 1.5,
    val weeklyOffMultiplier: Double = 2.0,
    val holidayMultiplier: Double = 3.0,
    val minimumOvertimeMinutes: Int = 30,
    val roundingMode: String = "NONE", // NONE, NEAREST, UP, DOWN
    val roundingMinutes: Int = 15,
    val enabled: Boolean = true,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun validateOvertimeRule(
            name: String,
            normalDayMultiplier: Double,
            weeklyOffMultiplier: Double,
            holidayMultiplier: Double,
            minimumOvertimeMinutes: Int,
            roundingMinutes: Int
        ): String? {
            if (name.trim().isEmpty()) return "Tên quy tắc OT không được để trống"
            if (normalDayMultiplier < 0.0 || normalDayMultiplier > 10.0) return "Hệ số ngày thường phải từ 0.0 đến 10.0"
            if (weeklyOffMultiplier < 0.0 || weeklyOffMultiplier > 10.0) return "Hệ số ngày nghỉ tuần phải từ 0.0 đến 10.0"
            if (holidayMultiplier < 0.0 || holidayMultiplier > 10.0) return "Hệ số ngày lễ phải từ 0.0 đến 10.0"
            if (minimumOvertimeMinutes < 0 || minimumOvertimeMinutes > 240) return "Số phút OT tối thiểu phải từ 0 đến 240 phút"
            if (roundingMinutes < 0 || roundingMinutes > 120) return "Số phút làm tròn phải từ 0 đến 120 phút"
            return null
        }

        fun createDefault(companyId: String, name: String = "Quy tắc tăng ca tiêu chuẩn"): OvertimeRule {
            val now = System.currentTimeMillis()
            return OvertimeRule(
                id = "ot_rule_${companyId}_v1_${now}",
                companyId = companyId,
                name = name,
                normalDayMultiplier = 1.5,
                weeklyOffMultiplier = 2.0,
                holidayMultiplier = 3.0,
                minimumOvertimeMinutes = 30,
                roundingMode = "NONE",
                roundingMinutes = 15,
                enabled = true,
                version = 1,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
