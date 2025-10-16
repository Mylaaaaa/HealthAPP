package com.example.myhealth.presentation.screen.mind

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * MindRootScreen
 *
 * - Bottom tabs: Overview / State
 * - Extra route: mind_session_timer (with title, mins, date ISO, tag, and autoStart)
 */
@Composable
fun MindRootScreen() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()

    Scaffold(
        backgroundColor = Color.White,
        bottomBar = {
            BottomNavigation(backgroundColor = MaterialTheme.colors.primary) {
                BottomNavigationItem(
                    selected = current?.destination?.route == "mind_overview",
                    onClick = {
                        nav.navigate("mind_overview") {
                            launchSingleTop = true
                            popUpTo("mind_overview") { inclusive = false }
                        }
                    },
                    icon = { Icon(Icons.Filled.Psychology, contentDescription = "Overview") },
                    label = { Text("Overview") },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
                BottomNavigationItem(
                    selected = current?.destination?.route == "mind_state",
                    onClick = { nav.navigate("mind_state") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Timeline, contentDescription = "State") },
                    label = { Text("State") },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "mind_overview",
            modifier = Modifier.padding(inner)
        ) {
            // Overview
            composable("mind_overview") {
                MindOverviewScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSession = { title, mins, date, tag, autoStart ->
                        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
                        val dateIso = date.toString() // yyyy-MM-dd
                        nav.navigate(
                            "mind_session_timer?title=$encodedTitle&mins=$mins&date=$dateIso&tag=$tag&auto=$autoStart"
                        )
                    }
                )
            }

            // State
            composable("mind_state") {
                MindStateScreen(onBack = { nav.popBackStack() })
            }

            // Timer
            composable(
                route = "mind_session_timer?title={title}&mins={mins}&date={date}&tag={tag}&auto={auto}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "Session" },
                    navArgument("mins") { type = NavType.IntType; defaultValue = 3 },
                    navArgument("date") { type = NavType.StringType; defaultValue = LocalDate.now().toString() },
                    navArgument("tag") { type = NavType.StringType; defaultValue = "breathing" },
                    navArgument("auto") { type = NavType.BoolType; defaultValue = false }
                )
            ) { back ->
                val title = back.arguments?.getString("title") ?: "Session"
                val mins = back.arguments?.getInt("mins") ?: 3
                val dateIso = back.arguments?.getString("date") ?: LocalDate.now().toString()
                val tag = back.arguments?.getString("tag") ?: "breathing"
                val auto = back.arguments?.getBoolean("auto") ?: false

                MindSessionTimerScreen(
                    title = title,
                    minutes = mins,
                    dateIso = dateIso,
                    tag = tag,
                    autoStart = auto,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
