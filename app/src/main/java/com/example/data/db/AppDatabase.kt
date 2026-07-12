package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.TimeEntry
import com.example.data.model.UserConfig

@Database(entities = [TimeEntry::class, UserConfig::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun userConfigDao(): UserConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timesnap_pro_db"
                )
                .fallbackToDestructiveMigration() // safe for production prototyping iteration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
