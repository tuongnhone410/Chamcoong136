package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig

@Database(entities = [TimeEntry::class, UserConfig::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun userConfigDao(): UserConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timesnap_database"
                )
                .fallbackToDestructiveMigration() // safe for local updates/resets
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
