package com.zihanwang.myhealth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.zihanwang.myhealth.presentation.screen.nutrition.NutritionScreen
import org.junit.Rule
import org.junit.Test

class NutritionApiSearchUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun onlineSearch_showsLoadingIndicator() {

        rule.setContent { NutritionScreen() }

        rule.onNodeWithContentDescription("Add food")
            .performClick()

        rule.onNodeWithText("Search food (local or online)")
            .performTextInput("apple")

        rule.onNodeWithText("Search Online")
            .performClick()

        rule.onNodeWithTag("loading")
            .assertExists()
    }
}
