package com.example.myhealth.presentation.screen.mind.db

import android.content.Context
import androidx.room.*
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, MoodLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MindDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun moodDao(): MoodDao

    companion object {
        @Volatile private var INSTANCE: MindDatabase? = null
        fun get(ctx: Context): MindDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    MindDatabase::class.java,
                    "mind.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
