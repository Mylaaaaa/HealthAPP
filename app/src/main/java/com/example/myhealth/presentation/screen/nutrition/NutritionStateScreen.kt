package com.example.myhealth.presentation.screen.nutrition

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.example.myhealth.presentation.screen.nutrition.db.FoodEntity
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun NutritionStateScreen(
    // Use AndroidViewModelFactory to avoid crash (AndroidViewModel needs Application)
    vm: NutritionViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val totals by vm.totals.collectAsState()
    val weeklyTotals by vm.weeklyTotals.collectAsState()
    val kcalGoal by vm.kcalGoal.collectAsState()
    val macroTargets by vm.macroTargets.collectAsState()
    val conditions by vm.allConditions.collectAsState()
    val foods by vm.recommendedFoods.collectAsState()

    LaunchedEffect(Unit) { vm.loadWeeklyTotals() }

    Scaffold{ inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DailyEnergyCard(current = totals.kcal, goal = kcalGoal)
            DailyMacroTargetsCard(t = totals, targets = macroTargets)
            QualityGuardCard(sugar = totals.sugar, satFat = totals.satFat, sodium = totals.sodium)
            WeeklyCaloriesTrendCard(list = weeklyTotals, goal = kcalGoal)
            WeeklyMacroStackedCard(list = weeklyTotals)
            WeeklyQualityBarsCard(list = weeklyTotals)
            ConditionInsightsCard(conditions = conditions, totals = totals, foods = foods)
        }
    }
}

/* ---------------- DAILY CARDS ---------------- */

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
    // Compute labels and colors directly in the @Composable scope

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

    // Decide colors inside the @Composable scope (no helper function)
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

/* ---------------- WEEKLY CARDS ---------------- */

@Composable
private fun WeeklyCaloriesTrendCard(list: List<NutritionViewModel.DailyTotals>, goal: Int) {
    val fmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("7-Day Calorie Trend", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // simple column bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val maxKcal = (list.maxOfOrNull { it.t.kcal } ?: goal).coerceAtLeast(goal)
                list.forEach { day ->
                    val h = (day.t.kcal.toFloat() / maxKcal).coerceIn(0f, 1f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height((h * 80f).dp)
                                .background(MaterialTheme.colors.primary)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(day.date.format(fmt), style = MaterialTheme.typography.caption)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyMacroStackedCard(list: List<NutritionViewModel.DailyTotals>) {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("E", java.util.Locale.getDefault())
    val epsilon = 0.0001f // weight() must be > 0

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("7-Day Macro Ratio", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            list.forEach { d ->
                val p = max(d.t.protein, 0f)
                val c = max(d.t.carb, 0f)
                val f = max(d.t.fat, 0f)
                val sum = p + c + f

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sum <= 0f) {
                        // All zero → show a faint placeholder bar
                        Box(
                            modifier = Modifier
                                .height(12.dp)
                                .fillMaxWidth(0.7f) // short placeholder
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
                    Text(d.date.format(fmt), style = MaterialTheme.typography.caption)
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun WeeklyQualityBarsCard(list: List<NutritionViewModel.DailyTotals>) {
    val sugarGoal = 50f
    val sodiumGoal = 2300
    val fmt = DateTimeFormatter.ofPattern("E", Locale.getDefault())

    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("7-Day Sugar/Sodium", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            list.forEach { d ->
                val sColor = if (d.t.sugar > sugarGoal) MaterialTheme.colors.error else MaterialTheme.colors.primary
                val nColor = if (d.t.sodium > sodiumGoal) MaterialTheme.colors.error else MaterialTheme.colors.primary
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(d.date.format(fmt))
                    Text("Sugar ${d.t.sugar.toInt()} g", color = sColor)
                    Text("Na ${d.t.sodium} mg", color = nColor)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/* ---------------- INSIGHTS ---------------- */

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
