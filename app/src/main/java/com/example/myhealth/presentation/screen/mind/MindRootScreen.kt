package com.example.myhealth.presentation.screen.mind

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * MindRootScreen
 *
 * - Creates ONE shared MindViewModel and passes it down to all child screens.
 * - Keeps the bottom tabs (Overview / State).
 * - Timer route receives title/mins/date/tag/auto and also the SAME vm instance.
 */
@Composable
fun MindRootScreen() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()

    // ✅ Hoist a SINGLE MindViewModel at the root
    val app = LocalContext.current.applicationContext as Application
    val vm: MindViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )

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
            // Overview (uses the shared vm)
            composable("mind_overview") {
                MindOverviewScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSession = { title, mins, date, tag, autoStart ->
                        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
                        val dateIso = date.toString()
                        nav.navigate(
                            "mind_session_timer?title=$encodedTitle&mins=$mins&date=$dateIso&tag=$tag&auto=$autoStart"
                        )
                    },
                    vm = vm // ✅ pass the shared vm
                )
            }

            // State (uses the shared vm)
            composable("mind_state") {
                MindStateScreen(
                    onBack = { nav.popBackStack() },
                    vm = vm // ✅ pass the shared vm
                )
            }

            // Timer (also uses the shared vm so date/records update everywhere)
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
                    onBack = { nav.popBackStack() },
                    vm = vm // ✅ pass the SAME vm instance
                )
            }
        }
    }
}
