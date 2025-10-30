package com.zihanwang.myhealth.testing

import org.junit.Assert.assertEquals
import org.junit.Test

data class FoodEntity(
    val code: String,
    val name: String,
    val kcal: Int,
    val carb: Float,
    val protein: Float,
    val fat: Float
)

class NutritionRepository {
    fun combineTotals(foods: List<FoodEntity>): Totals {
        val kcal = foods.sumOf { it.kcal }
        val carb = foods.sumOf { it.carb.toDouble() }.toFloat()
        val protein = foods.sumOf { it.protein.toDouble() }.toFloat()
        val fat = foods.sumOf { it.fat.toDouble() }.toFloat()
        return Totals(kcal, carb, protein, fat)
    }

    data class Totals(val kcal: Int, val carb: Float, val protein: Float, val fat: Float)
}

class NutritionRepositoryTest {

    @Test
    fun testTotalsCalculation() {
        val repo = NutritionRepository()

        val food1 = FoodEntity("F001", "Apple", 52, 14f, 0.3f, 0.2f)
        val food2 = FoodEntity("F002", "Rice", 130, 28f, 2.7f, 0.3f)

        val totals = repo.combineTotals(listOf(food1, food2))

        assertEquals(182, totals.kcal)
        assertEquals(42.0f, totals.carb, 0.1f)
        assertEquals(3.0f, totals.protein, 0.1f)
        assertEquals(0.5f, totals.fat, 0.1f)
    }
}
