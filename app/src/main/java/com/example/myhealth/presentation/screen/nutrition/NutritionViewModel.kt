package com.example.myhealth.presentation.screen.nutrition
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.example.myhealth.presentation.screen.nutrition.db.MealEntryWithFood
import com.example.myhealth.presentation.screen.nutrition.db.NutritionDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class NutritionViewModel(app: Application) : AndroidViewModel(app) {

    private val db = NutritionDatabase.get(app)
    private val repo = NutritionRepository(db.foodDao(), db.mealEntryDao(), db.conditionDao())

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    // Expose a one-shot event to request opening the date picker from the app bar
    private val _openDatePicker = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openDatePicker: SharedFlow<Unit> = _openDatePicker

    fun requestOpenDatePicker() {
        _openDatePicker.tryEmit(Unit)
    }
    val meals: StateFlow<List<MealEntryWithFood>> =
        date.flatMapLatest { d -> repo.observeMeals(d) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totals = meals.map { repo.totals(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.totals(emptyList()))

    // Conditions
    val allConditions: StateFlow<List<ConditionEntity>> =
        repo.observeAllConditions()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recommendedFoods =
        repo.observeRecommendedFoods()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setDate(d: LocalDate) { _date.value = d }

    fun addMeal(meal: MealType, foodCode: String, grams: Int) =
        viewModelScope.launch { repo.addMeal(_date.value, meal, foodCode, grams) }

    fun deleteMeal(id: Long) =
        viewModelScope.launch { repo.deleteMeal(id) }

    fun addCondition(name: String) =
        viewModelScope.launch { if (name.isNotBlank()) repo.addCondition(name) }

    fun toggleCondition(id: Long, selected: Boolean) =
        viewModelScope.launch { repo.setConditionSelected(id, selected) }

    fun removeCondition(id: Long) =
        viewModelScope.launch { repo.removeCondition(id) }

    init {
        // Seed foods when table is empty so the Add dialog is never blank
        viewModelScope.launch {
            if (db.foodDao().count() == 0) {
                db.foodDao().upsertAll(
                    com.example.myhealth.presentation.screen.nutrition.db.Prepopulate.foods()
                )
            }
        }
    }
}
