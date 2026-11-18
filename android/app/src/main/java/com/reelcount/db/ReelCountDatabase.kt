package com.reelcount.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyCount::class, Settings::class],
    version = 1,
    exportSchema = true
)
abstract class ReelCountDatabase : RoomDatabase() {

    abstract fun dailyCountDao(): DailyCountDao

    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val DATABASE_NAME = "reelcount.db"

        @Volatile
        private var instance: ReelCountDatabase? = null

        fun get(context: Context): ReelCountDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        ReelCountDatabase::class.java,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
