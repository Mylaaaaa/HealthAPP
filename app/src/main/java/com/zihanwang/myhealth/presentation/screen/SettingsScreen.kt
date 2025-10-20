package com.zihanwang.myhealth.presentation.screen

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.zihanwang.myhealth.presentation.theme.ThemeMode
import com.zihanwang.myhealth.R

@Composable
fun SettingsScreen(
    revokeAllPermissions: () -> Unit,
    // New optional parameters (safe defaults keep old code working)
    currentThemeMode: ThemeMode = ThemeMode.System,
    onThemeChange: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* --------------------- Appearance / Theme section --------------------- */
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.subtitle1
                )
                Spacer(Modifier.height(8.dp))

                // Radio options; clicking one calls onThemeChange
                ThemeRadioRow(
                    label = "System default",
                    selected = currentThemeMode == ThemeMode.System,
                    onClick = { onThemeChange(ThemeMode.System) }
                )
                ThemeRadioRow(
                    label = "Light",
                    selected = currentThemeMode == ThemeMode.Light,
                    onClick = { onThemeChange(ThemeMode.Light) }
                )
                ThemeRadioRow(
                    label = "Dark",
                    selected = currentThemeMode == ThemeMode.Dark,
                    onClick = { onThemeChange(ThemeMode.Dark) }
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tip: System default respects your device appearance settings.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        /* ---------------- Health Connect settings (kept as-is) ---------------- */
        Button(onClick = {
            val settingsIntent = Intent().apply {
                action = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
            }
            context.startActivity(settingsIntent)
        }) {
            Text(text = stringResource(id = R.string.manage))
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = { revokeAllPermissions() }) {
            Text(text = stringResource(id = R.string.disconnect))
        }
    }
}

/**
 * Small reusable row for a labeled radio button.
 * Keeps spacing and ripple consistent with Material components.
 */
@Composable
private fun ThemeRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.weight(1f)
        )
    }
}


