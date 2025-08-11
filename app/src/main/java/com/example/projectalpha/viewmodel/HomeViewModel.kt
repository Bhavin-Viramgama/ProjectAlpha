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

    private val _monthlyTaskCount = MutableStateFlow(0)
    val monthlyTaskCount: StateFlow<Int> = _monthlyTaskCount.asStateFlow()

    private val _todaysTaskCount = MutableStateFlow(0)
    val todaysTaskCount: StateFlow<Int> = _todaysTaskCount.asStateFlow()

    init {
        loadMonthlyTaskCount()
        loadTodaysTaskCount()
        // Ensure streak exists (good place for this)
        viewModelScope.launch {
            streakRepository.ensureStreakExists()
        }
    }

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
            _todaysTaskCount.value = taskRepository.getTaskCountForToday(today)
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
