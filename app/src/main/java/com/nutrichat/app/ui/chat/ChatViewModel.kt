package com.nutrichat.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrichat.app.data.MealRepository
import com.nutrichat.app.data.UserPreferencesRepository
import com.nutrichat.app.network.GroqService
import com.nutrichat.app.network.NutritionalInfo
import com.nutrichat.app.network.NetworkUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
            text = "Hello! I am your AI Dietitian. Click the button below or tell me what you have in your fridge, and I'll suggest the perfect meal based on your goals and the time of day."
        )
    ),
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val mealRepo = MealRepository(application)
    private val userPrefs = UserPreferencesRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Real-time tracking of today's nutritional status
    private val todayStats = mealRepo.getMealsForDate(System.currentTimeMillis())
        .combine(userPrefs.goals) { meals, goals ->
            val consumedCal = meals.sumOf { it.calories }
            val consumedProt = meals.sumOf { it.protein }
            val consumedCarb = meals.sumOf { it.carbs }
            val consumedFat = meals.sumOf { it.fat }
            
            """
            Current Status:
            - Consumed: ${consumedCal.toInt()}/${goals.calories.toInt()} kcal
            - Protein: ${consumedProt.toInt()}/${goals.protein.toInt()}g
            - Carbs: ${consumedCarb.toInt()}/${goals.carbs.toInt()}g
            - Fat: ${consumedFat.toInt()}/${goals.fat.toInt()}g
            """.trimIndent()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun requestSuggestion(userInput: String = "") {
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _uiState.update { it.copy(snackbarMessage = "No internet connection.") }
            return
        }

        val key = userPrefs.getGroqApiKey()
        if (key.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please configure your Groq API Key in Settings.") }
            return
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val contextPrompt = """
            You are a professional dietitian. 
            Current time: $currentTime.
            ${todayStats.value}
            
            User's extra info/preferences: ${if (userInput.isBlank()) "None provided, just suggest something healthy." else userInput}
            
            Task: Propose ONE specific meal that the user should eat NOW. 
            Consider:
            1. What macronutrients are missing for today.
            2. The time of day (Breakfast/Lunch/Dinner/Snack).
            3. Keep the suggestion healthy and balanced.
            
            IMPORTANT: Provide a complete, detailed recipe including ingredients and step-by-step instructions.
            Respond with an encouraging explanation, the recipe, and the nutritional values.
        """.trimIndent()

        _uiState.update { s ->
            s.copy(
                messages = s.messages + (if (userInput.isNotBlank()) listOf(ChatMessage(isUser = true, text = userInput)) else emptyList())
                        + ChatMessage(isUser = false, text = "", isLoading = true),
                isLoading = true
            )
        }

        viewModelScope.launch {
            val result = GroqService(key).analyzeFood(contextPrompt)
            _uiState.update { s ->
                val withoutLoading = s.messages.filterNot { it.isLoading }
                result.fold(
                    onSuccess = { info ->
                        s.copy(
                            messages = withoutLoading + ChatMessage(
                                isUser = false,
                                text = info.summary,
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

    /**
     * Directly analyzes a food item or multiple items without suggesting recipes or whole meals.
     * Used for logging specific foods already eaten.
     */
    fun analyzeFoodDirectly(userInput: String) {
        if (!NetworkUtils.isInternetAvailable(getApplication())) {
            _uiState.update { it.copy(snackbarMessage = "No internet connection.") }
            return
        }

        val key = userPrefs.getGroqApiKey()
        if (key.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please configure your Groq API Key in Settings.") }
            return
        }

        _uiState.update { s ->
            s.copy(
                messages = s.messages + ChatMessage(isUser = true, text = userInput)
                        + ChatMessage(isUser = false, text = "", isLoading = true),
                isLoading = true
            )
        }

        viewModelScope.launch {
            // Passing a stricter prompt to prevent suggestions/recipes
            val directPrompt = """
                Analyze EXACTLY what the user provided: "$userInput".
                Provide nutritional values for this specific input only.
                Do NOT suggest recipes or other meals.
                If multiple items are listed, provide the sum of their nutritional values.
            """.trimIndent()

            val result = GroqService(key).analyzeFood(directPrompt)
            _uiState.update { s ->
                val withoutLoading = s.messages.filterNot { it.isLoading }
                result.fold(
                    onSuccess = { info ->
                        s.copy(
                            messages = withoutLoading + ChatMessage(
                                isUser = false,
                                text = "Analysis complete for: ${info.productName}",
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
            val meal = com.nutrichat.app.data.MealEntity(
                name = info.productName,
                amount = info.amount,
                calories = info.calories,
                protein = info.protein,
                carbs = info.carbs,
                fat = info.fat,
                fiber = info.fiber,
                recipe = info.recipe
            )
            mealRepo.insert(meal)
            _uiState.update { it.copy(snackbarMessage = "✅ ${info.productName} added to your day!") }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
}
