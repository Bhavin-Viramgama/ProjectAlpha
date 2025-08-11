package com.example.projectalpha.data.repository

import com.example.projectalpha.data.local.dao.HabitDao
import com.example.projectalpha.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {

    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    fun getHabitById(habitId: Int): Flow<HabitEntity?> = habitDao.getHabitById(habitId)

    suspend fun insertHabit(habit: HabitEntity) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabit(habit)
    }

    suspend fun resetAllHabitsCompletionStatus() {
        habitDao.resetAllHabitsCompletionStatus()
    }
}