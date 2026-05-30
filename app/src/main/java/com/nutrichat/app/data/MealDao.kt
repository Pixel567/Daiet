package com.nutrichat.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insert(meal: MealEntity): Long

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE dateMillis >= :startOfDay AND dateMillis < :endOfDay ORDER BY dateMillis DESC")
    fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>>
}
