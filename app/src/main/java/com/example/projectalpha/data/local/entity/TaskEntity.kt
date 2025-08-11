package com.example.projectalpha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime // Requires API 26+ or desugaring for older APIs

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String?,
    val deadline: LocalDateTime?, // Nullable if task has no deadline
    val priority: String, // "High", "Medium", "Low"
    val isCompleted: Boolean = false,
    val date: LocalDate // Date the task is for or created on
)
