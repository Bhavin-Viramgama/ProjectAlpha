package com.example.projectalpha.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.projectalpha.data.local.entity.HabitCompletionLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitCompletionLogDao {

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE) // Ignore if already logged for that day (shouldn't happen with proper logic)
    suspend fun insertCompletionLog(log: HabitCompletionLogEntity)

    @Delete
    suspend fun deleteCompletionLog(log: HabitCompletionLogEntity) // For undoing a completion

    // Get all completion dates for a specific habit
    @Query("SELECT dateCompleted FROM habit_completion_logs WHERE habitId = :habitId ORDER BY dateCompleted DESC")
    fun getCompletionDatesForHabit(habitId: Int): Flow<List<LocalDate>>

    // Get completion dates for a specific habit within a date range (for the graph)
    @Query("SELECT dateCompleted FROM habit_completion_logs WHERE habitId = :habitId AND dateCompleted BETWEEN :startDate AND :endDate ORDER BY dateCompleted ASC")
    fun getCompletionDatesForHabitInRange(
        habitId: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<LocalDate>>

    // Check if a specific habit was completed on a specific date
    @Query("SELECT EXISTS(SELECT 1 FROM habit_completion_logs WHERE habitId = :habitId AND dateCompleted = :date LIMIT 1)")
    fun wasHabitCompletedOnDate(habitId: Int, date: LocalDate): Flow<Boolean>

    // For deleting all logs of a specific habit (e.g., if the habit itself is deleted, though CASCADE should handle this)
    @Query("DELETE FROM habit_completion_logs WHERE habitId = :habitId")
    suspend fun deleteAllLogsForHabit(habitId: Int)
}