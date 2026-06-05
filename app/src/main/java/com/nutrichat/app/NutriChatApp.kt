package com.nutrichat.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nutrichat.app.data.AppTheme
import com.nutrichat.app.data.UserPreferencesRepository
import com.nutrichat.app.ui.auth.LoginScreen
import com.nutrichat.app.ui.auth.RegisterScreen
import com.nutrichat.app.ui.auth.WelcomeScreen
import com.nutrichat.app.ui.chat.ChatScreen
import com.nutrichat.app.ui.dashboard.DashboardScreen
import com.nutrichat.app.ui.dashboard.AddMealScreen
import com.nutrichat.app.ui.dashboard.MealSelectionScreen
import com.nutrichat.app.ui.settings.SettingsScreen
import com.nutrichat.app.ui.theme.NutriChatTheme
import androidx.compose.material.icons.filled.Settings

sealed class Screen(val route: String, val label: String) {
    object Welcome : Screen("welcome", "Welcome")
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Chat : Screen("chat", "Chat")
    object Dashboard : Screen("dashboard", "Meals")
    object AddMeal : Screen("add_meal", "Add Meal")
    object MealSelection : Screen("meal_selection", "Recipes")
    object Settings : Screen("settings", "Settings")
}

@Composable
fun NutriChatApp() {
    val context = LocalContext.current
    // Use the Singleton instance to ensure real-time updates across the app
    val userPrefs = remember { UserPreferencesRepository.getInstance(context) }
    val appTheme by userPrefs.theme.collectAsState()

    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    NutriChatTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val currentUser = Firebase.auth.currentUser
        val startDestination = if (currentUser != null) Screen.Chat.route else Screen.Welcome.route

        val bottomBarScreens = listOf(Screen.Chat, Screen.Dashboard, Screen.MealSelection, Screen.Settings)
        val showBottomBar = currentDestination?.route in bottomBarScreens.map { it.route }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomBarScreens.forEach { screen ->
                            NavigationBarItem(
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    when (screen) {
                                        Screen.Chat -> Icon(Icons.Default.Chat, contentDescription = screen.label)
                                        Screen.Dashboard -> Icon(Icons.Default.BarChart, contentDescription = screen.label)
                                        Screen.MealSelection -> Icon(Icons.Default.RestaurantMenu, contentDescription = screen.label)
                                        Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = screen.label)
                                        else -> {}
                                    }
                                },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp))
            ) {
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                    )
                }
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(Screen.Register.route)
                        }
                    )
                }
                composable(Screen.Register.route) {
                    RegisterScreen(
                        onRegisterSuccess = {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = {
                            navController.navigate(Screen.Login.route)
                        }
                    )
                }
                composable(Screen.Chat.route) { ChatScreen() }
                composable(Screen.Dashboard.route) { 
                    DashboardScreen(onAddMeal = { navController.navigate(Screen.AddMeal.route) }) 
                }
                composable(Screen.AddMeal.route) { 
                    AddMealScreen(
                        onBack = { navController.popBackStack() }
                    ) 
                }
                composable(Screen.MealSelection.route) {
                    MealSelectionScreen(
                        onBack = { 
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                        },
                        onMealSaved = { 
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onLogout = {
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
