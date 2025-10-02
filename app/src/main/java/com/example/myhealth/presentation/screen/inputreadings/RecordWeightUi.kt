package com.example.myhealth.presentation.screen.inputreadings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import kotlin.math.max

/** Row model used by the UI layer only. */
data class WeightRow(
    val id: String,
    val valueKg: Double,
    val timeText: String
)

/** Drop-in UI for the Record weight screen. */
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
            .background(Color(0xFFF7F9FC)),
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
            item {
                PreviousMeasurementsCard(items = recent, onDelete = onDelete)
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

/* ---------- Pieces ---------- */

@Composable
private fun WeightInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    onAddClick: () -> Unit
) {
    Card(
        elevation = 6.dp,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("New record (Kg)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                leadingIcon = { Icon(Icons.Filled.Accessibility, null, tint = Color(0xFF00B894)) },
                trailingIcon = { Text("Kg", color = Color.Gray) },
                isError = error != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error, color = Color(0xFFD32F2F), fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddClick,
                enabled = error == null && value.isNotBlank(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4C6FFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Filled.FitnessCenter, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Add", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color(0xFF3E5FFF))
}

@Composable
private fun PreviousMeasurementsCard(
    items: List<WeightRow>,
    onDelete: (String) -> Unit
) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            items.forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Accessibility, null, tint = Color(0xFF4C6FFF))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${trim(row.valueKg)} Kg", fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Timer, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(row.timeText, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Icon(
                        Icons.Filled.Delete, null, tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp).clickable { onDelete(row.id) }
                    )
                }
                if (row != items.last()) Divider(color = Color(0xFFF1F1F1))
            }
        }
    }
}

@Composable
private fun WeeklyAverageCard(avgText: String, sparkValues: List<Float>) {
    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(14.dp),
        backgroundColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Average this week", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(avgText, fontSize = 18.sp, color = Color(0xFF263238))
            Spacer(Modifier.height(10.dp))
            Sparkline(data = sparkValues, height = 44.dp, tint = Color(0xFF7C4DFF))
        }
    }
}

/* ---------- Tiny sparkline ---------- */
@Composable
private fun Sparkline(data: List<Float>, height: Dp, tint: Color) {
    if (data.size < 2) {
        Box(Modifier.fillMaxWidth().height(height))
        return
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F7FF))
    ) {
        val minV = data.minOrNull() ?: 0f
        val maxV = data.maxOrNull() ?: 1f
        val span = max(1e-6f, maxV - minV)
        val w = size.width
        val h = size.height
        val stepX = w / (data.size - 1)
        val path = Path()
        data.forEachIndexed { idx, v ->
            val x = idx * stepX
            val y = h - ((v - minV) / span) * h
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(
            color = Color(0xFFE5E1FF),
            start = androidx.compose.ui.geometry.Offset(0f, h - 1f),
            end = androidx.compose.ui.geometry.Offset(w, h - 1f),
            strokeWidth = 2f
        )
        drawPath(path = path, color = tint, style = Stroke(width = 5f, cap = StrokeCap.Round))
        val lastY = h - ((data.last() - minV) / span) * h
        drawCircle(color = tint, radius = 6f, center = androidx.compose.ui.geometry.Offset(w, lastY))
    }
}

/* ---------- Helpers ---------- */
private fun trim(v: Double): String =
    if (abs(v - v.toInt()) < 1e-4) v.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", v)
