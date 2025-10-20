package com.zihanwang.myhealth.presentation.screen.nutrition.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room database for nutrition logging.
 * Includes Foods, MealEntries, and Conditions.
 */
@Database(
    entities = [FoodEntity::class, MealEntryEntity::class, ConditionEntity::class],
    version = 2, // bumped because we added a new table
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NutritionDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun conditionDao(): ConditionDao

    companion object {
        @Volatile private var INSTANCE: NutritionDatabase? = null

        fun get(context: Context): NutritionDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutritionDatabase::class.java,
                    "nutrition.db"
                )
                    // Dev-friendly: wipe on incompatible schema change.
                    // Provide real Migration in production.
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            val appDb = get(context)
                            CoroutineScope(Dispatchers.IO).launch {
                                appDb.foodDao().upsertAll(Prepopulate.foods())
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
