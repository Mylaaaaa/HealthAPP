package com.zihanwang.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
 * Root container for Mindfulness module.
 * Supports both light and dark themes (auto follows system theme).
 *
 * Tabs:
 *  - Overview: daily mindfulness summary
 *  - State:    mental state insights or statistics
 */
@Composable
fun MindRootScreen() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()

    val app = LocalContext.current.applicationContext as Application
    val vm: MindViewModel =
        viewModel(factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app))

    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        bottomBar = {
            BottomNavigation(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface
            ) {
                // --- Overview Tab ---
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
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                // --- State Tab (kept functional) ---
                BottomNavigationItem(
                    selected = current?.destination?.route == "mind_state",
                    onClick = {
                        nav.navigate("mind_state") {
                            launchSingleTop = true
                            popUpTo("mind_overview") { inclusive = false }
                        }
                    },
                    icon = { Icon(Icons.Filled.Timeline, contentDescription = "State") },
                    label = { Text("State") },
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "mind_overview",
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- Overview Screen ---
            composable("mind_overview") {
                MindOverviewScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSession = { title, mins, date, tag, auto ->
                        val encodedTitle =
                            URLEncoder.encode(title, StandardCharsets.UTF_8.name())
                        nav.navigate(
                            "mind_session_timer?title=$encodedTitle&mins=$mins&date=$date&tag=$tag&auto=$auto"
                        )
                    },
                    vm = vm
                )
            }

            // --- State Screen (this route must exist to avoid crash) ---
            composable("mind_state") {
                MindStateScreen(
                    vm = vm
                )
            }

            // --- Timer Screen ---
            composable(
                route = "mind_session_timer?title={title}&mins={mins}&date={date}&tag={tag}&auto={auto}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "Session" },
                    navArgument("mins") { type = NavType.IntType; defaultValue = 3 },
                    navArgument("date") {
                        type = NavType.StringType; defaultValue = LocalDate.now().toString()
                    },
                    navArgument("tag") { type = NavType.StringType; defaultValue = "breathing" },
                    navArgument("auto") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStack ->
                val title = backStack.arguments?.getString("title") ?: "Session"
                val mins = backStack.arguments?.getInt("mins") ?: 3
                val dateIso = backStack.arguments?.getString("date") ?: LocalDate.now().toString()
                val tag = backStack.arguments?.getString("tag") ?: "breathing"
                val auto = backStack.arguments?.getBoolean("auto") ?: false

                MindSessionTimerScreen(
                    title = title,
                    minutes = mins,
                    dateIso = dateIso,
                    tag = tag,
                    autoStart = auto,
                    onBack = { nav.popBackStack() },
                    vm = vm
                )
            }
        }
    }
}
