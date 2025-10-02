package com.example.myhealth.presentation.screen.inputreadings

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.units.Mass
import java.util.Locale

/**
 * Record weight screen wired to your existing ViewModel/navigation.
 * Types aligned with the codelab:
 * - weeklyAvg: Mass?
 * - permissions: Set<String>
 */
@Composable
fun InputReadingsScreen(
    permissionsGranted: Boolean,
    permissions: Set<String>,                 // <- Set<String>
    uiState: Any,
    onInsertClick: (Double) -> Unit,          // Insert new weight (Kg)
    weeklyAvg: Mass?,                         // Health Connect Mass
    onDeleteClick: (String) -> Unit,
    readingsList: List<Any>,                  // VM-provided list (arbitrary model)
    onError: (Throwable) -> Unit,
    onPermissionsResult: () -> Unit,
    onPermissionsLaunch: (Set<String>) -> Unit // <- Set<String>
) {
    var input by remember { mutableStateOf("") }

    // Basic validation (Kg).
    val inputError: String? = when {
        input.isBlank() -> "Enter a value"
        input.toDoubleOrNull() == null -> "Please input a number"
        input.toDouble() <= 0 || input.toDouble() >= 1000 -> "Weight must be between 0 and 1000"
        else -> null
    }

    // Map unknown model items to UI rows; supports Mass fields too.
    val rows: List<WeightRow> = remember(readingsList) { readingsList.mapNotNull(::mapItemToRow) }

    // Format weekly average from Mass?.
    val weeklyAvgText: String = weeklyAvg?.inKilograms
        ?.let { String.format(Locale.US, "%.1f", it) + " Kg" }
        ?: "--"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        if (!permissionsGranted) {
            PermissionBanner(
                text = "Some permissions are missing. Grant to enable recording.",
                actionText = "Grant permissions"
            ) {
                // Pass Set<String> straight through; your launcher in Navigation expects Set<String>.
                onPermissionsLaunch(permissions)
                onPermissionsResult()
            }
            Spacer(Modifier.height(8.dp))
        }

        RecordWeightContent(
            weightText = input,
            onWeightTextChange = { input = it },
            onAdd = {
                if (inputError == null) {
                    onInsertClick(input.toDouble())   // pass Kg value
                    input = ""
                }
            },
            inputError = inputError,
            recent = rows,
            onDelete = onDeleteClick,
            weeklyAvgText = weeklyAvgText
        )
    }
}

/* ---------------- Permission banner ---------------- */

@Composable
private fun PermissionBanner(
    text: String,
    actionText: String,
    onClick: () -> Unit
) {
    Card(
        elevation = 2.dp,
        backgroundColor = Color(0xFFFFF3E0),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(60.dp)
            .wrapContentHeight(Alignment.CenterVertically)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFF57C00))
            Spacer(Modifier.width(10.dp))
            Text(text = text, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClick) { Text(actionText, color = Color(0xFFF57C00)) }
        }
    }
}

/* ---------------- Mapping helpers (top-level) ---------------- */

/**
 * Builds a UI row by trying common field names.
 * Supports Mass fields (e.g., weightInput: Mass) and numeric/string weights.
 */
private fun mapItemToRow(item: Any): WeightRow? = runCatching {
    val c = item.javaClass

    val id: String? =
        getStringField(c, item, "uid")
            ?: getStringField(c, item, "id")

    val valueKg: Double? =
        getMassKg(c, item, "weightKg")
            ?: getMassKg(c, item, "weight")
            ?: getMassKg(c, item, "weightInput")
            ?: getDoubleField(c, item, "weightKg")
            ?: getDoubleField(c, item, "weight")
            ?: getDoubleField(c, item, "weightInput")

    val timeText: String? =
        getStringField(c, item, "timeText")
            ?: getStringFromAny(c, item, "time")
            ?: getStringFromAny(c, item, "timestamp")

    if (id != null && valueKg != null && timeText != null) {
        WeightRow(id = id, valueKg = valueKg, timeText = timeText)
    } else null
}.getOrNull()

private fun getStringField(c: Class<*>, obj: Any, name: String): String? = runCatching {
    val f = c.getDeclaredField(name)
    f.isAccessible = true
    f.get(obj)?.toString()
}.getOrNull()

private fun getStringFromAny(c: Class<*>, obj: Any, name: String): String? = runCatching {
    val f = c.getDeclaredField(name)
    f.isAccessible = true
    f.get(obj)?.toString()
}.getOrNull()

private fun getDoubleField(c: Class<*>, obj: Any, name: String): Double? = runCatching {
    val f = c.getDeclaredField(name)
    f.isAccessible = true
    when (val v = f.get(obj)) {
        is Double -> v
        is Float -> v.toDouble()
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }
}.getOrNull()

/** Reads a Mass field and returns kilograms, if present. */
private fun getMassKg(c: Class<*>, obj: Any, name: String): Double? = runCatching {
    val f = c.getDeclaredField(name)
    f.isAccessible = true
    val v = f.get(obj)
    (v as? Mass)?.inKilograms
}.getOrNull()
