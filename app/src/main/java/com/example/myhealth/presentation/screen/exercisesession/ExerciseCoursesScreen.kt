package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Simple curated course list similar to Keep.
 * You can later replace the static data with content from your backend.
 */
@Composable
fun ExerciseCoursesScreen(modifier: Modifier = Modifier) {
    val courses = remember {
        listOf(
            Course("HIIT basics", "12 min", "40s fast / 20s easy × 12. Warm up & cool down."),
            Course("Form essentials", "15 min", "Squat / hinge / push / pull / core cues."),
            Course("Zone-2 walk", "25 min", "Conversational pace (RPE 4-5)."),
            Course("Full-body circuit", "30 min", "3 rounds × 5 moves. 45s on / 15s off.")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        courses.forEach { c ->
            CourseCard(course = c)
            Spacer(Modifier.height(8.dp))
        }
    }
}

private data class Course(val title: String, val length: String, val brief: String)

@Composable
private fun CourseCard(course: Course) {
    var expanded by remember { mutableStateOf(false) }

    Card(elevation = 3.dp, shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(course.title, style = MaterialTheme.typography.subtitle1)
                    Text(course.length, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(course.brief)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { /* TODO: Navigate to player */ }) { Text("Start") }
                }
            }
        }
    }
}
