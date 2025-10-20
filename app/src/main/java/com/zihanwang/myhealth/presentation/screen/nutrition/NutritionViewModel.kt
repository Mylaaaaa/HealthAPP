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

/**
 * ViewModel for Nutrition screens.
 * Changes in this version:
 * 1) Add delete-with-undo support via a one-shot UI event (Snackbar).
 * 2) Keep original data flows and architecture intact.
 * 3) No repository/db schema change is required.
 */
class NutritionViewModel(app: Application) : AndroidViewModel(app) {

    private val db = NutritionDatabase.get(app)
    private val repo = NutritionRepository(db.foodDao(), db.mealEntryDao(), db.conditionDao())

    // ---------------- Date selection ----------------
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    // One-shot event for opening date picker from the app bar
    private val _openDatePicker = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openDatePicker: SharedFlow<Unit> = _openDatePicker
    fun requestOpenDatePicker() { _openDatePicker.tryEmit(Unit) }

    // ---------------- Meals of current date ----------------
    val meals: StateFlow<List<MealEntryWithFood>> =
        date.flatMapLatest { d -> repo.observeMeals(d) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totals = meals.map { repo.totals(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.totals(emptyList()))

    // ---------------- Conditions & recommendations ----------------
    val allConditions: StateFlow<List<ConditionEntity>> =
        repo.observeAllConditions()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recommendedFoods =
        repo.observeRecommendedFoods()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------------- Targets (temporary defaults; can be loaded from settings later) ----------------
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

        // DB access on IO dispatcher
        val list: List<DailyTotals> = withContext(Dispatchers.IO) {
            val mealsByDay = repo.getMealsBetweenGroupedByDate(start, end)
            (0..6).map { i ->
                val d = start.plusDays(i.toLong())
                val t = repo.totals(mealsByDay[d].orEmpty())
                DailyTotals(date = d, t = t)
            }
        }

        _weeklyTotals.value = list
    }

    // ---------------- UI events (Snackbar) ----------------
    sealed class UiEvent {
        data class ShowUndoDelete(val message: String) : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    // Keep last deleted item for undo
    private var lastDeleted: MealEntryWithFood? = null

    // ---------------- Public commands ----------------
    fun setDate(d: LocalDate) { _date.value = d }

    fun addMeal(meal: MealType, foodCode: String, grams: Int) =
        viewModelScope.launch {
            repo.addMeal(
                date = _date.value,
                mealType = meal,
                foodCode = foodCode,
                grams = grams
            )
        }
    /**
     * Delete a meal (with undo support). We pass the full row so we can restore it on undo
     * without extra repository methods or schema changes.
     */
    fun deleteMeal(row: MealEntryWithFood) = viewModelScope.launch {
        // Delete by id in repository
        repo.deleteMeal(row.entry.id)
        // Cache last deleted for undo
        lastDeleted = row
        // Emit a one-shot event so the screen can show a Snackbar with "Undo"
        _events.emit(UiEvent.ShowUndoDelete("Deleted: ${row.food.name}"))
        // After deletion, any collectors of 'meals' and 'totals' will update automatically
    }

    /**
     * Undo the last deletion if any.
     */
    fun undoDelete() = viewModelScope.launch {
        val cached = lastDeleted ?: return@launch
        repo.addMeal(
            date = _date.value,
            mealType = cached.entry.mealType,
            foodCode = cached.food.code,
            grams = cached.entry.grams
        )
        lastDeleted = null
    }

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
                    com.zihanwang.myhealth.presentation.screen.nutrition.db.Prepopulate.foods()
                )
            }
        }
    }
}
