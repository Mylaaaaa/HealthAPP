package com.example.myhealth.presentation.screen.dashboard

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.BaseApplication
import com.example.myhealth.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.example.myhealth.presentation.screen.sleepsession.SleepSessionViewModel
import java.time.Duration
import java.time.ZonedDateTime

@Composable
fun DashboardScreen() {
    val app = LocalContext.current.applicationContext as Application
    val hcm = (app as BaseApplication).healthConnectManager

    val exerciseVm: ExerciseSessionViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExerciseSessionViewModel(hcm) as T
            }
        }
    )
    val sleepVm: SleepSessionViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SleepSessionViewModel(hcm) as T
            }
        }
    )

    val exercisePermGranted = exerciseVm.permissionsGranted.value
    val sleepPermGranted = sleepVm.permissionsGranted.value
    val exerciseSessions = exerciseVm.sessionsList.value
    val sleepSessions = sleepVm.sessionsList.value

    val now = ZonedDateTime.now()
    val todayStartZdt = now.toLocalDate().atStartOfDay(now.zone)   // Exercise 用
    val todayStartInstant = todayStartZdt.toInstant()              // Sleep 用

    // Today's Exercise
    val todayExercise = exerciseSessions.filter { it.startTime.isAfter(todayStartZdt) }
    val todayExerciseCount = todayExercise.size
    val todayExerciseMinutes = todayExercise.sumOf {
        Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(0)
    }

    // The sleep last night
    val latestSleep = sleepSessions.maxByOrNull { it.endTime }
    val lastSleepHours = latestSleep?.let {
        Duration.between(it.startTime, it.endTime).toHours().toInt().coerceAtLeast(0)
    } ?: 0

    // Sleep in the last 7 days
    val weekAgoZdt = now.minusDays(7)
    val weekAgoInstant = weekAgoZdt.toInstant()
    val weekSleepHours = sleepSessions
        .filter { it.startTime.isAfter(weekAgoInstant) }
        .sumOf { Duration.between(it.startTime, it.endTime).toHours() }
        .toInt()
        .coerceAtLeast(0)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Dashboard – Overview", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        Divider()

        if (!exercisePermGranted || !sleepPermGranted) {
            Text(
                "Some data requires permissions. Open Exercise/Sleep pages once to grant access.",
                style = MaterialTheme.typography.body2
            )
        }

        StatCard(title = "Today's workouts", value = "$todayExerciseCount sessions")
        StatCard(title = "Today's active minutes", value = "$todayExerciseMinutes min")
        StatCard(title = "Last night sleep", value = "$lastSleepHours h")
        StatCard(title = "Sleep (last 7 days)", value = "$weekSleepHours h")
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            Text(value, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        }
    }
}
