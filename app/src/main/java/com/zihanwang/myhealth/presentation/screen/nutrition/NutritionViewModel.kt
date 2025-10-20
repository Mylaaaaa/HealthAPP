package com.zihanwang.myhealth.presentation.screen.nutrition
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zihanwang.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.zihanwang.myhealth.presentation.screen.nutrition.db.MealEntryWithFood
import com.zihanwang.myhealth.presentation.screen.nutrition.db.NutritionDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    // ---------------- Targets (temporary defaults; you can later load from settings) ----------------
    data class MacroTargets(val p: Float, val c: Float, val f: Float)
    val kcalGoal = MutableStateFlow(2000)
    val macroTargets = MutableStateFlow(MacroTargets(p = 120f, c = 250f, f = 60f))

    // ---------------- Weekly totals for last 7 days ----------------
    data class DailyTotals(val date: LocalDate, val t: NutritionRepository.Totals)

    private val _weeklyTotals = MutableStateFlow<List<DailyTotals>>(emptyList())
    val weeklyTotals: StateFlow<List<DailyTotals>> = _weeklyTotals.asStateFlow()

    /**
     * Loads totals for the last 7 days ending at [center] (inclusive).
     * Requires NutritionRepository.getMealsBetweenGroupedByDate().
     */
    fun loadWeeklyTotals(center: LocalDate = date.value) = viewModelScope.launch {
        val end = center
        val start = end.minusDays(6)

        // DB access on IO
        val list: List<DailyTotals> = withContext(Dispatchers.IO) {
            val mealsByDay = repo.getMealsBetweenGroupedByDate(start, end)
            (0..6).map { i ->
                val d = start.plusDays(i.toLong())
                val t = repo.totals(mealsByDay[d].orEmpty())
                DailyTotals(date = d, t = t)
            }
        }

        // publish on Main
        _weeklyTotals.value = list
    }

    init {
        // Seed foods when table is empty so the Add dialog is never blank
        viewModelScope.launch {
            if (db.foodDao().count() == 0) {
                db.foodDao().upsertAll(
                    com.zihanwang.myhealth.presentation.screen.nutrition.db.Prepopulate.foods()
                )
            }
        }
    }
}
