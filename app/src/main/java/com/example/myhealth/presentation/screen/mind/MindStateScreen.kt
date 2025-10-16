package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * MindStateScreen (with 7-day trend moved here)
 *
 * - White TopAppBar (no blue secondary bar).
 * - NO date quick bar (Yesterday / Today removed).
 * - Cards in order: Summary, Adherence & Streak, 7-day trend, Mood distribution, Insights.
 * - Uses ViewModel reactive data already in your project.
 */
@Composable
fun MindStateScreen(
    onBack: () -> Unit = {},
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    // Reactive states
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()           // list of last 7 day minutes
    val streak by vm.streakDays.collectAsState()
    val moodDist by vm.moodDistribution.collectAsState()      // Map<Mood, Int> over 7d if available

    // Derived metrics
    val weekSum = weekly.sum()
    val weekGoal = 7 * 10
    val adherenceDays = weekly.count { it > 0 }
    val adherencePct = if (weekly.isEmpty()) 0 else (adherenceDays * 100 / 7)
    val wow = weekOverWeekPct(weekly)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mindfulness • State") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color.White,
                elevation = 0.dp
            )
        },
        backgroundColor = Color.White
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) Summary
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Summary", style = MaterialTheme.typography.subtitle1)
                    Text("Today: $todayMinutes / 10 min")
                    Text("This week: $weekSum / $weekGoal min")
                    Text("Streak: $streak days")
                }
            }

            // 2) Adherence & Streak
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Adherence & Streak", style = MaterialTheme.typography.subtitle1)
                    Text("Adherence: $adherenceDays / 7 days ($adherencePct%)")
                    Text("Best day: " + bestDayLabel(weekly) + " (${weekly.maxOrNull() ?: 0} min)")
                }
            }

            // 3) 7-day trend (moved from Overview)
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("7-day trend", style = MaterialTheme.typography.subtitle1)
                    // simple mini bar chart based on weekly minutes
                    BarMiniChart(values = weekly.map { it.toFloat() })
                    Text(
                        text = "Week over week: ${if (wow >= 0) "+" else ""}$wow%",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            // 4) Mood distribution (7d)
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mood distribution (7d)", style = MaterialTheme.typography.subtitle1)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val total = moodDist.values.sum().coerceAtLeast(1)
                        // Show each mood’s share with a colored square + emoji + percent
                        moodDist.forEach { (m, cnt) ->
                            val pct = (cnt * 100 / total)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(m.tint.copy(alpha = 0.18f))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("${m.glyph} ${pct}%", style = MaterialTheme.typography.body2)
                            }
                        }
                    }
                }
            }

            // 5) Insights & Suggestions (static hint for now)
            Card(elevation = 4.dp) {
                Column(Modifier.padding(12.dp)) {
                    Text("Insights & Suggestions", style = MaterialTheme.typography.subtitle1)
                    Text("• Weekdays look stronger; try short 3-min sessions on weekends.")
                }
            }
        }
    }
}

/* ------------------------------ Helpers ------------------------------ */

/** Best day label from a 7-day list (Mon..Sun). */
private fun bestDayLabel(weekly: List<Int>): String {
    if (weekly.isEmpty()) return "—"
    val idx = weekly.indexOf(weekly.maxOrNull() ?: 0)
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return names.getOrElse(idx) { "—" }
}

/** Simple week-over-week percentage comparing last two buckets. */
private fun weekOverWeekPct(weekly: List<Int>): Int {
    if (weekly.size < 2) return 0
    val last = weekly.last()
    val prev = weekly.dropLast(1).lastOrNull() ?: 0
    if (prev == 0) return if (last == 0) 0 else 100
    return ((last - prev) * 100f / prev).toInt()
}
