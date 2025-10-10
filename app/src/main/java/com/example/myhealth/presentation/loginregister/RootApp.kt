package com.example.myhealth.presentation.loginregister

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.HealthConnectApp

/**
 * Root gate:
 * - If not logged in -> show AuthNavHost
 * - If logged in -> show your original HealthConnectApp
 *
 * It observes FakeAuthStore.loggedIn.value. Logout will flip it to false,
 * causing RootApp to recompose and show AuthNavHost automatically.
 */
@Composable
fun RootApp(healthConnectManager: HealthConnectManager) {
    // Observe login state so UI switches automatically
    val isAuthed by remember { FakeAuthStore.loggedIn }
    val hcManager by rememberUpdatedState(healthConnectManager)

    if (isAuthed) {
        // Your original app (no changes needed inside)
        HealthConnectApp(healthConnectManager = hcManager)
    } else {
        // Auth flow
        AuthNavHost(onAuthenticated = {
            // When login/register succeeds, set store to true
            // (Login/Register already set it; this is a safety net)
            FakeAuthStore.loggedIn.value = true
        })
    }
}
