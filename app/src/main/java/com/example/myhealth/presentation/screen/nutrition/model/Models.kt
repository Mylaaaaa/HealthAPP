package com.example.myhealth.data.nutrition.model

// Simple nutrient container (per 100g for a food item).
data class Nutrient(
    val kcal: Int = 0,
    val carb: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val sodium: Int = 0,
    val sugar: Float = 0f,
    val satFat: Float = 0f
)

// Domain model used by UI (optional; we mainly use entities + relations).
data class Food(
    val code: String,
    val name: String,
    val per100g: Nutrient
)
