package com.zihanwang.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zihanwang.myhealth.data.ExerciseSession
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Minimal detail screen for a single ExerciseSession (Material 2).
 * No ViewModel or repository dependency.
 */
@Composable
fun ExerciseDetailScreen(
    session: ExerciseSession,
    onClose: () -> Unit
) {
    Surface(color = MaterialTheme.colors.background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: title + close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.title.orEmpty().ifBlank { "Session" },
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Date / time
            DetailRow(
                icon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                label = "Date",
                value = formatDate(session.startTime)
            )

            Spacer(Modifier.height(8.dp))

            // Duration (auto formats as Xm or Xh Ym)
            DetailRow(
                icon = { Icon(Icons.Filled.Timelapse, contentDescription = null) },
                label = "Duration",
                value = formatDuration(session.startTime, session.endTime)
            )

            Spacer(Modifier.height(8.dp))

            // Source app (best-effort, safe on nulls)
            session.sourceAppInfo?.let { src ->
                DetailRow(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = "Source",
                    value = listOfNotNull(src.packageName).firstOrNull().orEmpty()
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

/**
 * Helper: render by id using an in-memory list from the parent screen.
 * Keeps your ViewModel unchanged.
 */
@Composable
fun ExerciseDetailScreenById(
    id: String,
    sessions: List<ExerciseSession>,
    onClose: () -> Unit
) {
    val target = sessions.firstOrNull { it.id == id } ?: return
    ExerciseDetailScreen(session = target, onClose = onClose)
}

/* ---------- Small internal UI pieces ---------- */

@Composable
private fun DetailRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.caption)
            Text(value, style = MaterialTheme.typography.body1)
        }
    }
}

/** Formats a ZonedDateTime into a compact, readable string. */
private fun formatDate(zdt: ZonedDateTime): String {
    val fmt = DateTimeFormatter.ofPattern("EEE, MMM d • HH:mm")
    return runCatching { zdt.format(fmt) }.getOrDefault("")
}

/** Formats duration between start and end as "Xm" or "Xh Ym". */
private fun formatDuration(start: ZonedDateTime, end: ZonedDateTime): String {
    val minutes = runCatching {
        Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)
    }.getOrDefault(0)
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}
