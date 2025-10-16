package com.example.myhealth.data.nutrition.db

import androidx.room.Embedded
import androidx.room.Relation

// Relation that joins a meal entry with the corresponding food.
data class MealEntryWithFood(
    @Embedded val entry: MealEntryEntity,
    @Relation(
        parentColumn = "foodCode",
        entityColumn = "code"
    )
    val food: FoodEntity
)
