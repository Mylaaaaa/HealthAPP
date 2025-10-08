package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Full-screen detail for a selected plan day.
 * Shows all items and two primary actions:
 *  - Start guided → start Guided flow with a ViewModel
 *  - Mark done quickly → record without execution
 *
 * This screen is OPTIONAL if you already use a BottomSheet in ExercisePlanScreen.
 * Keep it for future navigation-based UX or full preview.
 */
@Composable
fun DayPlanDetailScreen(
    day: PlanDay,
    onBack: () -> Unit,
    onStartGuided: (PlanDay) -> Unit,
    onQuickDone: (PlanDay) -> Unit
) {
    Surface {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(day.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
            Spacer(Modifier.height(8.dp))

            Card(Modifier.padding(16.dp), elevation = 2.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    day.items.forEach { Text("• $it") }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { onStartGuided(day) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Start guided")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { onQuickDone(day) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp)); Text("Mark done")
                }
            }
        }
    }
}