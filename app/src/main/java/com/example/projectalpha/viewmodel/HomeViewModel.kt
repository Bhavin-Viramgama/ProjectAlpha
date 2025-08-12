package com.example.projectalpha.viewmodel

import androidx.lifecycle.*
import com.example.projectalpha.data.local.entity.StreakEntity
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.data.repository.StreakRepository
import com.example.projectalpha.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class HomeViewModel(
    private val taskRepository: TaskRepository,
    private val streakRepository: StreakRepository
) : ViewModel() {

    private val _username = MutableStateFlow("User") // Placeholder
    val username: StateFlow<String> = _username.asStateFlow()

    // Streak points
    val streakPoints: StateFlow<StreakEntity?> = streakRepository.getStreakFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // Or a default StreakEntity(points = 0)
        )

    // Today's tasks
    val todaysTasks: StateFlow<List<TaskEntity>> = taskRepository.getTodaysIncompleteTasks(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
/*
This was Wrong in Tasks Count which was not updating automatically while changes,
Two big issues here:
todaysTaskCount.collect inside here is actually collecting itself (endless loop / no trigger), so it doesn’t listen to DB changes.
You’re using taskRepository.getTaskCountForToday(date) as a suspend function returning an Int, which means it runs only when explicitly called, not automatically when the table changes.

How to fix
You need getTaskCountForToday() in your repository/DAO to return a Flow<Int> instead of a one-time Int, so the ViewModel can observe changes.

DAO
@Query("SELECT COUNT(*) FROM tasks WHERE date = :date")
fun getTaskCountForToday(date: LocalDate): Flow<Int>


Repository
fun getTaskCountForToday(date: LocalDate): Flow<Int> =
    taskDao.getTaskCountForToday(date)


ViewModel
Instead of manually launching and setting _todaysTaskCount, just convert it to a StateFlow directly:
val todaysTaskCount: StateFlow<Int> =
    taskRepository.getTaskCountForToday(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

This way, whenever a task is added, removed, or updated for today, the count will instantly update in your UI without reopening the app.

////Old Code!!!!
    private val _monthlyTaskCount = MutableStateFlow(0)
    val monthlyTaskCount: StateFlow<Int> = _monthlyTaskCount.asStateFlow()

    private val _todaysTaskCount = MutableStateFlow(0)
    val todaysTaskCount1 = StateFlow<Int> = _todaysTaskCount.asStateFlow()

    private fun loadMonthlyTaskCount() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val firstDayOfMonth = today.withDayOfMonth(1)
            val lastDayOfMonth = YearMonth.from(today).atEndOfMonth()
            _monthlyTaskCount.value = taskRepository.getTaskCountForMonth(firstDayOfMonth, lastDayOfMonth)
        }
    }

    private fun loadTodaysTaskCount() {
        viewModelScope.launch {
            val today = LocalDate.now()
            todaysTaskCount1.collect { // Initially getTaskCountForToday was returning Int
                _todaysTaskCount.value = taskRepository.getTaskCountForToday(today)
            }

        }
    }

 */


    val todaysTaskCount: StateFlow<Int> =
        taskRepository.getTaskCountForToday(LocalDate.now())
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )


    init {
        // Ensure streak exists (good place for this)
        viewModelScope.launch {
            streakRepository.ensureStreakExists()
        }
    }

    // You can add functions here to interact with repositories if needed for the home screen
}

// ViewModel Factory for manual instantiation (without Hilt)
class HomeViewModelFactory(
    private val taskRepository: TaskRepository,
    private val streakRepository: StreakRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(taskRepository, streakRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
