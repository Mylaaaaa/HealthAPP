package com.example.myhealth.presentation.loginregister

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
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
 * Login screen (Material 3).
 * - Email uses ASCII keyboard + ASCII filtering so '.' can be typed with any IME.
 * - onLoginSuccess(): call after FakeAuthStore.login() succeeds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenM3(
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = email.contains("@") && pwd.length >= 6

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Sign in", fontWeight = FontWeight.SemiBold) })
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
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = { s ->
                    // Keep ASCII only to avoid full-width punctuation like '。'
                    email = s.filter { it.code in 33..126 }
                    error = null
                },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
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
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val ok = FakeAuthStore.login(email.trim(), pwd)
                    if (ok) onLoginSuccess() else error = "Invalid email or password"
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Login") }

            TextButton(onClick = onNavigateRegister) { Text("Create an account") }
        }
    }
}
