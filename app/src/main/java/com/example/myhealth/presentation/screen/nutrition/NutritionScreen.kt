package com.example.myhealth.presentation.screen.nutrition

import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.roundToInt


// -------------------- Data models (in-memory for now) --------------------

data class FoodItem(
    val code: String,
    val name: String,
    val kcalPer100: Int,
    val carbPer100: Float,
    val proteinPer100: Float,
    val fatPer100: Float,
    val sodiumMgPer100: Int = 0,
    val sugarPer100: Float = 0f,
    val satFatPer100: Float = 0f,
)

data class MealEntry(
    val id: Long,
    val date: LocalDate,
    val mealType: String,      // Breakfast / Lunch / Dinner / Snack
    val foodCode: String,
    val grams: Int
)

data class HealthProfile(
    val hasHypertension: Boolean = false,
    val hasDiabetes: Boolean = false,
    val hasHyperlipidemia: Boolean = false,
    val isVegetarian: Boolean = false,
    val kcalTarget: Int = 2000
)

data class Nutrient(
    val kcal: Int,
    val carb: Float,
    val protein: Float,
    val fat: Float,
    val sodium: Int,
    val sugar: Float,
    val satFat: Float
)

data class Recipe(
    val id: String,
    val title: String,
    val items: List<Pair<String, Int>>, // foodCode -> grams
    val tags: List<String> = emptyList()
)

// -------------------- Demo seeds --------------------

private fun demoFoods(): List<FoodItem> = listOf(
    FoodItem("apple_raw", "Apple (raw)", 52, 14.0f, 0.3f, 0.2f, sugarPer100 = 10.4f),
    FoodItem("rice_cooked", "Rice (cooked)", 130, 28.0f, 2.4f, 0.3f),
    FoodItem("chicken_breast", "Chicken breast", 165, 0f, 31.0f, 3.6f, satFatPer100 = 1.0f),
    FoodItem("milk_2p", "Milk 2%", 50, 5.0f, 3.4f, 2.0f, sugarPer100 = 5.0f, satFatPer100 = 1.3f, sodiumMgPer100 = 44),
    FoodItem("broccoli", "Broccoli", 35, 7.2f, 2.4f, 0.4f, sodiumMgPer100 = 33),
    FoodItem("tofu_firm", "Tofu (firm)", 76, 1.9f, 8.0f, 4.8f),
)

private fun demoRecipes(): List<Recipe> = listOf(
    Recipe(
        id = "r1",
        title = "Chicken + rice + broccoli",
        items = listOf("chicken_breast" to 150, "rice_cooked" to 200, "broccoli" to 120),
        tags = listOf("high-protein")
    ),
    Recipe(
        id = "r2",
        title = "Tofu rice bowl",
        items = listOf("tofu_firm" to 180, "rice_cooked" to 200, "broccoli" to 120),
        tags = listOf("vegetarian")
    ),
    Recipe(
        id = "r3",
        title = "Milk + apple snack",
        items = listOf("milk_2p" to 250, "apple_raw" to 150),
        tags = listOf("snack")
    )
)

// -------------------- Calculations --------------------

private fun calc(food: FoodItem, grams: Int): Nutrient {
    val f = grams / 100f
    return Nutrient(
        kcal = (food.kcalPer100 * f).roundToInt(),
        carb = food.carbPer100 * f,
        protein = food.proteinPer100 * f,
        fat = food.fatPer100 * f,
        sodium = (food.sodiumMgPer100 * f).roundToInt(),
        sugar = food.sugarPer100 * f,
        satFat = food.satFatPer100 * f
    )
}

private fun plus(a: Nutrient, b: Nutrient) = Nutrient(
    kcal = a.kcal + b.kcal,
    carb = a.carb + b.carb,
    protein = a.protein + b.protein,
    fat = a.fat + b.fat,
    sodium = a.sodium + b.sodium,
    sugar = a.sugar + b.sugar,
    satFat = a.satFat + b.satFat
)

private fun Nutrient.pretty(): String =
    "${kcal} kcal · C ${carb.format1()}g · P ${protein.format1()}g · F ${fat.format1()}g"

// Basic rule filter by conditions
private fun filterByProfile(
    recipes: List<Recipe>,
    profile: HealthProfile,
    foods: Map<String, FoodItem>
): List<Recipe> {
    fun Recipe.ok(): Boolean {
        val n = items.fold(Nutrient(0,0f,0f,0f,0,0f,0f)) { acc, (code, g) ->
            val f = foods[code] ?: return@fold acc
            plus(acc, calc(f, g))
        }
        if (profile.hasHypertension && n.sodium > 600) return false
        if (profile.hasDiabetes && n.sugar > 20f) return false
        if (profile.hasHyperlipidemia && n.satFat > 10f) return false
        if (profile.isVegetarian && items.any { (code, _) -> foods[code]?.name?.contains("Chicken", true) == true }) return false
        return true
    }
    return recipes.filter { it.ok() }
}

private fun Float.format1() = String.format("%.1f", this)

// -------------------- Screen --------------------

