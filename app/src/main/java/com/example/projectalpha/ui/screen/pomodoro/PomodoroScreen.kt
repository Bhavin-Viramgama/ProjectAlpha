package com.example.projectalpha.ui.screen.pomodoro

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.projectalpha.viewmodel.DEFAULT_LONG_BREAK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.DEFAULT_SHORT_BREAK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.DEFAULT_WORK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.PomodoroSessionType
import com.example.projectalpha.viewmodel.PomodoroViewModel
import kotlin.text.minByOrNull
import kotlin.text.toFloat

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
    var showSetTimeDialog by remember { mutableStateOf(false) } // State for new time picker dialog

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
    if (showSetTimeDialog) {
        val initialMinutes = timeRemainingSeconds / 60
        val initialSeconds = timeRemainingSeconds % 60
        CustomScrollableTimePickerDialog( // <<< CALLING THE NEW CUSTOM DIALOG
            initialMinutes = initialMinutes,
            initialSeconds = initialSeconds,
            onDismiss = { showSetTimeDialog = false },
            onTimeSet = { selectedMinutes, selectedSeconds ->
                pomodoroViewModel.setCurrentSessionTime(selectedMinutes, selectedSeconds)
                showSetTimeDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {pomodoroViewModel.pauseTimer()
                showEditDialog = true }) {
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
                onClickEdit = {pomodoroViewModel.pauseTimer() // Pause before allowing user to set time
                    showSetTimeDialog = true}
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

// --- New Custom Scrollable Time Picker Dialog ---
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CustomScrollableTimePickerDialog(
    initialMinutes: Int,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onTimeSet: (minutes: Int, seconds: Int) -> Unit
) {
    val minutesRange = (0..59).toList()
    val secondsRange = (0..59).toList()

    val minutesListState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinutes)
    val secondsListState = rememberLazyListState(initialFirstVisibleItemIndex = initialSeconds)

    var selectedMinutes by remember { mutableStateOf(initialMinutes) }
    var selectedSeconds by remember { mutableStateOf(initialSeconds) }

    // Update selected values when scroll stops (due to snapping)
    LaunchedEffect(minutesListState.isScrollInProgress) {
        if (!minutesListState.isScrollInProgress) {
            val centerIndex = calculateCenterIndex(minutesListState)
            if (centerIndex in minutesRange.indices) {
                selectedMinutes = minutesRange[centerIndex]
            }
        }
    }
    LaunchedEffect(secondsListState.isScrollInProgress) {
        if (!secondsListState.isScrollInProgress) {
            val centerIndex = calculateCenterIndex(secondsListState)
            if (centerIndex in secondsRange.indices) {
                selectedSeconds = secondsRange[centerIndex]
            }
        }
    }


    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Set Duration", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScrollableNumberPicker(
                        range = minutesRange,
                        listState = minutesListState,
                        onValueSelected = { selectedMinutes = it },
                        label = "Minutes",
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    ScrollableNumberPicker(
                        range = secondsRange,
                        listState = secondsListState,
                        onValueSelected = { selectedSeconds = it },
                        label = "Seconds",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onTimeSet(selectedMinutes, selectedSeconds) }) {
                        Text("Set")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollableNumberPicker(
    range: List<Int>,
    listState: LazyListState,
    onValueSelected: (Int) -> Unit, // Callback when a value is considered selected by snapping
    label: String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp, // Height of each item in the picker
    visibleItemsCount: Int = 3 // How many items are visible (e.g., center + one above + one below)
) {
    val centralItemTextStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary)
    val peripheralItemTextStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    // This derived state helps in updating the text style based on the centered item.
    val centeredIndex by remember {
        derivedStateOf {
            if (listState.layoutInfo.visibleItemsInfo.isEmpty() || listState.isScrollInProgress) {
                // While scrolling or if not laid out, we might not have a definitive center,
                // or we use the estimated center based on firstVisibleItemIndex.
                // For simplicity, we'll use a more direct way in LaunchedEffect to set the final value.
                // Here, we just determine which item is *visually* closest to center for styling.
                calculateCenterIndex(listState)
            } else {
                calculateCenterIndex(listState)
            }
        }
    }


    Box(modifier = modifier.height(itemHeight * visibleItemsCount)) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Padding items to allow first and last actual numbers to reach the center
            items(1) { Spacer(Modifier.height(itemHeight * (visibleItemsCount / 2))) }

            items(range.size) { index ->
                val itemValue = range[index]
                val isCentered = index == centeredIndex

                Text(
                    text = String.format("%02d", itemValue),
                    style = if (isCentered) centralItemTextStyle else peripheralItemTextStyle,
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.CenterVertically), // Center text vertically in item
                    textAlign = TextAlign.Center
                )
            }
            // Padding items
            items(1) { Spacer(Modifier.height(itemHeight * (visibleItemsCount / 2))) }
        }

        // Optional: Add overlays for a "drum" effect (e.g., fading edges or horizontal lines)
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = itemHeight * (visibleItemsCount / 2) - (itemHeight / 2)+20.dp), // Adjust position
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(itemHeight * (visibleItemsCount / 2) - (itemHeight / 2) +20.dp)), // Adjust position
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

// Helper to find the visually centered item index
private fun calculateCenterIndex(listState: LazyListState): Int {
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) return -1

    val viewportCenterY = layoutInfo.viewportSize.height / 2
    val centerItem = layoutInfo.visibleItemsInfo.minByOrNull {
        kotlin.math.abs((it.offset.toFloat() + it.size.toFloat() / 2) - viewportCenterY.toFloat())
    }
    return centerItem?.index?.minus(1) ?: listState.firstVisibleItemIndex // -1 because of the padding item
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
