package com.zihanwang.myhealth.presentation.screen.inputreadings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/** Row model used by the UI layer only. */
data class WeightRow(
    val id: String,
    val valueKg: Double,
    val timeText: String
)

/**
 * Weight screen content that adapts to light/dark theme automatically.
 * All fixed colors have been replaced with MaterialTheme tokens.
 */
@Composable
fun RecordWeightContent(
    weightText: String,
    onWeightTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    inputError: String?,                // null when valid
    recent: List<WeightRow>,
    onDelete: (String) -> Unit,
    weeklyAvgText: String               // e.g., "54.5 Kg"
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WeightInputCard(
                value = weightText,
                onValueChange = onWeightTextChange,
                error = inputError,
                onAddClick = onAdd
            )
        }

        if (recent.isNotEmpty()) {
            item { SectionHeader(text = "Previous measurements") }
            // Render each row inside an item{} to stay within a @Composable context
            recent.forEachIndexed { index, row ->
                item {
                    PreviousMeasurementRow(
                        row = row,
                        showDivider = index != recent.lastIndex,
                        onDelete = onDelete
                    )
                }
            }
        }

        item { SectionHeader(text = "Weekly average") }
        item {
            WeeklyAverageCard(
                avgText = weeklyAvgText,
                sparkValues = recent.takeLast(7).map { it.valueKg.toFloat() }
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

/* ---------- Components ---------- */

@Composable
private fun WeightInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    onAddClick: () -> Unit
) {
    Card(
        elevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "New record (Kg)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                leadingIcon = {
                    Icon(Icons.Filled.Accessibility, null, tint = MaterialTheme.colors.primary)
                },
                trailingIcon = {
                    Text("Kg", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                },
                isError = error != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    error,
                    color = MaterialTheme.colors.error,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddClick,
                enabled = error == null && value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Filled.FitnessCenter, null)
                Spacer(Modifier.width(8.dp))
                Text("Add", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = MaterialTheme.colors.primary
    )
}

@Composable
private fun PreviousMeasurementRow(
    row: WeightRow,
    showDivider: Boolean,
    onDelete: (String) -> Unit
) {
    // Light → blue (primary), Dark → red (error)
    val trashTint = if (MaterialTheme.colors.isLight) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.error
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Accessibility, null, tint = MaterialTheme.colors.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${trim(row.valueKg)} Kg",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Timer,
                        null,
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        row.timeText,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Filled.Delete,
                null,
                tint = trashTint,                 // ← use theme-aware tint
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDelete(row.id) }
            )
        }

        if (showDivider) {
            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
        }
    }
}

@Composable
private fun WeeklyAverageCard(avgText: String, sparkValues: List<Float>) {
    Card(
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Average this week",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                avgText,
                fontSize = 18.sp,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Sparkline(
                data = sparkValues,
                height = 44.dp,
                tint = MaterialTheme.colors.primary   // ← same color in light/dark
            )
        }
    }
}

/* ---------- Sparkline ---------- */
@Composable
private fun Sparkline(data: List<Float>, height: Dp, tint: Color) {
    if (data.size < 2) {
        Spacer(Modifier.fillMaxWidth().height(height))
        return
    }

    // Capture theme colors in Composable scope (OK)
    val trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
    val bgColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
    ) {
        val minV = data.minOrNull() ?: 0f
        val maxV = data.maxOrNull() ?: 1f
        val span = kotlin.math.max(1e-6f, maxV - minV)
        val w = size.width
        val h = size.height
        val stepX = w / (data.size - 1)
        val path = Path()
        data.forEachIndexed { idx, v ->
            val x = idx * stepX
            val y = h - ((v - minV) / span) * h
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Use captured colors inside draw scope (NOT calling MaterialTheme here)
        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(0f, h - 1f),
            end   = androidx.compose.ui.geometry.Offset(w, h - 1f),
            strokeWidth = 2f
        )
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        val lastY = h - ((data.last() - minV) / span) * h
        drawCircle(color = tint, radius = 6f, center = androidx.compose.ui.geometry.Offset(w, lastY))
    }
}

/* ---------- Helpers ---------- */
private fun trim(v: Double): String =
    if (abs(v - v.toInt()) < 1e-4) v.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", v)
