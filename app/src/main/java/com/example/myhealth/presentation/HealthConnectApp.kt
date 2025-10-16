/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.myhealth.presentation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myhealth.R
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.navigation.Drawer
import com.example.myhealth.presentation.navigation.HealthConnectNavigation
import com.example.myhealth.presentation.navigation.Screen
import com.example.myhealth.presentation.theme.HealthConnectTheme
import kotlinx.coroutines.launch

const val TAG = "Health Connect sample"

/** Determines whether it is a top-level page (three dots are displayed on the homepage, while a return arrow is shown on other pages) */
private fun isTopLevel(route: String?): Boolean {
    return route?.startsWith(Screen.Home.route) == true
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HealthConnectApp(healthConnectManager: HealthConnectManager) {
    HealthConnectTheme {
        val scaffoldState = rememberScaffoldState()
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val availability by healthConnectManager.availability
        val context = LocalContext.current

        // System return key logic: First close the Drawer → Then return → Otherwise exit the App
        BackHandler(enabled = true) {
            when {
                scaffoldState.drawerState.isOpen -> {
                    scope.launch { scaffoldState.drawerState.close() }
                }
                navController.previousBackStackEntry != null -> {
                    navController.popBackStack()
                }
                else -> {
                    (context as? Activity)?.let {
                        ActivityCompat.finishAfterTransition(it)
                    }
                }
            }
        }

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = {
                        val titleId = when (currentRoute) {
                            Screen.ExerciseSessions.route -> Screen.ExerciseSessions.titleId
                            Screen.SleepSessions.route -> Screen.SleepSessions.titleId
                            Screen.InputReadings.route -> Screen.InputReadings.titleId
                            Screen.DifferentialChanges.route -> Screen.DifferentialChanges.titleId
                            Screen.WeightRecords.route -> Screen.WeightRecords.titleId
                            Screen.Nutrition.route -> Screen.Nutrition.titleId
                            else -> R.string.app_name
                        }
                        Text(stringResource(titleId))
                    },
                    navigationIcon = {
                        if (isTopLevel(currentRoute)) {
                            IconButton(
                                onClick = {
                                    if (availability == SDK_AVAILABLE) {
                                        scope.launch {
                                            scaffoldState.drawerState.open()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        } else {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }

                )
            },

            drawerContent = {
                if (availability == SDK_AVAILABLE) {
                    Drawer(
                        scope = scope,
                        scaffoldState = scaffoldState,
                        navController = navController
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(it) { data -> Snackbar(snackbarData = data) }
            }
        ) {
            HealthConnectNavigation(
                healthConnectManager = healthConnectManager,
                navController = navController,
                scaffoldState = scaffoldState
            )
        }
    }
}
