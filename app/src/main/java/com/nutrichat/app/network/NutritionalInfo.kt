package com.nutrichat.app.network

data class NutritionalInfo(
    val productName: String = "",
    val amount: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val summary: String = ""
)
