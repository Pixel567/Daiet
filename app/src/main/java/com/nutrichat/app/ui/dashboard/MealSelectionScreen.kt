package com.nutrichat.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealSelectionScreen(
    onBack: () -> Unit,
    onMealSaved: () -> Unit,
    viewModel: MealSelectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var portionInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (uiState.selectedMealDetail != null) "Recipe Detail"
                        else if (uiState.selectedCategory != null) uiState.selectedCategory!!
                        else "Browse Recipes"
                    ) 
                },
                navigationIcon = {
                    if (uiState.selectedMealDetail != null || uiState.selectedCategory != null) {
                        IconButton(onClick = {
                            if (uiState.selectedMealDetail != null) {
                                viewModel.resetSelection()
                            } else if (uiState.selectedCategory != null) {
                                viewModel.backToCategories()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.errorMessage != null && uiState.categories.isEmpty() && uiState.meals.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Connection Error", fontWeight = FontWeight.Bold)
                    Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadCategories() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            } else {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.selectedMealDetail != null -> {
                        val detail = uiState.selectedMealDetail!!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = detail.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = detail.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(Modifier.height(16.dp))

                            if (uiState.isFetchingFullNutrition) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Calculating recipe nutrition...", fontSize = 12.sp)
                                }
                            } else if (uiState.fullRecipeInfo != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Estimated Full Recipe Nutrition", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            NutrientMiniPill("${uiState.fullRecipeInfo!!.calories.toInt()} kcal")
                                            NutrientMiniPill("P: ${uiState.fullRecipeInfo!!.protein.toInt()}g")
                                            NutrientMiniPill("C: ${uiState.fullRecipeInfo!!.carbs.toInt()}g")
                                            NutrientMiniPill("F: ${uiState.fullRecipeInfo!!.fat.toInt()}g")
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Ingredients", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(detail.getIngredientsList(), fontSize = 15.sp, lineHeight = 22.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Instructions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    val steps = detail.instructions
                                        .split(Regex("(?<=[.!?])\\s+|\\r?\\n+"))
                                        .filter { it.isNotBlank() && it.length > 5 }
                                    
                                    steps.forEachIndexed { index, step ->
                                        Row {
                                            Text("${index + 1}. ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(step.trim().removePrefix("-").trim(), fontSize = 15.sp, lineHeight = 22.sp)
                                        }
                                        if (index < steps.size - 1) {
                                            Spacer(Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(32.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(24.dp))
                            
                            if (uiState.analyzedInfo == null) {
                                Text("Log Your Portion", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Enter how much of this dish you ate to calculate nutrition.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = portionInput,
                                    onValueChange = { portionInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("e.g. 'one regular plate', '50%'") },
                                    label = { Text("Portion Description") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.analyzePortion(portionInput) },
                                    enabled = portionInput.isNotBlank() && !uiState.isAnalyzing,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (uiState.isAnalyzing) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                    } else {
                                        Text("Analyze & Add to Diary", fontSize = 16.sp)
                                    }
                                }
                            } else {
                                NutritionalResultView(
                                    info = uiState.analyzedInfo!!,
                                    editMode = EditMode.NONE,
                                    onEditModeChange = { },
                                    onAdd = {
                                        viewModel.saveMeal(it)
                                        onMealSaved()
                                    },
                                    onRecalculateAmount = { viewModel.analyzePortion(it) },
                                    onUpdateLocally = { }
                                )
                            }
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                    uiState.selectedCategory != null -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.meals) { meal ->
                                MealSummaryCard(meal) { viewModel.selectMeal(meal.id) }
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.categories) { category ->
                                CategoryCard(category) { viewModel.selectCategory(category.name) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientMiniPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun CategoryCard(category: com.nutrichat.app.network.MealCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            AsyncImage(
                model = category.thumbnail,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            Text(category.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MealSummaryCard(meal: com.nutrichat.app.network.MealSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            AsyncImage(
                model = meal.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Crop
            )
            Text(
                text = meal.name,
                modifier = Modifier.padding(12.dp),
                maxLines = 2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )
        }
    }
}
