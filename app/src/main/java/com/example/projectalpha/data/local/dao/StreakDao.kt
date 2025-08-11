package com.example.projectalpha.data.local.dao

import androidx.room.*
import com.example.projectalpha.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakEntity)

    @Query("SELECT * FROM streak WHERE id = 0") // Assuming ID 0 for the single streak row
    fun getStreak(): Flow<StreakEntity?>

    // You might also have specific update methods if needed
    @Query("UPDATE streak SET points = :newPoints WHERE id = 0")
    suspend fun updateStreakPoints(newPoints: Int)

    @Query("SELECT points FROM streak WHERE id = 0")
    suspend fun getCurrentStreakPoints(): Int? // Can be nullable if no streak exists

}