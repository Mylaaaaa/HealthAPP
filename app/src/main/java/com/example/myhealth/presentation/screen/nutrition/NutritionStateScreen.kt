// NutritionStateScreen.kt
// Purpose: A focused "State" page that reuses your existing NutritionViewModel.totals.
// It shows: Calories progress, Macros (P/C/F) bar, and other metrics (Sugar/SatFat/Sodium).
// No rename of existing files is required.

package com.example.myhealth.presentation.screen.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState // <-- IMPORTANT: correct import

@Composable
fun NutritionStateScreen(
    // If you already provide the VM from a parent via parameters, pass it in and remove viewModel() here.
    vm: NutritionViewModel = viewModel()
) {
    // Collect your existing totals StateFlow from the ViewModel
    val totals by vm.totals.collectAsState()

    // Temporary calorie goal. Later you can inject from user settings.
    val dailyKcalGoal = 2000

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nutrition State") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Calories card (large) ---
            Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Calories", style = MaterialTheme.typography.caption)

                    val kcalNow = totals.kcal // likely Int
                    Text(
                        text = "$kcalNow/$dailyKcalGoal kcal",
                        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold)
                    )

                    val progress = (kcalNow.toFloat() / dailyKcalGoal.coerceAtLeast(1))
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                    )
                }
            }

            // --- Macros card (P/C/F) ---
            Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Macros", style = MaterialTheme.typography.caption)

                    // Your totals use carb/protein/fat (likely Float grams)
                    val p = totals.protein
                    val c = totals.carb
                    val f = totals.fat

                    MacroBarSimple(
                        protein = p,
                        carbs = c,
                        fat = f
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("P ${formatGrams(p)}")
                        Text("C ${formatGrams(c)}")
                        Text("F ${formatGrams(f)}")
                    }
                }
            }

            // --- Other metrics card (Sugar, SatFat, Sodium) ---
            Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Other metrics", style = MaterialTheme.typography.caption)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricChip(label = "Sugar", value = formatGrams(totals.sugar))
                        MetricChip(label = "SatFat", value = formatGrams(totals.satFat))
                        MetricChip(label = "Sodium", value = "${totals.sodium} mg")
                    }
                }
            }

            // (Optional) Placeholder for future 7-day charts
            /*
            Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last 7 days (charts coming soon)")
                }
            }
            */
        }
    }
}

/* -------------------- Local simple UI helpers -------------------- */

@Composable
private fun MacroBarSimple(protein: Float, carbs: Float, fat: Float) {
    val total = (protein + carbs + fat).coerceAtLeast(0.0001f)
    val wp = protein / total
    val wc = carbs / total
    val wf = fat / total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        Spacer(modifier = Modifier.weight(wp))
        Spacer(modifier = Modifier.width(2.dp))
        Spacer(modifier = Modifier.weight(wc))
        Spacer(modifier = Modifier.width(2.dp))
        Spacer(modifier = Modifier.weight(wf))
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Surface(elevation = 2.dp) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: ")
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatGrams(x: Float): String {
    // Show one decimal place, e.g., "23.5 g"
    return String.format(java.util.Locale.getDefault(), "%.1f g", x)
}
