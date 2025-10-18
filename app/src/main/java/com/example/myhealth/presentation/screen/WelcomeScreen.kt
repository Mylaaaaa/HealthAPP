package com.example.myhealth.presentation.screen

import androidx.compose.material.TextButton
import com.example.myhealth.presentation.loginregister.FakeAuthStore
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myhealth.presentation.home.HomeUiState
import com.example.myhealth.presentation.navigation.Screen
import com.example.myhealth.presentation.theme.HealthConnectTheme
import kotlin.math.max
import kotlin.math.min
import androidx.compose.material.Colors
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    navController: NavController,
    ui: HomeUiState,
    userName: String = "User"
) {
    val colors = MaterialTheme.colors

    val steps = ui.steps
    val sleepHours = ui.sleepHours
    val bodyWeightKg = ui.bodyWeightKg

    val weeklySteps = ui.weeklySteps
    val weeklySleepHours = ui.weeklySleep
    val weeklyWeight = ui.weeklyWeight

    val stepGoal = ui.stepGoal
    val activeMinToday = ui.activeMinToday
    val activeMinGoal = ui.activeMinGoal
    val sleepTodayHours = ui.sleepTodayHours
    val sleepGoalHours = ui.sleepGoalHours

    val hasAllPermissions = ui.permissions.hasAll
    val hasBackgroundReadPermission = ui.permissions.hasBackgroundRead
    val lastWeighInDaysAgo = ui.lastWeighInDaysAgo
    val currentStreakDays = ui.currentStreakDays
    val showBadgeUnlocked = ui.showBadgeUnlocked

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {

        // ---------- Gradient hero ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (colors.isLight)
                            listOf(colors.primary, colors.primary.copy(alpha = 0.70f))
                        else
                            listOf(colors.primary.copy(alpha = 0.85f), colors.background)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLoggedIn = FakeAuthStore.currentUserEmail != null
                val displayName = FakeAuthStore.currentUserName() ?: userName

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "👋 Hi, $displayName",
                        color = colors.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    CapsuleChip(text = "🔥 ${currentStreakDays}-day streak")
                }

                if (!isLoggedIn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = {
                            // 切换到登录：RootApp 会显示 AuthNavHost
                            FakeAuthStore.logout()
                        }) { Text("Login", color = Color.White) }

                        TextButton(onClick = {
                            // 切换到注册：RootApp 里先登出再显示 AuthNavHost；在 Auth 里选择 register
                            FakeAuthStore.logout()
                        }) { Text("Register", color = Color.White) }
                    }
                } else {
                    TextButton(onClick = {
                        FakeAuthStore.logout() // RootApp 观察到 false 后自动显示 AuthNavHost（登录页）
                    }) {
                        Text("Logout", color = Color.White)
                    }
                }
            }

        }

        // ---------- Permission banners ----------
        if (!hasAllPermissions) {
            PermissionBanner(
                text = "Some permissions are missing. Grant permissions to unlock auto-tracking.",
                actionText = "Grant permissions",
                icon = Icons.Filled.Error
            ) { navController.navigate(Screen.SettingsScreen.route) }
            Spacer(Modifier.height(8.dp))
        }
        if (!hasBackgroundReadPermission) {
            PermissionBanner(
                text = "Background read is off. Enable to keep data up to date.",
                actionText = "Enable background read"
            ) { navController.navigate(Screen.SettingsScreen.route) }
            Spacer(Modifier.height(4.dp))
        }

        // ---------- Reminder (no duplicate CTA) ----------
        val stepsRemaining = max(0, stepGoal - steps)
        val reminderText = when {
            stepsRemaining > 0 -> "You're ${stepsRemaining} steps away from today's goal."
            lastWeighInDaysAgo > 2 -> "No weigh-in for ${lastWeighInDaysAgo} days. Log a weight?"
            else -> "Nice momentum! Keep it up today."
        }
        ReminderCard(
            text = reminderText,
            primaryText = "start exercise",        // key change to avoid duplicating Start Exercise
            secondaryText = "Record Weight",
            onPrimary = { navController.navigate(Screen.ExerciseSessions.route) },
            onSecondary = { navController.navigate(Screen.InputReadings.route) }
        )

        // ---------- Stat pills ----------
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item { StatPill(Icons.Filled.FitnessCenter, "$steps", "Steps", Color(0xFF4C6FFF)) }
            item { StatPill(Icons.Filled.Hotel, "${String.format(java.util.Locale.US, "%.1f", sleepHours)} h", "Sleep", Color(0xFF7C4DFF)) }
            item { StatPill(Icons.Filled.Accessibility, "${String.format(java.util.Locale.US, "%.1f", bodyWeightKg)} kg", "Weight", Color(0xFF00B894)) }
        }

        // ---------- Weekly trends ----------
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrendMiniCard(
                title = "Steps",
                values = weeklySteps.map { it.toFloat() },
                unit = "",
                wowText = wowLabel(
                    current = weeklySteps.lastOrNull()?.toFloat() ?: 0f,
                    prev = weeklySteps.dropLast(1).lastOrNull()?.toFloat() ?: 0f
                ),
                accent = Color(0xFF4C6FFF),
                onClick = { navController.navigate(Screen.ExerciseSessions.route) },
                modifier = Modifier.weight(1f)
            )
            TrendMiniCard(
                title = "Sleep",
                values = weeklySleepHours.map { it.toFloat() },
                unit = "h",
                wowText = wowLabel(
                    current = weeklySleepHours.lastOrNull()?.toFloat() ?: 0f,
                    prev = weeklySleepHours.dropLast(1).lastOrNull()?.toFloat() ?: 0f
                ),
                accent = Color(0xFF7C4DFF),
                onClick = { navController.navigate(Screen.SleepSessions.route) },
                modifier = Modifier.weight(1f)
            )
            TrendMiniCard(
                title = "Weight",
                values = weeklyWeight.map { it.toFloat() },
                unit = "kg",
                wowText = wowLabel(
                    current = weeklyWeight.lastOrNull()?.toFloat() ?: 0f,
                    prev = weeklyWeight.dropLast(1).lastOrNull()?.toFloat() ?: 0f
                ),
                accent = Color(0xFF00B894),
                onClick = { navController.navigate(Screen.InputReadings.route) },
                modifier = Modifier.weight(1f)
            )
        }

        // ---------- Feature grid (4 columns, compact & lively) ----------
        // ---------- Feature grid (4 columns, compact & lively) ----------
        val entries = listOf(
            NavEntry(Screen.ExerciseSessions,  "Exercise sessions", Icons.Filled.FitnessCenter),
            NavEntry(Screen.SleepSessions,     "Sleep sessions",    Icons.Filled.Hotel),
            NavEntry(Screen.Nutrition,         "Nutrition",         Icons.Filled.Restaurant),
            NavEntry(Screen.Mind,              "Mindfulness",       Icons.Filled.SelfImprovement),
            NavEntry(Screen.InputReadings,     "Record weight",     Icons.Filled.Accessibility),
            NavEntry(Screen.SettingsScreen,    "Settings",          Icons.Filled.Settings)
        )

        val columns = 3
        val itemHeightDp = 108
        val vSpacing = 12
        val contentPaddingV = 12
        val rows = (entries.size + columns - 1) / columns
        val gridHeight = (rows * itemHeightDp + (rows - 1) * vSpacing + contentPaddingV).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            verticalArrangement = Arrangement.spacedBy(vSpacing.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            items(entries.size) { index ->
                val e = entries[index]
                val tint = accentFor(index)
                FeatureCard(
                    icon = e.icon,
                    title = e.title,
                    tint = tint,
                    onClick = { navController.navigate(e.screen.route) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))


        // ---------- Streak & Badge ----------
        StreakBadgeRow(
            streakDays = currentStreakDays,
            showBadgeUnlocked = showBadgeUnlocked
        )

        // ---------- Goal rings ----------
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoalRingCard(
                label = "Steps",
                valueLabel = "$steps / $stepGoal",
                ratio = safeRatio(steps.toFloat(), stepGoal.toFloat()),
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.weight(1f)
            )
            GoalRingCard(
                label = "Active min",
                valueLabel = "$activeMinToday / $activeMinGoal",
                ratio = safeRatio(activeMinToday.toFloat(), activeMinGoal.toFloat()),
                tint = MaterialTheme.colors.secondary,
                modifier = Modifier.weight(1f)
            )
            GoalRingCard(
                label = "Sleep",
                valueLabel = "${String.format(java.util.Locale.US, "%.1f", sleepTodayHours)} / ${String.format(java.util.Locale.US, "%.1f", sleepGoalHours)} h",
                ratio = safeRatio(sleepTodayHours.toFloat(), sleepGoalHours.toFloat()),
                tint = MaterialTheme.colors.primary.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f)
            )
        }

        // ---------- Explore ----------
        Text(
            text = "Explore",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold)
        )

        // ---------- Health tip ----------
        HealthTipCard(
            tip = "Avoid screens 1 hour before bed.",
            actionText = "Learn more"
        ) { navController.navigate(Screen.SleepSessions.route) }

        // ---------- Recent activity ----------
        RecentActivitySection(
            items = ui.recent.map {
                RecentItem(
                    icon = when (it.type) {
                        com.example.myhealth.data.ActivityType.EXERCISE -> Icons.Filled.FitnessCenter
                        com.example.myhealth.data.ActivityType.WEIGHT -> Icons.Filled.Accessibility
                        com.example.myhealth.data.ActivityType.SLEEP -> Icons.Filled.Hotel
                    },
                    title = it.title,
                    time = it.timeText
                )
            },
            onViewAll = { navController.navigate(Screen.ExerciseSessions.route) },
            onItemClick = { icon ->
                when (icon) {
                    Icons.Filled.FitnessCenter -> navController.navigate(Screen.ExerciseSessions.route)
                    Icons.Filled.Accessibility -> navController.navigate(Screen.InputReadings.route)
                    Icons.Filled.Hotel -> navController.navigate(Screen.SleepSessions.route)
                    else -> {}
                }
            }
        )
    }
}

