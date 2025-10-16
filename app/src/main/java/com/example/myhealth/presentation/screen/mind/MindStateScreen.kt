package com.example.myhealth.presentation.screen.mind
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * - Full-width pastel sections, soft colors.
 * - "7-day trend (rolling)" with wider bars & spacing.
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
    val todayMinutes by vm.todayMinutes.collectAsState()
    val weekly by vm.weeklyMinutes.collectAsState()     // rolling last 7 days (D-6..D)
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
        topBar = {
            TopAppBar(
                title = { Text("Mindfulness • State") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                backgroundColor = Color.White, elevation = 0.dp
            )
        },
        backgroundColor = Color.White
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
                // Custom mini bar chart with controllable spacing/width/height
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
                Text("Week over week: ${if (wow >= 0) "+" else ""}$wow%",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f))
            }

            Section(title = "Mood distribution (7d)", containerColor = cMood) {
                val total = moodDist.values.sum().coerceAtLeast(1)

                // Sort by count desc so the most frequent mood is on top (optional)
                val ordered = moodDist.entries.sortedByDescending { it.value }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ordered.forEach { (mood, cnt) ->
                        val frac = cnt.toFloat() / total.toFloat()         // 0f..1f
                        val pctText = "${(frac * 100).toInt()}%"

                        MoodBarRow(
                            emoji = mood.glyph,
                            label = mood.label,
                            fraction = frac,
                            barColor = mood.tint,                            // use mood color
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

/* ------------------------------ Building blocks ------------------------------ */

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
            Text(title, style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold))
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
/**
 * A single horizontal bar row:
 * [emoji] [label]  |██████████.....|  [valueText]
 * The bar width is proportional to [fraction] (0f..1f).
 */
@Composable
private fun MoodBarRow(
    emoji: String,
    label: String,
    fraction: Float,
    barColor: Color,
    valueText: String
) {
    Column(Modifier.fillMaxWidth()) {
        // Header line: 😀 Happy        42%
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

        // Track + Fill bar
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
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))  // proportional width
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor.copy(alpha = 0.85f)) // vivid bar
                )
            }
        }
    }
}

/* ------------------------------ Helpers ------------------------------ */

/** week-over-week % comparing last two values in the 7d window. */
private fun weekOverWeekPct(weekly: List<Int>): Int {
    if (weekly.size < 2) return 0
    val last = weekly.last()
    val prev = weekly.dropLast(1).lastOrNull() ?: 0
    if (prev == 0) return if (last == 0) 0 else 100
    return ((last - prev) * 100f / prev).toInt()
}
