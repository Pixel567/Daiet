package com.nutrichat.app.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class MealCategory(
    @SerializedName("strCategory") val name: String,
    @SerializedName("strCategoryThumb") val thumbnail: String
)

data class MealSummary(
    @SerializedName("strMeal") val name: String,
    @SerializedName("strMealThumb") val thumbnail: String,
    @SerializedName("idMeal") val id: String
)

data class MealDetail(
    @SerializedName("idMeal") val id: String,
    @SerializedName("strMeal") val name: String,
    @SerializedName("strInstructions") val instructions: String,
    @SerializedName("strMealThumb") val thumbnail: String,
    val strIngredient1: String?, val strMeasure1: String?,
    val strIngredient2: String?, val strMeasure2: String?,
    val strIngredient3: String?, val strMeasure3: String?,
    val strIngredient4: String?, val strMeasure4: String?,
    val strIngredient5: String?, val strMeasure5: String?,
    val strIngredient6: String?, val strMeasure6: String?,
    val strIngredient7: String?, val strMeasure7: String?,
    val strIngredient8: String?, val strMeasure8: String?,
    val strIngredient9: String?, val strMeasure9: String?,
    val strIngredient10: String?, val strMeasure10: String?,
    val strIngredient11: String?, val strMeasure11: String?,
    val strIngredient12: String?, val strMeasure12: String?,
    val strIngredient13: String?, val strMeasure13: String?,
    val strIngredient14: String?, val strMeasure14: String?,
    val strIngredient15: String?, val strMeasure15: String?,
    val strIngredient16: String?, val strMeasure16: String?,
    val strIngredient17: String?, val strMeasure17: String?,
    val strIngredient18: String?, val strMeasure18: String?,
    val strIngredient19: String?, val strMeasure19: String?,
    val strIngredient20: String?, val strMeasure20: String?,
) {
    fun getIngredientsList(): String {
        val ingredients = mutableListOf<String>()
        val fields = this::class.java.declaredFields
        for (i in 1..20) {
            val ingField = fields.find { it.name == "strIngredient$i" }
            val measField = fields.find { it.name == "strMeasure$i" }
            val ing = ingField?.get(this) as? String
            val meas = measField?.get(this) as? String
            if (!ing.isNullOrBlank()) {
                ingredients.add("${meas ?: ""} $ing".trim())
            }
        }
        return ingredients.joinToString("\n• ", prefix = "• ")
    }
}

private data class CategoryResponse(val categories: List<MealCategory>)
private data class MealResponse(val meals: List<MealSummary>?)
private data class MealDetailResponse(val meals: List<MealDetail>?)

class MealDbService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://www.themealdb.com/api/json/v1/1"

    suspend fun getCategories(): List<MealCategory> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/categories.php").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        gson.fromJson(body, CategoryResponse::class.java).categories
    }

    suspend fun getMealsByCategory(category: String): List<MealSummary> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/filter.php?c=$category").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        gson.fromJson(body, MealResponse::class.java).meals ?: emptyList()
    }

    suspend fun getMealDetails(id: String): MealDetail? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/lookup.php?i=$id").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null
        gson.fromJson(body, MealDetailResponse::class.java).meals?.firstOrNull()
    }
}