/* -------------------- Reused components -------------------- */

@Composable
private fun CapsuleChip(text: String) {
    val colors = MaterialTheme.colors
    val isDark = !colors.isLight

    Card(
        backgroundColor = if (isDark)
            Color.White.copy(alpha = 0.12f) // darker background, subtle
        else
            colors.primary.copy(alpha = 0.12f),
        elevation = 0.dp,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text,
            color = if (isDark) Color.White else colors.onPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
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
    Card(
        backgroundColor = bg,
        elevation = 6.dp,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
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
    Card(
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier
            .height(64.dp)
            .widthIn(min = 140.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colors.onSurface)
                Text(label, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}

/**
 * Feature card with soft icon accent, centered 2-line title, and press feedback.
 * Uses scale animation + no Indication (no deprecated ripple).
 */
/**
 * Feature card with soft icon accent, centered title, and press feedback.
 * Single-word titles stay on one line (ellipsis); multi-word titles may wrap to two lines.
 */
/**
 * Feature card with soft icon accent, press feedback, and smart title sizing.
 * - Single-word titles: auto-fit to ONE line (no ellipsis, no wrap).
 * - Multi-word titles: up to TWO lines, neat center alignment.
 */
@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.98f else 1f, label = "card-scale")

    val isSingleWord = !title.contains(' ')
    // Start a little larger, we’ll shrink only if needed
    var fontSize by remember(title) { mutableStateOf(if (isSingleWord) 14.sp else 13.sp) }
    val minFont = if (isSingleWord) 11.sp else 12.sp

    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier
            .height(108.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with soft colored circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Title: auto-fit single-word to one line
            Text(
                text = title,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                // For single word: 1 line + Clip (no ellipsis) + auto downsize
                // For multi word: up to 2 lines
                maxLines = if (isSingleWord) 1 else 2,
                overflow = if (isSingleWord) TextOverflow.Clip else TextOverflow.Clip,
                fontSize = fontSize,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth(),
                onTextLayout = { layout ->
                    if (isSingleWord && layout.hasVisualOverflow && fontSize > minFont) {
                        fontSize = TextUnit(fontSize.value - 1f, fontSize.type)
                    }
                }
            )
        }
    }
}

/** Per-index accent color so cards look less uniform. */
private fun accentFor(index: Int): Color {
    val palette = listOf(
        Color(0xFF4C6FFF), // blue
        Color(0xFF7C4DFF), // violet
        Color(0xFF00B894), // green
        Color(0xFFFF7043), // orange
        Color(0xFF26C6DA), // cyan
        Color(0xFFEC407A), // pink
        Color(0xFF66BB6A), // mint
        Color(0xFF5C6BC0)  // indigo
    )
    return palette[index % palette.size]
}

/* -------------------- Trend sparklines -------------------- */

@Composable
private fun TrendMiniCard(
    title: String,
    values: List<Float>,
    unit: String,
    wowText: Pair<String, Color>,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = modifier
            .height(96.dp)
            .clickable { onClick() }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                val wowColor = if (wowText.first.isEmpty())
                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                else
                    wowText.second
                Text(wowText.first, color = wowColor, fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            Sparkline(
                data = values,
                strokeWidth = 2.dp,
                tint = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
            if (values.isNotEmpty()) {
                val end = values.last()
                Text(
                    "${trimNumber(end)} $unit",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Sparkline(
    data: List<Float>,
    strokeWidth: Dp,
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val minV = data.minOrNull() ?: 0f
        val maxV = data.maxOrNull() ?: 1f
        val span = max(1e-6f, maxV - minV)
        val w = size.width
        val h = size.height
        val stepX = w / (data.size - 1)
        val path = Path()
        data.forEachIndexed { idx, v ->
            val x = idx * stepX
            val y = h - ((v - minV) / span) * h
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = tint, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
        val lastY = h - ((data.last() - minV) / span) * h
        drawCircle(color = tint, radius = 3.5.dp.toPx(), center = Offset(w, lastY))
    }
}

private fun trimNumber(v: Float): String =
    if (kotlin.math.abs(v - v.toInt()) < 1e-4) v.toInt().toString()
    else String.format(java.util.Locale.US, "%.1f", v)

/* -------------------- Reminder -------------------- */

@Composable
private fun ReminderCard(
    text: String,
    primaryText: String,
    secondaryText: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Card(
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CtaButton(primaryText, Icons.Filled.Dashboard, MaterialTheme.colors.primary, onPrimary)
                CtaButton(secondaryText, Icons.Filled.Accessibility, Color(0xFF00B894), onSecondary)
            }
        }
    }
}

/* -------------------- Streak & Badge -------------------- */

@Composable
private fun StreakBadgeRow(streakDays: Int, showBadgeUnlocked: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---- Left card: streak ----
        Card(
            elevation = 3.dp,
            backgroundColor = MaterialTheme.colors.surface, // follow theme
            modifier = Modifier
                .weight(1f)
                .height(70.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFFFFA726))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Streak", fontWeight = FontWeight.SemiBold)
                    Text(
                        "$streakDays days",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ---- Right card: badge ----
        Card(
            elevation = 3.dp,
            backgroundColor = MaterialTheme.colors.surface, // ✅ follow theme
            modifier = Modifier
                .weight(1f)
                .height(70.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Badge,
                    null,
                    tint = if (showBadgeUnlocked) Color(0xFF66BB6A) else Color(0xFFBDBDBD)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Badge", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (showBadgeUnlocked) "New badge unlocked!" else "Keep going to unlock",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), // ✅ changed
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/* -------------------- Goal rings -------------------- */

private fun safeRatio(now: Float, goal: Float): Float =
    if (goal <= 0f) 0f else min(1f, max(0f, now / goal))

@Composable
private fun GoalRingCard(
    label: String,
    valueLabel: String,
    ratio: Float,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 3.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = modifier.height(112.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RingProgress(progress = ratio, size = 46.dp, strokeWidth = 6.dp, tint = tint)
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                valueLabel,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun RingProgress(progress: Float, size: Dp, strokeWidth: Dp, tint: Color) {
    val track = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val sweep = 360f * progress
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

/* -------------------- Health tip -------------------- */

@Composable
private fun HealthTipCard(
    tip: String,
    actionText: String,
    onClick: () -> Unit
) {
    Card(
        elevation = 3.dp,
        backgroundColor = MaterialTheme.colors.surface, // ✅ Use theme-based surface color for light/dark mode
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                // Main title for the tip section
                Text(
                    "Daily tip",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface // ✅ Adapt text color to theme
                )
                // The actual health tip text (slightly dimmed)
                Text(
                    tip,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), // ✅ Dimmed for readability
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            // Action text ("Learn more") – keep blue for consistent accent
            Text(
                actionText,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }

    // Spacing below the card
    Spacer(Modifier.height(8.dp))
}

/* -------------------- Recent activity -------------------- */

data class RecentItem(val icon: ImageVector, val title: String, val time: String)

@Composable
private fun RecentActivitySection(
    items: List<RecentItem>,
    onViewAll: () -> Unit,
    onItemClick: (ImageVector) -> Unit
) {
    val colors = MaterialTheme.colors

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Section title follows theme text color
        Text(
            "Recent",
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        // Link color uses primary so it pops in both light/dark
        Text(
            "View all",
            color = colors.primary, // was Color(0xFF4C6FFF)
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onViewAll() }
        )
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        items.take(3).forEach { item ->
            Card(
                elevation = 2.dp,
                backgroundColor = colors.surface, // was Color.White
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp)
                    .clickable { onItemClick(item.icon) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .semantics {
                            contentDescription = "${item.title}: ${item.time}".trim()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use primary for icon tint to keep brand accent
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = colors.primary
                    )

                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        // Title uses onSurface for proper contrast
                        Text(
                            item.title,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Secondary text uses a dimmed onSurface to de-emphasize
                        Text(
                            item.time,
                            color = colors.onSurface.copy(alpha = 0.6f), // was Color.Gray
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}


/* -------------------- Permission banner -------------------- */

@Composable
private fun PermissionBanner(
    text: String,
    actionText: String,
    icon: ImageVector = Icons.Filled.Error,
    onClick: () -> Unit
) {
    Card(
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colors.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(text = text, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colors.onSurface)
            Spacer(Modifier.width(8.dp))
            Text(actionText, color = MaterialTheme.colors.primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

/* -------------------- WoW label helper -------------------- */
private fun wowLabel(current: Float, prev: Float, colors: androidx.compose.material.Colors): Pair<String, androidx.compose.ui.graphics.Color> {
    if (prev <= 0f) return "" to colors.onSurface.copy(alpha = 0.6f)
    val change = ((current - prev) / kotlin.math.max(1e-6f, prev)) * 100f
    val arrow  = if (change >= 0) "↑" else "↓"
    val tint   = if (change >= 0) colors.secondary else colors.error
    val text   = String.format(java.util.Locale.US, "%s %.0f%%", arrow, kotlin.math.abs(change))
    return text to tint
}

@androidx.compose.runtime.Composable
private fun wowLabel(current: Float, prev: Float): Pair<String, androidx.compose.ui.graphics.Color> {
    return wowLabel(current, prev, androidx.compose.material.MaterialTheme.colors)
}


/* -------------------- Grid model -------------------- */

private data class NavEntry(val screen: Screen, val title: String, val icon: ImageVector)

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    HealthConnectTheme {
        WelcomeScreen(
            navController = rememberNavController(),
            ui = HomeUiState()
        )
    }
}
