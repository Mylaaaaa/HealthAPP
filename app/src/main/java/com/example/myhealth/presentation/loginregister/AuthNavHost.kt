package com.example.myhealth.presentation.loginregister

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Auth-only NavHost.
 * Shows Login -> Register. When auth succeeds, call onAuthenticated().
 */
@Composable
fun AuthNavHost(
    onAuthenticated: () -> Unit
) {
    val nav: NavHostController = rememberNavController()

    NavHost(navController = nav, startDestination = "login") {

        composable("login") {
            LoginScreenM3(
                // Return Boolean for the screen to show/hide error;
                // if success, notify the host.
                onLogin = { email, password ->
                    val ok = FakeAuthStore.login(email, password)
                    if (ok) onAuthenticated()
                    ok
                },
                onNavigateRegister = { nav.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreenM3(
                onRegister = { name, email, password ->
                    val ok = FakeAuthStore.register(name, email, password)
                    if (ok) onAuthenticated()
                    ok
                },
                onNavigateLogin = { nav.popBackStack() }
            )
        }
    }
}
