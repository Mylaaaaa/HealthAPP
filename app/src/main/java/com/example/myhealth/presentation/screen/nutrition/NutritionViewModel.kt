package com.example.myhealth.presentation.screen.nutrition

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myhealth.data.nutrition.MealType
import com.example.myhealth.data.nutrition.NutritionRepository
import com.example.myhealth.data.nutrition.db.NutritionDatabase
import com.example.myhealth.data.nutrition.db.MealEntryWithFood
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

// AndroidViewModel so we can easily access application context for Room.
class NutritionViewModel(app: Application) : AndroidViewModel(app) {

    private val db = NutritionDatabase.get(app)
    private val repo = NutritionRepository(db.foodDao(), db.mealEntryDao())

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    // Meals for the selected date.
    val meals: StateFlow<List<MealEntryWithFood>> =
        date.flatMapLatest { d -> repo.observeMeals(d) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Totals derived from current meals.
    val totals = meals.map { repo.totals(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repo.totals(emptyList()))

    fun setDate(d: LocalDate) { _date.value = d }

    fun addMeal(meal: MealType, foodCode: String, grams: Int) {
        viewModelScope.launch { repo.addMeal(_date.value, meal, foodCode, grams) }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { repo.deleteMeal(id) }
    }
}
