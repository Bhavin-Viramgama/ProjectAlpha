package com.example.projectalpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class ToDoViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    // Represents tasks associated with the _selectedDate
    private val _tasksForSelectedDate = MutableStateFlow<List<TaskEntity>>(emptyList())
    val tasksForSelectedDate: StateFlow<List<TaskEntity>> = _tasksForSelectedDate.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Observe changes to selectedDate and reload tasks
        viewModelScope.launch {
            selectedDate.collect { date ->
                loadTasksForDateInternal(date)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun loadTasksForDateInternal(date: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            taskRepository.getTasksForDate(date) // Assumes getTasksForDate uses the 'date' field
                .catch { e ->
                    _error.value = "Failed to load tasks: ${e.message}"
                    _isLoading.value = false
                }
                .collect { tasks ->
                    _tasksForSelectedDate.value = tasks
                    _isLoading.value = false
                    _error.value = null
                }
        }
    }

    /**
     * Adds a new task.
     * @param title The title of the task.
     * @param description Optional description for the task.
     * @param taskDate The primary date this task is associated with (maps to TaskEntity.date).
     * @param deadline Optional specific deadline with time (maps to TaskEntity.deadline).
     * @param priority Priority of the task.
     */
    fun addTask(
        title: String,
        description: String?,
        taskDate: LocalDate, // This will be stored in TaskEntity.date
        deadline: LocalDateTime?, // This will be stored in TaskEntity.deadline
        priority: String = "Medium" // Default priority
    ) {
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title,
                description = description,
                date = taskDate, // Set the date field
                deadline = deadline, // Set the deadline field
                priority = priority,
                isCompleted = false
            )
            try {
                _isLoading.value = true
                taskRepository.insertTask(newTask)
                // The flow from loadTasksForDateInternal should pick up the new task automatically
                // if the selectedDate matches the taskDate of the new task.
                // If not, you might want to explicitly call loadTasksForDateInternal(_selectedDate.value)
                // or ensure your UI updates based on the current _selectedDate.
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to add task: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                taskRepository.updateTask(task)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to update task: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to update task completion: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                taskRepository.deleteTask(task)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to delete task: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

class ToDoViewModelFactory(private val taskRepository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ToDoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ToDoViewModel(taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ToDo")
    }
}
