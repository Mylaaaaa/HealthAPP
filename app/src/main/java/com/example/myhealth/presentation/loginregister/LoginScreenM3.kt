package com.example.myhealth.presentation.loginregister

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Material 2 implementation of your login screen.
 *
 * Notes:
 * - Kept the original function name `LoginScreenM3` so callers don't need to change.
 * - Replaced all Material3 widgets with Material (M2) counterparts.
 * - Kept the logic and parameters unchanged.
 * - KeyboardOptions(keyboardType = Email) allows typing '.' normally.
 */
@Composable
fun LoginScreenM3(
    onLogin: (email: String, password: String) -> Boolean,
    onNavigateRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSubmit = email.contains("@") && pwd.length >= 6

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome back to MyHealth",
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it; error = null },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(
                            imageVector = if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )

            if (error != null) {
                Text(
                    error!!,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2
                )
            }

            Button(
                onClick = {
                    val ok = onLogin(email.trim(), pwd)
                    if (!ok) error = "Invalid email or password"
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) { Text("Login") }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onNavigateRegister) {
                Text("Create an account")
            }
        }
    }
}
