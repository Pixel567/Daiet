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

class GeminiService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun analyzeFood(foodDescription: String): Result<NutritionalInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

            val prompt = """
                Analyze the nutritional content of the following food and amount: "$foodDescription"
                
                Respond ONLY with a valid JSON object — no markdown, no extra text:
                {
                  "productName": "name of the food",
                  "amount": "amount as provided",
                  "calories": 0.0,
                  "protein": 0.0,
                  "carbs": 0.0,
                  "fat": 0.0,
                  "fiber": 0.0,
                  "summary": "1-2 sentence nutritional highlight"
                }
                
                Numeric values: grams (except calories = kcal). Use standard nutritional database estimates.
            """.trimIndent()

            val body = gson.toJson(
                mapOf(
                    "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))),
                    "generationConfig" to mapOf("temperature" to 0.1, "maxOutputTokens" to 512)
                )
            ).toRequestBody("application/json".toMediaType())

            val response = client.newCall(Request.Builder().url(url).post(body).build()).execute()
            val raw = response.body?.string() ?: error("Empty response")

            if (!response.isSuccessful) {
                val msg = gson.fromJson(raw, JsonObject::class.java)
                    .getAsJsonObject("error")?.get("message")?.asString ?: "HTTP ${response.code}"
                return@withContext Result.failure(Exception(msg))
            }

            val text = gson.fromJson(raw, JsonObject::class.java)
                .getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
                ?: error("Unexpected response structure")

            val clean = text.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            Result.success(gson.fromJson(clean, NutritionalInfo::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
