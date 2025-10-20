package com.zihanwang.myhealth.presentation.home

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.health.connect.client.HealthConnectClient
import com.zihanwang.myhealth.presentation.screen.WelcomeScreen

// Use your interface and our in-memory implementation
import com.zihanwang.myhealth.data.HealthDataSource
import com.zihanwang.myhealth.data.InMemoryHealthDataSource

@Composable
fun HomeHost(
    navController: NavController
) {
    val context = LocalContext.current

    // Health Connect might be unavailable on some devices; guard with try/catch.
    val client: HealthConnectClient? = remember {
        try { HealthConnectClient.getOrCreate(context) } catch (_: Throwable) { null }
    }

    // For now use the in-memory data source (no DB/HC dependency).
    val source: HealthDataSource = remember { InMemoryHealthDataSource() }

    // ViewModel factory because HomeViewModel has a constructor parameter.
    val factory = remember(source) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(source) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
            }
        }
    }

    val vm: HomeViewModel = viewModel(factory = factory)
    val ui by vm.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    // Refresh permissions whenever Home is resumed.
    LaunchedEffect(client) {
        if (client == null) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            vm.refreshPermissions(client)
        }
    }

    WelcomeScreen(navController = navController, ui = ui)
}
