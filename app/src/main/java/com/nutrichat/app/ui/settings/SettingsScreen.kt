package com.nutrichat.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nutrichat.app.data.AppTheme
import com.nutrichat.app.data.DailyGoals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val goals by viewModel.goals.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    
    var editCalories by remember(goals) { mutableStateOf(goals.calories.toInt().toString()) }
    var editProtein by remember(goals) { mutableStateOf(goals.protein.toInt().toString()) }
    var editCarbs by remember(goals) { mutableStateOf(goals.carbs.toInt().toString()) }
    var editFat by remember(goals) { mutableStateOf(goals.fat.toInt().toString()) }
    var editFiber by remember(goals) { mutableStateOf(goals.fiber.toInt().toString()) }
    var groqApiKey by remember { mutableStateOf(viewModel.getApiKey()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val user = Firebase.auth.currentUser
            
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2ECC71)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Logged in as:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user?.email ?: "Unknown User",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2ECC71)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ThemeOption("Light", AppTheme.LIGHT, currentTheme) { viewModel.setTheme(it) }
                    ThemeOption("Dark", AppTheme.DARK, currentTheme) { viewModel.setTheme(it) }
                    ThemeOption("System", AppTheme.SYSTEM, currentTheme) { viewModel.setTheme(it) }
                }
            }

            Text(
                text = "Nutrition Goals",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2ECC71)
            )

            GoalInputField("Calories (kcal)", editCalories) { editCalories = it }
            GoalInputField("Protein (g)", editProtein) { editProtein = it }
            GoalInputField("Carbs (g)", editCarbs) { editCarbs = it }
            GoalInputField("Fat (g)", editFat) { editFat = it }
            GoalInputField("Fiber (g)", editFiber) { editFiber = it }

            Button(
                onClick = {
                    val newGoals = DailyGoals(
                        calories = editCalories.toDoubleOrNull() ?: goals.calories,
                        protein = editProtein.toDoubleOrNull() ?: goals.protein,
                        carbs = editCarbs.toDoubleOrNull() ?: goals.carbs,
                        fat = editFat.toDoubleOrNull() ?: goals.fat,
                        fiber = editFiber.toDoubleOrNull() ?: goals.fiber
                    )
                    viewModel.saveGoals(newGoals)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
            ) {
                Text("Save Goals")
            }

            Text(
                text = "AI Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2ECC71)
            )

            OutlinedTextField(
                value = groqApiKey,
                onValueChange = { groqApiKey = it },
                label = { Text("Groq API Key") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("gsk_...") },
                singleLine = true
            )

            Button(
                onClick = { viewModel.saveApiKey(groqApiKey) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
            ) {
                Text("Save API Key")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    Firebase.auth.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, theme: AppTheme, currentTheme: AppTheme, onClick: (AppTheme) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentTheme == theme,
            onClick = { onClick(theme) }
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun GoalInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}
