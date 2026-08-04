package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "work_rules")
data class WorkRule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val companyId: String = "default_company",
    val name: String = "Quy tắc giờ công tiêu chuẩn",
    val standardHoursPerDay: Double = 8.0,
    val breakCalculationMode: String = "DEDUCT_BREAK_TIME", // DEDUCT_BREAK_TIME, FIXED, NO_DEDUCTION
    val overtimeEnabled: Boolean = true,
    val overtimeStartAfterHours: Double = 8.0,
    val roundingMode: String = "NONE", // NONE, NEAREST, UP, DOWN
    val roundingMinutes: Int = 15,
    val lateToleranceMinutes: Int = 5,
    val earlyLeaveToleranceMinutes: Int = 5,
    val enabled: Boolean = true,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun validateWorkRule(
            name: String,
            standardHoursPerDay: Double,
            overtimeStartAfterHours: Double,
            roundingMinutes: Int,
            lateToleranceMinutes: Int,
            earlyLeaveToleranceMinutes: Int
        ): String? {
            if (name.trim().isEmpty()) return "Tên quy tắc không được để trống"
            if (standardHoursPerDay <= 0.0 || standardHoursPerDay > 24.0) return "Giờ công tiêu chuẩn phải > 0 và <= 24 giờ"
            if (overtimeStartAfterHours < 0.0 || overtimeStartAfterHours > 24.0) return "Giờ bắt đầu tính OT phải từ 0 đến 24 giờ"
            if (roundingMinutes < 0 || roundingMinutes > 120) return "Số phút làm tròn phải từ 0 đến 120 phút"
            if (lateToleranceMinutes < 0 || lateToleranceMinutes > 120) return "Thời gian cho phép đi trễ phải >= 0 phút"
            if (earlyLeaveToleranceMinutes < 0 || earlyLeaveToleranceMinutes > 120) return "Thời gian cho phép về sớm phải >= 0 phút"
            return null
        }

        fun createDefault(companyId: String, name: String = "Quy tắc giờ công tiêu chuẩn"): WorkRule {
            val now = System.currentTimeMillis()
            return WorkRule(
                id = "rule_${companyId}_v1_${now}",
                companyId = companyId,
                name = name,
                standardHoursPerDay = 8.0,
                breakCalculationMode = "DEDUCT_BREAK_TIME",
                overtimeEnabled = true,
                overtimeStartAfterHours = 8.0,
                roundingMode = "NONE",
                roundingMinutes = 15,
                lateToleranceMinutes = 5,
                earlyLeaveToleranceMinutes = 5,
                enabled = true,
                version = 1,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
