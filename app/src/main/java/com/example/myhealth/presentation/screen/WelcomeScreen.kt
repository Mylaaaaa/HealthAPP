package com.example.myhealth.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myhealth.presentation.navigation.Screen
import com.example.myhealth.presentation.theme.HealthConnectTheme


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    navController: NavController,
    userName: String = "User",
    steps: Int = 7560,
    sleepHours: Double = 7.2,
    bodyWeightKg: Double = 54.8
) {
    Column(Modifier.fillMaxSize()) {

        // 顶部渐变 Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF4C6FFF), Color(0xFF7C9BFF))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "👋 Hi, $userName",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    CapsuleChip(text = "🔥 3-day streak")
                }
                Icon(
                    Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 双按钮 CTA（用 weight 均分）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CtaButton(
                title = "Start Exercise",
                icon = Icons.Filled.PlayArrow,            // 兼容性好
                bg = Color(0xFF4C6FFF),
                onClick = { navController.navigate(Screen.ExerciseSessions.route) },
                modifier = Modifier.weight(1f)
            )
            CtaButton(
                title = "Record Weight",
                icon = Icons.Filled.Accessibility,        // 兼容性替代 MonitorWeight
                bg = Color(0xFF00B894),
                onClick = { navController.navigate(Screen.InputReadings.route) },
                modifier = Modifier.weight(1f)
            )
        }

        // 横向统计胶囊
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item { StatPill(Icons.Filled.FitnessCenter, "$steps", "Steps", Color(0xFF4C6FFF)) }
            item { StatPill(Icons.Filled.Hotel, "${String.format("%.1f", sleepHours)} h", "Sleep", Color(0xFF7C4DFF)) }
            item { StatPill(Icons.Filled.Accessibility, "${String.format("%.1f", bodyWeightKg)} kg", "Weight", Color(0xFF00B894)) }
        }

        Text(
            text = "Explore",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
        )

        // 功能网格
        val entries = listOf(
            NavEntry(Screen.Dashboard,        "Dashboard",         Icons.Filled.Dashboard),
            NavEntry(Screen.ExerciseSessions,  "Exercise sessions", Icons.Filled.FitnessCenter),
            NavEntry(Screen.SleepSessions,     "Sleep sessions",    Icons.Filled.Hotel),
            NavEntry(Screen.Nutrition,         "Nutrition",         Icons.Filled.Restaurant),
            NavEntry(Screen.Mind,              "Mindfulness",       Icons.Filled.SelfImprovement),
            NavEntry(Screen.Reports,           "Reports",           Icons.Filled.Assessment),
            NavEntry(Screen.InputReadings,     "Record weight",     Icons.Filled.Accessibility),
            NavEntry(Screen.SettingsScreen,    "Settings",          Icons.Filled.Settings)
        ).filter { it.screen.hasMenuItem }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(entries) { e ->
                FeatureCard(
                    icon = e.icon,
                    title = e.title,
                    onClick = { navController.navigate(e.screen.route) }
                )
            }
        }
    }
}

/* ---------- 子组件 ---------- */

@Composable
private fun CapsuleChip(text: String) {
    Card(backgroundColor = Color.White.copy(alpha = 0.18f), elevation = 0.dp) {
        Text(text, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp)
    }
}

@Composable
private fun CtaButton(
    title: String,
    icon: ImageVector,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(backgroundColor = bg, elevation = 6.dp, modifier = modifier
        .height(44.dp)
        .clickable { onClick() }
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatPill(icon: ImageVector, value: String, label: String, tint: Color) {
    Card(elevation = 2.dp, backgroundColor = Color.White, modifier = Modifier.height(64.dp).widthIn(min = 140.dp)) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(label, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(elevation = 6.dp, backgroundColor = Color.White, modifier = Modifier
        .height(118.dp)
        .clickable { onClick() }
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color(0xFF4C6FFF), modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 14.sp)
        }
    }
}

/* ---------- 数据 & 预览 ---------- */

private data class NavEntry(val screen: Screen, val title: String, val icon: ImageVector)

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    HealthConnectTheme { WelcomeScreen(rememberNavController()) }
}
