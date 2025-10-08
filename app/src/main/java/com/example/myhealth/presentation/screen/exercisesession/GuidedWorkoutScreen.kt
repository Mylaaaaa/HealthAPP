package com.example.myhealth.presentation.screen.exercisesession

import java.time.LocalDate
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Guided workout execution screen.
 *
 * Additions in this version:
 * - Persist/restore per-item progress for the day (using ActiveDayProgressStore).
 * - On (re)enter, we restore which items were completed and the current index.
 * - After each Done/Skip we save partial progress.
 * - On back exit we also save so the user can resume later.
 * - "Finish" still delegates to host (summary screen will clear partial progress).
 *
 * NOTE:
 * We treat both DONE and SKIPPED as "completed" for persistence purposes.
 */
@Composable
fun GuidedWorkoutScreen(
    vm: GuidedWorkoutViewModel,
    onExitConfirm: () -> Unit,
    onFinishRequest: () -> Unit, // host will open Summary (RPE/notes there)
) {
    val active = vm.activeWorkout ?: return

    val context = LocalContext.current
    val activeStore = remember { ActiveDayProgressStore(context.applicationContext) }
    val today = remember { LocalDate.now() }
    val dayKey = remember(active.dayTitle) { active.dayTitle }

    // ---- Restore previously saved partial progress (once per session title) ----
    LaunchedEffect(dayKey) {
        activeStore.load(today, dayKey)?.let { (flags, idxSaved) ->
            // We assume screen opens at index 0 with all pending.
            // Simulate prior actions (DONE -> markDone; not-done -> skip) up to idxSaved.
            // Guard against out-of-range / mismatched sizes.
            val limit = minOf(idxSaved, active.items.size)
            for (i in 0 until limit) {
                if (flags.getOrNull(i) == true) {
                    vm.markDone()  // advances index by 1
                } else {
                    vm.skip()      // advances index by 1 (as "skipped")
                }
            }
        }
    }

    // Derive current index / item / total each recomposition
    val idx = vm.currentExerciseIndex.coerceIn(0, active.items.lastIndex)
    val current = active.items[idx]
    val total = active.items.size
    val isLast = idx == total - 1

    // 1s ticker while running
    LaunchedEffect(vm.isWorkoutRunning) {
        while (vm.isWorkoutRunning) {
            delay(1000)
            vm.onTick(SystemClock.elapsedRealtime())
        }
    }

    // Helper: save partial progress (completed flags + current index)
    fun savePartial() {
        val flags = active.items.map { it.status != ItemStatus.PENDING } // DONE or SKIPPED -> true
        activeStore.save(today, dayKey, flags, vm.currentExerciseIndex)
    }

    var showConfirmExit by remember { mutableStateOf(false) }

    Surface {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(active.dayTitle)
                        // Small progress + elapsed timer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx + 1} / $total", style = MaterialTheme.typography.caption)
                            Spacer(Modifier.width(12.dp))
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(formatMs(vm.totalElapsedMs), style = MaterialTheme.typography.caption)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showConfirmExit = true }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleRunPause(SystemClock.elapsedRealtime()) }) {
                        if (vm.isWorkoutRunning) Icon(Icons.Default.Pause, contentDescription = "Pause")
                        else Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    }
                    // "Finish" stays always available (opens summary)
                    IconButton(onClick = {
                        // Let summary decide finalization; partial state stays until summary saves.
                        onFinishRequest()
                    }) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Finish")
                    }
                }
            )

            // Current exercise card
            Card(Modifier.padding(16.dp), elevation = 2.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(6.dp))
                    Text(current.name, style = MaterialTheme.typography.body1)
                    Spacer(Modifier.height(8.dp))

                    when (current) {
                        is ActiveItem.Strength -> StrengthInputs(vm)
                        is ActiveItem.Cardio -> Text(
                            "Track by elapsed time. Your RPE will be recorded on the summary screen.",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        is ActiveItem.Core -> Text("Track by elapsed time.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                        is ActiveItem.Mobility -> Text("Track by elapsed time.", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // On the last item, pressing the primary button leads to Summary
                        Button(
                            onClick = {
                                vm.markDone()
                                savePartial() // NEW: persist after change
                                if (isLast) onFinishRequest()
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isLast) "Finish" else "Done")
                        }
                        OutlinedButton(
                            onClick = {
                                vm.skip()
                                savePartial() // NEW: persist after change
                                if (isLast) onFinishRequest()
                            }
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Skip")
                        }
                    }
                }
            }

            // Queue header
            Text(
                "Queue",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.SemiBold)
            )

            // Queue list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                itemsIndexed(active.items) { i, item ->
                    val isCurrent = i == idx
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) MaterialTheme.colors.primary.copy(alpha = 0.06f)
                                else Color.Transparent
                            )
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${i + 1}. ${item.name}")
                        Spacer(Modifier.weight(1f))
                        val statusText = item.status.name.lowercase().replaceFirstChar { it.uppercase() }
                        Text(
                            statusText,
                            color = when (item.status) {
                                ItemStatus.DONE -> Color(0xFF2E7D32)
                                ItemStatus.SKIPPED -> Color(0xFF8D6E63)
                                ItemStatus.PENDING -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.alpha(if (isCurrent) 1f else 0.85f)
                        )
                    }
                    Divider(modifier = Modifier.alpha(0.08f))
                }
            }
        }
    }

    // Exit confirm (we DO keep progress)
    if (showConfirmExit) {
        AlertDialog(
            onDismissRequest = { showConfirmExit = false },
            title = { Text("Leave workout?") },
            text = {
                Text(
                    "Your current progress will be saved so you can resume later.",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Save once more before leaving
                    val flags = vm.activeWorkout?.items?.map { it.status != ItemStatus.PENDING } ?: emptyList()
                    activeStore.save(today, dayKey, flags, vm.currentExerciseIndex)
                    showConfirmExit = false
                    onExitConfirm()
                }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmExit = false }) { Text("Cancel") }
            }
        )
    }
}

/** Strength item UI: add sets (reps x weight). */
@Composable
private fun StrengthInputs(vm: GuidedWorkoutViewModel) {
    var repsText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = repsText, onValueChange = { repsText = it },
            label = { Text("Reps") }, singleLine = true, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = weightText, onValueChange = { weightText = it },
            label = { Text("Weight (kg)") }, singleLine = true, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = {
            vm.addStrengthSet(reps = repsText.toIntOrNull(), weightKg = weightText.toFloatOrNull())
            repsText = ""; weightText = ""
        }) { Text("Add set") }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}