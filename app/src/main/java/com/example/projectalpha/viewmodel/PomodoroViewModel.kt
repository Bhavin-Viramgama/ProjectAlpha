package com.example.projectalpha.viewmodel

import android.os.CountDownTimer // Using classic Android CountDownTimer for simplicity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Default Pomodoro times in seconds - these will be our initial values
const val DEFAULT_WORK_DURATION_SECONDS = 25 * 60
const val DEFAULT_SHORT_BREAK_DURATION_SECONDS = 5 * 60
const val DEFAULT_LONG_BREAK_DURATION_SECONDS = 15 * 60

enum class PomodoroSessionType { WORK, SHORT_BREAK, LONG_BREAK }

// Data class to hold custom durations
data class PomodoroDurations(
    val work: Int = DEFAULT_WORK_DURATION_SECONDS,
    val shortBreak: Int = DEFAULT_SHORT_BREAK_DURATION_SECONDS,
    val longBreak: Int = DEFAULT_LONG_BREAK_DURATION_SECONDS
)

class PomodoroViewModel : ViewModel() { // Removed StreakRepository

    private val _customDurations = MutableStateFlow(PomodoroDurations())
    val customDurations: StateFlow<PomodoroDurations> = _customDurations.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(_customDurations.value.work)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentSessionType = MutableStateFlow(PomodoroSessionType.WORK)
    val currentSessionType: StateFlow<PomodoroSessionType> = _currentSessionType.asStateFlow()

    private var workSessionsCompleted = 0
    private var countDownTimer: CountDownTimer? = null

    init {
        // Initialize time remaining based on the current (default) session type and custom durations
        _timeRemainingSeconds.value = getDurationForSessionType(_currentSessionType.value)
    }


    fun pauseTimer() {
        if (_isRunning.value) {
            _isRunning.value = false
            countDownTimer?.cancel()
        }
    }

    fun startPauseTimer() {
        if (_isRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_timeRemainingSeconds.value <= 0) return // Don't start if time is zero

        _isRunning.value = true
        countDownTimer = object : CountDownTimer(_timeRemainingSeconds.value * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeRemainingSeconds.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                handleSessionFinished()
            }
        }.start()
    }


    fun resetTimer() {
        pauseTimer()
        _timeRemainingSeconds.value = getDurationForSessionType(_currentSessionType.value)
    }

    private fun handleSessionFinished() {
        _isRunning.value = false
        // Streak logic removed
        if (_currentSessionType.value == PomodoroSessionType.WORK) {
            workSessionsCompleted++
            if (workSessionsCompleted % 4 == 0 && workSessionsCompleted > 0) { // Long break after 4 work sessions
                _currentSessionType.value = PomodoroSessionType.LONG_BREAK
            } else {
                _currentSessionType.value = PomodoroSessionType.SHORT_BREAK
            }
        } else { // Break finished, go back to work
            _currentSessionType.value = PomodoroSessionType.WORK
        }
        _timeRemainingSeconds.value = getDurationForSessionType(_currentSessionType.value)
        // Optionally auto-start next session or wait for user
    }

    fun skipSession() {
        pauseTimer()
        // Reset work sessions count if skipping a break to ensure next long break is correct
        if (_currentSessionType.value != PomodoroSessionType.WORK) {
            // If skipping a break, and it was supposed to be a long break,
            // we effectively "reset" the cycle for the next long break count
            // or if it's a short break, just move to work.
            // This logic can be refined based on exact desired skip behavior for long breaks.
        }
        handleSessionFinished()
    }

    // Public getter for UI to know max duration
    fun getDurationForSessionType(type: PomodoroSessionType): Int {
        return when (type) {
            PomodoroSessionType.WORK -> _customDurations.value.work
            PomodoroSessionType.SHORT_BREAK -> _customDurations.value.shortBreak
            PomodoroSessionType.LONG_BREAK -> _customDurations.value.longBreak
        }
    }

    // Function to update custom durations (e.g., from a settings dialog)
    // Durations are expected in seconds
    fun updateCustomDurations(work: Int? = null, shortBreak: Int? = null, longBreak: Int? = null) {
        val current = _customDurations.value
        _customDurations.update {
            it.copy(
                work = work ?: current.work,
                shortBreak = shortBreak ?: current.shortBreak,
                longBreak = longBreak ?: current.longBreak
            )
        }
        // If the current session is not running, update its timer to the new duration
        if (!_isRunning.value) {
            _timeRemainingSeconds.value = getDurationForSessionType(_currentSessionType.value)
        }
    }

    /**
     * Directly sets the time remaining for the current session.
     * This is typically used when the user manually picks a time.
     * Only applies if the timer is not currently running.
     */
    fun setCurrentSessionTime(minutes: Int, seconds: Int) {
        if (!_isRunning.value) {
            val totalSeconds = (minutes * 60) + seconds
            // Cap the time to a reasonable maximum if needed, e.g., 99 minutes 59 seconds
            val maxAllowedSeconds = 99 * 60 + 59
            _timeRemainingSeconds.value = totalSeconds.coerceAtMost(maxAllowedSeconds).coerceAtLeast(0)
        }
    }



    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}


// Factory is needed because StreakRepository is a dependency
class PomodoroViewModelFactory(
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PomodoroViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for Pomodoro")
    }
}
