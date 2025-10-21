package com.zihanwang.myhealth.presentation.screen.nutrition.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(food: FoodEntity)
    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun getAll(): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods 
        WHERE name LIKE '%' || :q || '%' OR code LIKE '%' || :q || '%' 
        ORDER BY name ASC
    """)
    fun search(q: String): Flow<List<FoodEntity>>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FoodEntity>)
}
