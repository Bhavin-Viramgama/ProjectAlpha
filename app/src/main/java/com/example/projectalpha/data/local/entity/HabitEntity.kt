package com.example.projectalpha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.projectalpha.data.local.database.Converters
import java.time.LocalDate // Import LocalDate

@Entity(tableName = "habits")
@TypeConverters(Converters::class) // Ensure Converters can handle LocalDate
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val daysOfWeek: List<String>, // e.g., ["MONDAY", "WEDNESDAY", "FRIDAY"] - use full names
    var streakCount: Int = 0,
    var isCompletedForToday: Boolean = false,
    var lastCompletedDate: LocalDate? = null // <<< ADDED
)

