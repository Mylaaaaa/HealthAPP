package com.example.myhealth.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstraction for health-related data used by the home screen.
 * This file provides a fake in-memory implementation for coursework.
 * Replace FakeHealthDataSource with a real source when integrating Health Connect/DB.
 */
data class PermissionsState(
    val hasAll: Boolean,
    val hasBackgroundRead: Boolean
)

data class RecentActivity(
    val type: ActivityType,
    val title: String,
    val timeText: String
)

enum class ActivityType { EXERCISE, WEIGHT, SLEEP }

interface HealthDataSource {
    suspend fun refresh()
    fun weeklySteps(): Flow<List<Int>>
    fun weeklySleepHours(): Flow<List<Double>>
    fun weeklyWeight(): Flow<List<Double>>
    fun todaySteps(): Flow<Int>
    fun todayActiveMinutes(): Flow<Int>
    fun todaySleepHours(): Flow<Double>
    fun stepGoal(): Flow<Int>
    fun activeMinGoal(): Flow<Int>
    fun sleepHourGoal(): Flow<Double>
    fun lastWeighInDaysAgo(): Flow<Int>
    fun permissions(): Flow<PermissionsState>
    fun recent(): Flow<List<RecentActivity>>
}

/**
 * Fake data source for realistic UI without external dependencies.
 * Values can be replaced later by a real implementation.
 */
class FakeHealthDataSource : HealthDataSource {
    private val _weeklySteps = MutableStateFlow(listOf(5200, 6800, 7400, 8100, 7900, 8600, 7560))
    private val _weeklySleep = MutableStateFlow(listOf(6.2, 7.0, 7.5, 7.8, 7.1, 7.4, 7.2))
    private val _weeklyWeight = MutableStateFlow(listOf(55.4, 55.1, 54.9, 55.0, 54.7, 54.8, 54.8))

    private val _todaySteps = MutableStateFlow(7560)
    private val _todayActive = MutableStateFlow(22)
    private val _todaySleep = MutableStateFlow(7.2)

    private val _stepGoal = MutableStateFlow(10_000)
    private val _activeGoal = MutableStateFlow(30)
    private val _sleepGoal = MutableStateFlow(8.0)

    private val _lastWeighInDays = MutableStateFlow(3)

    private val _permissions = MutableStateFlow(
        PermissionsState(
            hasAll = false,
            hasBackgroundRead = false
        )
    )

    private val _recent = MutableStateFlow(
        listOf(
            RecentActivity(ActivityType.EXERCISE, "New workout: 28 min run", "Today 10:05"),
            RecentActivity(ActivityType.WEIGHT, "Weight recorded: 54.8 kg", "Yesterday 21:20"),
            RecentActivity(ActivityType.SLEEP, "Sleep: 7.2 h", "Yesterday 07:10")
        )
    )

    override suspend fun refresh() {
        // No-op for the fake source. A real source would fetch/sync here.
    }

    override fun weeklySteps() = _weeklySteps.asStateFlow()
    override fun weeklySleepHours() = _weeklySleep.asStateFlow()
    override fun weeklyWeight() = _weeklyWeight.asStateFlow()
    override fun todaySteps() = _todaySteps.asStateFlow()
    override fun todayActiveMinutes() = _todayActive.asStateFlow()
    override fun todaySleepHours() = _todaySleep.asStateFlow()
    override fun stepGoal() = _stepGoal.asStateFlow()
    override fun activeMinGoal() = _activeGoal.asStateFlow()
    override fun sleepHourGoal() = _sleepGoal.asStateFlow()
    override fun lastWeighInDaysAgo() = _lastWeighInDays.asStateFlow()
    override fun permissions() = _permissions.asStateFlow()
    override fun recent() = _recent.asStateFlow()
}
