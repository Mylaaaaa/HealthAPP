package com.example.myhealth.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.example.myhealth.data.FakeHealthDataSource
import com.example.myhealth.presentation.screen.WelcomeScreen

/**
 * Thin adapter that wires a fake data source to the home screen.
 * Use HomeHost(navController) in your NavGraph for the home route.
 */
@Composable
fun HomeHost(navController: NavController) {
    val vm = remember { HomeViewModel(FakeHealthDataSource()) } // simple local VM for coursework
    val ui by vm.state.collectAsState()

    WelcomeScreen(
        navController = navController,
        ui = ui
    )
}
