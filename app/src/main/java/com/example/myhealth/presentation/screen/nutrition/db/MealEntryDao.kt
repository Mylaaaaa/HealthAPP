package com.example.myhealth.presentation.screen.nutrition.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealEntryDao {
    @Transaction
    @Query("SELECT * FROM meal_entries WHERE date = :date ORDER BY id DESC")
    fun observeByDate(date: LocalDate): Flow<List<MealEntryWithFood>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MealEntryEntity): Long

    @Delete
    suspend fun delete(entry: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Transaction
    @Query("""
        SELECT * FROM meal_entries 
        JOIN foods ON foods.code = meal_entries.foodCode
        WHERE date BETWEEN :start AND :end
        ORDER BY date ASC, meal_entries.id ASC
    """)
    fun observeBetween(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<MealEntryWithFood>>
}
