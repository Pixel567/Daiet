package com.nutrichat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green80 = Color(0xFF2E7D32)
val Green60 = Color(0xFF4CAF50)
val Green20 = Color(0xFFE8F5E9)
val Teal60 = Color(0xFF00897B)
val Orange60 = Color(0xFFF57C00)

private val LightColors = lightColorScheme(
    primary = Green80,
    onPrimary = Color.White,
    primaryContainer = Green20,
    onPrimaryContainer = Green80,
    secondary = Teal60,
    onSecondary = Color.White,
    tertiary = Orange60,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEEEEEE),
    outline = Color(0xFFBDBDBD)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF00332E),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF333333),
    outline = Color(0xFF757575)
)

@Composable
fun NutriChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
