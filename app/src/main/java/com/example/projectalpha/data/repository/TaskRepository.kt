package com.example.projectalpha.data.repository

import com.example.projectalpha.data.local.dao.TaskDao
import com.example.projectalpha.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTaskById(taskId: Int): Flow<TaskEntity?> = taskDao.getTaskById(taskId)

    fun getTasksForDate(date: LocalDate): Flow<List<TaskEntity>> = taskDao.getTasksForDate(date)

    fun getTodaysIncompleteTasks(today: LocalDate): Flow<List<TaskEntity>> =
        taskDao.getTodaysIncompleteTasks(today)

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun getTaskCountForMonth(startDate: LocalDate, endDate: LocalDate): Int =
        taskDao.getTaskCountForMonth(startDate, endDate)

    fun getTaskCountForToday(date: LocalDate): Flow<Int> =
        taskDao.getTaskCountForToday(date)
}