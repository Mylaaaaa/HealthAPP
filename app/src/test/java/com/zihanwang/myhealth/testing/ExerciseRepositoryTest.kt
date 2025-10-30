package com.zihanwang.myhealth.testing

import org.junit.Assert.assertEquals
import org.junit.Test

data class ExerciseSession(
    val durationMinutes: Int,
    val caloriesBurned: Int
)

class ExerciseRepository {
    fun calculateTotalCalories(sessions: List<ExerciseSession>): Int {
        return sessions.sumOf { it.caloriesBurned }
    }

    fun calculateAverageDuration(sessions: List<ExerciseSession>): Double {
        return if (sessions.isEmpty()) 0.0 else sessions.map { it.durationMinutes }.average()
    }
}

class ExerciseRepositoryTest {

    @Test
    fun testTotalCaloriesAndAverageDuration() {
        val repo = ExerciseRepository()

        val sessions = listOf(
            ExerciseSession(durationMinutes = 30, caloriesBurned = 150),
            ExerciseSession(durationMinutes = 45, caloriesBurned = 220),
            ExerciseSession(durationMinutes = 60, caloriesBurned = 320)
        )

        val totalCalories = repo.calculateTotalCalories(sessions)
        val avgDuration = repo.calculateAverageDuration(sessions)

        assertEquals(690, totalCalories)
        assertEquals(45.0, avgDuration, 0.1)
    }
}