@Composable
fun NutritionScreen() {
    val foods = remember { demoFoods() }
    val foodMap = remember(foods) { foods.associateBy { it.code } }
    var entries by remember { mutableStateOf<List<MealEntry>>(emptyList()) }   // ✅ custom type -> remember
    var idCounter by rememberSaveable { mutableStateOf(1L) }                    // ✅ primitive ok
    var profile by remember { mutableStateOf(HealthProfile()) }                // ✅ custom type -> remember
    var query by rememberSaveable { mutableStateOf("") }                        // ✅ String ok
    var showAdd by rememberSaveable { mutableStateOf(false) }                   // ✅ Boolean ok

    val today = LocalDate.now()

    val totals: Nutrient = remember(entries) {
        entries.filter { it.date == today }.fold(Nutrient(0,0f,0f,0f,0,0f,0f)) { acc, e ->
            val f = foodMap[e.foodCode] ?: return@fold acc
            plus(acc, calc(f, e.grams))
        }
    }

    val suggestions = remember(query, foods) {
        if (query.isBlank()) foods.take(5) else foods.filter { it.name.contains(query, ignoreCase = true) }
    }

    val recs = remember(profile, foodMap) {
        filterByProfile(demoRecipes(), profile, foodMap)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add meal")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text("Nutrition", style = MaterialTheme.typography.h6, modifier = Modifier.padding(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Today total", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(totals.pretty())
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = (totals.kcal / profile.kcalTarget.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${totals.kcal}/${profile.kcalTarget} kcal", style = MaterialTheme.typography.caption)
                    }
                }
            }

            if (entries.isEmpty()) {
                item { Text("No meals yet. Tap + to log food.", Modifier.padding(16.dp)) }
            } else {
                items(entries, key = { it.id }) { e ->
                    val f = foodMap[e.foodCode]
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        elevation = 1.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${e.mealType} · ${f?.name ?: e.foodCode}", fontWeight = FontWeight.SemiBold)
                                val n = f?.let { calc(it, e.grams) }
                                Text("${e.grams} g · ${n?.kcal ?: 0} kcal", style = MaterialTheme.typography.caption)
                            }
                            IconButton(onClick = {
                                entries = entries.filterNot { it.id == e.id }
                            }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        }
                    }
                }
            }

            item {
                Text(
                    "Health conditions",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip("Hypertension", profile.hasHypertension) {
                        profile = profile.copy(hasHypertension = !profile.hasHypertension)
                    }
                    FilterChip("Diabetes", profile.hasDiabetes) {
                        profile = profile.copy(hasDiabetes = !profile.hasDiabetes)
                    }
                    FilterChip("Hyperlipidemia", profile.hasHyperlipidemia) {
                        profile = profile.copy(hasHyperlipidemia = !profile.hasHyperlipidemia)
                    }
                    FilterChip("Vegetarian", profile.isVegetarian) {
                        profile = profile.copy(isVegetarian = !profile.isVegetarian)
                    }
                }
            }

            if (recs.isNotEmpty()) {
                item {
                    Text(
                        "Recommended meals",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(recs, key = { it.id }) { r ->
                    val rn = r.items.fold(Nutrient(0,0f,0f,0f,0,0f,0f)) { acc, (code, g) ->
                        foodMap[code]?.let { plus(acc, calc(it, g)) } ?: acc
                    }
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        elevation = 1.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(r.title, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(rn.pretty(), style = MaterialTheme.typography.caption)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                r.items.joinToString { (code, g) -> "${foodMap[code]?.name ?: code} ${g}g" },
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddMealDialog(
            query = query,
            suggestions = suggestions,
            onQueryChange = { query = it },
            onDismiss = { showAdd = false },
            onConfirm = { food, grams, mealType ->
                entries = entries + MealEntry(
                    id = idCounter++,
                    date = today,
                    mealType = mealType,
                    foodCode = food.code,
                    grams = grams
                )
                showAdd = false
                query = ""
            }
        )
    }
}

// -------------------- UI bits --------------------

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = .12f) else MaterialTheme.colors.surface,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = .12f)),
        modifier = Modifier
            .clickable { onClick() }
    ) { Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
}

@Composable
private fun AddMealDialog(
    query: String,
    suggestions: List<FoodItem>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (FoodItem, Int, String) -> Unit
) {
    var selected by remember { mutableStateOf<FoodItem?>(null) }
    var gramsText by remember { mutableStateOf("100") }
    var meal by remember { mutableStateOf("Lunch") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log food") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search food") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Column {
                    suggestions.forEach { f ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selected = f },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected == f, onClick = { selected = f })
                            Text(f.name)
                            Spacer(Modifier.weight(1f))
                            Text("${f.kcalPer100} kcal/100g", style = MaterialTheme.typography.caption)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it.filter { ch -> ch.isDigit() }.ifBlank { "0" } },
                    label = { Text("Grams") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Breakfast","Lunch","Dinner","Snack").forEach { m ->
                        FilterChip(m, meal == m) { meal = m }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && gramsText.toIntOrNull() != null,
                onClick = {
                    val g = gramsText.toIntOrNull() ?: return@TextButton
                    onConfirm(selected!!, g, meal)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
