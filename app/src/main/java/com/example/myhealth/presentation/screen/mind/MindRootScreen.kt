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

/**
 * MindRootScreen
 *
 * Hosts the Mind feature:
 *  - Overview (dashboard)
 *  - State (analytics)
 *  - SessionTimer (guided practice timer)
 *
 * No external parameters needed; this function owns its NavController.
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
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "mind_overview",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Overview
            composable("mind_overview") {
                MindOverviewScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSession = { title, mins ->
                        val encoded = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8.name())
                        nav.navigate("mind_session_timer?title=$encoded&mins=$mins")
                    }
                )
            }


            // State
            composable("mind_state") {
                MindStateScreen(onBack = { nav.popBackStack() })
            }

            // Guided session timer
            composable(
                route = "mind_session_timer?title={title}&mins={mins}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "Session" },
                    navArgument("mins") { type = NavType.IntType; defaultValue = 3 }
                )
            ) { bs ->
                val title = bs.arguments?.getString("title") ?: "Session"
                val mins = bs.arguments?.getInt("mins") ?: 3
                MindSessionTimerScreen(
                    title = title,
                    minutes = mins,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
