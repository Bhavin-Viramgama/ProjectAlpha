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

class HabitsViewModel(
    private val habitRepository: HabitRepository,
    private val streakRepository: StreakRepository,
    private val application: Application // For SharedPreferences example
) : ViewModel() {

    private val _today = MutableStateFlow(LocalDate.now())
    private val sharedPreferences = application.getSharedPreferences("HabitPrefs", Context.MODE_PRIVATE)

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
            val today = LocalDate.now() // Always use current date for toggling
            val wasCompletedToday = habit.isCompletedForToday && habit.lastCompletedDate == today
            val newCompletionStatus = !wasCompletedToday // Toggle based on actual completion for today

            var newStreakCount = habit.streakCount
            var newLastCompletedDate = habit.lastCompletedDate

            if (newCompletionStatus) { // Marking as complete for today
                if (habit.lastCompletedDate == today.minusDays(1)) {
                    newStreakCount = habit.streakCount + 1 // Continued streak
                } else if (habit.lastCompletedDate != today) { // Not completed yesterday, nor today yet
                    newStreakCount = 1 // Start new streak
                }
                // If habit.lastCompletedDate == today, it means it's already marked completed today,
                // newStreakCount remains the same. This branch shouldn't be hit if wasCompletedToday is accurate.

                newLastCompletedDate = today
                if (!wasCompletedToday) { // Only award global points if it wasn't already marked complete
                    streakRepository.incrementStreakPoints(1)
                }
            } else { // Marking as incomplete for today
                if (wasCompletedToday) { // Only if it was actually completed today
                    // If it was the completion that formed/continued the streak today
                    if (habit.lastCompletedDate == today) {
                        newStreakCount = (habit.streakCount - 1).coerceAtLeast(0)
                    }
                    // Revert lastCompletedDate carefully, check if there was a completion yesterday
                    newLastCompletedDate = allHabits.value.find { it.id == habit.id }?.let { originalHabit ->
                        if (originalHabit.streakCount > newStreakCount && newStreakCount > 0) today.minusDays(1) else null
                    } ?: if (newStreakCount > 0) today.minusDays(1) else null


                    streakRepository.incrementStreakPoints(-1) // Decrement global points
                }
            }

            val updatedHabit = habit.copy(
                isCompletedForToday = newCompletionStatus,
                streakCount = newStreakCount,
                lastCompletedDate = newLastCompletedDate
            )
            habitRepository.updateHabit(updatedHabit)
            _today.tryEmit(LocalDate.now()) // Refresh today's habits view
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
            val today = LocalDate.now()
            if (habit.isCompletedForToday && habit.lastCompletedDate == today) {
                streakRepository.incrementStreakPoints(-1) // Decrement global points if deleted while completed today
            }
            habitRepository.deleteHabit(habit)
        }
    }

    fun performDailyResetIfNeeded() {
        viewModelScope.launch {
            val today = LocalDate.now()
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

