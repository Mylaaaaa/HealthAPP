package com.example.myhealth.data.nutrition.db

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
}
