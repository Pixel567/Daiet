package com.nutrichat.app.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OpenFoodFactsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun getProduct(barcode: String): Result<NutritionalInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: error("Empty response")

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val json = gson.fromJson(raw, JsonObject::class.java)
            val status = json.get("status")?.asInt
            if (status != 1) {
                return@withContext Result.failure(Exception("Product not found"))
            }

            val product = json.getAsJsonObject("product")
            val productName = product.get("product_name")?.asString ?: "Unknown Product"
            val nutriments = product.getAsJsonObject("nutriments")

            // OFF provides values per 100g
            val calories = nutriments?.get("energy-kcal_100g")?.asDouble ?: 0.0
            val protein = nutriments?.get("proteins_100g")?.asDouble ?: 0.0
            val carbs = nutriments?.get("carbohydrates_100g")?.asDouble ?: 0.0
            val fat = nutriments?.get("fat_100g")?.asDouble ?: 0.0
            val fiber = nutriments?.get("fiber_100g")?.asDouble ?: 0.0

            Result.success(NutritionalInfo(
                productName = productName,
                amount = "100g",
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                summary = "Nutritional info per 100g"
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
