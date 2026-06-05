package com.nutrichat.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

data class DailyGoals(
    val calories: Double = 2000.0,
    val protein: Double = 150.0,
    val carbs: Double = 250.0,
    val fat: Double = 65.0,
    val fiber: Double = 30.0
)

class UserPreferencesRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nutrichat_prefs", Context.MODE_PRIVATE)

    private val _goals = MutableStateFlow(loadGoals())
    val goals = _goals.asStateFlow()

    private val _theme = MutableStateFlow(loadTheme())
    val theme = _theme.asStateFlow()

    fun saveGoals(newGoals: DailyGoals) {
        prefs.edit()
            .putFloat("goal_calories", newGoals.calories.toFloat())
            .putFloat("goal_protein", newGoals.protein.toFloat())
            .putFloat("goal_carbs", newGoals.carbs.toFloat())
            .putFloat("goal_fat", newGoals.fat.toFloat())
            .putFloat("goal_fiber", newGoals.fiber.toFloat())
            .apply()
        _goals.value = newGoals
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _theme.value = theme
    }

    fun getGroqApiKey(): String {
        return prefs.getString("groq_api_key", "") ?: ""
    }

    fun saveGroqApiKey(key: String) {
        prefs.edit().putString("groq_api_key", key).apply()
    }

    private fun loadGoals() = DailyGoals(
        calories = prefs.getFloat("goal_calories", 2000f).toDouble(),
        protein = prefs.getFloat("goal_protein", 150f).toDouble(),
        carbs = prefs.getFloat("goal_carbs", 250f).toDouble(),
        fat = prefs.getFloat("goal_fat", 65f).toDouble(),
        fiber = prefs.getFloat("goal_fiber", 30f).toDouble()
    )

    private fun loadTheme(): AppTheme {
        val themeName = prefs.getString("app_theme", AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(themeName ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
