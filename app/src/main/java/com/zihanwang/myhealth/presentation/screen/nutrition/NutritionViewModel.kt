package com.zihanwang.myhealth.presentation.screen.nutrition

import kotlinx.coroutines.flow.*
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zihanwang.myhealth.presentation.screen.nutrition.db.ConditionEntity
import com.zihanwang.myhealth.presentation.screen.nutrition.db.MealEntryWithFood
import com.zihanwang.myhealth.presentation.screen.nutrition.db.NutritionDatabase
import com.zihanwang.myhealth.data.api.NutritionApiService
import com.zihanwang.myhealth.presentation.screen.nutrition.db.FoodEntity
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for Nutrition screens.
 *
 * Responsibilities:
 * - Manage meals, totals, and weekly data (local Room DB)
 * - Provide access to online nutrition API (external data)
 * - Support undo, health condition, and recommendations
 */
class NutritionViewModel(app: Application) : AndroidViewModel(app) {

    /* ---------------- Database and Repository ---------------- */
    private val db = NutritionDatabase.get(app)
    private val repo = NutritionRepository(db.foodDao(), db.mealEntryDao(), db.conditionDao())

    /* ---------------- External API Setup ---------------- */
    private val apiService = NutritionApiService.create()

    /* ------------------------------------------------------------------
     * External API Results (for Add Food dialog)
     * ------------------------------------------------------------------ */

    /** Lightweight model representing a food item from the API */
    data class ExternalFoodItem(
        val name: String,
        val kcal: Int,
        val protein: Float,
        val carb: Float,
        val fat: Float
    )

    // Holds the latest API search results
    private val _onlineSearchResults = MutableStateFlow<List<ExternalFoodItem>>(emptyList())
    val onlineSearchResults: StateFlow<List<ExternalFoodItem>> = _onlineSearchResults.asStateFlow()

    // Optional: indicates API request loading/error state
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    /**
     * Performs an online search using the Nutrition API.
     * Converts raw API results into ExternalFoodItem and updates UI.
     */
    fun searchFoodOnline(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _apiError.value = null
            try {
                val apiKey = "qAy0fX5QZpX0GgiEznevCBXia2YaS2hkiOMnJQ3g"
                val response = apiService.searchFood(query, apiKey)

                // Map the USDA response to simplified ExternalFoodItem model
                val mapped = response.foods.orEmpty().map { item ->
                    val nutrients = item.foodNutrients.orEmpty()

                    val kcal = nutrients.find { it.nutrientName?.contains("Energy", true) == true }?.value ?: 0.0
                    val protein = nutrients.find { it.nutrientName?.contains("Protein", true) == true }?.value ?: 0.0
                    val carb = nutrients.find { it.nutrientName?.contains("Carbohydrate", true) == true }?.value ?: 0.0
                    val fat = nutrients.find { it.nutrientName?.contains("Total lipid", true) == true }?.value ?: 0.0

                    ExternalFoodItem(
                        name = item.description ?: "Unknown food",
                        kcal = kcal.toInt(),
                        protein = protein.toFloat(),
                        carb = carb.toFloat(),
                        fat = fat.toFloat()
                    )
                }

                _onlineSearchResults.value = mapped.take(20) // Limit to top 20
            } catch (e: Exception) {
                e.printStackTrace()
                _apiError.value = e.message ?: "Unknown API error"
                _onlineSearchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }


    /* ---------------- Date selection ---------------- */
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _openDatePicker = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openDatePicker: SharedFlow<Unit> = _openDatePicker
    fun requestOpenDatePicker() { _openDatePicker.tryEmit(Unit) }

    /* ---------------- Meals of current date ---------------- */
    val meals: StateFlow<List<MealEntryWithFood>> =
        date.flatMapLatest { d -> repo.observeMeals(d) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totals = meals
        .map { repo.totals(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.totals(emptyList()))

    /* ---------------- Conditions & recommendations ---------------- */
    val allConditions: StateFlow<List<ConditionEntity>> =
        repo.observeAllConditions()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recommendedFoods =
        repo.observeRecommendedFoods()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /* ---------------- Macro targets ---------------- */
    data class MacroTargets(val p: Float, val c: Float, val f: Float)
    val kcalGoal = MutableStateFlow(2000)
    val macroTargets = MutableStateFlow(MacroTargets(p = 120f, c = 250f, f = 60f))

    /* ---------------- Weekly totals for last 7 days ---------------- */
    data class DailyTotals(val date: LocalDate, val t: NutritionRepository.Totals)
    private val _weeklyTotals = MutableStateFlow<List<DailyTotals>>(emptyList())
    val weeklyTotals: StateFlow<List<DailyTotals>> = _weeklyTotals.asStateFlow()

    fun loadWeeklyTotals(center: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val end = center
            val start = end.minusDays(6)
            val mealsByDay = repo.getMealsBetweenGroupedByDate(start, end)
            val list = (0..6).map { i ->
                val d = start.plusDays(i.toLong())
                val t = repo.totals(mealsByDay[d].orEmpty())
                DailyTotals(date = d, t = t)
            }
            _weeklyTotals.value = list
        }
    }

    /* ---------------- Snackbar & Undo Events ---------------- */
    sealed class UiEvent {
        data class ShowUndoDelete(val message: String) : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events
    private var lastDeleted: MealEntryWithFood? = null

    /* ---------------- CRUD Operations ---------------- */
    fun setDate(d: LocalDate) { _date.value = d }

    fun addMeal(meal: MealType, foodCode: String, grams: Int, foodEntity: FoodEntity? = null) =
        viewModelScope.launch {
            if (foodEntity != null) {
                repo.upsertFood(foodEntity)
            }

            repo.addMeal(
                date = _date.value,
                mealType = meal,
                foodCode = foodCode,
                grams = grams
            )

            loadWeeklyTotals(LocalDate.now())
        }

    fun deleteMeal(row: MealEntryWithFood) = viewModelScope.launch {
        repo.deleteMeal(row.entry.id)
        lastDeleted = row

        // 安全地访问 food（防止 NullPointerException）
        val foodName = row.food?.name ?: "Unknown food"
        _events.emit(UiEvent.ShowUndoDelete("Deleted: $foodName"))

        loadWeeklyTotals(LocalDate.now())
    }

    fun undoDelete() = viewModelScope.launch {
        val cached = lastDeleted ?: return@launch

        // 如果 food 为 null，跳过添加，避免崩溃
        val foodCode = cached.food?.code ?: return@launch

        repo.addMeal(
            date = _date.value,
            mealType = cached.entry.mealType,
            foodCode = foodCode,
            grams = cached.entry.grams
        )

        lastDeleted = null
        loadWeeklyTotals(LocalDate.now())
    }
    fun addCondition(name: String) =
        viewModelScope.launch { if (name.isNotBlank()) repo.addCondition(name) }

    fun toggleCondition(id: Long, selected: Boolean) =
        viewModelScope.launch { repo.setConditionSelected(id, selected) }

    fun removeCondition(id: Long) =
        viewModelScope.launch { repo.removeCondition(id) }

    /* ---------------- Initialization ---------------- */
    init {
        viewModelScope.launch {
            if (db.foodDao().count() == 0) {
                db.foodDao().upsertAll(
                    com.zihanwang.myhealth.presentation.screen.nutrition.db.Prepopulate.foods()
                )
            }
        }
        loadWeeklyTotals(LocalDate.now())
    }
}
