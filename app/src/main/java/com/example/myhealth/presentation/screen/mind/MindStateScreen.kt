package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
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
 * State/Insights screen using theme colors.
 */
@Composable
fun MindStateScreen(
    vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val moodDist by vm.moodDistribution.collectAsState()

    val weekSum = weekly.sum()
    val weekGoal = 7 * 10
    val adherenceDays = weekly.count { it > 0 }
    val adherencePct = if (weekly.isEmpty()) 0 else (adherenceDays * 100 / 7)
    val wow = weekOverWeekPct(weekly)

    val cSummary = MaterialTheme.colors.primary.copy(alpha = 0.06f)
    val cAdherence = MaterialTheme.colors.secondary.copy(alpha = 0.06f)
    val cTrend = MaterialTheme.colors.primary.copy(alpha = 0.10f)
    val cMood = MaterialTheme.colors.secondary.copy(alpha = 0.10f)
    val cInsights = MaterialTheme.colors.primary.copy(alpha = 0.05f)

    Scaffold(

        backgroundColor = MaterialTheme.colors.background
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Section(title = "Summary", containerColor = cSummary) {
                InfoLine("Today", "$todayMinutes / 10 min")
                InfoLine("This week (rolling 7d sum)", "$weekSum / $weekGoal min")
                InfoLine("Streak", "$streak days")
            }

            Section(title = "Adherence & Streak", containerColor = cAdherence) {
                InfoLine("Adherence (7d)", "$adherenceDays / 7 days ($adherencePct%)")
                InfoLine("Best day (7d)", (weekly.maxOrNull() ?: 0).toString() + " min")
            }

            Section(title = "7-day trend (rolling)", containerColor = cTrend) {
                val barWidth = 24.dp
                val spacing = 12.dp
                val chartHeight = 160.dp
                val maxVal = (weekly.maxOrNull() ?: 0).coerceAtLeast(1)
                val barColor = MaterialTheme.colors.primary
                Row(
                    modifier = Modifier.fillMaxWidth().height(chartHeight),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekly.forEach { minutes ->
                        val h = (minutes.toFloat() / maxVal) * chartHeight.value
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(h.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(barColor.copy(alpha = 0.85f))
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Week over week: ${if (wow >= 0) "+" else ""}$wow%",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                )
            }

            Section(title = "Mood distribution (7d)", containerColor = cMood) {
                val total = moodDist.values.sum().coerceAtLeast(1)
                val ordered = moodDist.entries.sortedByDescending { it.value }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ordered.forEach { (mood, cnt) ->
                        val frac = cnt.toFloat() / total.toFloat()
                        val pctText = "${(frac * 100).toInt()}%"

                        MoodBarRow(
                            emoji = mood.glyph,
                            label = mood.label,
                            fraction = frac,
                            barColor = mood.tint,
                            valueText = pctText
                        )
                    }
                }
            }

            Section(title = "Insights & Suggestions", containerColor = cInsights) {
                Text("• Weekdays look stronger; try short 3-min sessions on weekends.")
                Spacer(Modifier.height(4.dp))
                Text("• A 5-min body scan can improve consistency on off days.")
            }
        }
    }
}

/* ---------- local building blocks (kept here to avoid visibility issues) ---------- */

@Composable
private fun Section(
    title: String,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(color = containerColor, shape = RoundedCornerShape(16.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MoodBarRow(
    emoji: String,
    label: String,
    fraction: Float,         // 0f..1f
    barColor: Color,
    valueText: String
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(emoji)
                Text(label)
            }
            Text(valueText, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor.copy(alpha = 0.85f))
                )
            }
        }
    }
}

/** week-over-week % comparing last two values. */
private fun weekOverWeekPct(weekly: List<Int>): Int {
    if (weekly.size < 2) return 0
    val last = weekly.last()
    val prev = weekly.dropLast(1).lastOrNull() ?: 0
    if (prev == 0) return if (last == 0) 0 else 100
    return ((last - prev) * 100f / prev).toInt()
}
