package com.example.myhealth.presentation.screen.reports

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myhealth.presentation.BaseApplication
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.screen.exercisesession.ExerciseSessionViewModel
import com.example.myhealth.presentation.screen.sleepsession.SleepSessionViewModel
import java.time.Duration
import java.time.ZonedDateTime

@Composable
fun ReportsScreen() {
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

    val exercises = exerciseVm.sessionsList.value
    val sleeps = sleepVm.sessionsList.value

    val now = ZonedDateTime.now()
    val weekStartZdt = now.minusDays(7)
    val weekStartInstant = weekStartZdt.toInstant()

    val weeklyExerciseMinutes = exercises
        .filter { it.startTime.isAfter(weekStartZdt) }  // ZDT 对 ZDT
        .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        .toInt()
        .coerceAtLeast(0)

    val weeklySleepHours = sleeps
        .filter { it.startTime.isAfter(weekStartInstant) } // Instant 对 Instant
        .sumOf { Duration.between(it.startTime, it.endTime).toHours() }
        .toInt()
        .coerceAtLeast(0)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Weekly Reports", style = MaterialTheme.typography.h6)
        Divider()

        MetricCard(label = "Exercise (last 7 days)", value = "$weeklyExerciseMinutes min")
        MetricCard(label = "Sleep (last 7 days)", value = "$weeklySleepHours h")

        Spacer(Modifier.height(8.dp))
        Text(
            "Tip: aim for ≥150 min moderate activity/week and ≥7 h sleep/night.",
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.subtitle1)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.h6)
        }
    }
}
