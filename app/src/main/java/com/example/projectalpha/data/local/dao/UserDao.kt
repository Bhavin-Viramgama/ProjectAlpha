package com.example.projectalpha.data.local.dao

import androidx.room.*
import com.example.projectalpha.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Query("SELECT * FROM user_profile WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("UPDATE user_profile SET username = :newName WHERE id = :userId")
    suspend fun updateUsername(userId: String, newName: String)
}