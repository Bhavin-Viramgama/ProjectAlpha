package com.example.projectalpha.data.repository

import com.example.projectalpha.data.local.entity.UserEntity // Assuming UserEntity exists
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    fun getUserProfile(): Flow<UserEntity?>
    suspend fun saveUserProfile(user: UserEntity)
    suspend fun updateUsername(userId: String, newName: String) // Example
    // Add other methods as needed
}