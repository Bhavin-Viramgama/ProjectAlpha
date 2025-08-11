package com.example.projectalpha.data.repository

import com.example.projectalpha.data.local.dao.UserDao
import com.example.projectalpha.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Example Implementation (can be a placeholder)
class UserRepository(private val userDao: UserDao? = null) :
    IUserRepository { // Make userDao optional for easy placeholder

    override fun getUserProfile(): Flow<UserEntity?> {
        // return userDao?.getUserById("default_user") ?: flowOf(null) // Real implementation
        return flowOf(
            UserEntity(
                username = "Placeholder User",
                email = "contact@projectalpha.dev"
            )
        ) // Placeholder
    }

    override suspend fun saveUserProfile(user: UserEntity) {
        // userDao?.insertOrUpdateUser(user) // Real implementation
    }

    override suspend fun updateUsername(userId: String, newName: String) {
        // userDao?.updateUsername(userId, newName) // Real implementation
    }
}