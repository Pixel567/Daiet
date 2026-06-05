package com.nutrichat.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutrichat.app.network.NutritionalInfo

enum class EditMode { NONE, AMOUNT, NUTRIENTS }

@Composable
fun NutritionalResultView(
    info: NutritionalInfo,
    editMode: EditMode,
    onEditModeChange: (EditMode) -> Unit,
    onAdd: (NutritionalInfo) -> Unit,
    onRecalculateAmount: (String) -> Unit,
    onUpdateLocally: (NutritionalInfo) -> Unit
) {
    var editAmount by remember(info, editMode) { mutableStateOf(info.amount) }
    var editCalories by remember(info, editMode) { mutableStateOf(info.calories.toInt().toString()) }
    var editProtein by remember(info, editMode) { mutableStateOf(info.protein.toString()) }
    var editCarbs by remember(info, editMode) { mutableStateOf(info.carbs.toString()) }
    var editFat by remember(info, editMode) { mutableStateOf(info.fat.toString()) }
    var editFiber by remember(info, editMode) { mutableStateOf(info.fiber.toString()) }
    var showRecipe by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(info.productName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            if (info.summary.isNotBlank() && editMode == EditMode.NONE) {
                Spacer(Modifier.height(4.dp))
                Text(info.summary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (info.recipe != null && editMode == EditMode.NONE) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showRecipe = !showRecipe },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        if (showRecipe) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showRecipe) "Hide Recipe" else "Show Full Recipe", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (showRecipe) {
                    Text(
                        text = info.recipe,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (editMode == EditMode.AMOUNT) {
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it },
                    label = { Text("Portion (e.g. 200g, half)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onRecalculateAmount(editAmount) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Calculate AI")
                    }
                    OutlinedButton(
                        onClick = { onEditModeChange(EditMode.NONE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            } else if (editMode == EditMode.NUTRIENTS) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientEditField("kcal", editCalories, Modifier.weight(1f)) { editCalories = it }
                        NutrientEditField("Protein", editProtein, Modifier.weight(1f)) { editProtein = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NutrientEditField("Carbs", editCarbs, Modifier.weight(1f)) { editCarbs = it }
                        NutrientEditField("Fat", editFat, Modifier.weight(1f)) { editFat = it }
                    }
                    NutrientEditField("Fiber", editFiber, Modifier.fillMaxWidth()) { editFiber = it }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdateLocally(info.copy(
                                    calories = editCalories.toDoubleOrNull() ?: info.calories,
                                    protein = editProtein.toDoubleOrNull() ?: info.protein,
                                    carbs = editCarbs.toDoubleOrNull() ?: info.carbs,
                                    fat = editFat.toDoubleOrNull() ?: info.fat,
                                    fiber = editFiber.toDoubleOrNull() ?: info.fiber
                                ))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                        OutlinedButton(
                            onClick = { onEditModeChange(EditMode.NONE) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            } else {
                Text("Portion: ${info.amount}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MacroBox("${info.calories.toInt()} kcal", "Calories", MaterialTheme.colorScheme.primary)
                    MacroBox("${"%.1f".format(info.protein)}g", "Protein", Color(0xFF1565C0))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MacroBox("${"%.1f".format(info.carbs)}g", "Carbs", Color(0xFFF57C00))
                    MacroBox("${"%.1f".format(info.fat)}g", "Fat", Color(0xFF6A1B9A))
                }

                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { onAdd(info) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add meal")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEditModeChange(EditMode.AMOUNT) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit portion", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { onEditModeChange(EditMode.NUTRIENTS) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit values", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientEditField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> 
            if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) onValueChange(input) 
        },
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

@Composable
fun MacroBox(value: String, label: String, color: Color) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
