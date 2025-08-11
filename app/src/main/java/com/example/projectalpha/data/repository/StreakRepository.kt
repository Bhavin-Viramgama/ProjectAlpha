package com.example.projectalpha.data.repository

import com.example.projectalpha.data.local.dao.StreakDao
import com.example.projectalpha.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull // To get a single value from Flow

class StreakRepository(private val streakDao: StreakDao) {

    fun getStreakFlow(): Flow<StreakEntity?> = streakDao.getStreak() // Keep the flow for observation

    suspend fun getCurrentStreak(): StreakEntity? {
        return streakDao.getStreak().firstOrNull() // Get the current value once
    }


    suspend fun updateStreakPoints(newPoints: Int) {
        streakDao.insertOrUpdateStreak(StreakEntity(points = newPoints))
    }

    suspend fun incrementStreakPoints(pointsToAdd: Int) {
        val currentPoints = streakDao.getCurrentStreakPoints() ?: 0 // Default to 0 if no streak
        streakDao.insertOrUpdateStreak(StreakEntity(points = currentPoints + pointsToAdd))
    }

    suspend fun ensureStreakExists() {
        if (streakDao.getCurrentStreakPoints() == null) {
            streakDao.insertOrUpdateStreak(StreakEntity(points = 0))
        }
    }
}