package com.example.projectalpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.projectalpha.data.repository.IUserRepository // Assuming interface
import com.example.projectalpha.data.repository.StreakRepository
import com.example.projectalpha.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Placeholder UI State for the Profile Screen
data class ProfileUiStatePlaceholder(
    val username: String = "User Name", // Placeholder
    val email: String = "user@example.com", // Placeholder
    val currentStreak: Int = 0, // Placeholder
    val tasksCompletedThisMonth: Int = 0, // Placeholder
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    private val userRepository: IUserRepository, // Keep for future use
    private val streakRepository: StreakRepository, // Keep for future use
    private val taskRepository: TaskRepository    // Keep for future use
) : ViewModel() {

    // Using a simple placeholder state for now
    private val _uiState = MutableStateFlow(ProfileUiStatePlaceholder())
    val uiState: StateFlow<ProfileUiStatePlaceholder> = _uiState.asStateFlow()

    init {
        // In a real ViewModel, you'd load data here:
        // loadUserProfile()
        // loadStreak()
        // loadTaskStats()
        // For the placeholder, the initial state is sufficient.
    }

    // Placeholder function for future implementation
    fun updateUsername(newName: String) {
        // Simulate update
        _uiState.value = _uiState.value.copy(username = newName, error = null)
        // In real implementation:
        // viewModelScope.launch {
        //     try {
        //         userRepository.updateUsername(newName)
        //         // Refresh or observe user data
        //     } catch (e: Exception) {
        //         _uiState.value = _uiState.value.copy(error = "Failed to update username")
        //     }
        // }
    }

    // Placeholder function
    fun refreshData() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        // Simulate loading
        // viewModelScope.launch {
        //    delay(1000) // Simulate network call
        //    _uiState.value = ProfileUiStatePlaceholder(
        //        username = "Refreshed User",
        //        currentStreak = 5, // Example
        //        tasksCompletedThisMonth = 10, // Example
        //        isLoading = false
        //    )
        // }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class ProfileViewModelFactory(
    private val userRepository: IUserRepository,
    private val streakRepository: StreakRepository,
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository, streakRepository, taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for Profile")
    }
}
