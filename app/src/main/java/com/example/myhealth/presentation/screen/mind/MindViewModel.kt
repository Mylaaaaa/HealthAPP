package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myhealth.presentation.screen.mind.db.MindDatabase
import com.example.myhealth.presentation.screen.mind.db.MoodLogEntity
import com.example.myhealth.presentation.screen.mind.db.SessionEntity
import com.example.myhealth.presentation.screen.mind.db.MoodCountDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.DayOfWeek

class MindViewModel(app: Application) : AndroidViewModel(app) {

    private val db = MindDatabase.get(app)
    private val repo = MindRepository(db.sessionDao(), db.moodDao())

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    // Today minutes (daily sum)
    val todayMinutes: StateFlow<Int> =
        today.flatMapLatest { d -> repo.observeMinutesOf(d) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Weekly minutes (Mon..Sun, size = 7)
    val weeklyMinutes: StateFlow<List<Int>> =
        today.flatMapLatest { d -> repo.observeWeekMinutesOf(d) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, List(7) { 0 })

    // Streak: consecutive days with minutes > 0 ending today
    val streakDays: StateFlow<Int> =
        today.flatMapLatest { d -> repo.observeStreakEndingAt(d) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Mood (latest for today if any)
    val lastMood: StateFlow<Mood?> =
        today.flatMapLatest { d -> repo.observeLastMoodOf(d) }
            .map { it?.let { m -> Mood.valueOf(m.mood.uppercase()) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Mood distribution (7 days) -> Map<Mood, Int> for UI
    val moodDistribution: StateFlow<Map<Mood, Int>> =
        today.flatMapLatest { d -> repo.observeMoodCountOfWeek(d) }
            .map { raw: Map<String, Int> ->
                val map = mutableMapOf<Mood, Int>()
                Mood.values().forEach { map[it] = 0 }
                raw.forEach { (key, count) ->
                    val k = runCatching { Mood.valueOf(key.uppercase()) }.getOrNull()
                    if (k != null) map[k] = (map[k] ?: 0) + count
                }
                map.toMap()
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, Mood.values().associateWith { 0 })

    // Reminder (in-memory demo; can swap to DataStore later)
    private val _reminderEnabled = MutableStateFlow(true)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    fun setDate(d: LocalDate) { _today.value = d }

    // Actions to wire from UI
    fun addSession(minutes: Int, tag: String) = viewModelScope.launch {
        if (minutes > 0) repo.addSession(_today.value, minutes, tag)
    }

    fun checkInMood(m: Mood) = viewModelScope.launch {
        repo.addMood(_today.value, m.name.lowercase())
    }

    fun setReminder(enabled: Boolean) { _reminderEnabled.value = enabled }
}

/* ---------------- Repository ---------------- */

class MindRepository(
    private val sessionDao: com.example.myhealth.presentation.screen.mind.db.SessionDao,
    private val moodDao: com.example.myhealth.presentation.screen.mind.db.MoodDao
) {
    fun observeMinutesOf(date: LocalDate): Flow<Int> =
        sessionDao.observeSumByDate(date).map { it ?: 0 }

    fun observeWeekMinutesOf(anyDay: LocalDate): Flow<List<Int>> {
        val start = anyDay.with(DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        return sessionDao.observeBetween(start, end).map { list ->
            val map = list.groupBy { it.date }
                .mapValues { entry -> entry.value.sumOf { it.minutes } }
            (0..6).map { i -> map[start.plusDays(i.toLong())] ?: 0 }
        }
    }

    fun observeStreakEndingAt(today: LocalDate): Flow<Int> =
        sessionDao.observeRecentDays(today.minusDays(30), today).map { list ->
            val datesWithMinutes = list.groupBy { it.date }
                .mapValues { it.value.sumOf { s -> s.minutes } }
            var streak = 0
            var d = today
            while ((datesWithMinutes[d] ?: 0) > 0) {
                streak += 1; d = d.minusDays(1)
            }
            streak
        }

    fun observeLastMoodOf(date: LocalDate): Flow<MoodLogEntity?> =
        moodDao.observeLatestOf(date)

    /**
     * ⚠️ 这里把 DAO 的 List<MoodCountDto> 转成 Map<String, Int>
     * 这样上层 UI/旧代码无需修改。
     */
    fun observeMoodCountOfWeek(anyDay: LocalDate): Flow<Map<String, Int>> {
        val start = anyDay.with(DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        return moodDao.observeCountByMoodBetween(start, end)
            .map { list: List<MoodCountDto> -> list.associate { it.mood to it.count } }
    }

    suspend fun addSession(date: LocalDate, minutes: Int, tag: String) {
        sessionDao.insert(SessionEntity(date = date, minutes = minutes, tag = tag))
    }

    suspend fun addMood(date: LocalDate, mood: String) {
        moodDao.insert(MoodLogEntity(date = date, mood = mood))
    }
}
