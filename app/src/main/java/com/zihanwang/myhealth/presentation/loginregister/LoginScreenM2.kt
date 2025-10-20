package com.zihanwang.myhealth.presentation.loginregister

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Material 2 login screen.
 * - Keeps original function name so callers don't change.
 * - TopAppBar/StatusBar are color-aligned to MaterialTheme.colors.primary.
 * - Email field does not filter/trim while typing -> '.' is allowed.
 */
@Composable
fun LoginScreenM3(
    onLogin: (email: String, password: String) -> Boolean,
    onNavigateRegister: () -> Unit
) {
    val colors = MaterialTheme.colors

    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canSubmit = email.contains("@") && pwd.length >= 6

    val focus = LocalFocusManager.current

    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        // App bar uses theme primary; icons/text derive from contentColorFor(primary)
        topBar = {
            TopAppBar(
                title = { Text("Sign in", fontWeight = FontWeight.SemiBold) },
                backgroundColor = colors.primary,
                contentColor = contentColorFor(colors.primary),
                elevation = 0.dp
            )
        },

        modifier = Modifier.background(colors.background)
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Headline follows onSurface
            Text(
                "Welcome back to MyHealth",
                style = MaterialTheme.typography.subtitle1,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )

            // ---------------- Email ----------------
            OutlinedTextField(
                value = email,
                onValueChange = { text ->
                    // Do not trim/filter while typing, to avoid blocking dots
                    email = text
                    error = null
                },
                label = { Text("Email", color = colors.onSurface.copy(alpha = 0.7f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focus.clearFocus() }
                ),
                visualTransformation = VisualTransformation.None,
                // Make all field colors theme-aware for Light/Dark
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = colors.onSurface,
                    cursorColor = colors.primary,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurface.copy(alpha = 0.4f),
                    focusedLabelColor = colors.primary,
                    unfocusedLabelColor = colors.onSurface.copy(alpha = 0.7f),
                    placeholderColor = colors.onSurface.copy(alpha = 0.6f),
                    leadingIconColor = colors.onSurface,
                    trailingIconColor = colors.onSurface
                )
            )

            // ---------------- Password ----------------
            OutlinedTextField(
                value = pwd,
                onValueChange = { pwd = it; error = null },
                label = { Text("Password", color = colors.onSurface.copy(alpha = 0.7f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPwd = !showPwd }) {
                        Icon(
                            imageVector = if (showPwd) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = colors.onSurface
                        )
                    }
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = colors.onSurface,
                    cursorColor = colors.primary,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurface.copy(alpha = 0.4f),
                    focusedLabelColor = colors.primary,
                    unfocusedLabelColor = colors.onSurface.copy(alpha = 0.7f),
                    placeholderColor = colors.onSurface.copy(alpha = 0.6f),
                    trailingIconColor = colors.onSurface
                )
            )

            // Error text uses theme error color
            if (error != null) {
                Text(
                    error!!,
                    color = colors.error,
                    style = MaterialTheme.typography.body2
                )
            }

            // Buttons already pick proper colors from theme
            Button(
                onClick = {
                    val ok = onLogin(email.trim(), pwd) // Only trim on submit
                    if (!ok) error = "Invalid email or password"
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) { Text("Login") }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onNavigateRegister) {
                Text("Create an account", color = colors.primary)
            }
        }
    }
}

