package com.example.projectalpha.viewmodel

import android.app.Application // Needed for SharedPreferences example
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projectalpha.data.local.entity.HabitEntity
import com.example.projectalpha.data.repository.HabitRepository
import com.example.projectalpha.data.repository.StreakRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class HabitFilterType {
    TODAY,
    ALL
}

class HabitsViewModel(
    private val habitRepository: HabitRepository,
    private val streakRepository: StreakRepository,
    private val application: Application // For SharedPreferences example
) : ViewModel() {

    private val _today = MutableStateFlow(LocalDate.now())
    private val sharedPreferences = application.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)

    // Filter type state
    private val _selectedFilterType = MutableStateFlow(HabitFilterType.TODAY)
    val selectedFilterType: StateFlow<HabitFilterType> = _selectedFilterType.asStateFlow()

    // Raw flow for all habits from the repository
    private val _allHabitsFlow: Flow<List<HabitEntity>> = habitRepository.getAllHabits()

    // Flow for today's scheduled habits (for filtering logic)
    private val _todaysScheduledHabitsFlow: Flow<List<HabitEntity>> =
        combine(_allHabitsFlow, _today) { allHabits, todayDate ->
            val currentDayName = todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
            allHabits.filter { habit ->
                habit.daysOfWeek.any { dayString -> dayString.equals(currentDayName, ignoreCase = true) }
            }.map { habit -> // Apply UI-specific logic like ensuring isCompletedForToday is accurate
                if (habit.lastCompletedDate == todayDate && !habit.isCompletedForToday) {
                    habit.copy(isCompletedForToday = true)
                } else if (habit.lastCompletedDate != null && habit.lastCompletedDate!! < todayDate && habit.isCompletedForToday) {
                    habit.copy(isCompletedForToday = false)
                } else {
                    habit
                }
            }
        }

    // The main flow observed by the UI, switches based on selectedFilterType
    val habitsToDisplay: StateFlow<List<HabitEntity>> =
        selectedFilterType.flatMapLatest { filterType ->
            when (filterType) {
                HabitFilterType.TODAY -> _todaysScheduledHabitsFlow
                HabitFilterType.ALL -> _allHabitsFlow.map { allHabits -> // For "All", map to ensure isCompletedForToday is also accurate for the current day
                    val todayDate = _today.value
                    allHabits.map { habit ->
                        val isScheduledToday = habit.daysOfWeek.any { dayString -> dayString.equals(todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(), ignoreCase = true) }
                        if (isScheduledToday) {
                            if (habit.lastCompletedDate == todayDate && !habit.isCompletedForToday) {
                                habit.copy(isCompletedForToday = true)
                            } else if (habit.lastCompletedDate != null && habit.lastCompletedDate!! < todayDate && habit.isCompletedForToday) {
                                habit.copy(isCompletedForToday = false)
                            } else {
                                habit
                            }
                        } else {
                            // If not scheduled for today, ensure isCompletedForToday is false
                            habit.copy(isCompletedForToday = false)
                        }
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    fun setFilterType(filterType: HabitFilterType) {
        _selectedFilterType.value = filterType
    }

    val todaysHabits: StateFlow<List<HabitEntity>> =
        combine(habitRepository.getAllHabits(), _today) { allHabits, todayDate ->
            val currentDayName = todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
            // Log.d("HabitsViewModel", "Filtering for day: $currentDayName. All habits count: ${allHabits.size}")

            allHabits.filter { habit ->
                val isScheduled = habit.daysOfWeek.any { dayString -> dayString.equals(currentDayName, ignoreCase = true) }
                // Log.d("HabitsViewModel", "Habit '${habit.name}' scheduled for $currentDayName? $isScheduled. Days: ${habit.daysOfWeek}")
                isScheduled
            }.map { habit ->
                // Ensure isCompletedForToday reflects reality if app was closed
                if (habit.lastCompletedDate == todayDate && !habit.isCompletedForToday) {
                    habit.copy(isCompletedForToday = true) // Was completed today but flag is false
                } else if (habit.lastCompletedDate != null && habit.lastCompletedDate!! < todayDate && habit.isCompletedForToday) {
                    habit.copy(isCompletedForToday = false) // Last completed before today, reset flag
                } else {
                    habit
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // For managing all habits (e.g., in a settings screen or a different view)
    val allHabits: StateFlow<List<HabitEntity>> =
        habitRepository.getAllHabits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleHabitCompletion(habit: HabitEntity) {
        viewModelScope.launch {
            val today = _today.value
            val isScheduledForToday = habit.daysOfWeek.any {
                it.equals(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(), ignoreCase = true)
            }

            if (!isScheduledForToday) {
                // If not scheduled today, only toggle the UI state but don’t touch streaks
                val updatedHabit = habit.copy(isCompletedForToday = !habit.isCompletedForToday)
                habitRepository.updateHabit(updatedHabit)
                return@launch
            }

            // Habit is scheduled today
            val wasCompleted = habit.isCompletedForToday && habit.lastCompletedDate == today
            val nowCompleted = !wasCompleted

            var newStreakCount = habit.streakCount
            var newLastCompletedDate = habit.lastCompletedDate

            if (nowCompleted) {
                // First tap → complete
                newStreakCount += 1
                newLastCompletedDate = today
                streakRepository.incrementStreakPoints(1) // global +1
            } else {
                // Second tap → undo
                if (newStreakCount > 0) newStreakCount -= 1
                newLastCompletedDate = null
                streakRepository.incrementStreakPoints(-1) // global -1
            }

            val updatedHabit = habit.copy(
                isCompletedForToday = nowCompleted,
                streakCount = newStreakCount,
                lastCompletedDate = newLastCompletedDate
            )
            habitRepository.updateHabit(updatedHabit)
        }
    }


    fun addHabit(name: String, daysOfWeekStrings: List<String>) {
        if (name.isBlank() || daysOfWeekStrings.isEmpty()) {
            Log.e("HabitsViewModel", "Attempted to add habit with blank name or no selected days.")
            return
        }
        viewModelScope.launch {
            val habit = HabitEntity(
                name = name,
                daysOfWeek = daysOfWeekStrings.map { it.uppercase(Locale.ENGLISH) } // Store uppercase
            )
            habitRepository.insertHabit(habit)
        }
    }

    fun updateExistingHabit(habitToUpdate: HabitEntity) {
        viewModelScope.launch {
            // Ensure daysOfWeek are stored in uppercase consistently
            val correctlyCasedHabit = habitToUpdate.copy(
                daysOfWeek = habitToUpdate.daysOfWeek.map { it.uppercase(Locale.ENGLISH) }
            )
            habitRepository.updateHabit(correctlyCasedHabit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val today = _today.value
            if (habit.isCompletedForToday && habit.lastCompletedDate == today) {
                streakRepository.incrementStreakPoints(-1) // Decrement global points if deleted while completed today
            }
            habitRepository.deleteHabit(habit)
        }
    }

    fun performDailyResetIfNeeded() {
        viewModelScope.launch {
            val today = LocalDate.now() // Use fresh LocalDate.now() here for the check
            val lastResetDateStr = sharedPreferences.getString("lastHabitResetDate", null)
            val lastResetDate = lastResetDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today.minusDays(1)

            if (today.isAfter(lastResetDate)) {
                Log.d("HabitsViewModel", "Performing daily reset for habits. Today: $today, Last Reset: $lastResetDate")
                habitRepository.resetAllHabitsCompletionStatus() // Sets isCompletedForToday = false for all

                val habitsToCheck = habitRepository.getAllHabits().first() // Get current state from DB

                // Iterate from lastResetDate + 1 day up to yesterday
                var dateToProcess = lastResetDate.plusDays(1)
                while (!dateToProcess.isAfter(today.minusDays(1))) {
                    val dayToProcessName = dateToProcess.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
                    habitsToCheck.forEach { habit ->
                        if (habit.daysOfWeek.any { it.equals(dayToProcessName, ignoreCase = true) } && // Was scheduled
                            (habit.lastCompletedDate == null || habit.lastCompletedDate!! < dateToProcess) && // Not completed on or after this day
                            habit.streakCount > 0
                        ) {
                            Log.d("HabitsViewModel", "Breaking streak for habit: ${habit.name} for not completing on $dateToProcess")
                            habitRepository.updateHabit(habit.copy(streakCount = 0))
                        }
                    }
                    dateToProcess = dateToProcess.plusDays(1)
                }
                sharedPreferences.edit().putString("lastHabitResetDate", today.toString()).apply()
            }
            // Ensure _today StateFlow reflects the actual current date if logic runs across midnight or on app start
            if (_today.value != today) {
                _today.value = today
            }
        }
    }

    init {
        Log.d("HabitsViewModel", "ViewModel initialized. Performing daily reset check.")
        performDailyResetIfNeeded()
    }
}

class HabitsViewModelFactory(
    private val habitRepository: HabitRepository,
    private val streakRepository: StreakRepository,
    private val application: Application // Add Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitsViewModel(habitRepository, streakRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

