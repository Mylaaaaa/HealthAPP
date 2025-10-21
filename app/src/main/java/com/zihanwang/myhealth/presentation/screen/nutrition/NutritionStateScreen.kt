package com.zihanwang.myhealth.presentation.screen.nutrition

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.zihanwang.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.zihanwang.myhealth.presentation.screen.nutrition.db.FoodEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun NutritionStateScreen(
    // Keep AndroidViewModel factory as-is (your ViewModel extends AndroidViewModel)
    vm: NutritionViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    /* ---------- Reactive states from ViewModel (unchanged) ---------- */
    val totals by vm.totals.collectAsState()
    val weeklyTotals by vm.weeklyTotals.collectAsState()
    val kcalGoal by vm.kcalGoal.collectAsState()
    val macroTargets by vm.macroTargets.collectAsState()
    val conditions by vm.allConditions.collectAsState()
    val foods by vm.recommendedFoods.collectAsState()

    /* ---------- Anchor all weekly charts to the REAL "today" ---------- */
    // Do NOT remember this; it must update when the calendar day changes.
    val anchorToday = LocalDate.now()

    // Re-run weekly DB subscription whenever today changes.
    LaunchedEffect(anchorToday) { vm.loadWeeklyTotals(center = anchorToday) }

    // Always render exactly 7 days ending at "today". Missing days are zero-filled.
    val weekly7 = remember(weeklyTotals, anchorToday) {
        padTo7Days(weeklyTotals, anchor = anchorToday)
    }

    Scaffold { inner ->
        // Use LazyColumn so the State page can scroll on smaller screens.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DailyEnergyCard(current = totals.kcal, goal = kcalGoal)
            }
            item {
                DailyMacroTargetsCard(t = totals, targets = macroTargets)
            }
            item {
                QualityGuardCard(sugar = totals.sugar, satFat = totals.satFat, sodium = totals.sodium)
            }
            item {
                WeeklyCaloriesTrendCard(list = weekly7, goal = kcalGoal, anchor = anchorToday)
            }
            item {
                WeeklyMacroStackedCard(list = weekly7, anchor = anchorToday)
            }
            item {
                WeeklyQualityBarsCard(list = weekly7, anchor = anchorToday)
            }
            item {
                ConditionInsightsCard(conditions = conditions, totals = totals, foods = foods)
            }
        }
    }
}

/* ---------------- DAILY CARDS (kept same UI) ---------------- */

@Composable
private fun DailyEnergyCard(current: Int, goal: Int) {
    val diff = current - goal
    val pct = diff.toFloat() / goal.coerceAtLeast(1)
    val (status, color) = when {
        pct > 0.10f -> "Above target ⚠️" to MaterialTheme.colors.error
        pct < -0.10f -> "Below target ⚠️" to MaterialTheme.colors.secondary
        else -> "On target 🎯" to MaterialTheme.colors.primary
    }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Energy Balance", fontWeight = FontWeight.Bold)
            Text("$current / $goal kcal")
            LinearProgressIndicator(
                progress = (current.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )
            Text(status, color = color)
        }
    }
}

