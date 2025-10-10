package com.example.myhealth.presentation.loginregister

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Small auth-only NavHost.
 * RootApp decides whether to show this or the real app based on FakeAuthStore.loggedIn.
 */
@Composable
fun AuthNavHost(
    onAuthenticated: () -> Unit
) {
    val nav: NavHostController = rememberNavController()

    NavHost(navController = nav, startDestination = "login") {

        composable("login") {
            LoginScreenM3(
                onLoginSuccess = { onAuthenticated() },
                onNavigateRegister = { nav.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreenM3(
                onRegisterSuccess = { onAuthenticated() },
                onNavigateLogin = { nav.popBackStack() }
            )
        }
    }
}
