package com.nutrichat.app.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GroqService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun analyzeFood(foodDescription: String): Result<NutritionalInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.groq.com/openai/v1/chat/completions"

            val prompt = """
                Analyze the nutritional content of the following food: "$foodDescription"
                
                Instructions:
                1. Identify the food and the EXACT quantity (e.g., "half a carrot", "2 slices of bread", "one medium apple").
                2. If the quantity is descriptive (e.g., "half", "one piece", "a handful"), ESTIMATE the weight in grams for that specific portion.
                3. IMPORTANT: Calculate ALL nutritional values (calories, protein, carbs, fat, fiber) for the SPECIFIC PORTION mentioned, NOT for 100g.
                4. If no amount is specified, assume a standard serving size and mention it.
                5. The "amount" field should contain the descriptive quantity and the estimated weight, e.g., "half a carrot (~35g)".
                6. Respond in ENGLISH.
                
                Respond ONLY with a valid JSON object:
                {
                  "productName": "product name",
                  "amount": "quantity and estimated weight",
                  "calories": 0.0,
                  "protein": 0.0,
                  "carbs": 0.0,
                  "fat": 0.0,
                  "fiber": 0.0,
                  "summary": "short nutritional summary"
                }
                
                Numeric values: Use grams (except calories = kcal).
            """.trimIndent()

            val bodyJson = mapOf(
                "model" to "llama-3.3-70b-versatile",
                "messages" to listOf(
                    mapOf("role" to "user", "content" to prompt)
                ),
                "response_format" to mapOf("type" to "json_object"),
                "temperature" to 0.1
            )

            val body = gson.toJson(bodyJson).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: error("Empty response")

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val json = gson.fromJson(raw, JsonObject::class.java)
                    json.getAsJsonObject("error")?.get("message")?.asString ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $raw"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val content = gson.fromJson(raw, JsonObject::class.java)
                .getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString
                ?: error("Unexpected response structure")

            Result.success(gson.fromJson(content.trim(), NutritionalInfo::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
