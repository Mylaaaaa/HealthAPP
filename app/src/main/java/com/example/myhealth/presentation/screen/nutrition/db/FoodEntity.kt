package com.example.myhealth.presentation.screen.nutrition.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Food reference table (nutrients are per 100g).
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val code: String,
    val name: String,
    val kcal: Int,
    val carb: Float,
    val protein: Float,
    val fat: Float,
    val sodium: Int,
    val sugar: Float,
    val satFat: Float
)
