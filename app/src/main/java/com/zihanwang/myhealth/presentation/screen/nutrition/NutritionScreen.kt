package com.zihanwang.myhealth.presentation.screen.nutrition

import androidx.compose.ui.graphics.Color
import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zihanwang.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.zihanwang.myhealth.presentation.screen.nutrition.db.FoodEntity
import java.time.LocalDate
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.lerp

@Composable
fun NutritionScreen(
    vm: NutritionViewModel = viewModel()
) {
    val date by vm.date.collectAsState()
    val meals by vm.meals.collectAsState()
    val totals by vm.totals.collectAsState()
    val conditions by vm.allConditions.collectAsState()
    val recommended by vm.recommendedFoods.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var preselectFood by remember { mutableStateOf<FoodEntity?>(null) }

    val context = LocalContext.current
    val datePicker = remember(date) {
        DatePickerDialog(
            context,
            { _, y, m, d -> vm.setDate(LocalDate.of(y, m + 1, d)) },
            date.year, date.monthValue - 1, date.dayOfMonth
        )
    }
    LaunchedEffect(Unit) { vm.openDatePicker.collect { datePicker.show() } }

    // ---- THEME HOOKS ----
    // IMPORTANT: derive darkness from the *current Material theme*, not the system setting.
    val isDark = !MaterialTheme.colors.isLight

    // Force obvious contrast for cards so dark/light are undeniably different
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    // FAB rule: Dark = green, Light = blue
    val fabContainer = if (isDark) Color(0xFF2E7D32) else Color(0xFF1976D2)
    val fabContent = Color.White

    // Recommended chips background: stronger on dark
    val recChipBg = if (isDark)
        MaterialTheme.colors.primary.copy(alpha = 0.28f)
    else
        MaterialTheme.colors.primary.copy(alpha = 0.08f)

    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        topBar = {
            TopAppBar(
                title = { /* keep empty or show date if you want */ },
                actions = {
                    IconButton(onClick = { datePicker.show() }) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = "Pick date",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                elevation = 0.dp
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                backgroundColor = fabContainer,
                contentColor = fabContent
            ) { Icon(Icons.Default.Add, contentDescription = "Add food") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------- Health Conditions --------
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = 3.dp,
                backgroundColor = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    ConditionSection(
                        conditions = conditions,
                        onAdd = { vm.addCondition(it) },
                        onToggle = { id, sel -> vm.toggleCondition(id, sel) },
                        onRemove = { id -> vm.removeCondition(id) }
                    )
                }
            }

            // -------- Recommended Foods --------
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = 3.dp,
                backgroundColor = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Recommended Foods", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    if (recommended.isEmpty()) {
                        Text(
                            "No recommended foods yet. Add your health conditions to get suggestions.",
                            style = MaterialTheme.typography.caption
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recommended) { f ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.10f)),
                                    color = recChipBg,
                                    modifier = Modifier.clickable {
                                        preselectFood = f
                                        showAdd = true
                                    }
                                ) {
                                    Text(
                                        f.name,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------- Daily Summary --------
            Divider()
            Text(
                "Daily Summary",
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp)
            )
            SummaryRow(totals = totals)

            // -------- Meals List --------
            Card(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                backgroundColor = cardBg,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (meals.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape))
                        Spacer(Modifier.height(8.dp))
                        Text("No items yet", style = MaterialTheme.typography.subtitle1)
                        Text("Tap + to log your first meal", style = MaterialTheme.typography.body2)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp)) {
                        items(meals, key = { it.entry.id }) { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MealBadge(type = m.entry.mealType)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        m.food.name,
                                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    val scaled = scale(m.food, m.entry.grams)
                                    Text("${m.entry.grams} g • ${scaled.pretty()}", style = MaterialTheme.typography.body2)
                                }
                                IconButton(onClick = { vm.deleteMeal(m.entry.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddFoodDialog(
            preselected = preselectFood,
            onDismiss = { showAdd = false },
            onConfirm = { selected, grams, meal ->
                vm.addMeal(meal, selected.code, grams)
                showAdd = false
            }
        )
    }
}

// ------------ Condition Section ------------
@Composable
private fun ConditionSection(
    conditions: List<ConditionEntity>,
    onAdd: (String) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onRemove: (Long) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Health Conditions", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = { adding = !adding }) { Text(if (adding) "Close" else "Add") }
    }

    AnimatedVisibility(visible = adding) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("e.g., Diabetes, Hypertension") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val t = text.trim()
                if (t.isNotEmpty()) {
                    onAdd(t); text = ""; adding = false
                }
            }) { Text("Save") }
        }
    }

    Spacer(Modifier.height(6.dp))

    if (conditions.isEmpty()) {
        Text("Add your chronic conditions to see tailored food suggestions.", style = MaterialTheme.typography.caption)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            conditions.forEach { c ->
                ConditionChip(
                    name = c.name,
                    selected = c.selected,
                    onToggle = { onToggle(c.id, !c.selected) },
                    onRemove = { onRemove(c.id) }
                )
            }
        }
    }
}

@Composable
private fun ConditionChip(
    name: String,
    selected: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val isDark = !MaterialTheme.colors.isLight
    val chipBg =
        if (selected) {
            if (isDark) MaterialTheme.colors.primary.copy(alpha = 0.28f)
            else MaterialTheme.colors.primary.copy(alpha = 0.12f)
        } else {
            // unselected: keep readable on dark (not pitch black)
            if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
        }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = chipBg,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.caption, modifier = Modifier.clickable { onToggle() })
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.Clear,
                contentDescription = "Remove",
                modifier = Modifier.size(16.dp).clickable { onRemove() }
            )
        }
    }
}

