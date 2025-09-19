package com.example.projectalpha.ui.screen.pomodoro

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.projectalpha.viewmodel.DEFAULT_LONG_BREAK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.DEFAULT_SHORT_BREAK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.DEFAULT_WORK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.PomodoroSessionType
import com.example.projectalpha.viewmodel.PomodoroViewModel

@SuppressLint("DefaultLocale")
@Composable
fun PomodoroScreen(pomodoroViewModel: PomodoroViewModel) {
    val timeRemainingSeconds by pomodoroViewModel.timeRemainingSeconds.collectAsState()
    val isRunning by pomodoroViewModel.isRunning.collectAsState()
    val currentSessionType by pomodoroViewModel.currentSessionType.collectAsState()
    val customDurations by pomodoroViewModel.customDurations.collectAsState()

    val currentSessionMaxDuration = pomodoroViewModel.getDurationForSessionType(currentSessionType)
    val progress = if (currentSessionMaxDuration > 0) {
        animateFloatAsState(
            targetValue = timeRemainingSeconds.toFloat() / currentSessionMaxDuration.toFloat(),
            label = "progressAnimation"
        ).value
    } else {
        1f // Full progress if duration is 0 to avoid division by zero
    }


    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val sessionTypeTitle = when (currentSessionType) {
        PomodoroSessionType.WORK -> "Focus Time"
        PomodoroSessionType.SHORT_BREAK -> "Short Break"
        PomodoroSessionType.LONG_BREAK -> "Long Break"
    }

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditDurationsDialog(
            currentDurations = customDurations,
            onDismiss = { showEditDialog = false },
            onSave = { work, short, long ->
                pomodoroViewModel.updateCustomDurations(work, short, long)
                showEditDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Durations")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp), // Additional screen padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround // Distribute space
        ) {
            Text(
                text = sessionTypeTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            CircularTimerView(
                progress = progress,
                timeFormatted = timeFormatted,
                strokeWidth = 16.dp,
                timerColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                onClickEdit = {pomodoroViewModel.pauseTimer()
                    showEditDialog = true}
            )

            ControlButtons(
                isRunning = isRunning,
                onStartPause = { pomodoroViewModel.startPauseTimer() },
                onReset = { pomodoroViewModel.resetTimer() },
                onSkip = { pomodoroViewModel.skipSession() },
                canReset = timeRemainingSeconds < currentSessionMaxDuration && timeRemainingSeconds > 0 // Enable reset if not at full or zero
            )
        }
    }
}

@Composable
fun CircularTimerView(
    progress: Float,
    timeFormatted: String,
    size: Dp = 240.dp,
    strokeWidth: Dp = 12.dp,
    timerColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onClickEdit: () -> Unit
) {
    val stroke = with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round) }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        // Background for the timer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = backgroundColor) // Optional: explicit background color
        }
        // Track for the timer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
        }
        // Progress arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = timerColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke
            )
        }
        Text(
            text = timeFormatted,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable(onClick = onClickEdit)
        )
    }
}

@Composable
fun ControlButtons(
    isRunning: Boolean,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    canReset: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onReset, enabled = canReset) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Reset Timer",
                modifier = Modifier.size(36.dp),
                tint = if (canReset) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )
        }

        FilledIconButton( // More prominent start/pause button
            onClick = onStartPause,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.KeyboardArrowUp else Icons.Filled.PlayArrow,
                contentDescription = if (isRunning) "Pause Timer" else "Start Timer",
                modifier = Modifier.size(48.dp)
            )
        }

        IconButton(onClick = onSkip) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Skip Session",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDurationsDialog(
    currentDurations: com.example.projectalpha.viewmodel.PomodoroDurations, // Use the correct import
    onDismiss: () -> Unit,
    onSave: (workSeconds: Int, shortBreakSeconds: Int, longBreakSeconds: Int) -> Unit
) {
    var workMinutes by remember { mutableStateOf((currentDurations.work / 60).toString()) }
    var shortBreakMinutes by remember { mutableStateOf((currentDurations.shortBreak / 60).toString()) }
    var longBreakMinutes by remember { mutableStateOf((currentDurations.longBreak / 60).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Durations (Minutes)", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = workMinutes,
                    onValueChange = { workMinutes = it.filter { char -> char.isDigit() } },
                    label = { Text("Work Session") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shortBreakMinutes,
                    onValueChange = { shortBreakMinutes = it.filter { char -> char.isDigit() } },
                    label = { Text("Short Break") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = longBreakMinutes,
                    onValueChange = { longBreakMinutes = it.filter { char -> char.isDigit() } },
                    label = { Text("Long Break") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val workSec = (workMinutes.toIntOrNull() ?: (DEFAULT_WORK_DURATION_SECONDS / 60)) * 60
                        val shortSec = (shortBreakMinutes.toIntOrNull() ?: (DEFAULT_SHORT_BREAK_DURATION_SECONDS / 60)) * 60
                        val longSec = (longBreakMinutes.toIntOrNull() ?: (DEFAULT_LONG_BREAK_DURATION_SECONDS / 60)) * 60
                        onSave(workSec, shortSec, longSec)
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
