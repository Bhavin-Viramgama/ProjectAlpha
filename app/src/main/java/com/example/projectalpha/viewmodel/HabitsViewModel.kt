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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
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

    // --- State for Month-wise Graph ---
    private val _currentDisplayGraphMonth = MutableStateFlow(YearMonth.now())
    val currentDisplayGraphMonth: StateFlow<YearMonth> = _currentDisplayGraphMonth.asStateFlow()

    // --- Filter type state for the main list ---
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

    // Example of simplifying habitsToDisplay if graph becomes main detail view.
    // Otherwise, keep your more complex habitsToDisplay.
    private val _allHabitsWithCorrectedTodayStatus: Flow<List<HabitEntity>> =
        combine(habitRepository.getAllHabits(), _today) { allHabits, todayDate ->
            allHabits.map { habit ->
                val isScheduledToday = habit.daysOfWeek.any { dayString ->
                    dayString.equals(todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(), ignoreCase = true)
                }
                val isCompletedTodayCorrectly = if (isScheduledToday) {
                    (habit.lastCompletedDate == todayDate)
                } else {
                    false // Not scheduled, so not completed for today in terms of tracking
                }
                habit.copy(isCompletedForToday = isCompletedTodayCorrectly)
            }
        }

    val habitsToDisplay: StateFlow<List<HabitEntity>> =
        selectedFilterType.flatMapLatest { filterType ->
            when (filterType) {
                HabitFilterType.TODAY -> {
                    combine(_allHabitsWithCorrectedTodayStatus, _today) { allHabits, todayDate ->
                        val currentDayName = todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
                        allHabits.filter { habit ->
                            habit.daysOfWeek.any { dayString -> dayString.equals(currentDayName, ignoreCase = true) }
                        }
                    }
                }
                HabitFilterType.ALL -> _allHabitsWithCorrectedTodayStatus
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Functions to change graph month ---
    fun showNextMonthForGraph() {
        _currentDisplayGraphMonth.value = _currentDisplayGraphMonth.value.plusMonths(1)
    }

    fun showPreviousMonthForGraph() {
        _currentDisplayGraphMonth.value = _currentDisplayGraphMonth.value.minusMonths(1)
    }

    fun resetGraphMonthToCurrent() {
        _currentDisplayGraphMonth.value = YearMonth.now()
    }


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


    /**
     * Fetches completion history for a specific habit for the given month.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getHabitCompletionHistoryForMonth(habitId: Int): Flow<Set<LocalDate>> {
        return currentDisplayGraphMonth.flatMapLatest { yearMonth ->
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()
            habitRepository.getCompletionDatesForHabitInRange(habitId, startDate, endDate)
                .map { it.toSet() } // Convert List to Set
                .catch { e ->
                    Log.e("HabitsViewModel", "Error getting completion history for $habitId for month $yearMonth: ${e.message}")
                    emit(emptySet())
                }
        }
    }

    fun toggleHabitCompletion(habit: HabitEntity) {
        viewModelScope.launch {
            val todayDate = _today.value // Use the StateFlow's current value
            val isScheduledForToday = habit.daysOfWeek.any {
                it.equals(todayDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(), ignoreCase = true)
            }

            if (!isScheduledForToday) {
                val updatedHabitNonScheduled = habit.copy(isCompletedForToday = !habit.isCompletedForToday)
                habitRepository.updateHabit(updatedHabitNonScheduled)
                return@launch
            }

            val wasPreviouslyCompletedTodayAccordingToFlag = habit.isCompletedForToday // Check based on flag for UI consistency
            val nowMarkingAsComplete = !wasPreviouslyCompletedTodayAccordingToFlag

            var newStreakCount = habit.streakCount
            val newLastCompletedDate: LocalDate?

            if (nowMarkingAsComplete) {
                // If it's already marked as completed today via lastCompletedDate, don't increment streak again
                if (habit.lastCompletedDate != todayDate) {
                    newStreakCount += 1
                }
                newLastCompletedDate = todayDate
                habitRepository.addHabitCompletionLog(habit.id, todayDate)
                if (habit.lastCompletedDate != todayDate) { // Only increment global points if it's a new completion for the day
                    streakRepository.incrementStreakPoints(1)
                }
            } else { // Undoing completion
                // Only decrement streak if it was truly completed today
                if (habit.lastCompletedDate == todayDate) {
                    if (newStreakCount > 0) newStreakCount -= 1
                    streakRepository.incrementStreakPoints(-1)
                }
                // If unchecking, what should lastCompletedDate be?
                // If there are other logs for this habit, find the latest one before today.
                // For simplicity now, if unchecking today, set it to null or yesterday if streak >0
                val previousCompletions = habitRepository.getCompletionDatesForHabit(habit.id).first()
                newLastCompletedDate = previousCompletions.filter { it.isBefore(todayDate) }.maxOrNull()

                habitRepository.removeHabitCompletionLog(habit.id, todayDate)
            }

            val updatedHabit = habit.copy(
                isCompletedForToday = nowMarkingAsComplete,
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

    // Modify performDailyResetIfNeeded to be aware of the new log table
    // The current reset logic primarily resets isCompletedForToday flag and breaks streaks
    // based on lastCompletedDate. The new log table is more for historical record.
    // The streak breaking logic in performDailyResetIfNeeded should ideally use the logs
    // to determine if a streak should be broken, not just lastCompletedDate.

    // ... (rest of your ViewModel, especially performDailyResetIfNeeded, will need review)
    // For performDailyResetIfNeeded, when checking if a streak should be broken for a past day,
    // you'd query wasHabitCompletedOnDate(habit.id, dateToProcess) from the new log.
    // If it returns false, then break the streak.

    fun performDailyResetIfNeeded() {
        viewModelScope.launch {
            val todayDate = LocalDate.now() // Use a fresh now() for the check
            if (_today.value != todayDate) { // Update internal _today if necessary
                _today.value = todayDate
            }

            val lastResetDateStr = sharedPreferences.getString("lastHabitResetDate", null)
            val lastResetDate = lastResetDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: todayDate.minusDays(1)

            if (todayDate.isAfter(lastResetDate)) {
                Log.d("HabitsViewModel", "Performing daily reset. Today: $todayDate, Last Reset: $lastResetDate")
                habitRepository.resetAllHabitsCompletionStatus()

                val allHabitsCurrentState = habitRepository.getAllHabits().first()

                var dateToCheck = lastResetDate.plusDays(1)
                while (!dateToCheck.isAfter(todayDate.minusDays(1))) {
                    val dayNameForDateToCheck = dateToCheck.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
                    for (habit in allHabitsCurrentState) {
                        val isScheduledOnDateToCheck = habit.daysOfWeek.any { it.equals(dayNameForDateToCheck, ignoreCase = true) }
                        if (isScheduledOnDateToCheck) {
                            val completedOnDateToCheck = habitRepository.wasHabitCompletedOnDate(habit.id, dateToCheck).first()
                            if (!completedOnDateToCheck && habit.streakCount > 0) {
                                Log.d("HabitsViewModel", "Streak broken for '${habit.name}' on $dateToCheck. Old streak: ${habit.streakCount}")
                                // When breaking streak, lastCompletedDate should be the actual last completion before the break.
                                val previousLogs = habitRepository.getCompletionDatesForHabit(habit.id).first()
                                val newLastCompleted = previousLogs.filter { it.isBefore(dateToCheck) }.maxOrNull()
                                habitRepository.updateHabit(habit.copy(streakCount = 0, lastCompletedDate = newLastCompleted))
                            }
                        }
                    }
                    dateToCheck = dateToCheck.plusDays(1)
                }
                sharedPreferences.edit().putString("lastHabitResetDate", todayDate.toString()).apply()
            }
        }
    }

    init {
        Log.d("HabitsViewModel", "ViewModel initialized. Performing daily reset check.")
        performDailyResetIfNeeded()
    }

    // New function to provide data for the contribution graph
    fun getHabitCompletionHistory(habitId: Int, numberOfMonths: Int = 6): Flow<List<LocalDate>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(numberOfMonths.toLong()).withDayOfMonth(1)
        return habitRepository.getCompletionDatesForHabitInRange(habitId, startDate, endDate)
            .catch { e ->
                Log.e("HabitsViewModel", "Error getting habit completion history for $habitId: ${e.message}")
                emit(emptyList()) // Emit empty list on error
            }
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

