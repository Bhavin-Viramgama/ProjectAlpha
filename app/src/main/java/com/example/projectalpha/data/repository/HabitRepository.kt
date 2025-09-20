package com.example.projectalpha.data.repository

import com.example.projectalpha.data.local.dao.HabitDao
import com.example.projectalpha.data.local.dao.HabitCompletionLogDao // <<< ADD IMPORT
import com.example.projectalpha.data.local.entity.HabitEntity
import com.example.projectalpha.data.local.entity.HabitCompletionLogEntity // <<< ADD IMPORT
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitCompletionLogDao: HabitCompletionLogDao // <<< ADD DAO
) {

    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    fun getHabitById(habitId: Int): Flow<HabitEntity?> = habitDao.getHabitById(habitId)

    suspend fun insertHabit(habit: HabitEntity) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        // Logs for this habit will be deleted automatically due to ForeignKey onDelete = CASCADE
        habitDao.deleteHabit(habit)
        // If you didn't have CASCADE, you'd call:
        // habitCompletionLogDao.deleteAllLogsForHabit(habit.id)
    }

    suspend fun resetAllHabitsCompletionStatus() {
        habitDao.resetAllHabitsCompletionStatus()
    }

    // --- New methods for completion logs ---
    suspend fun addHabitCompletionLog(habitId: Int, dateCompleted: LocalDate) {
        habitCompletionLogDao.insertCompletionLog(HabitCompletionLogEntity(habitId, dateCompleted))
    }

    suspend fun removeHabitCompletionLog(habitId: Int, dateCompleted: LocalDate) {
        habitCompletionLogDao.deleteCompletionLog(HabitCompletionLogEntity(habitId, dateCompleted))
    }

    fun getCompletionDatesForHabit(habitId: Int): Flow<List<LocalDate>> {
        return habitCompletionLogDao.getCompletionDatesForHabit(habitId)
    }

    fun getCompletionDatesForHabitInRange(habitId: Int, startDate: LocalDate, endDate: LocalDate): Flow<List<LocalDate>> {
        return habitCompletionLogDao.getCompletionDatesForHabitInRange(habitId, startDate, endDate)
    }

    fun wasHabitCompletedOnDate(habitId: Int, date: LocalDate): Flow<Boolean> {
        return habitCompletionLogDao.wasHabitCompletedOnDate(habitId, date)
    }
}

