package com.example.myhealth.presentation.screen.exercisesession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Curated course list with expandable descriptions.
@Composable
fun ExerciseCoursesScreen(modifier: Modifier = Modifier) {
    val courses = remember {
        listOf(
            Course("HIIT basics", "12 min", "Short intervals to safely raise heart rate."),
            Course("Form essentials", "15 min", "Posture cues for squats, push-ups, and hinges."),
            Course("Zone-2 walk", "25 min", "Steady conversational pace for fat oxidation."),
            Course("Full-body circuit", "30 min", "Strength + cardio rotation, minimal equipment.")
        )
    }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        courses.forEach { CourseCard(it) }
    }
}

private data class Course(val name: String, val duration: String, val desc: String)

@Composable
private fun CourseCard(course: Course) {
    var expanded by remember { mutableStateOf(false) }
    Card(elevation = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Icon(Icons.Default.Timer, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(course.name, fontWeight = FontWeight.SemiBold)
                        Text(course.duration, style = MaterialTheme.typography.body2)
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            AnimatedVisibility(visible = expanded) {
                Text(course.desc, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.body2)
            }
        }
    }
}
