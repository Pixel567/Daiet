package com.nutrichat.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrichat.app.data.MealEntity
import com.nutrichat.app.data.MealRepository
import com.nutrichat.app.data.UserPreferencesRepository
import com.nutrichat.app.data.DailyGoals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val meals: List<MealEntity> = emptyList(),
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val goals: DailyGoals = DailyGoals(),
    val selectedDate: Long = System.currentTimeMillis()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MealRepository(application)
    private val userPrefs = UserPreferencesRepository.getInstance(application)

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedDate.flatMapLatest { date -> repo.getMealsForDate(date) },
        userPrefs.goals,
        _selectedDate
    ) { meals, goals, selectedDate ->
        DashboardUiState(
            meals = meals,
            calories = meals.sumOf { it.calories },
            protein = meals.sumOf { it.protein },
            carbs = meals.sumOf { it.carbs },
            fat = meals.sumOf { it.fat },
            fiber = meals.sumOf { it.fiber },
            goals = goals,
            selectedDate = selectedDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun selectDate(dateMillis: Long) {
        _selectedDate.value = dateMillis
    }

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch { repo.delete(meal) }
    }
}
