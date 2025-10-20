// file: app/src/main/java/com/example/myhealth/presentation/loginregister/RootApp.kt
package com.zihanwang.myhealth.presentation.loginregister

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.zihanwang.myhealth.data.HealthConnectManager
import com.zihanwang.myhealth.presentation.HealthConnectApp
import com.zihanwang.myhealth.presentation.theme.ThemeViewModel

/**
 * Root gate:
 * - If not logged in -> show AuthNavHost
 * - If logged in -> show your original HealthConnectApp
 *
 * ThemeViewModel is passed down so the whole app sits under one theme wrapper in MainActivity.
 */
@Composable
fun RootApp(
    healthConnectManager: HealthConnectManager,
    themeViewModel: ThemeViewModel
) {
    val isAuthed by remember { FakeAuthStore.loggedIn }
    val hcManager by rememberUpdatedState(healthConnectManager)

    if (isAuthed) {
        //  pass themeViewModel into HealthConnectApp
        HealthConnectApp(
            healthConnectManager = hcManager,
            themeViewModel = themeViewModel
        )
    } else {
        AuthNavHost(
            onAuthenticated = { FakeAuthStore.loggedIn.value = true }
        )
    }
}
