package com.nutrichat.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nutrichat.app.data.AppTheme
import com.nutrichat.app.data.DailyGoals
import com.nutrichat.app.data.UserPreferencesRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository.getInstance(application)
    
    val goals = repository.goals
    val theme = repository.theme

    fun saveGoals(newGoals: DailyGoals) {
        repository.saveGoals(newGoals)
    }

    fun setTheme(theme: AppTheme) {
        repository.setTheme(theme)
    }

    fun getApiKey(): String = repository.getGroqApiKey()

    fun saveApiKey(key: String) {
        repository.saveGroqApiKey(key)
    }
}
