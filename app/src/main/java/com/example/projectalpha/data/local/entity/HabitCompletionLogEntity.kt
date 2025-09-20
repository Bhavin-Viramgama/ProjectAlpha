package com.example.projectalpha.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.example.projectalpha.data.local.database.Converters
import java.time.LocalDate

@Entity(
    tableName = "habit_completion_logs",
    primaryKeys = ["habitId", "dateCompleted"], // Composite primary key
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.Companion.CASCADE // If a habit is deleted, its completion logs are also deleted
        )
    ],
    indices = [Index(value = ["habitId"]), Index(value = ["dateCompleted"])] // Indices for faster querying
)
@TypeConverters(Converters::class) // To handle LocalDate
data class HabitCompletionLogEntity(
    val habitId: Int,
    val dateCompleted: LocalDate
)