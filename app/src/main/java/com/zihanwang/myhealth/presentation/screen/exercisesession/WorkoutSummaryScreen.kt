package com.zihanwang.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Workout summary screen (shown after finishing a guided workout).
 *
 * Improvements:
 * - Added onClose() callback: pressing "X" returns to the Plan screen instead of Guided.
 * - Clean layout with padding consistency.
 * - Inline comments for clarity.
 */
@Composable
fun WorkoutSummaryScreen(
    preview: WorkoutSummaryData,                    // PREVIEW (built by host without final RPE/notes)
    onSave: (rpe: Int, notes: String?) -> Unit,     // Called when Save pressed
    onClose: () -> Unit                             // NEW: Called when "X" pressed (return to Plan)
) {
    var rpe by remember { mutableStateOf(if (preview.overallRpe in 1..10) preview.overallRpe else 7) }
    var notes by remember { mutableStateOf(preview.notes.orEmpty()) }

    Surface {
        Column(Modifier.fillMaxSize()) {
            // --- Top Bar ---
            TopAppBar(
                title = { Text("Workout Summary") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            // --- Summary Card (Title / Time / Completion) ---
            Card(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                elevation = 2.dp
            ) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Title: ${preview.title}")
                    Text("Total time: ${formatMs(preview.totalMs)}")
                    Text("Completion: ${(preview.completionRate * 100).toInt()}%")
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- RPE + Notes Input ---
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("How hard was it? (RPE 1–10)")
                RpeDots(selected = rpe, onSelect = { rpe = it })
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Notes (optional)") }
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- Item Summary ---
            Text(
                "Completed Exercises",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.subtitle2
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(preview.items) { i, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${i + 1}. ${item.name} • ${item.status.name.lowercase()} • ${formatMs(item.elapsedMs)}"
                        )
                    }
                }
            }

            // --- Save Button Row ---
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onSave(rpe, notes.ifBlank { null }) },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/**
 * Round buttons for selecting RPE (Rate of Perceived Exertion, 1–10).
 * Selected value is highlighted.
 */
@Composable
fun RpeDots(selected: Int, onSelect: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        (1..10).forEach { v ->
            val isOn = v == selected
            OutlinedButton(
                onClick = { onSelect(v) },
                modifier = Modifier.size(36.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (isOn)
                        MaterialTheme.colors.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colors.surface
                )
            ) { Text("$v") }
            Spacer(Modifier.width(6.dp))
        }
    }
}

/** Utility: Formats milliseconds to readable mm:ss or h:mm:ss if long. */
private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
