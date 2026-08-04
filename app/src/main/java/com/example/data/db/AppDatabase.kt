package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ShiftEntity
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig
import com.example.data.model.WorkRule

@Database(entities = [TimeEntry::class, UserConfig::class, ShiftEntity::class, WorkRule::class, com.example.data.model.OvertimeRule::class], version = 13, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun userConfigDao(): UserConfigDao
    abstract fun shiftDao(): ShiftDao
    abstract fun workRuleDao(): WorkRuleDao
    abstract fun overtimeRuleDao(): OvertimeRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_config` (`userId` TEXT NOT NULL, `luongCoBan` REAL NOT NULL DEFAULT 0.0, `luongDongBaoHiem` REAL NOT NULL DEFAULT 0.0, `tiLeDongBaoHiem` REAL NOT NULL DEFAULT 10.5, `ngayChotLuong` INTEGER NOT NULL DEFAULT 1, `doanPhiCongDoan` REAL NOT NULL DEFAULT 0.0, `heSoOtNgayThuong` REAL NOT NULL DEFAULT 1.5, `heSoOtChuNhat` REAL NOT NULL DEFAULT 2.0, `heSoOtNgayLe` REAL NOT NULL DEFAULT 3.0, `tienChuyenCanGoc` REAL NOT NULL DEFAULT 0.0, `soNgayPhepNam` INTEGER NOT NULL DEFAULT 0, `phepNamConLai` INTEGER NOT NULL DEFAULT 0, `lastAccumulatedMonth` TEXT NOT NULL DEFAULT '', `pcKyThuat` REAL NOT NULL DEFAULT 0.0, `pcTrachNhiem` REAL NOT NULL DEFAULT 0.0, `pcChucVu` REAL NOT NULL DEFAULT 0.0, `pcHieuSuat` REAL NOT NULL DEFAULT 0.0, `pcSanPham` REAL NOT NULL DEFAULT 0.0, `pcComCa` REAL NOT NULL DEFAULT 0.0, `pcComOt` REAL NOT NULL DEFAULT 0.0, `pcNhaO` REAL NOT NULL DEFAULT 0.0, `pcDocHai` REAL NOT NULL DEFAULT 0.0, `pcDtDoanhThu` REAL NOT NULL DEFAULT 0.0, `pcXangXe` REAL NOT NULL DEFAULT 0.0, `pcThamNien` REAL NOT NULL DEFAULT 0.0, `pcKhac1` REAL NOT NULL DEFAULT 0.0, `pcCaDem` REAL NOT NULL DEFAULT 0.0, `allowanceCalcTypes` TEXT NOT NULL DEFAULT '', `soGioNghiGiaiLao` REAL NOT NULL DEFAULT 1.5, `tinhKhauTruNghi` INTEGER NOT NULL DEFAULT 0, `hoVaTen` TEXT NOT NULL DEFAULT 'User Demo', `maNhanVien` TEXT NOT NULL DEFAULT 'demo_9bcad9a7', `emailDangKy` TEXT NOT NULL DEFAULT '', `soDienThoai` TEXT NOT NULL DEFAULT '', `boPhan` TEXT NOT NULL DEFAULT '', `lichTrinh` TEXT NOT NULL DEFAULT '08:00 - 17:00', `ngayVaoLam` TEXT NOT NULL DEFAULT '', `tienComMoiNgay` REAL NOT NULL DEFAULT 50000.0, `phuCap` REAL NOT NULL DEFAULT 1000000.0, `phuCapXangXe` REAL NOT NULL DEFAULT 500000.0, `phuCapDienThoai` REAL NOT NULL DEFAULT 300000.0, `phuCapNhaO` REAL NOT NULL DEFAULT 1000000.0, `phuCapChuyenCan` REAL NOT NULL DEFAULT 500000.0, `thuong` REAL NOT NULL DEFAULT 0.0, `heSoOtDem` REAL NOT NULL DEFAULT 1.75, `caDemStart` TEXT NOT NULL DEFAULT '22:00', `caDemEnd` TEXT NOT NULL DEFAULT '06:00', `isAdmin` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`userId`))")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 2 to 3 transition
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 3 to 4 transition
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 4 to 5 transition
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_entries ADD COLUMN shiftId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN shiftType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN rawCheckIn INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN rawCheckOut INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN normalizedCheckIn INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN normalizedCheckOut INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN workDay REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN otHours REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN lateMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN earlyLeaveMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 6 to 7 transition
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_entries ADD COLUMN customBreakDeduction INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN customBreakHours REAL DEFAULT NULL")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shifts` (
                        `id` TEXT NOT NULL,
                        `companyId` TEXT NOT NULL DEFAULT 'default_company',
                        `name` TEXT NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `breakMinutes` INTEGER NOT NULL DEFAULT 0,
                        `standardHours` REAL NOT NULL DEFAULT 8.0,
                        `crossesMidnight` INTEGER NOT NULL DEFAULT 0,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `work_rules` (
                        `id` TEXT NOT NULL,
                        `companyId` TEXT NOT NULL DEFAULT 'default_company',
                        `name` TEXT NOT NULL,
                        `standardHoursPerDay` REAL NOT NULL DEFAULT 8.0,
                        `breakCalculationMode` TEXT NOT NULL DEFAULT 'DEDUCT_BREAK_TIME',
                        `overtimeEnabled` INTEGER NOT NULL DEFAULT 1,
                        `overtimeStartAfterHours` REAL NOT NULL DEFAULT 8.0,
                        `roundingMode` TEXT NOT NULL DEFAULT 'NONE',
                        `roundingMinutes` INTEGER NOT NULL DEFAULT 15,
                        `lateToleranceMinutes` INTEGER NOT NULL DEFAULT 5,
                        `earlyLeaveToleranceMinutes` INTEGER NOT NULL DEFAULT 5,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `overtime_rules` (
                        `id` TEXT NOT NULL,
                        `companyId` TEXT NOT NULL DEFAULT 'default_company',
                        `name` TEXT NOT NULL,
                        `normalDayMultiplier` REAL NOT NULL DEFAULT 1.5,
                        `weeklyOffMultiplier` REAL NOT NULL DEFAULT 2.0,
                        `holidayMultiplier` REAL NOT NULL DEFAULT 3.0,
                        `minimumOvertimeMinutes` INTEGER NOT NULL DEFAULT 30,
                        `roundingMode` TEXT NOT NULL DEFAULT 'NONE',
                        `roundingMinutes` INTEGER NOT NULL DEFAULT 15,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_entries ADD COLUMN workRuleId TEXT")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN workRuleVersion INTEGER")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN overtimeRuleId TEXT")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN overtimeRuleVersion INTEGER")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN snapshotStandardHours REAL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN snapshotOtMultiplier REAL")
                db.execSQL("ALTER TABLE time_entries ADD COLUMN snapshotRoundingMinutes INTEGER")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_config ADD COLUMN companyId TEXT NOT NULL DEFAULT 'default_company'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timesnap_pro_db"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
