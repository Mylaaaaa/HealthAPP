package com.zihanwang.myhealth.presentation.screen.exercisesession

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zihanwang.myhealth.R

/**
 * Drop-in CRUD panel for your Workout page.
 * - Non-destructive: it does not change your existing screens; just call this Composable.
 * - Provide your list + callbacks; the panel will handle add/edit/delete/toggle with a dialog.
 */
@Composable
fun WorkoutCrudPanel(
    items: List<ExerciseItemUi>,
    onAdd: (date: java.time.LocalDate, minutes: Int, completed: Boolean) -> Unit,
    onEdit: (id: String, date: java.time.LocalDate, minutes: Int, completed: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    onToggle: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExerciseItemUi?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_entry))
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.date.toString(), fontWeight = FontWeight.Bold)
                            Text("${item.duration.toMinutes()} ${stringResource(R.string.minutes)}")
                        }
                        IconButton(onClick = { onToggle(item.id) }) {
                            Icon(
                                imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = stringResource(R.string.toggle_complete)
                            )
                        }
                        TextButton(onClick = { editing = item; showDialog = true }) {
                            Text(stringResource(R.string.edit))
                        }
                        TextButton(onClick = { onDelete(item.id) }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddEditExerciseDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onConfirm = { id, date, minutes, completed ->
                if (id == null) onAdd(date, minutes, completed)
                else onEdit(id, date, minutes, completed)
                showDialog = false
            }
        )
    }
}
