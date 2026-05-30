package com.nutrichat.app.ui.chat

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrichat.app.data.MealEntity
import com.nutrichat.app.data.MealRepository
import com.nutrichat.app.network.GroqService
import com.nutrichat.app.network.NutritionalInfo
import com.nutrichat.app.network.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val nutritionalInfo: NutritionalInfo? = null,
    val isLoading: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            isUser = false,
            text = "Tell me about your dish."
        )
    ),
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MealRepository(application)
    private val prefs = application.getSharedPreferences("nutrichat_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(input: String) {
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _uiState.update { it.copy(snackbarMessage = "No internet connection. Please check your network.") }
            return
        }

        val key = getApiKey()
        if (key.isBlank()) {
            appendBot("Please configure your Groq API Key in Settings.")
            return
        }

        _uiState.update { s ->
            s.copy(
                messages = s.messages + ChatMessage(isUser = true, text = input)
                        + ChatMessage(isUser = false, text = "", isLoading = true),
                isLoading = true
            )
        }

        viewModelScope.launch {
            val result = GroqService(key).analyzeFood(input)
            _uiState.update { s ->
                val withoutLoading = s.messages.filterNot { it.isLoading }
                result.fold(
                    onSuccess = { info ->
                        s.copy(
                            messages = withoutLoading + ChatMessage(
                                isUser = false,
                                text = buildText(info),
                                nutritionalInfo = info
                            ),
                            isLoading = false
                        )
                    },
                    onFailure = { err ->
                        s.copy(
                            messages = withoutLoading + ChatMessage(
                                isUser = false,
                                text = "❌ Error: ${err.message}"
                            ),
                            isLoading = false
                        )
                    }
                )
            }
        }
    }

    fun addToMealPlan(info: NutritionalInfo) {
        viewModelScope.launch {
            repo.insert(
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
            _uiState.update { it.copy(snackbarMessage = "✅ ${info.productName} added to meals!") }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun saveApiKey(key: String) = prefs.edit().putString("groq_api_key", key).apply()

    fun getApiKey(): String = prefs.getString("groq_api_key", "") ?: ""

    private fun appendBot(text: String) {
        _uiState.update { s ->
            s.copy(messages = s.messages + ChatMessage(isUser = false, text = text))
        }
    }

    private fun buildText(info: NutritionalInfo) = """
📊 ${info.productName} (${info.amount})

🔥 Calories:  ${info.calories.toInt()} kcal
💪 Protein:   ${"%.1f".format(info.protein)} g
🌾 Carbs: ${"%.1f".format(info.carbs)} g
🧈 Fat:  ${"%.1f".format(info.fat)} g
🌿 Fiber:  ${"%.1f".format(info.fiber)} g

${info.summary}
    """.trimIndent()
}
