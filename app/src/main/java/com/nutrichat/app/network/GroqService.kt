package com.nutrichat.app.network

import com.google.gson.Gson
import com.google.gson.JsonElement
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

            val systemPrompt = """
                You are a precise nutritional calculator. Your task is to extract food items and quantities from the input and calculate their total nutritional values.
                
                Rules:
                1. Identify the food and the EXACT quantity.
                2. Calculate values (calories, protein, carbs, fat, fiber) for the SPECIFIC PORTION provided, NOT for 100g.
                3. The "amount" field should show the quantity and estimated weight, e.g. "2 large eggs (~110g)".
                4. Set the "recipe" field ONLY if the user specifically asks for a recipe or meal suggestion. Otherwise, set "recipe" to null.
                5. IMPORTANT: Every field in the JSON MUST be a simple primitive (String, Number, or null). NEVER return an Object or Array inside any field.
                6. Respond in ENGLISH.
                
                JSON Format:
                {
                  "productName": "name of food",
                  "amount": "description and weight",
                  "calories": 0.0,
                  "protein": 0.0,
                  "carbs": 0.0,
                  "fat": 0.0,
                  "fiber": 0.0,
                  "summary": "one sentence nutritional summary",
                  "recipe": null
                }
            """.trimIndent()

            val bodyJson = mapOf(
                "model" to "llama-3.3-70b-versatile",
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to foodDescription)
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

            val responseJson = gson.fromJson(raw, JsonObject::class.java)
            val content = responseJson.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString
                ?: error("Unexpected response structure")

            // Safe parsing to handle cases where AI might ignore "no objects" rule
            val resultJsonObject = gson.fromJson(content.trim(), JsonObject::class.java)
            
            fun safeString(key: String): String {
                val element = resultJsonObject.get(key)
                return when {
                    element == null || element.isJsonNull -> ""
                    element.isJsonPrimitive -> element.asString
                    else -> element.toString()
                }
            }

            fun safeDouble(key: String): Double {
                val element = resultJsonObject.get(key)
                return when {
                    element == null || element.isJsonNull -> 0.0
                    element.isJsonPrimitive -> {
                        if (element.asJsonPrimitive.isNumber) element.asDouble
                        else element.asString.toDoubleOrNull() ?: 0.0
                    }
                    else -> 0.0
                }
            }

            val recipeElement = resultJsonObject.get("recipe")
            val recipeString = when {
                recipeElement == null || recipeElement.isJsonNull -> null
                recipeElement.isJsonPrimitive -> recipeElement.asString
                else -> recipeElement.toString()
            }

            val info = NutritionalInfo(
                productName = safeString("productName"),
                amount = safeString("amount"),
                calories = safeDouble("calories"),
                protein = safeDouble("protein"),
                carbs = safeDouble("carbs"),
                fat = safeDouble("fat"),
                fiber = safeDouble("fiber"),
                summary = safeString("summary"),
                recipe = recipeString
            )

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
