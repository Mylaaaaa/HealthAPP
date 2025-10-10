package com.example.myhealth.presentation.loginregister

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AuthNavHost(
    onAuthenticated: () -> Unit
) {
    val nav: NavHostController = rememberNavController()
    val store = remember { FakeAuthStore }

    NavHost(navController = nav, startDestination = "login") {

        composable("login") {
            LoginScreenM3(
                onLogin = { email, pwd ->
                    if (store.login(email, pwd)) {
                        onAuthenticated()
                        true      // ← 关键：返回 true
                    } else {
                        false
                    }
                },
                onNavigateRegister = { nav.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreenM3(
                onRegister = { name, email, pwd ->
                    if (store.register(name, email, pwd)) {
                        onAuthenticated()  // 或者 nav.popBackStack()
                        true               // ← 返回 true
                    } else {
                        false
                    }
                },
                onNavigateLogin = { nav.popBackStack() }
            )
        }
    }
}
