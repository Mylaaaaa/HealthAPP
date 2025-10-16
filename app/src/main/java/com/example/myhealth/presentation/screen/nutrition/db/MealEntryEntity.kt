package com.example.myhealth.presentation.screen.nutrition.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myhealth.presentation.screen.nutrition.MealType
import java.time.LocalDate

// User meal logs (date + meal type + food code + grams).
@Entity(
    tableName = "meal_entries",
    indices = [Index("date"), Index("mealType"), Index("foodCode")]
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mealType: MealType,
    val foodCode: String,
    val grams: Int
)
