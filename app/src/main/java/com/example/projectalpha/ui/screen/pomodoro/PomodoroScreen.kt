package com.example.projectalpha.ui.screen.pomodoro

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// Using Material3 components
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme // For consistent styling if needed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectalpha.ui.theme.AppTypography // Assuming this is defined
import com.example.projectalpha.viewmodel.PomodoroSessionType
import com.example.projectalpha.viewmodel.PomodoroViewModel

@SuppressLint("DefaultLocale")
@Composable
fun PomodoroScreen(pomodoroViewModel: PomodoroViewModel) {
    // Collect all relevant states from the ViewModel
    val timeRemainingSeconds by pomodoroViewModel.timeRemainingSeconds.collectAsState()
    val isRunning by pomodoroViewModel.isRunning.collectAsState()
    val currentSessionType by pomodoroViewModel.currentSessionType.collectAsState()

    // Get the full duration for the current session type to help with enabling/disabling reset
    // This assumes getDurationForSessionType is now 'internal' or 'public' in PomodoroViewModel
    val currentSessionMaxDuration = pomodoroViewModel.getDurationForSessionType(currentSessionType)

    // Format time for display (mm:ss)
    // This calculation is fine here as it's purely display logic derived from state
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    // Determine the text for the session type
    val sessionTypeText = when (currentSessionType) {
        PomodoroSessionType.WORK -> "Work Session"
        PomodoroSessionType.SHORT_BREAK -> "Short Break"
        PomodoroSessionType.LONG_BREAK -> "Long Break"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = sessionTypeText,
            style = AppTypography.headlineMedium // Use your app's typography
            // For Material 3, you might use: style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = timeFormatted,
            // Apply a more prominent style for the timer display
            style = AppTypography.displayLarge.copy(fontSize = 72.sp)
            // For Material 3: style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically // Good practice for rows with buttons
        ) {
            Button(onClick = { pomodoroViewModel.startPauseTimer() }) {
                Text(if (isRunning) "Pause" else "Start")
            }

            Button(
                onClick = { pomodoroViewModel.resetTimer() },
                // Enable reset only if the timer is not already at its full session duration
                // and it's not currently running (optional: you might want to allow reset while running)
                enabled = timeRemainingSeconds < currentSessionMaxDuration // && !isRunning (if you want to prevent reset while running)
            ) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ensure the skip button is always enabled, or add specific logic if needed
        Button(onClick = { pomodoroViewModel.skipSession() }) {
            Text("Skip Session")
        }
    }
}