@Composable
private fun DailyMacroTargetsCard(
    t: NutritionRepository.Totals,
    targets: NutritionViewModel.MacroTargets
) {
    val pDiff = (t.protein - targets.p) / targets.p.coerceAtLeast(1f)
    val pLabel = when {
        pDiff > 0.15f -> "Protein High"
        pDiff < -0.15f -> "Protein Low"
        else -> "Protein OK"
    }
    val pColor = when {
        pDiff > 0.15f -> MaterialTheme.colors.error
        pDiff < -0.15f -> MaterialTheme.colors.secondary
        else -> MaterialTheme.colors.primary
    }

    val cDiff = (t.carb - targets.c) / targets.c.coerceAtLeast(1f)
    val cLabel = when {
        cDiff > 0.15f -> "Carbs High"
        cDiff < -0.15f -> "Carbs Low"
        else -> "Carbs OK"
    }
    val cColor = when {
        cDiff > 0.15f -> MaterialTheme.colors.error
        cDiff < -0.15f -> MaterialTheme.colors.secondary
        else -> MaterialTheme.colors.primary
    }

    val fDiff = (t.fat - targets.f) / targets.f.coerceAtLeast(1f)
    val fLabel = when {
        fDiff > 0.15f -> "Fat High"
        fDiff < -0.15f -> "Fat Low"
        else -> "Fat OK"
    }
    val fColor = when {
        fDiff > 0.15f -> MaterialTheme.colors.error
        fDiff < -0.15f -> MaterialTheme.colors.secondary
        else -> MaterialTheme.colors.primary
    }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Macro Targets", fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("$pLabel: ${t.protein.toInt()} g", color = pColor)
                Text("$cLabel: ${t.carb.toInt()} g",    color = cColor)
                Text("$fLabel: ${t.fat.toInt()} g",     color = fColor)
            }
        }
    }
}

@Composable
private fun QualityGuardCard(sugar: Float, satFat: Float, sodium: Int) {
    val sugarGoal = 50f
    val fatGoal = 20f
    val sodiumGoal = 2300

    val sugarColor  = if (sugar  > sugarGoal)  MaterialTheme.colors.error else MaterialTheme.colors.primary
    val satFatColor = if (satFat > fatGoal)    MaterialTheme.colors.error else MaterialTheme.colors.primary
    val sodiumColor = if (sodium > sodiumGoal) MaterialTheme.colors.error else MaterialTheme.colors.primary

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Quality Guard", fontWeight = FontWeight.Bold)
            Text("Sugar: ${sugar.toInt()} g (≤ ${sugarGoal.toInt()} g)", color = sugarColor)
            Text("Sat Fat: ${satFat.toInt()} g (≤ ${fatGoal.toInt()} g)", color = satFatColor)
            Text("Sodium: $sodium mg (≤ $sodiumGoal mg)", color = sodiumColor)
        }
    }
}

/* ---------------- WEEKLY CARDS (anchored to today) ---------------- */

/** Calorie bars for last 7 days; weekday labels derived from [anchor] so "today" is guaranteed. */
@Composable
private fun WeeklyCaloriesTrendCard(
    list: List<NutritionViewModel.DailyTotals>,
    goal: Int,
    anchor: LocalDate
) {
    val fmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val start = remember(anchor) { anchor.minusDays(6) }
    val labels = remember(anchor) { (0..6).map { start.plusDays(it.toLong()).format(fmt) } }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("7-Day Calorie Trend", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val maxKcal = (list.maxOfOrNull { it.t.kcal } ?: goal).coerceAtLeast(goal)
                list.forEachIndexed { idx, day ->
                    val h = (day.t.kcal.toFloat() / maxKcal).coerceIn(0f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height((h * 80f).dp)
                                .background(MaterialTheme.colors.primary)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(labels.getOrElse(idx) { "?" }, style = MaterialTheme.typography.caption)
                    }
                }
            }
        }
    }
}

