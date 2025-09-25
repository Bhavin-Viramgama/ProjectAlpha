package com.example.projectalpha.viewmodel

import android.os.CountDownTimer // Using classic Android CountDownTimer for simplicity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projectalpha.viewmodel.DEFAULT_SHORT_BREAK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.DEFAULT_WORK_DURATION_SECONDS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.Int

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

    // Holds the duration the *current* countdown is based on (either default or manually set)
    private val _effectiveMaxDurationSeconds = MutableStateFlow(0) // Will be set in init
    val effectiveMaxDurationSeconds: StateFlow<Int> = _effectiveMaxDurationSeconds.asStateFlow()


    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentSessionType = MutableStateFlow(PomodoroSessionType.WORK)
    val currentSessionType: StateFlow<PomodoroSessionType> = _currentSessionType.asStateFlow()

    private val _upcomingSessionSequence = MutableStateFlow<List<PomodoroSessionType>>(emptyList())
    val upcomingSessionSequence: StateFlow<List<PomodoroSessionType>> = _upcomingSessionSequence.asStateFlow()


    private var workSessionsCompleted = 0
    private var countDownTimer: CountDownTimer? = null

    // Stores the time if manually set by the scrollable picker for the *current session instance*
    private var _lastManuallySetTimeForCurrentSessionInstance: Int? = null


    init {
        // Initialize time remaining based on the current (default) session type and custom durations
        val initialDuration = getDurationForSessionTypeFromDefaults(_currentSessionType.value)
        _timeRemainingSeconds.value = initialDuration
        _effectiveMaxDurationSeconds.value = initialDuration
        updateUpcomingSessionSequence() // Initialize the sequence

    }


    fun pauseTimer() {
        if (_isRunning.value) {
            _isRunning.value = false
            countDownTimer?.cancel()
            Log.d("PomodoroVM", "Timer Paused. Remaining: ${_timeRemainingSeconds.value}")
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
        // If time is zero, reset to what it should be for the current session instance
        if (_timeRemainingSeconds.value <= 0) {
            val resetTime = _lastManuallySetTimeForCurrentSessionInstance ?: getDurationForSessionTypeFromDefaults(_currentSessionType.value)
            if (resetTime <= 0) {
                Log.d("PomodoroVM", "Attempted to start with zero or negative time. Effective duration: $resetTime")
                // Potentially advance to next session if current is truly zero duration by default
                // For now, just prevent starting.
                return
            }
            _timeRemainingSeconds.value = resetTime
            _effectiveMaxDurationSeconds.value = resetTime
        }

        _isRunning.value = true
        Log.d("PomodoroVM", "Timer Started. Duration: ${_timeRemainingSeconds.value}, EffectiveMax: ${_effectiveMaxDurationSeconds.value}")
        countDownTimer = object : CountDownTimer(_timeRemainingSeconds.value * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeRemainingSeconds.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                Log.d("PomodoroVM", "Timer Finished. Session: ${_currentSessionType.value}")
                _timeRemainingSeconds.value = 0 // Ensure it hits zero on UI
                handleSessionFinished()
            }
        }.start()
    }


    fun resetTimer() {
        pauseTimer()
        val resetTime = _lastManuallySetTimeForCurrentSessionInstance ?: getDurationForSessionTypeFromDefaults(_currentSessionType.value)
        _timeRemainingSeconds.value = resetTime
        _effectiveMaxDurationSeconds.value = resetTime
        Log.d("PomodoroVM", "Timer Reset. To: $resetTime, Session: ${_currentSessionType.value}")
    }

    private fun handleSessionFinished() {
        _isRunning.value = false
        _lastManuallySetTimeForCurrentSessionInstance = null // Clear manual override for the *next* session

        val previousSessionType = _currentSessionType.value
        if (previousSessionType == PomodoroSessionType.WORK) {
            workSessionsCompleted++
            _currentSessionType.value = if (workSessionsCompleted % 4 == 0 && workSessionsCompleted > 0) {
                PomodoroSessionType.LONG_BREAK
            } else {
                PomodoroSessionType.SHORT_BREAK
            }
        } else {
            _currentSessionType.value = PomodoroSessionType.WORK
        }
        val newDefaultDuration = getDurationForSessionTypeFromDefaults(_currentSessionType.value)
        _timeRemainingSeconds.value = newDefaultDuration
        _effectiveMaxDurationSeconds.value = newDefaultDuration
        updateUpcomingSessionSequence()
        Log.d("PomodoroVM", "Session Handled. New Session: ${_currentSessionType.value}, Duration: $newDefaultDuration")

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
        Log.d("PomodoroVM", "Session Skipped. New Session: ${_currentSessionType.value}, Duration: ${_timeRemainingSeconds.value}")
    }

    private fun updateUpcomingSessionSequence() {
        val sequence = mutableListOf<PomodoroSessionType>()
        var tempWorkSessionsCompleted = workSessionsCompleted
        var nextSession = _currentSessionType.value

        // Add the current session first if you want to highlight it in the sequence
        // sequence.add(nextSession) // Optional: include current session

        // Predict next N sessions (e.g., next 4-5 sessions to show the cycle)
        for (i in 0 until 5) { // Show next 5 sessions in the sequence
            // Determine what the session *after* 'nextSession' would be
            val sessionAfterNext = if (nextSession == PomodoroSessionType.WORK) {
                tempWorkSessionsCompleted++ // Simulate completion of this work session
                if (tempWorkSessionsCompleted % 4 == 0 && tempWorkSessionsCompleted > 0) {
                    PomodoroSessionType.LONG_BREAK
                } else {
                    PomodoroSessionType.SHORT_BREAK
                }
            } else { // If nextSession is a break
                PomodoroSessionType.WORK
            }
            sequence.add(sessionAfterNext)
            nextSession = sessionAfterNext // Update nextSession for the next iteration
        }
        _upcomingSessionSequence.value = sequence
    }


    // Public getter for UI to know max duration
    fun getDurationForSessionTypeFromDefaults(type: PomodoroSessionType): Int {
        return when (type) {
            PomodoroSessionType.WORK -> _customDurations.value.work
            PomodoroSessionType.SHORT_BREAK -> _customDurations.value.shortBreak
            PomodoroSessionType.LONG_BREAK -> _customDurations.value.longBreak
        }
    }

    // Function to update custom durations (e.g., from a settings dialog)
    // Durations are expected in seconds
    fun updateCustomDurations(work: Int? = null, shortBreak: Int? = null, longBreak: Int? = null) {
        val currentGlobalDefaults = _customDurations.value
        _customDurations.update {
            it.copy(
                work = work ?: currentGlobalDefaults.work,
                shortBreak = shortBreak ?: currentGlobalDefaults.shortBreak,
                longBreak = longBreak ?: currentGlobalDefaults.longBreak
            )
        }
        // When global defaults are updated, the current session (if not running) should reflect this new global default,
        // and any prior manual override for this instance is voided.
        _lastManuallySetTimeForCurrentSessionInstance = null
        if (!_isRunning.value) {
            val newDefaultDuration = getDurationForSessionTypeFromDefaults(_currentSessionType.value)
            _timeRemainingSeconds.value = newDefaultDuration
            _effectiveMaxDurationSeconds.value = newDefaultDuration
        }
        Log.d("PomodoroVM", "Custom Durations Updated. Work: ${_customDurations.value.work}")
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
            val newTime = totalSeconds.coerceAtMost(maxAllowedSeconds).coerceAtLeast(0)

            _timeRemainingSeconds.value = newTime
            _effectiveMaxDurationSeconds.value = newTime // This is the new base for the progress bar
            _lastManuallySetTimeForCurrentSessionInstance = newTime // Store this as the manual override
            Log.d("PomodoroVM", "Manual Time Set: $newTime for session ${_currentSessionType.value}")
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