// ------------ Summary chips ------------
@Composable
private fun SummaryRow(totals: NutritionRepository.Totals) {
    // Pastel backgrounds for light; reduce blending on dark so they don't wash out.
    val bgCalories = pastel(Color(0xFFEF9A9A), darkStrength = 0.75f)   // soft red
    val bgCarbs    = pastel(Color(0xFFFFCC80), darkStrength = 0.75f)   // warm orange
    val bgProtein  = pastel(Color(0xFF90CAF9), darkStrength = 0.75f)   // soft blue
    val bgFat      = pastel(Color(0xFFA5D6A7), darkStrength = 0.75f)   // soft green

    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryChip(title = "Calories", value = "${totals.kcal} kcal", background = bgCalories)
        SummaryChip(title = "Carbs",    value = "%.1f g".format(totals.carb),    background = bgCarbs)
        SummaryChip(title = "Protein",  value = "%.1f g".format(totals.protein), background = bgProtein)
        SummaryChip(title = "Fat",      value = "%.1f g".format(totals.fat),     background = bgFat)
    }
}

/** Flat chip with pastel background; adjusts text via onSurface. */
@Composable
private fun SummaryChip(
    title: String,
    value: String,
    background: Color
) {
    val onSurface = MaterialTheme.colors.onSurface
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = background,
        elevation = 0.dp,
        modifier = Modifier.padding(horizontal = 4.dp).defaultMinSize(minWidth = 86.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.caption.copy(color = onSurface.copy(alpha = 0.70f)))
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold, color = onSurface))
        }
    }
}

@Composable
fun pastel(
    lightColor: Color,
    darkStrength: Float = 0.60f
): Color {
    // Use given pastel in light theme
    if (MaterialTheme.colors.isLight) return lightColor
    // In dark: blend from surface to pastel, smaller darkStrength => more contrast
    val surface = MaterialTheme.colors.surface
    val t = darkStrength.coerceIn(0f, 1f)
    return lerp(surface, lightColor, t)
}

@Composable
private fun MealBadge(type: MealType) {
    val label = when (type) {
        MealType.Breakfast -> "Breakfast"
        MealType.Lunch -> "Lunch"
        MealType.Dinner -> "Dinner"
        MealType.Snack -> "Snack"
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
        elevation = 0.dp
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
        )
    }
}

// ------------ Add Food Dialog ------------
@Composable
private fun AddFoodDialog(
    preselected: FoodEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (FoodEntity, Int, MealType) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { com.zihanwang.myhealth.presentation.screen.nutrition.db.NutritionDatabase.get(ctx) }

    var query by remember { mutableStateOf("") }
    var gramsText by remember { mutableStateOf("100") }
    var selected by remember { mutableStateOf<FoodEntity?>(preselected) }
    var meal by remember { mutableStateOf(MealType.Breakfast) }

    LaunchedEffect(Unit) {
        if (db.foodDao().count() == 0) {
            db.foodDao().upsertAll(com.zihanwang.myhealth.presentation.screen.nutrition.db.Prepopulate.foods())
        }
    }

    val foodsFlow = remember(query) {
        if (query.isBlank()) db.foodDao().getAll() else db.foodDao().search(query)
    }
    val foods by foodsFlow.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Food") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search food") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                ) {
                    if (foods.isEmpty()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No results", modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        LazyColumn {
                            items(foods) { f ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { selected = f }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(f.name, fontWeight = FontWeight.Medium)
                                        Text(f.code, style = MaterialTheme.typography.caption)
                                    }
                                    RadioButton(selected = (selected == f), onClick = { selected = f })
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Meal:", modifier = Modifier.width(56.dp))
                    MealType.values().forEach {
                        FilterChip(selected = meal == it, onClick = { meal = it }, text = it.name)
                        Spacer(Modifier.width(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { gramsText = it.filter(Char::isDigit) },
                    label = { Text("Grams") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = selected != null) {
                    val s = selected
                    if (s != null) {
                        val preview = scale(s, gramsText.toIntOrNull() ?: 100)
                        Spacer(Modifier.height(8.dp))
                        Text("Preview: ${preview.pretty()}", style = MaterialTheme.typography.body2)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && (gramsText.toIntOrNull() ?: 0) > 0,
                onClick = {
                    val g = gramsText.toIntOrNull() ?: return@TextButton
                    val s = selected ?: return@TextButton
                    onConfirm(s, g, meal)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FilterChip(selected: Boolean, onClick: () -> Unit, text: String) {
    val isDark = !MaterialTheme.colors.isLight
    val bg = if (selected) {
        if (isDark) MaterialTheme.colors.primary.copy(alpha = 0.28f)
        else MaterialTheme.colors.primary.copy(alpha = 0.12f)
    } else {
        if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.10f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.caption)
    }
}

// ------------ Utilities ------------
private data class NutrientScaled(
    val kcal: Int,
    val carb: Float,
    val protein: Float,
    val fat: Float
)

private fun scale(f: FoodEntity, grams: Int): NutrientScaled {
    val factor = grams / 100f
    return NutrientScaled(
        kcal = (f.kcal * factor).roundToInt(),
        carb = f.carb * factor,
        protein = f.protein * factor,
        fat = f.fat * factor
    )
}

private fun NutrientScaled.pretty(): String =
    "$kcal kcal • C ${"%.1f".format(carb)}g • P ${"%.1f".format(protein)}g • F ${"%.1f".format(fat)}g"
