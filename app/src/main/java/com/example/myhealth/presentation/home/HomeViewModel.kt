package com.example.myhealth.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myhealth.data.HealthDataSource
import com.example.myhealth.data.PermissionsState
import com.example.myhealth.data.RecentActivity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Health Connect imports for permission refresh
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

/**
 * Aggregates streams into a single UI state for Home.
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
    val currentStreakDays: Int = 3,       // demo only
    val showBadgeUnlocked: Boolean = true // demo only
)

class HomeViewModel(
    private val source: HealthDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // VM-owned permissions flow; UI derives from this.
    private val permissionsState = MutableStateFlow(_state.value.permissions)

    init {
        viewModelScope.launch {
            // Use combine(Iterable) to avoid arity limits.
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
                    permissionsState,             // [10] PermissionsState  (VM-owned)
                    source.recent()               // [11] List<RecentActivity>
                )
            ) { arr: Array<Any?> ->
                val tSteps   = (arr[0] as? Int) ?: 0
                val tSleep   = (arr[1] as? Double) ?: 0.0
                val wSteps   = (arr[2] as? List<Int>) ?: emptyList()
                val wSleep   = (arr[3] as? List<Double>) ?: emptyList()
                val wWeight  = (arr[4] as? List<Double>) ?: emptyList()
                val sGoal    = (arr[5] as? Int) ?: 10_000
                val tActive  = (arr[6] as? Int) ?: 0
                val aGoal    = (arr[7] as? Int) ?: 30
                val sGoalH   = (arr[8] as? Double) ?: 8.0
                val lastWeigh= (arr[9] as? Int) ?: 0
                val perms    = (arr[10] as? PermissionsState) ?: PermissionsState(true, true)
                val recent   = (arr[11] as? List<RecentActivity>) ?: emptyList()

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
            }.collect { newState -> _state.value = newState }
        }

        // Initial refresh hook for your other data source.
        viewModelScope.launch { source.refresh() }
    }

    /**
     * Recompute granted permissions from Health Connect and update the UI.
     * Call this when the screen is RESUMED (user may have changed permissions).
     */
    fun refreshPermissions(client: HealthConnectClient) = viewModelScope.launch {
        val required = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        )
        // Library version: no-arg API returns all granted permissions.
        val granted: Set<String> = client.permissionController.getGrantedPermissions()
        val hasAll = required.all { it in granted }

        // Tie backgroundRead to hasAll for now (replace with real toggle later).
        permissionsState.value = PermissionsState(hasAll = hasAll, hasBackgroundRead = hasAll)
    }
}
