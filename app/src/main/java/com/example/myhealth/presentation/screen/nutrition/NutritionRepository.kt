package com.example.myhealth.data.nutrition

import com.example.myhealth.data.nutrition.db.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.math.roundToInt

// Repository coordinates DAOs and exposes business-level operations.
class NutritionRepository(
    private val foodDao: FoodDao,
    private val entryDao: MealEntryDao
) {
    fun searchFoods(query: String): Flow<List<FoodEntity>> =
        if (query.isBlank()) foodDao.getAll() else foodDao.search(query)

    fun observeMeals(date: LocalDate): Flow<List<MealEntryWithFood>> =
        entryDao.observeByDate(date)

    suspend fun addMeal(date: LocalDate, mealType: MealType, foodCode: String, grams: Int) {
        entryDao.insert(MealEntryEntity(date = date, mealType = mealType, foodCode = foodCode, grams = grams))
    }

    suspend fun deleteMeal(id: Long) {
        entryDao.deleteById(id)
    }

    // Aggregated totals for the UI summary chips.
    data class Totals(
        val kcal: Int,
        val carb: Float,
        val protein: Float,
        val fat: Float,
        val sodium: Int,
        val sugar: Float,
        val satFat: Float
    )

    fun totals(meals: List<MealEntryWithFood>): Totals {
        var kcal = 0; var carb=0f; var protein=0f; var fat=0f; var sodium=0; var sugar=0f; var satFat=0f
        meals.forEach { m ->
            val f = m.food
            val factor = m.entry.grams / 100f
            kcal += (f.kcal * factor).roundToInt()
            carb += f.carb * factor
            protein += f.protein * factor
            fat += f.fat * factor
            sodium += (f.sodium * factor).roundToInt()
            sugar += f.sugar * factor
            satFat += f.satFat * factor
        }
        return Totals(kcal, carb, protein, fat, sodium, sugar, satFat)
    }
}
