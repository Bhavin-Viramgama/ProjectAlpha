package com.example.projectalpha.ui.screen.habits

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projectalpha.viewmodel.HabitsViewModel

@Composable
fun HabitsScreen(habitsViewModel: HabitsViewModel) {
    val habits by habitsViewModel.habits.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Today's Habits", style = MaterialTheme.typography.headlineSmall)

        habits.forEach { habit ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${habit.name} (Streak: ${habit.streakCount})",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { habitsViewModel.toggleHabitCompletion(habit) }
                ) {
                    Text(
                        if (habit.isCompletedForToday) "Mark Incomplete" else "Mark Complete"
                    )
                }
            }
        }
    }
}
