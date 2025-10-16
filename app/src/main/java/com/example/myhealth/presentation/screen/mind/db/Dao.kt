package com.example.myhealth.presentation.screen.mind.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: SessionEntity)

    @Query("SELECT * FROM mind_sessions WHERE date BETWEEN :start AND :end ORDER BY date ASC, id ASC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<SessionEntity>>

    @Query("SELECT SUM(minutes) FROM mind_sessions WHERE date = :date")
    fun observeSumByDate(date: LocalDate): Flow<Int?>

    @Query("SELECT * FROM mind_sessions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeRecentDays(start: LocalDate, end: LocalDate): Flow<List<SessionEntity>>
}

@Dao
interface MoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: MoodLogEntity)

    @Query("SELECT * FROM mind_moods WHERE date = :date ORDER BY id DESC LIMIT 1")
    fun observeLatestOf(date: LocalDate): Flow<MoodLogEntity?>

    @Query("""
        SELECT mood AS mood, COUNT(*) AS count
        FROM mind_moods
        WHERE date BETWEEN :start AND :end
        GROUP BY mood
        ORDER BY count DESC
    """)
    fun observeCountByMoodBetween(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<MoodCountDto>>
}
