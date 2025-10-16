package com.example.myhealth.presentation.screen.nutrition.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user's chronic conditions.
 * Keep IDs as Long to match ConditionEntity.id.
 */
@Dao
interface ConditionDao {

    @Query("SELECT * FROM conditions ORDER BY name ASC")
    fun observeAll(): Flow<List<ConditionEntity>>

    @Query("SELECT * FROM conditions WHERE selected = 1 ORDER BY name ASC")
    fun observeSelected(): Flow<List<ConditionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(condition: ConditionEntity): Long

    @Query("UPDATE conditions SET selected = :selected WHERE id = :id")
    suspend fun setSelected(id: Long, selected: Boolean)

    @Query("DELETE FROM conditions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
