package com.example.myhealth.presentation.screen.nutrition.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User chronic conditions stored locally.
 * Keep it simple: each row is a condition user cares about, and whether it's selected.
 */
@Entity(tableName = "conditions")
data class ConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,        // e.g., "Diabetes", "Hypertension", "Hyperlipidemia"
    val selected: Boolean = true
)
