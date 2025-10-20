package com.zihanwang.myhealth.presentation.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zihanwang.myhealth.presentation.theme.HealthConnectTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.zihanwang.myhealth.R

/**
 * Navigation drawer with visual grouping (Track / Tools) and your new routes.
 * Navigation behavior and DrawerItem usage are kept intact.
 */
@Composable
fun Drawer(
    scope: CoroutineScope,
    scaffoldState: ScaffoldState,
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .width(96.dp)
                    .clickable {
                        navController.navigate(Screen.Home.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { scaffoldState.drawerState.close() }
                    },
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = stringResource(id = R.string.health_connect_logo)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primary
        )
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // ---------- Track ----------
        SectionHeader(text = "Track")


        val primaryItems = listOf(
            Screen.ExerciseSessions,
            Screen.SleepSessions,
            Screen.InputReadings,
            Screen.WeightRecords,
            Screen.Nutrition,
            Screen.Mind,
        ).filter { it.hasMenuItem }

        primaryItems.forEach { item ->
            DrawerItem(
                item = item,
                selected = item.route == currentRoute,
                onItemClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    scope.launch { scaffoldState.drawerState.close() }
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        // ---------- Tools ----------
        SectionHeader(text = "Tools")

        val secondaryItems = listOf(
            Screen.DifferentialChanges,
            Screen.SettingsScreen
        ).filter { it.hasMenuItem }

        secondaryItems.forEach { item ->
            DrawerItem(
                item = item,
                selected = item.route == currentRoute,
                onItemClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    scope.launch { scaffoldState.drawerState.close() }
                }
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

/** Small caption-like header to group menu items visually. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.overline,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Preview
@Composable
fun DrawerPreview() {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    HealthConnectTheme {
        Drawer(
            scope = scope,
            scaffoldState = scaffoldState,
            navController = navController
        )
    }
}
