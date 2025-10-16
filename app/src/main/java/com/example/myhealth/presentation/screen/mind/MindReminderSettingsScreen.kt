package com.example.myhealth.presentation.screen.mind

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.util.Calendar

/**
 * Reminder/Date settings screen:
 * - Quick chips: Yesterday, Today, Tomorrow, Day before yesterday
 * - Button: "Pick a date..." opens system DatePickerDialog
 * - Shows the currently selected date; Save applies (you can wire it to DataStore later)
 */
@Composable
fun MindReminderSettingsScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(LocalDate.now()) }

    fun pick(date: LocalDate) { selected = date }

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = Color.White,
                elevation = 0.dp,
                title = { Text("Reminder date") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        backgroundColor = Color.White
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .background(Color.White),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Choose a date for your reminder", style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold))

            // Quick choices
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pick(LocalDate.now().minusDays(1)) }) { Text("Yesterday") }
                OutlinedButton(onClick = { pick(LocalDate.now()) }) { Text("Today") }
                OutlinedButton(onClick = { pick(LocalDate.now().plusDays(1)) }) { Text("Tomorrow") }
                OutlinedButton(onClick = { pick(LocalDate.now().minusDays(2)) }) { Text("Day before yesterday") }
            }

            // DatePickerDialog
            Button(onClick = {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selected.year)
                    set(Calendar.MONTH, selected.monthValue - 1)
                    set(Calendar.DAY_OF_MONTH, selected.dayOfMonth)
                }
                DatePickerDialog(
                    ctx,
                    { _, y, m, d -> selected = LocalDate.of(y, m + 1, d) },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Text("Pick a date…")
            }

            // Current selection
            Text("Selected: $selected", style = MaterialTheme.typography.body1)

            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                // TODO persist to DataStore/Room if needed
                onBack()
            }) {
                Text("Save")
            }
        }
    }
}
