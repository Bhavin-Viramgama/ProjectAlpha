package com.example.projectalpha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.projectalpha.data.local.database.Converters

@Entity(tableName = "habits")
@TypeConverters(Converters::class)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val daysOfWeek: List<String>, // e.g., ["Mon", "Wed", "Fri"]
    var streakCount: Int = 0,
    var isCompletedForToday: Boolean = false
)
