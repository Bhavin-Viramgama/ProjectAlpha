package com.example.projectalpha.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.projectalpha.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Int): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY date DESC, priority ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    fun getAllTasksList(): List<TaskEntity> // Synchronous for pre-population check

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY deadline ASC")
    fun getTasksForDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND date = :today ORDER BY deadline ASC")
    fun getTodaysIncompleteTasks(today: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTaskCountForMonth(startDate: LocalDate, endDate: LocalDate): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date")
    suspend fun getTaskCountForToday(date: LocalDate): Int
}