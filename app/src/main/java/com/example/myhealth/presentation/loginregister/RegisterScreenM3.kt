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
import androidx.compose.ui.unit.dp

/**
 * Register screen (Material 3).
 * - Email uses ASCII keyboard + ASCII filtering so '.' can be typed with any IME.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreenM3(
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val validEmail = email.contains("@")
    val pwdOk = pwd.length >= 6
    val same = pwd == confirm
    val canSubmit = name.isNotBlank() && validEmail && pwdOk && same

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Create account", fontWeight = FontWeight.SemiBold) })
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { s ->
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
                label = { Text("Password (min 6 chars)") },
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

            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                label = { Text("Confirm password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            imageVector = if (showConfirm) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )

            if (!same && confirm.isNotEmpty()) {
                Text("Passwords do not match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val ok = FakeAuthStore.register(name.trim(), email.trim(), pwd)
                    if (ok) onRegisterSuccess() else error = "Email already exists"
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Create account") }

            TextButton(onClick = onNavigateLogin) { Text("Back to sign in") }
        }
    }
}
