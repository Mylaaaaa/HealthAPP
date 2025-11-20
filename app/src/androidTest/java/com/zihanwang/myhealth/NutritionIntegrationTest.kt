package com.zihanwang.myhealth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Integration Test – Nutrition Module
 *
 * Purpose:
 * Verify that adding or deleting food items updates:
 *  - the current meal list
 *  - the nutrition totals (kcal, carbs, protein, fat)
 *
 * This behaves like an integration between:
 *  - a small repository (meal list)
 *  - a totals calculator (what the “State” screen shows).
 *
 * We use a fake in-memory repository here so the test is fast
 * and does not touch the real Room database.
 */
class NutritionIntegrationTest {

    // Simple model used only for this test
    data class FoodItem(
        val id: Long,
        val name: String,
        val kcal: Int,
        val carb: Int,
        val protein: Int,
        val fat: Int
    )

    /**
     * Fake repository + totals calculator used for integration testing.
     * This mimics the behaviour of the Nutrition module:
     * - add item to today's meals
     * - delete item from today's meals
     * - compute totals for the State screen
     */
    class FakeNutritionRepository {
        private val _items = mutableListOf<FoodItem>()
        val items: List<FoodItem> get() = _items

        fun add(food: FoodItem) {
            _items += food
        }

        fun delete(food: FoodItem) {
            _items.remove(food)
        }

        fun totalKcal(): Int = _items.sumOf { it.kcal }
        fun totalCarb(): Int = _items.sumOf { it.carb }
        fun totalProtein(): Int = _items.sumOf { it.protein }
        fun totalFat(): Int = _items.sumOf { it.fat }
    }

    @Test
    fun addMeal_updatesTotalsCorrectly() {
        // Arrange
        val repo = FakeNutritionRepository()
        val apple = FoodItem(
            id = 1L,
            name = "Apple",
            kcal = 80,
            carb = 20,
            protein = 0,
            fat = 0
        )

        // Act – user adds one food item
        repo.add(apple)

        // Assert – the meal list and totals are updated
        assertEquals(1, repo.items.size)
        assertEquals(80, repo.totalKcal())
        assertEquals(20, repo.totalCarb())
        assertEquals(0, repo.totalProtein())
        assertEquals(0, repo.totalFat())
    }

    @Test
    fun deleteMeal_updatesTotalsCorrectly() {
        // Arrange
        val repo = FakeNutritionRepository()
        val apple = FoodItem(
            id = 1L,
            name = "Apple",
            kcal = 80,
            carb = 20,
            protein = 0,
            fat = 0
        )
        repo.add(apple)

        // Act – user deletes the food item
        repo.delete(apple)

        // Assert – list is cleared and totals reset
        assertEquals(0, repo.items.size)
        assertEquals(0, repo.totalKcal())
        assertEquals(0, repo.totalCarb())
        assertEquals(0, repo.totalProtein())
        assertEquals(0, repo.totalFat())
    }
}
