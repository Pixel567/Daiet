package com.nutrichat.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutrichat.app.data.MealEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onAddMeal: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                WeeklyCalendar(
                    selectedDate = state.selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMeal,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add meal")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CalorieSummaryCard(state)
            }
            item {
                MacroProgressCard(state)
            }
            item {
                Text(
                    "Daily Meals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (state.meals.isEmpty()) {
                item {
                    EmptyMealsCard()
                }
            } else {
                items(state.meals, key = { it.id }) { meal ->
                    MealCard(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeeklyCalendar(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 4.dp)
    ) { page ->
        val weekOffset = page - initialPage
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)

            for (i in 0 until 7) {
                val date = calendar.timeInMillis
                val isSelected = isSameDay(date, selectedDate)
                val dayName = SimpleDateFormat("E", Locale.ENGLISH).format(date).take(1)
                val dayNumber = SimpleDateFormat("d", Locale.ENGLISH).format(date)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onDateSelected(date) }
                        .padding(8.dp)
                        .width(40.dp)
                ) {
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNumber,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun CalorieSummaryCard(state: DashboardUiState) {
    val progress = if (state.goals.calories > 0) (state.calories / state.goals.calories).coerceIn(0.0, 1.0).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "calories_progress"
    )
    val remaining = (state.goals.calories - state.calories).coerceAtLeast(0.0)
    val overGoal = state.calories > state.goals.calories

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Calories", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "${state.calories.toInt()} kcal",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (overGoal) "Exceeded!" else "Remaining",
                        fontSize = 12.sp,
                        color = if (overGoal) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (overGoal) "+${(state.calories - state.goals.calories).toInt()}"
                        else "${remaining.toInt()} kcal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (overGoal) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = if (overGoal) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Goal: ${state.goals.calories.toInt()} kcal",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroProgressCard(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Macronutrients", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            MacroRow(
                label = "Protein",
                current = state.protein,
                goal = state.goals.protein,
                unit = "g",
                color = Color(0xFF1565C0)
            )
            Spacer(Modifier.height(12.dp))
            MacroRow(
                label = "Carbs",
                current = state.carbs,
                goal = state.goals.carbs,
                unit = "g",
                color = Color(0xFFF57C00)
            )
            Spacer(Modifier.height(12.dp))
            MacroRow(
                label = "Fat",
                current = state.fat,
                goal = state.goals.fat,
                unit = "g",
                color = Color(0xFF6A1B9A)
            )
            Spacer(Modifier.height(12.dp))
            MacroRow(
                label = "Fiber",
                current = state.fiber,
                goal = state.goals.fiber,
                unit = "g",
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun MacroRow(label: String, current: Double, goal: Double, unit: String, color: Color) {
    val progress = if (goal > 0) (current / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "macro_$label"
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "${"%.1f".format(current)} / ${goal.toInt()} $unit",
                fontSize = 13.sp,
                color = if (current > goal) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun MealCard(meal: MealEntity, onDelete: () -> Unit) {
    val timeStr = remember(meal.dateMillis) {
        SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(meal.dateMillis))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🍽️", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(meal.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    meal.amount,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroPill("${meal.calories.toInt()} kcal", Color(0xFF388E3C))
                    MacroPill("P: ${"%.0f".format(meal.protein)}g", Color(0xFF1565C0))
                    MacroPill("C: ${"%.0f".format(meal.carbs)}g", Color(0xFFF57C00))
                    MacroPill("F: ${"%.0f".format(meal.fat)}g", Color(0xFF6A1B9A))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MacroPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyMealsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🍽️", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("No meals logged", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Go to the Chat tab and tell me what you ate, or scan a product!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
