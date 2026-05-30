package com.nutrichat.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrichat.app.data.MealEntity
import com.nutrichat.app.data.MealRepository
import com.nutrichat.app.network.GroqService
import com.nutrichat.app.network.MealCategory
import com.nutrichat.app.network.MealDbService
import com.nutrichat.app.network.MealDetail
import com.nutrichat.app.network.MealSummary
import com.nutrichat.app.network.NutritionalInfo
import com.nutrichat.app.network.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MealSelectionUiState(
    val categories: List<MealCategory> = emptyList(),
    val meals: List<MealSummary> = emptyList(),
    val selectedCategory: String? = null,
    val selectedMealDetail: MealDetail? = null,
    val isLoading: Boolean = false,
    val analyzedInfo: NutritionalInfo? = null,
    val fullRecipeInfo: NutritionalInfo? = null,
    val isAnalyzing: Boolean = false,
    val isFetchingFullNutrition: Boolean = false,
    val errorMessage: String? = null
)

class MealSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val mealDbService = MealDbService()
    private val mealRepo = MealRepository(application)
    private val prefs = application.getSharedPreferences("nutrichat_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MealSelectionUiState())
    val uiState: StateFlow<MealSelectionUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _uiState.update { it.copy(errorMessage = "No internet connection.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val categories = mealDbService.getCategories()
                _uiState.update { it.copy(categories = categories, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load categories.") }
            }
        }
    }

    fun selectCategory(category: String) {
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _uiState.update { it.copy(errorMessage = "No internet connection.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedCategory = category, meals = emptyList(), errorMessage = null) }
            try {
                val meals = mealDbService.getMealsByCategory(category)
                _uiState.update { it.copy(meals = meals, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load meals.") }
            }
        }
    }

    fun selectMeal(mealId: String) {
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _uiState.update { it.copy(errorMessage = "No internet connection.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, fullRecipeInfo = null, analyzedInfo = null, errorMessage = null) }
            try {
                val detail = mealDbService.getMealDetails(mealId)
                _uiState.update { it.copy(selectedMealDetail = detail, isLoading = false) }
                if (detail != null) {
                    fetchFullRecipeNutrition(detail)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load recipe details.") }
            }
        }
    }

    private fun fetchFullRecipeNutrition(detail: MealDetail) {
        val apiKey = prefs.getString("groq_api_key", "") ?: ""
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingFullNutrition = true) }
            val ingredients = detail.getIngredientsList()
            val prompt = "Calculate total nutritional values for the ENTIRE RECIPE of this dish. Name: ${detail.name}. Ingredients: $ingredients. Note: This is for the whole preparation."
            
            val result = GroqService(apiKey).analyzeFood(prompt)
            result.onSuccess { info ->
                _uiState.update { it.copy(fullRecipeInfo = info, isFetchingFullNutrition = false) }
            }.onFailure {
                _uiState.update { it.copy(isFetchingFullNutrition = false) }
            }
        }
    }

    fun analyzePortion(portion: String) {
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _uiState.update { it.copy(errorMessage = "No internet connection.") }
            return
        }
        val detail = _uiState.value.selectedMealDetail ?: return
        val apiKey = prefs.getString("groq_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please configure your Groq API Key in Settings.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            val ingredients = detail.getIngredientsList()
            val prompt = "Dish name: ${detail.name}. Ingredients: $ingredients. Portion eaten: $portion. Calculate nutrition for this portion."
            
            val result = GroqService(apiKey).analyzeFood(prompt)
            result.onSuccess { info ->
                _uiState.update { it.copy(analyzedInfo = info, isAnalyzing = false) }
            }.onFailure {
                _uiState.update { it.copy(isAnalyzing = false, errorMessage = "AI analysis failed.") }
            }
        }
    }

    fun saveMeal(info: NutritionalInfo) {
        viewModelScope.launch {
            mealRepo.insert(
                MealEntity(
                    name = info.productName,
                    amount = info.amount,
                    calories = info.calories,
                    protein = info.protein,
                    carbs = info.carbs,
                    fat = info.fat,
                    fiber = info.fiber
                )
            )
        }
    }

    fun resetSelection() {
        _uiState.update { it.copy(selectedMealDetail = null, analyzedInfo = null, fullRecipeInfo = null, errorMessage = null) }
    }
    
    fun backToCategories() {
        _uiState.update { it.copy(selectedCategory = null, meals = emptyList(), errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
