package com.example.myhealth.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myhealth.data.HealthDataSource
import com.example.myhealth.data.PermissionsState
import com.example.myhealth.data.RecentActivity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel that aggregates streams from HealthDataSource into a single UI state.
 */
data class HomeUiState(
    val steps: Int = 0,
    val sleepHours: Double = 0.0,
    val bodyWeightKg: Double = 0.0,
    val weeklySteps: List<Int> = emptyList(),
    val weeklySleep: List<Double> = emptyList(),
    val weeklyWeight: List<Double> = emptyList(),
    val stepGoal: Int = 10_000,
    val activeMinToday: Int = 0,
    val activeMinGoal: Int = 30,
    val sleepTodayHours: Double = 0.0,
    val sleepGoalHours: Double = 8.0,
    val lastWeighInDaysAgo: Int = 0,
    val permissions: PermissionsState = PermissionsState(hasAll = true, hasBackgroundRead = true),
    val recent: List<RecentActivity> = emptyList(),
    val currentStreakDays: Int = 3,       // UI-only sample
    val showBadgeUnlocked: Boolean = true // UI-only sample
)

class HomeViewModel(
    private val source: HealthDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Use combine(Iterable) to avoid arity limits of combine(flow1, flow2, ...).
        viewModelScope.launch {
            combine(
                listOf(
                    source.todaySteps(),          // [0] Int
                    source.todaySleepHours(),     // [1] Double
                    source.weeklySteps(),         // [2] List<Int>
                    source.weeklySleepHours(),    // [3] List<Double>
                    source.weeklyWeight(),        // [4] List<Double>
                    source.stepGoal(),            // [5] Int
                    source.todayActiveMinutes(),  // [6] Int
                    source.activeMinGoal(),       // [7] Int
                    source.sleepHourGoal(),       // [8] Double
                    source.lastWeighInDaysAgo(),  // [9] Int
                    source.permissions(),         // [10] PermissionsState
                    source.recent()               // [11] List<RecentActivity>
                )
            ) { arr: Array<Any?> ->
                // Safe casts with defaults to keep UI resilient.
                val tSteps              = (arr[0] as? Int) ?: 0
                val tSleep              = (arr[1] as? Double) ?: 0.0
                val wSteps              = (arr[2] as? List<Int>) ?: emptyList()
                val wSleep              = (arr[3] as? List<Double>) ?: emptyList()
                val wWeight             = (arr[4] as? List<Double>) ?: emptyList()
                val sGoal               = (arr[5] as? Int) ?: 10_000
                val tActive             = (arr[6] as? Int) ?: 0
                val aGoal               = (arr[7] as? Int) ?: 30
                val sGoalH              = (arr[8] as? Double) ?: 8.0
                val lastWeigh           = (arr[9] as? Int) ?: 0
                val perms               = (arr[10] as? PermissionsState) ?: PermissionsState(true, true)
                val recent              = (arr[11] as? List<RecentActivity>) ?: emptyList()

                _state.value.copy(
                    steps = tSteps,
                    sleepHours = tSleep,
                    bodyWeightKg = wWeight.lastOrNull() ?: 0.0,
                    weeklySteps = wSteps,
                    weeklySleep = wSleep,
                    weeklyWeight = wWeight,
                    stepGoal = sGoal,
                    activeMinToday = tActive,
                    activeMinGoal = aGoal,
                    sleepTodayHours = tSleep,
                    sleepGoalHours = sGoalH,
                    lastWeighInDaysAgo = lastWeigh,
                    permissions = perms,
                    recent = recent
                )
            }.collect { newState ->
                _state.value = newState
            }
        }

        // Refresh hook; real sources would fetch/sync here.
        viewModelScope.launch { source.refresh() }
    }
}
