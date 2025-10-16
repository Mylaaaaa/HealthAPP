package com.example.myhealth.presentation.screen.nutrition

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
import com.example.myhealth.presentation.screen.nutrition.MealType
import com.example.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.example.myhealth.presentation.screen.nutrition.db.FoodEntity
import com.example.myhealth.presentation.screen.nutrition.db.NutritionDatabase
import java.time.LocalDate
import kotlin.math.roundToInt

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { datePicker.show() }) {
                        Icon(Icons.Default.Today, contentDescription = "Pick date")
                    }
                },
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { preselectFood = null; showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add food")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // -------- Health Conditions --------
            ConditionSection(
                conditions = conditions,
                onAdd = { vm.addCondition(it) },
                onToggle = { id, sel -> vm.toggleCondition(id, sel) },
                onRemove = { id -> vm.removeCondition(id) }
            )

            Spacer(Modifier.height(8.dp))

            // -------- Recommended Foods --------
            if (recommended.isNotEmpty()) {
                Text("Recommended Foods", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recommended) { f ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
                            modifier = Modifier.clickable {
                                preselectFood = f
                                showAdd = true
                            }
                        ) {
                            Text(
                                f.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // -------- Summary Chips --------
            SummaryRow(totals = totals)

            Spacer(Modifier.height(8.dp))

            // -------- Meals List --------
            Card(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (meals.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape))
                        Spacer(Modifier.height(8.dp))
                        Text("No items yet", style = MaterialTheme.typography.subtitle1)
                        Text("Tap + to log your first meal", style = MaterialTheme.typography.body2)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp)) {
                        items(meals, key = { it.entry.id }) { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MealBadge(type = m.entry.mealType)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        m.food.name,
                                        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val scaled = scale(m.food, m.entry.grams)
                                    Text("${m.entry.grams} g • ${scaled.pretty()}",
                                        style = MaterialTheme.typography.body2)
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
                    onAdd(t)
                    text = ""
                    adding = false
                }
            }) { Text("Save") }
        }
    }

    Spacer(Modifier.height(6.dp))

    if (conditions.isEmpty()) {
        Text(
            "Add your chronic conditions to see tailored food suggestions.",
            style = MaterialTheme.typography.caption
        )
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
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
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

// ------------ Summary chips ------------
@Composable
private fun SummaryRow(totals: NutritionRepository.Totals) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryChip(title = "Calories", value = "${totals.kcal} kcal")
        SummaryChip(title = "Carbs", value = "%.1f g".format(totals.carb))
        SummaryChip(title = "Protein", value = "%.1f g".format(totals.protein))
        SummaryChip(title = "Fat", value = "%.1f g".format(totals.fat))
    }
}

@Composable
private fun SummaryChip(title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.06f))
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.caption)
            Text(value, style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold))
        }
    }
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
        color = MaterialTheme.colors.primary.copy(alpha = 0.08f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
        )
    }
}

// ------------ Add Food Dialog (supports preselected food) ------------
@Composable
private fun AddFoodDialog(
    preselected: FoodEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (FoodEntity, Int, MealType) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { com.example.myhealth.presentation.screen.nutrition.db.NutritionDatabase.get(ctx) }

    var query by remember { mutableStateOf("") }
    var gramsText by remember { mutableStateOf("100") }
    var selected by remember { mutableStateOf<FoodEntity?>(preselected) }
    var meal by remember { mutableStateOf(MealType.Breakfast) }

    // Ensure seed once when dialog opens (if table empty)
    LaunchedEffect(Unit) {
        if (db.foodDao().count() == 0) {
            db.foodDao().upsertAll(com.example.myhealth.presentation.screen.nutrition.db.Prepopulate.foods())
        }
    }

    // Show all foods when query is blank; otherwise filter by query
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    if (foods.isEmpty()) {
                        // Rare: immediately after seed or user typed a very narrow query
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No results", modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        LazyColumn {
                            items(foods) { f ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { selected = f }
                                        .padding(12.dp),
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
                        FilterChip(
                            selected = meal == it,
                            onClick = { meal = it },
                            text = it.name
                        )
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
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