/** Macro stacked bars for last 7 days; labels derived from [anchor] so the last row is "today". */
@Composable
private fun WeeklyMacroStackedCard(
    list: List<NutritionViewModel.DailyTotals>,
    anchor: LocalDate
) {
    val fmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val epsilon = 0.0001f // weight() must be strictly > 0
    val start = remember(anchor) { anchor.minusDays(6) }
    val labels = remember(anchor) { (0..6).map { start.plusDays(it.toLong()).format(fmt) } }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("7-Day Macro Ratio", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            list.forEachIndexed { idx, d ->
                val p = max(d.t.protein, 0f)
                val c = max(d.t.carb, 0f)
                val f = max(d.t.fat, 0f)
                val sum = p + c + f

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sum <= 0f) {
                        // Placeholder bar when a day has no macros recorded
                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .fillMaxWidth(0.7f)
                                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                        )
                    } else {
                        val denom = max(sum, epsilon)
                        val wP = max(p / denom, epsilon)
                        val wC = max(c / denom, epsilon)
                        val wF = max(f / denom, epsilon)

                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .weight(wP)
                                .background(MaterialTheme.colors.primary)
                        )
                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .weight(wC)
                                .background(MaterialTheme.colors.secondary)
                        )
                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .weight(wF)
                                .background(MaterialTheme.colors.error)
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(labels.getOrElse(idx) { "?" }, style = MaterialTheme.typography.caption)
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** Sugar / Sodium simple lines for last 7 days; labels also from [anchor] for consistency. */
@Composable
private fun WeeklyQualityBarsCard(
    list: List<NutritionViewModel.DailyTotals>,
    anchor: LocalDate
) {
    val sugarGoal = 50f
    val sodiumGoal = 2300
    val fmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val start = remember(anchor) { anchor.minusDays(6) }
    val labels = remember(anchor) { (0..6).map { start.plusDays(it.toLong()).format(fmt) } }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("7-Day Sugar/Sodium", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            list.forEachIndexed { idx, d ->
                val sColor = if (d.t.sugar > sugarGoal) MaterialTheme.colors.error else MaterialTheme.colors.primary
                val nColor = if (d.t.sodium > sodiumGoal) MaterialTheme.colors.error else MaterialTheme.colors.primary
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(labels.getOrElse(idx) { "?" })
                    Text("Sugar ${d.t.sugar.toInt()} g", color = sColor)
                    Text("Na ${d.t.sodium} mg", color = nColor)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/* ---------------- Insights (unchanged) ---------------- */

@Composable
private fun ConditionInsightsCard(
    conditions: List<ConditionEntity>,
    totals: NutritionRepository.Totals,
    foods: List<FoodEntity>
) {
    val insights = buildList {
        val selected = conditions.filter { it.selected }.map { it.name.trim().lowercase() }
        if (selected.any { it.contains("diabetes") || it.contains("dm") } && totals.sugar > 50) {
            add("High sugar detected. Prefer low-GI carbs and increase fiber.")
        }
        if (selected.any { it.contains("hypertension") || it.contains("high blood pressure") } && totals.sodium > 2300) {
            add("Sodium too high. Reduce sauces/processed food; choose low-sodium options.")
        }
        if (selected.any { it.contains("weight loss") } && totals.kcal > 2000) {
            add("Slightly above calorie goal. Prefer lean protein for dinner.")
        }
        if (isEmpty()) add("Keep going! Balanced intake today 🎉")
    }

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Personalized Insights", fontWeight = FontWeight.Bold)
            insights.forEach { Text("• $it") }
            if (foods.isNotEmpty()) {
                Text("Suggested Foods:", fontWeight = FontWeight.SemiBold)
                foods.take(3).forEach { f -> Text("- ${f.name}") }
            }
        }
    }
}

/* ---------------- Utility: produce exactly 7 consecutive days ending at today ---------------- */

/**
 * Build exactly 7 consecutive days ending at [anchor] (today).
 * Missing dates are filled with zero totals so charts and labels never skip a weekday.
 */
private fun padTo7Days(
    src: List<NutritionViewModel.DailyTotals>,
    anchor: LocalDate
): List<NutritionViewModel.DailyTotals> {
    val start = anchor.minusDays(6)
    val map = src.associateBy { it.date }

    return (0..6).map { i ->
        val d = start.plusDays(i.toLong())
        map[d] ?: NutritionViewModel.DailyTotals(
            date = d,
            t = NutritionRepository.Totals(
                kcal = 0,
                carb = 0f,
                protein = 0f,
                fat = 0f,
                sugar = 0f,
                satFat = 0f, // keep schema-compatible with your Totals
                sodium = 0
            )
        )
    }
}
