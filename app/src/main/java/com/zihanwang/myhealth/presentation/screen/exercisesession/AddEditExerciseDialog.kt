package com.zihanwang.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zihanwang.myhealth.R
import java.time.Duration
import java.time.LocalDate

/**
 * Reusable add/edit dialog for a workout item.
 * - If [initial] is null => this dialog behaves as "Add".
 * - If [initial] is not null => this dialog behaves as "Edit".
 * It does not perform storage; it calls [onConfirm] with the input values.
 */
@Composable
fun AddEditExerciseDialog(
    onDismiss: () -> Unit,
    initial: ExerciseItemUi? = null,
    onConfirm: (id: String?, date: LocalDate, minutes: Int, completed: Boolean) -> Unit
) {
    // Local editable states
    var minutesText by remember { mutableStateOf((initial?.duration?.toMinutes() ?: 30).toString()) }
    var completed by remember { mutableStateOf(initial?.isCompleted ?: false) }
    // For simplicity, we use "today" as date. If you have a DatePicker, you can swap it in later.
    val date = initial?.date ?: LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) stringResource(R.string.dialog_add_title) else stringResource(R.string.dialog_edit_title))
        },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Date (read-only for now; replace with your DatePicker if needed)
                OutlinedTextField(
                    value = date.toString(),
                    onValueChange = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.date)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Duration in minutes
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { input ->
                        minutesText = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text(stringResource(R.string.duration_min)) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Completed checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = completed, onCheckedChange = { completed = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mark_completed))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val mins = minutesText.toIntOrNull() ?: 30
                onConfirm(initial?.id, date, mins, completed)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Lightweight UI model dedicated to the Workout list.
 * This is separate from your existing ExerciseSession domain model to avoid coupling.
 */
data class ExerciseItemUi(
    val id: String,
    val date: LocalDate,
    val duration: Duration,
    val isCompleted: Boolean
)
