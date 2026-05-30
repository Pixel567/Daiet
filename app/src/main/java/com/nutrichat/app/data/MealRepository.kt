package com.nutrichat.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MealRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).mealDao()

    suspend fun insert(meal: MealEntity): Long = dao.insert(meal)

    suspend fun delete(meal: MealEntity) = dao.delete(meal)

    fun getMealsForDate(dateMillis: Long): Flow<List<MealEntity>> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + 86_400_000L
        return dao.getMealsForDay(start, end)
    }
}
