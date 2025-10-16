package com.example.myhealth.presentation.screen.mind.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * DAO for mindful practice sessions.
 *
 * NOTE:
 * - All existing APIs are kept as-is.
 * - Use `observeBetween()` and `observeRecentDays()` for time-ranged queries.
 */
@Dao
interface SessionDao {

    /** Insert or replace a session entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: SessionEntity)

    /**
     * Observe all sessions between [start] and [end] (inclusive),
     * ordered by date ASC then id ASC.
     */
    @Query("SELECT * FROM mind_sessions WHERE date BETWEEN :start AND :end ORDER BY date ASC, id ASC")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<SessionEntity>>

    /**
     * Observe the total minutes for a specific date.
     */
    @Query("SELECT SUM(minutes) FROM mind_sessions WHERE date = :date")
    fun observeSumByDate(date: LocalDate): Flow<Int?>

    /**
     * Observe recent sessions for a range, ordered by date DESC then id DESC.
     * Useful for "latest first" lists.
     */
    @Query("SELECT * FROM mind_sessions WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeRecentDays(start: LocalDate, end: LocalDate): Flow<List<SessionEntity>>
}

/**
 * DAO for mood logs.
 *
 * NOTE:
 * - All existing APIs are kept.
 * - NEW: `observeLogsBetween()` streams all mood logs in a date range ordered by
 *   date DESC / id DESC so the first row for a given date is the latest mood of that day.
 *   This is perfect for building the "Recent moods (3 days)" UI in ViewModel by
 *   simply grouping by date and taking the first item per day.
 */
@Dao
interface MoodDao {

    /** Insert or replace a mood log entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: MoodLogEntity)

    /**
     * Observe the latest mood of a specific date (if any).
     */
    @Query("SELECT * FROM mind_moods WHERE date = :date ORDER BY id DESC LIMIT 1")
    fun observeLatestOf(date: LocalDate): Flow<MoodLogEntity?>

    /**
     * Aggregate mood counts within [start, end] (inclusive), grouped by mood.
     * Useful for "Mood distribution (7d)" pie/bars.
     */
    @Query(
        """
        SELECT mood AS mood, COUNT(*) AS count
        FROM mind_moods
        WHERE date BETWEEN :start AND :end
        GROUP BY mood
        ORDER BY count DESC
        """
    )
    fun observeCountByMoodBetween(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<MoodCountDto>>

    // -------------------- NEW API (kept minimal & generic) --------------------

    /**
     * NEW: Observe all mood logs in [start, end] (inclusive), ordered by
     * date DESC then id DESC. This makes it trivial to derive the "latest mood per day"
     * on the ViewModel layer:
     *
     * - For each date, take the first item as the latest mood of that day.
     * - Works great for "Recent moods (today / yesterday / 2 days ago)".
     */
    @Query("SELECT * FROM mind_moods WHERE date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    fun observeLogsBetween(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<MoodLogEntity>>
}
