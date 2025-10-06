package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myhealth.data.ExerciseSession
import java.time.Duration
import java.time.ZonedDateTime

@Composable
fun ExerciseStatsScreen(
    modifier: Modifier = Modifier,
    sessions: List<ExerciseSession>
) {
    // Use Instant for Duration to avoid DST/zone issues
    val total = sessions.fold(Duration.ZERO) { acc, s ->
        acc + Duration.between(s.startTime.toInstant(), s.endTime.toInstant())
    }
    val totalMinutes = total.toMinutes().toInt()
    val totalHours = totalMinutes / 60
    val remainMinutes = totalMinutes % 60

    // Compare ZonedDateTime with ZonedDateTime (same type)
    val now = ZonedDateTime.now()
    val weekAgo = now.minusDays(7)
    val weekCount = sessions.count { it.startTime.isAfter(weekAgo) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(title = "Total time", value = "${totalHours}h ${remainMinutes}m")
        StatCard(title = "Sessions this week", value = "$weekCount")
        StatCard(title = "Average per session", value = averagePerSession(sessions))
        StatCard(
            title = "Source apps",
            value = sessions.groupBy { it.sourceAppInfo?.appLabel ?: "Unknown" }
                .entries.joinToString { "${it.key}: ${it.value.size}" }
        )
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(elevation = 3.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.h5)
        }
    }
}

private fun averagePerSession(sessions: List<ExerciseSession>): String {
    if (sessions.isEmpty()) return "–"
    val total = sessions.fold(Duration.ZERO) { acc, s ->
        acc + Duration.between(s.startTime.toInstant(), s.endTime.toInstant())
    }
    val avg = total.dividedBy(sessions.size.toLong())
    val min = avg.toMinutes().toInt()
    return "${min / 60}h ${min % 60}m"
}
