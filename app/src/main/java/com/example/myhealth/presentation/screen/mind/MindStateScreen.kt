package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MindStateScreen(
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val today by vm.today.collectAsState()
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val moodDist by vm.moodDistribution.collectAsState()

    val weekSum = weekly.sum()
    val weekGoal = 7 * 10
    val adherenceDays = weekly.count { it > 0 }
    val adherencePct = if (weekly.isEmpty()) 0 else (adherenceDays * 100 / 7)

    Scaffold(topBar = { TopAppBar(title = { Text("Mindfulness • State") }) }) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Summary", style = MaterialTheme.typography.subtitle1)
                    Text("Today: $todayMinutes / 10 min")
                    Text("This week: $weekSum / $weekGoal min")
                    Text("This month: (hook monthly query later)")
                }
            }

            // Adherence & Streak
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Adherence & Streak", style = MaterialTheme.typography.subtitle1)
                    Text("Streak: $streak days")
                    Text("Adherence: $adherenceDays / 7 days ($adherencePct%)")
                    Text("Best day: ${bestDay(weekly)}")
                }
            }

            // Trend compare
            val lastWeek = remember(weekly, today) {
                // For demo: derive a baseline; replace with real DAO query if需要
                weekly.map { (it * 0.82).toInt() }
            }
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Trends & Comparison", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("This week")
                            BarMiniChart(weekly.map { it.toFloat() })
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Last week")
                            BarMiniChart(lastWeek.map { it.toFloat() })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val delta = pctDelta(weekly.sum(), lastWeek.sum())
                    Text("Week over week: ${if (delta >= 0) "+" else ""}$delta%")
                }
            }

            // Mood distribution
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mood distribution (7d)", style = MaterialTheme.typography.subtitle1)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        moodDist.forEach { (m, count) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val total = moodDist.values.sum().coerceAtLeast(1)
                                val pct = (count * 100 / total)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(m.tint.copy(alpha = 0.18f))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("${m.glyph} ${pct}%", style = MaterialTheme.typography.caption)
                            }
                        }
                    }
                }
            }

            // Insights
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Insights & Suggestions", style = MaterialTheme.typography.subtitle1)
                    Text("• Weekdays look stronger; try short 3-min sessions on weekends.")
                    Text("• Night reminder ~9:30 PM seems to work.")
                    Text("• Aim 10 min/day to extend streak.")
                }
            }
        }
    }
}

/* helpers */

private fun bestDay(weekly: List<Int>): String {
    if (weekly.isEmpty()) return "-"
    val idx = weekly.indices.maxByOrNull { weekly[it] } ?: 0
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return "${names[idx]} (${weekly[idx]} min)"
}

private fun pctDelta(a: Int, b: Int): Int {
    if (b <= 0) return 0
    return (((a - b) * 100f) / b).toInt()
}
