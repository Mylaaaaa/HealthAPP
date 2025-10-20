package com.zihanwang.myhealth.presentation.screen.nutrition

import com.zihanwang.myhealth.presentation.screen.nutrition.db.ConditionDao
import com.zihanwang.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.zihanwang.myhealth.presentation.screen.nutrition.db.FoodDao
import com.zihanwang.myhealth.presentation.screen.nutrition.db.FoodEntity
import com.zihanwang.myhealth.presentation.screen.nutrition.db.MealEntryDao
import com.zihanwang.myhealth.presentation.screen.nutrition.db.MealEntryEntity
import com.zihanwang.myhealth.presentation.screen.nutrition.db.MealEntryWithFood
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

class NutritionRepository(
    private val foodDao: FoodDao,
    private val entryDao: MealEntryDao,
    private val conditionDao: ConditionDao
) {
    // -------- Foods & Meals --------
    fun searchFoods(query: String): Flow<List<FoodEntity>> =
        if (query.isBlank()) foodDao.getAll() else foodDao.search(query)

    fun observeMeals(date: LocalDate): Flow<List<MealEntryWithFood>> =
        entryDao.observeByDate(date)

    suspend fun addMeal(date: LocalDate, mealType: MealType, foodCode: String, grams: Int) {
        entryDao.insert(MealEntryEntity(date = date, mealType = mealType, foodCode = foodCode, grams = grams))
    }

    suspend fun deleteMeal(id: Long) = entryDao.deleteById(id)

    data class Totals(
        val kcal: Int, val carb: Float, val protein: Float, val fat: Float,
        val sodium: Int, val sugar: Float, val satFat: Float
    )

    fun totals(meals: List<MealEntryWithFood>): Totals {
        var kcal = 0; var carb=0f; var protein=0f; var fat=0f; var sodium=0; var sugar=0f; var satFat=0f
        meals.forEach { m ->
            val f = m.food; val factor = m.entry.grams / 100f
            kcal += (f.kcal * factor).roundToInt()
            carb += f.carb * factor; protein += f.protein * factor; fat += f.fat * factor
            sodium += (f.sodium * factor).roundToInt(); sugar += f.sugar * factor; satFat += f.satFat * factor
        }
        return Totals(kcal, carb, protein, fat, sodium, sugar, satFat)
    }

    // -------- Conditions --------
    fun observeAllConditions(): Flow<List<ConditionEntity>> = conditionDao.observeAll()
    fun observeSelectedConditions(): Flow<List<ConditionEntity>> = conditionDao.observeSelected()
    suspend fun addCondition(name: String) = conditionDao.upsert(ConditionEntity(name = name.trim(), selected = true))
    suspend fun setConditionSelected(id: Long, selected: Boolean) = conditionDao.setSelected(id, selected)
    suspend fun removeCondition(id: Long) = conditionDao.deleteById(id)

    // -------- Recommended Foods (English-only version, always returns results) --------
    /**
     * Returns a recommended list of foods.
     * If health conditions are selected, filters foods according to common disease rules.
     * Otherwise, returns general healthy picks (high protein, low sugar/fat).
     */
    fun observeRecommendedFoods(): Flow<List<FoodEntity>> {
        val foods = foodDao.getAll()
        val selected = conditionDao.observeSelected()
        return combine(foods, selected) { allFoods, selectedConds ->
            val names = selectedConds.map { it.name.trim().lowercase() }

            // Define condition keywords in English
            val diabetesTokens = setOf("diabetes", "dm")
            val hypertensionTokens = setOf("hypertension", "high blood pressure")
            val hyperlipidemiaTokens = setOf("hyperlipidemia", "high cholesterol")

            // Helper to check if any token matches
            fun containsAny(tokens: Set<String>) =
                names.any { n -> tokens.any { t -> n.contains(t) } }

            var result = allFoods

            // Apply condition-based filters
            if (names.isNotEmpty()) {
                if (containsAny(diabetesTokens)) {
                    // Diabetes: prefer low-sugar foods
                    result = result.filter { it.sugar < 5f }
                }
                if (containsAny(hypertensionTokens)) {
                    // Hypertension: prefer low-sodium foods
                    result = result.filter { it.sodium < 100 }
                }
                if (containsAny(hyperlipidemiaTokens)) {
                    // High cholesterol: prefer low-fat foods
                    result = result.filter { it.fat < 3f }
                }

                // If everything got filtered out, fall back to all foods
                if (result.isEmpty()) result = allFoods
            }

            // Always produce a sorted recommendation list (even with no conditions)
            fun score(f: FoodEntity) = f.protein - 0.2f * f.fat - 0.1f * f.sugar
            result.sortedByDescending { score(it) }.take(12)
        }
    }

// -------- Range queries for weekly analysis --------
    /**
     * Returns all meals between [start] and [end] inclusive, grouped by LocalDate.
     * This method relies on MealEntryDao.getBetween(start, end).
     */
    suspend fun getMealsBetweenGroupedByDate(
        start: LocalDate,
        end: LocalDate
    ): Map<LocalDate, List<MealEntryWithFood>> {
        // Pull once from Flow
        val list: List<MealEntryWithFood> = entryDao.observeBetween(start, end).first()
        return list.groupBy { it.entry.date }
    }
}
