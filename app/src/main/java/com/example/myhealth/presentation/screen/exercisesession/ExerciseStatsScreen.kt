package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myhealth.data.ExerciseSession
import java.time.Duration
import java.time.ZonedDateTime

// Lightweight weekly stats cards (no chart dependencies).
@Composable
fun ExerciseStatsScreen(
    modifier: Modifier = Modifier,
    sessions: List<ExerciseSession>
) {
    val weekStart = ZonedDateTime.now().minusDays(6).toLocalDate()
    val inWeek = sessions.filter { it.startTime.toLocalDate() >= weekStart }

    val total = inWeek.fold(Duration.ZERO) { acc, s ->
        acc.plus(Duration.between(s.startTime, s.endTime))
    }
    val count = inWeek.size
    val avg = if (count > 0) total.dividedBy(count.toLong()) else Duration.ZERO
    val byApp = inWeek.groupBy { it.sourceAppInfo?.appLabel ?: "Unknown" }
        .mapValues { it.value.size }

    Column(modifier.padding(16.dp)) {
        StatCard("Total time", formatDuration(total))
        StatCard("Sessions this week", count.toString())
        StatCard("Average per session", formatDuration(avg))
        StatCard("Source apps", byApp.entries.joinToString { "${it.key}: ${it.value}" })
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Spacer(Modifier.height(8.dp))
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.subtitle2)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatDuration(d: Duration): String {
    val h = d.toHours()
    val m = d.minusHours(h).toMinutes()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
