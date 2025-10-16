package com.example.myhealth.data.nutrition.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Room database for nutrition logging (foods + meal entries).
@Database(
    entities = [FoodEntity::class, MealEntryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NutritionDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealEntryDao(): MealEntryDao

    companion object {
        @Volatile private var INSTANCE: NutritionDatabase? = null

        fun get(context: Context): NutritionDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutritionDatabase::class.java,
                    "nutrition.db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate the foods table on first DB creation.
                            val appDb = get(context)
                            CoroutineScope(Dispatchers.IO).launch {
                                appDb.foodDao().upsertAll(Prepopulate.foods())
                            }
                        }
                    })
                    .build().also { INSTANCE = it }
            }
    }
}
