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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * MindStateScreen
 *
 * Visual refresh:
 *  - Full-width pastel sections (no small white cards).
 *  - Each section uses a different soft color for better visual grouping.
 *  - No date quick bar; no secondary blue app bar.
 *
 * Data:
 *  - todayMinutes, weeklyMinutes (size=7), streakDays, moodDistribution(Map<Mood,Int>)
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
    // Reactive data
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val moodDist by vm.moodDistribution.collectAsState()

    // Derived
    val weekSum = weekly.sum()
    val weekGoal = 7 * 10
    val adherenceDays = weekly.count { it > 0 }
    val adherencePct = if (weekly.isEmpty()) 0 else (adherenceDays * 100 / 7)
    val wow = weekOverWeekPct(weekly)

    // Soft pastel container colors (based on theme)
    val cSummary = MaterialTheme.colors.primary.copy(alpha = 0.06f)
    val cAdherence = MaterialTheme.colors.secondary.copy(alpha = 0.06f)
    val cTrend = MaterialTheme.colors.primary.copy(alpha = 0.10f)
    val cMood = MaterialTheme.colors.secondary.copy(alpha = 0.10f)
    val cInsights = MaterialTheme.colors.primary.copy(alpha = 0.05f)

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) Summary
            Section(title = "Summary", containerColor = cSummary) {
                InfoLine("Today", "$todayMinutes / 10 min")
                InfoLine("This week", "$weekSum / $weekGoal min")
                InfoLine("Streak", "$streak days")
            }

            // 2) Adherence & Streak
            Section(title = "Adherence & Streak", containerColor = cAdherence) {
                InfoLine("Adherence", "$adherenceDays / 7 days ($adherencePct%)")
                InfoLine("Best day", bestDayLabel(weekly) + " (${weekly.maxOrNull() ?: 0} min)")
            }

            // 3) 7-day trend (moved here)
            Section(title = "7-day trend", containerColor = cTrend) {
                // mini bar chart with your existing util
                BarMiniChart(values = weekly.map { it.toFloat() })
                Spacer(Modifier.height(8.dp))
                Text(
                    "Week over week: ${if (wow >= 0) "+" else ""}$wow%",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                )
            }

            // 4) Mood distribution (7d)
            Section(title = "Mood distribution (7d)", containerColor = cMood) {
                val total = moodDist.values.sum().coerceAtLeast(1)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    moodDist.forEach { (m, cnt) ->
                        val pct = (cnt * 100 / total)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(m.tint.copy(alpha = 0.22f))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("${m.glyph} ${pct}%", style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }

            // 5) Insights
            Section(title = "Insights & Suggestions", containerColor = cInsights) {
                Text("• Weekdays look stronger; try short 3-min sessions on weekends.")
                Spacer(Modifier.height(4.dp))
                Text("• A 5-min body scan can improve consistency on off days.")
            }
        }
    }
}

/* ------------------------------ Building blocks ------------------------------ */

/**
 * Full-width pastel section container.
 * - No elevation to keep it flat/modern.
 * - Rounded corners and generous padding to feel like a card.
 */
@Composable
private fun Section(
    title: String,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )
            content()
        }
    }
}

/** A single labeled line like "Today — 12 / 10 min". */
@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

/* ------------------------------ Helpers ------------------------------ */

/** Best day label from a 7-day array (Mon..Sun). */
private fun bestDayLabel(weekly: List<Int>): String {
    if (weekly.isEmpty()) return "—"
    val idx = weekly.indexOf(weekly.maxOrNull() ?: 0)
    val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return names.getOrElse(idx) { "—" }
}

/** Simple week-over-week percentage comparing last two values. */
private fun weekOverWeekPct(weekly: List<Int>): Int {
    if (weekly.size < 2) return 0
    val last = weekly.last()
    val prev = weekly.dropLast(1).lastOrNull() ?: 0
    if (prev == 0) return if (last == 0) 0 else 100
    return ((last - prev) * 100f / prev).toInt()
}
