package com.example.myhealth.presentation.screen.exercisesession.course

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object CourseRoutes {
    const val HOME = "courses/home"
    const val PREVIEW = "courses/preview/{id}"
    const val PLAYER = "courses/player/{id}"

    fun preview(id: String) = "courses/preview/$id"
    fun player(id: String) = "courses/player/$id"
}

/** Drop-in demo host you can run stand-alone. */
@Composable
fun CoursesGraphHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = CourseRoutes.HOME) {
        coursesGraph(nav)
    }
}

fun NavGraphBuilder.coursesGraph(nav: NavController) {
    composable(CourseRoutes.HOME) {
        com.example.myhealth.presentation.screen.exercisesession.ExerciseCoursesScreen(
            onBack = { nav.popBackStack() },
            onContinue = { id -> nav.navigate(CourseRoutes.player(id)) },
            onSwitch = { /* you may show a toast or just ignore */ },
            onJoin = { id -> nav.navigate(CourseRoutes.player(id)) },
            onPreview = { id -> nav.navigate(CourseRoutes.preview(id)) }
        )
    }
    composable(CourseRoutes.PREVIEW) { back ->
        val id = back.arguments?.getString("id") ?: ""
        PreviewScreen(id = id) { nav.popBackStack() }
    }
    composable(CourseRoutes.PLAYER) { back ->
        val id = back.arguments?.getString("id") ?: ""
        PlayerScreen(id = id) { nav.popBackStack() }
    }
}

/* ----------------- Simple demo pages as navigation targets ---------------- */

@Composable
fun PreviewScreen(id: String, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("Preview course: $id")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
fun PlayerScreen(id: String, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("Course player (guided): $id")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onExit) { Text("Back") }
        }
    }
}
