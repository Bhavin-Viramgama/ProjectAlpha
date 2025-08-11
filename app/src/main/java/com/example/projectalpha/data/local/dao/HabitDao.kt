package com.example.projectalpha.data.local.dao

import androidx.room.*
import com.example.projectalpha.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habits ORDER BY name ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun getHabitById(habitId: Int): Flow<HabitEntity?>

    @Query("UPDATE habits SET isCompletedForToday = 0") // Reset for new day
    suspend fun resetAllHabitsCompletionStatus()
}