package com.example.myhealth.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myhealth.R
import com.example.myhealth.presentation.navigation.Screen
import com.example.myhealth.presentation.theme.HealthConnectTheme

/**
 * Home screen (formerly WelcomeScreen).
 * Greeting + Quick actions + Today strip + 2-col icon grid navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    navController: NavController,
    username: String? = null,
    stepsToday: Int? = 4567,
    sleepHours: Float? = 6.8f,
    weightKg: Float? = 54.2f,
) {
    val name = username?.takeIf { it.isNotBlank() } ?: "User"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 👋 Greeting
        Text(
            text = "👋 Welcome back, $name!",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        // ⚡ Quick actions（把 weight 放在 Row 里传给子组件）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                label = "Start Exercise",
                icon = Icons.Filled.FitnessCenter,
                onClick = { navController.navigate(Screen.ExerciseSessions.route) },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Record Weight",
                icon = Icons.Filled.NoteAdd,
                onClick = { navController.navigate(Screen.InputReadings.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // 📊 Today mini strip
        TodayStrip(
            steps = stepsToday,
            sleepHours = sleepHours,
            weightKg = weightKg
        )

        Spacer(Modifier.height(16.dp))

        // 🧭 Icon grid navigation
        val entries = listOf(
            FeatureCardData(Screen.Dashboard,        Icons.Filled.Dashboard),
            FeatureCardData(Screen.ExerciseSessions, Icons.Filled.FitnessCenter),
            FeatureCardData(Screen.SleepSessions,    Icons.Filled.Hotel),
            FeatureCardData(Screen.Nutrition,        Icons.Filled.Restaurant),
            FeatureCardData(Screen.Mind,             Icons.Filled.SelfImprovement),
            FeatureCardData(Screen.Reports,          Icons.Filled.Assessment),
            FeatureCardData(Screen.InputReadings,    Icons.Filled.NoteAdd),
            FeatureCardData(Screen.SettingsScreen,   Icons.Filled.Settings),
        ).filter { it.screen.hasMenuItem }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { item ->
                NavIconCard(
                    title = stringResource(id = item.screen.titleId),
                    icon = item.icon
                ) {
                    navController.navigate(item.screen.route)
                }
            }
        }
    }
}

/* ================= Building blocks ================ */

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        elevation = ButtonDefaults.elevation(4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun TodayStrip(
    steps: Int?,
    sleepHours: Float?,
    weightKg: Float?
) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniMetric(label = "Steps",  value = steps?.toString() ?: "–")
            DividerDot()
            MiniMetric(label = "Sleep",  value = sleepHours?.let { "%.1f h".format(it) } ?: "–")
            DividerDot()
            MiniMetric(label = "Weight", value = weightKg?.let { "%.1f kg".format(it) } ?: "–")
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.caption)
    }
}

@Composable
private fun DividerDot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(
                MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.small
            )
            .alpha(0.7f)
    )
}

private data class FeatureCardData(
    val screen: com.example.myhealth.presentation.navigation.Screen,
    val icon: ImageVector
)

@Composable
private fun NavIconCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        elevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/* ================= Preview ================ */

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    HealthConnectTheme {
        val nav = rememberNavController()
        WelcomeScreen(
            navController = nav,
            username = "Alex",
            stepsToday = 5234,
            sleepHours = 7.2f,
            weightKg = 55.1f
        )
    }
}
