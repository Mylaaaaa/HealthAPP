package com.zihanwang.myhealth.presentation.screen.nutrition

import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// Routes used only for inner navigation inside the Nutrition module
const val ROUTE_NUTRITION_OVERVIEW: String = "nutrition/overview"
const val ROUTE_NUTRITION_STATE: String = "nutrition/state"

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun NutritionRootScreen() {
    // Inner NavController specific to the Nutrition module
    val innerNav = rememberNavController()
    val tabs = listOf(
        TabItem(ROUTE_NUTRITION_OVERVIEW, "Overview", Icons.Filled.List),
        TabItem(ROUTE_NUTRITION_STATE, "State", Icons.Filled.Dashboard)
    )

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colors.background),
        // --- REPLACE ONLY THIS bottomBar BLOCK (keep everything else as-is) ---
        bottomBar = {
            BottomNavigation(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                elevation = 8.dp
            ) {
                val navBackStackEntry by innerNav.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route

                    // Match Sleep/Exercise: subtle scale-up for the selected tab
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.35f else 1f,
                        label = "nutrition_tab_scale"
                    )

                    BottomNavigationItem(
                        selected = selected,
                        onClick = {
                            innerNav.navigate(tab.route) {
                                // Keep a single instance per tab and restore its state
                                popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label, // a11y for screen readers
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )
                        },
                        label = { Text(tab.label) },
                        selectedContentColor = MaterialTheme.colors.primary,
                        unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        alwaysShowLabel = true
                    )
                }
            }
        }

    )
    { innerPadding ->
        // Create the AndroidViewModel once and share to both tabs
        val app = LocalContext.current.applicationContext as Application
        val sharedVm: NutritionViewModel = viewModel(
            factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )
        NavHost(
            navController = innerNav,
            startDestination = ROUTE_NUTRITION_OVERVIEW,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_NUTRITION_OVERVIEW) {
                com.zihanwang.myhealth.presentation.screen.nutrition.NutritionScreen() // if NutritionScreen doesn't accept a vm param
                // If your NutritionScreen CAN accept a vm param, prefer passing sharedVm:
                // NutritionScreen(vm = sharedVm)
            }
            composable(ROUTE_NUTRITION_STATE) {
                com.zihanwang.myhealth.presentation.screen.nutrition.NutritionStateScreen(vm = sharedVm)
            }
        }
    }
}
