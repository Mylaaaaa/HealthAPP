package com.example.myhealth.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of HealthDataSource.
 * Lets the Home screen run with real-looking data, without DB or Health Connect.
 */
class InMemoryHealthDataSource : HealthDataSource {

    private val todaySteps = MutableStateFlow(7560)
    private val todaySleepHours = MutableStateFlow(7.2)
    private val weeklySteps = MutableStateFlow(listOf(6200, 8300, 9100, 10200, 7560, 8800, 9400))
    private val weeklySleep = MutableStateFlow(listOf(7.5, 7.1, 6.8, 8.0, 7.2, 7.9, 7.4))
    private val weeklyWeight = MutableStateFlow(listOf(55.1, 55.0, 54.9, 55.0, 54.8, 54.9, 54.8))
    private val stepGoal = MutableStateFlow(10_000)
    private val todayActiveMin = MutableStateFlow(24)
    private val activeMinGoal = MutableStateFlow(30)
    private val sleepGoalHours = MutableStateFlow(8.0)
    private val lastWeighInDays = MutableStateFlow(1)

    // Default granted; ViewModel will overwrite by real permission check on resume.
    private val perms = MutableStateFlow(PermissionsState(hasAll = true, hasBackgroundRead = true))

    private val recent = MutableStateFlow(
        listOf(
            RecentActivity(ActivityType.EXERCISE, "Outdoor run · 24 min", "Today 09:20"),
            RecentActivity(ActivityType.WEIGHT,   "Recorded weight 54.8 kg", "Yesterday 21:12"),
            RecentActivity(ActivityType.SLEEP,    "Sleep 7.2 h", "Today 07:10")
        )
    )

    override fun todaySteps(): Flow<Int> = todaySteps.asStateFlow()
    override fun todaySleepHours(): Flow<Double> = todaySleepHours.asStateFlow()
    override fun weeklySteps(): Flow<List<Int>> = weeklySteps.asStateFlow()
    override fun weeklySleepHours(): Flow<List<Double>> = weeklySleep.asStateFlow()
    override fun weeklyWeight(): Flow<List<Double>> = weeklyWeight.asStateFlow()
    override fun stepGoal(): Flow<Int> = stepGoal.asStateFlow()
    override fun todayActiveMinutes(): Flow<Int> = todayActiveMin.asStateFlow()
    override fun activeMinGoal(): Flow<Int> = activeMinGoal.asStateFlow()
    override fun sleepHourGoal(): Flow<Double> = sleepGoalHours.asStateFlow()
    override fun lastWeighInDaysAgo(): Flow<Int> = lastWeighInDays.asStateFlow()
    override fun permissions(): Flow<PermissionsState> = perms.asStateFlow()
    override fun recent(): Flow<List<RecentActivity>> = recent.asStateFlow()

    override suspend fun refresh() {
        // No-op for demo. Real source would fetch/sync here.
    }
}
