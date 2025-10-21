package com.zihanwang.myhealth.presentation.screen.mind

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun Ring(progress: Float, size: Dp, stroke: Dp, tint: Color) {
    val surfaceTrack = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    Canvas(modifier = Modifier.size(size)) {
        drawArc(
            color = surfaceTrack, startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = tint, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
            style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 4.dp,
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = "Quick action icon", tint = tint) } // a11y: add description for screen readers
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colors.onSurface)
                Text(subtitle, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

data class MindSession(val id: String, val title: String, val mins: Int, val tag: String, val accent: Color)

@Composable
fun GuidedChip(s: MindSession, onStart: (MindSession) -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colors.surface)
            .clickable { onStart(s) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(s.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.PlayArrow, contentDescription = "Start guided session", tint = s.accent) } // a11y: describe the action
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, color = MaterialTheme.colors.onSurface)
                Text("${s.mins} min • ${s.tag}", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun BarMiniChart(values: List<Float>, maxHeight: Dp = 80.dp) {
    val maxV = max(10f, values.maxOrNull() ?: 10f)
    Row(
        Modifier.fillMaxWidth().height(maxHeight),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { v ->
            val h = (v / maxV).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.85f))
            )
        }
    }
}

enum class Mood(val label: String, val glyph: String, val tint: Color) {
    GREAT("great", "😄", Color(0xFF4CAF50)),
    GOOD("good", "🙂", Color(0xFF8BC34A)),
    OKAY("okay", "😐", Color(0xFFFFC107)),
    BAD("bad", "🙁", Color(0xFFFF7043)),
    STRESSED("stressed", "😣", Color(0xFFF44336))
}

fun safeRatio(now: Float, goal: Float): Float =
    if (goal <= 0f) 0f else min(1f, max(0f, now / goal))
