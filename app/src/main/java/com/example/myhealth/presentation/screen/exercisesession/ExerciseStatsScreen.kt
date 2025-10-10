@file:Suppress("unused")

package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myhealth.data.ExerciseSession   // use your real model

// ---------------------------
// Public API (matches your caller)
// ---------------------------

/**
 * Stats dashboard content for the Exercise Sessions module (Material 2).
 *
 * NOTE:
 * - This composable intentionally does NOT include a TopAppBar/Scaffold.
 *   Your parent screen (ExerciseSessionScreen) already provides the app bar,
 *   so we only render the inner content to avoid a duplicated title bar.
 *
 * - The function signature matches your current call site:
 *     ExerciseStatsScreen(modifier = ..., sessions = sessionsList)
 */
@Composable
fun ExerciseStatsScreen(
    modifier: Modifier = Modifier,
    sessions: List<ExerciseSession>
) {
    // Build UI state from raw sessions (placeholder aggregator for now).
    val state = remember(sessions) { buildStatsUiStateFrom(sessions) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1) Week summary + progress bar
        Text(
            "This week",
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        SummaryProgressCard(
            completed = state.weekCompleted,
            goal = state.weekGoal
        )

        Spacer(Modifier.height(16.dp))

        // 2) Quick stats row (3 cards)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard(
                title = "Total time",
                value = "${state.totalMinutes}m",
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                title = "Avg / session",
                value = "${state.avgPerSessionMinutes}m",
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                title = "Longest streak",
                value = "${state.longestStreakDays} days",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // 3) Weekly trend
        SectionTitle("Weekly activity")
        CardBox {
            WeeklyBarChart(
                values = state.weeklyMinutes,
                labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                barWidth = 18.dp,
                height = 140.dp
            )
        }

        Spacer(Modifier.height(20.dp))

        // 4) Activity breakdown
        SectionTitle("Activity breakdown")
        CardBox {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.activityBreakdown.forEach {
                    BreakdownRow(name = it.name, percent = it.percent)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------
// UI state definition
// ---------------------------

data class StatsUiState(
    val weekCompleted: Int,
    val weekGoal: Int,
    val totalMinutes: Int,
    val avgPerSessionMinutes: Int,
    val longestStreakDays: Int,
    val weeklyMinutes: List<Int>,            // size = 7 (Mon..Sun)
    val activityBreakdown: List<ActivityShare>
)

data class ActivityShare(val name: String, val percent: Int)

// ---------------------------
// Colors (Material 2 friendly)
// ---------------------------

private val BrandBlue = Color(0xFF4285F4)
private val CardStroke = Color(0xFFE6E6E6)
private val CardBg = Color.White
private val Muted = Color(0xFF777777)

// ---------------------------
// Building blocks
// ---------------------------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CardBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Draw a 1dp rounded border (no dependency on Card(border=…))
    Card(
        backgroundColor = CardBg,
        elevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CardStroke,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            Modifier.padding(8.dp),
            contentAlignment = Alignment.CenterStart,
            content = content
        )
    }
}

@Composable
private fun SummaryProgressCard(completed: Int, goal: Int) {
    val progress = if (goal <= 0) 0f else (completed.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    CardBox {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "$completed / $goal sessions completed",
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(Modifier.height(10.dp))
            LinearProgress(
                progress = progress,
                trackColor = CardStroke,
                barColor = BrandBlue,
                height = 10.dp,
                corner = 8.dp
            )
        }
    }
}

@Composable
private fun QuickStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        backgroundColor = CardBg,
        elevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .border(
                width = 1.dp,
                color = CardStroke,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth()
        ) {
            Text(
                title,
                style = androidx.compose.ui.text.TextStyle(
                    color = Muted,
                    fontSize = 13.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun BreakdownRow(name: String, percent: Int) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, fontSize = 15.sp)
            Text(
                "$percent%",
                style = androidx.compose.ui.text.TextStyle(color = Muted, fontSize = 14.sp)
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgress(
            progress = (percent / 100f).coerceIn(0f, 1f),
            trackColor = CardStroke,
            barColor = BrandBlue,
            height = 8.dp,
            corner = 6.dp
        )
    }
}

/** Simple rounded-corner horizontal progress bar (M2 safe). */
@Composable
private fun LinearProgress(
    progress: Float,
    trackColor: Color,
    barColor: Color,
    height: Dp,
    corner: Dp
) {
    val shape = RoundedCornerShape(corner)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(barColor)
        )
    }
}

/** Minimal bar chart with Canvas (no 3rd-party lib, works on M2). */
@Composable
private fun WeeklyBarChart(
    values: List<Int>,
    labels: List<String>,
    barWidth: Dp,
    height: Dp
) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val density = LocalDensity.current
    val barPx = with(density) { barWidth.toPx() }
    val minGapPx = with(density) { 22.dp.toPx() }
    val chartHeightPx = with(density) { height.toPx() }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val totalBars = values.size
            val totalWidth = size.width
            val totalBarsWidth = totalBars * barPx
            val remaining = totalWidth - totalBarsWidth
            val autoGap = if (totalBars > 1) remaining / (totalBars + 1) else 0f
            val gap = kotlin.math.max(minGapPx, autoGap)

            var x = gap
            values.forEach { v ->
                val h = (v.toFloat() / max.toFloat()) * chartHeightPx
                // track line
                drawLine(
                    color = CardStroke,
                    start = Offset(x + barPx / 2, size.height),
                    end = Offset(x + barPx / 2, size.height - chartHeightPx),
                    strokeWidth = barPx,
                    cap = StrokeCap.Round
                )
                // value line
                if (v > 0) {
                    drawLine(
                        color = BrandBlue,
                        start = Offset(x + barPx / 2, size.height),
                        end = Offset(x + barPx / 2, size.height - h),
                        strokeWidth = barPx,
                        cap = StrokeCap.Round
                    )
                }
                x += barPx + gap
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEachIndexed { i, lab ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        lab,
                        style = androidx.compose.ui.text.TextStyle(color = Muted, fontSize = 13.sp)
                    )
                    val v = values.getOrNull(i) ?: 0
                    if (v > 0) Text(
                        "${v}m",
                        style = androidx.compose.ui.text.TextStyle(color = Muted, fontSize = 12.sp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ---------------------------
// Placeholder aggregator
// (replace with real fields when ready)
// ---------------------------

/**
 * Builds a StatsUiState from raw [ExerciseSession] list.
 * Replace the TODO parts with your real fields (duration, date, type, etc.).
 */
private fun buildStatsUiStateFrom(sessions: List<ExerciseSession>): StatsUiState {
    val totalSessions = sessions.size
    val weekGoal = 5

    // TODO: If your model has durationMinutes, sum it here.
    val totalMinutes = 0

    // TODO: If your model has date, group by day-of-week to fill this array.
    val weekly = listOf(0, 0, 0, 0, 0, 0, 0)

    return StatsUiState(
        weekCompleted = totalSessions.coerceAtMost(weekGoal),
        weekGoal = weekGoal,
        totalMinutes = totalMinutes,
        avgPerSessionMinutes = if (totalSessions == 0) 0 else totalMinutes / totalSessions,
        longestStreakDays = 0, // TODO: compute streak if dates are available
        weeklyMinutes = weekly,
        activityBreakdown = listOf(
            ActivityShare("Zone-2 cardio", 0),
            ActivityShare("Core stability", 0),
            ActivityShare("Strength", 0)
        ) // TODO: compute based on session type/category
    )
}

// ---------------------------
// Preview (standalone)
// ---------------------------

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun Preview_ExerciseStatsScreen() {
    // Local preview uses dummy sessions; your app will call with real data.
    @Suppress("UNUSED_VARIABLE")
    val dummySessions: List<ExerciseSession> = emptyList()

    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ExerciseStatsScreen(
                modifier = Modifier.fillMaxSize(),
                sessions = emptyList()
            )
        }
    }
}
