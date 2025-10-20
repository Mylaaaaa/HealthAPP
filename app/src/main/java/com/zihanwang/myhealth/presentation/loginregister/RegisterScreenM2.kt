package com.zihanwang.myhealth.presentation.loginregister

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Material 2 register screen.
 * - Keeps original function name to avoid touching callers.
 * - TopAppBar background matches status bar via theme.
 * - Email field allows '.' by not filtering while typing.
 */
@Composable
fun RegisterScreenM3(
    onRegister: (name: String, email: String, password: String) -> Boolean,
    onNavigateLogin: () -> Unit
) {
    // Use theme colors so the screen reacts to Light/Dark/System
    val colors = MaterialTheme.colors

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

    val focus = LocalFocusManager.current

    Scaffold(
        // 1) Make the page background follow the theme (light surface / dark near-black)
        backgroundColor = colors.background,

        // 2) Keep your original top app bar styling but driven by theme
        topBar = {
            TopAppBar(
                title = { Text("Create account", fontWeight = FontWeight.SemiBold) },
                backgroundColor = colors.primary,                      // theme primary
                contentColor = contentColorFor(colors.primary),        // proper contrast on primary
                elevation = 0.dp
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
            // All TextFields/Buttons already take their colors from MaterialTheme in M2.
            // No need to override unless you hard-coded a color.

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { text ->
                    email = text // do not trim/filter while typing
                    error = null
                },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focus.clearFocus() }),
                visualTransformation = VisualTransformation.None
            )

            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it; error = null },
                label = { Text("Password (min 6 chars)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    // Trailing icon color is provided by TextFieldDefaults (theme-aware)
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
                Text(
                    "Passwords do not match",
                    color = MaterialTheme.colors.error,                // theme error color
                    style = MaterialTheme.typography.body2
                )
            }
            if (error != null) {
                Text(
                    error!!,
                    color = MaterialTheme.colors.error,                // theme error color
                    style = MaterialTheme.typography.body2
                )
            }

            Button(
                onClick = {
                    val ok = onRegister(name.trim(), email.trim(), pwd) // unchanged logic
                    if (!ok) error = "Email already exists"
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) { Text("Create account") }

            TextButton(onClick = onNavigateLogin) {
                // TextButton uses primary by default, which adapts to theme
                Text("Back to sign in")
            }
        }
    }
}

