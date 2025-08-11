package com.example.projectalpha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.projectalpha.data.local.entity.HabitEntity
import com.example.projectalpha.data.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HabitsViewModel(private val habitRepository: HabitRepository) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> =
        habitRepository.getAllHabits()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleHabitCompletion(habit: HabitEntity) {
        viewModelScope.launch {
            val updated = habit.copy(isCompletedForToday = !habit.isCompletedForToday)
            habitRepository.updateHabit(updated)
        }
    }

    fun addHabit(name: String, daysOfWeek: List<String>) {
        viewModelScope.launch {
            val habit = HabitEntity(
                name = name,
                daysOfWeek = daysOfWeek
            )
            habitRepository.insertHabit(habit)
        }
    }
}

class HabitsViewModelFactory(
    private val habitRepository: HabitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitsViewModel(habitRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}