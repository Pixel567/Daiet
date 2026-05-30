package com.nutrichat.app.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.nutrichat.app.data.AppTheme
import com.nutrichat.app.data.DailyGoals
import com.nutrichat.app.data.UserPreferencesRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)
    private val prefs = application.getSharedPreferences("nutrichat_prefs", Context.MODE_PRIVATE)
    
    val goals = repository.goals
    val theme = repository.theme

    fun saveGoals(newGoals: DailyGoals) {
        repository.saveGoals(newGoals)
    }

    fun setTheme(theme: AppTheme) {
        repository.setTheme(theme)
    }

    fun getApiKey(): String = prefs.getString("groq_api_key", "") ?: ""

    fun saveApiKey(key: String) {
        prefs.edit().putString("groq_api_key", key).apply()
    }
}
