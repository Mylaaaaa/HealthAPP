package com.example.myhealth.presentation.loginregister

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.example.myhealth.data.HealthConnectManager
import com.example.myhealth.presentation.HealthConnectApp
import com.example.myhealth.presentation.theme.ThemeViewModel

@Composable
fun RootApp(
    healthConnectManager: HealthConnectManager,
    themeViewModel: ThemeViewModel // <-- added
) {
    // Observe login state so UI switches automatically
    val isAuthed by remember { FakeAuthStore.loggedIn }
    val hcManager by rememberUpdatedState(healthConnectManager)

    if (isAuthed) {
        // Main app (with theme support)
        HealthConnectApp(
            healthConnectManager = hcManager,
            themeViewModel = themeViewModel // <-- pass down
        )
    } else {
        // Auth flow
        AuthNavHost(onAuthenticated = {
            FakeAuthStore.loggedIn.value = true
        })
    }
}
