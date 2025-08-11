package com.example.projectalpha.viewmodel

import android.os.CountDownTimer // Using classic Android CountDownTimer for simplicity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projectalpha.data.repository.StreakRepository // To update streak on session completion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Default Pomodoro times in seconds
const val WORK_DURATION_SECONDS = 25 * 60
const val SHORT_BREAK_DURATION_SECONDS = 5 * 60
const val LONG_BREAK_DURATION_SECONDS = 15 * 60

enum class PomodoroSessionType { WORK, SHORT_BREAK, LONG_BREAK }

class PomodoroViewModel(
    private val streakRepository: StreakRepository // Inject if you update streak
) : ViewModel() {

    private val _timeRemainingSeconds = MutableStateFlow(WORK_DURATION_SECONDS)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentSessionType = MutableStateFlow(PomodoroSessionType.WORK)
    val currentSessionType: StateFlow<PomodoroSessionType> = _currentSessionType.asStateFlow()

    private var workSessionsCompleted = 0
    private var countDownTimer: CountDownTimer? = null

    fun startPauseTimer() {
        if (_isRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
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

    private fun pauseTimer() {
        _isRunning.value = false
        countDownTimer?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _timeRemainingSeconds.value = getDurationForSessionType(_currentSessionType.value)
    }

    private fun handleSessionFinished() {
        _isRunning.value = false
        // Potentially award points or log completion
        if (_currentSessionType.value == PomodoroSessionType.WORK) {
            workSessionsCompleted++
            viewModelScope.launch {
                streakRepository.incrementStreakPoints(1) // Example: 1 point per work session
            }
            if (workSessionsCompleted % 4 == 0) { // Long break after 4 work sessions
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
        handleSessionFinished() // Similar logic to finishing a session
    }

    internal fun getDurationForSessionType(type: PomodoroSessionType): Int {
        return when (type) {
            PomodoroSessionType.WORK -> WORK_DURATION_SECONDS
            PomodoroSessionType.SHORT_BREAK -> SHORT_BREAK_DURATION_SECONDS
            PomodoroSessionType.LONG_BREAK -> LONG_BREAK_DURATION_SECONDS
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel() // Ensure timer is cancelled when ViewModel is cleared
    }
}

// Factory is needed because StreakRepository is a dependency
class PomodoroViewModelFactory(
    private val streakRepository: StreakRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PomodoroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PomodoroViewModel(streakRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for Pomodoro")
    }
}
