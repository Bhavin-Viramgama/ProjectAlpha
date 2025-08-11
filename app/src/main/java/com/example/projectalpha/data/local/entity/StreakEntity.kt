package com.example.projectalpha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey
    val id: Int = 0, // Fixed ID since we'll likely only have one row for total points
    var points: Int
)