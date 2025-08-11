package com.example.projectalpha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: String = "default_user", // Or generate dynamically
    val username: String,
    val email: String?
    // Add other user-related fields
)