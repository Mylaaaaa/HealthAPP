package com.zihanwang.myhealth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import com.zihanwang.myhealth.presentation.screen.nutrition.NutritionScreen

class NutritionAddFoodUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addFoodButton_opensAddFoodDialog() {

        composeRule.setContent {
            NutritionScreen()
        }

        composeRule
            .onNodeWithContentDescription("Add food")
            .performClick()

        composeRule
            .onNodeWithText("Add Food")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Search Online")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Grams")
            .assertIsDisplayed()
    }
}
