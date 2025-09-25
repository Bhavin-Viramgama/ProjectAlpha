package com.example.projectalpha.ui.screen.pomodoro

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.projectalpha.R
import com.example.projectalpha.viewmodel.DEFAULT_LONG_BREAK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.DEFAULT_SHORT_BREAK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.DEFAULT_WORK_DURATION_SECONDS
import com.example.projectalpha.viewmodel.PomodoroDurations
import com.example.projectalpha.viewmodel.PomodoroSessionType
import com.example.projectalpha.viewmodel.PomodoroViewModel
import kotlin.math.abs

@SuppressLint("DefaultLocale")
@Composable
fun PomodoroScreen(pomodoroViewModel: PomodoroViewModel) {
    val timeRemainingSeconds by pomodoroViewModel.timeRemainingSeconds.collectAsState()
    val isRunning by pomodoroViewModel.isRunning.collectAsState()
    val currentSessionType by pomodoroViewModel.currentSessionType.collectAsState()
    val customDurations by pomodoroViewModel.customDurations.collectAsState()
    val upcomingSessions by pomodoroViewModel.upcomingSessionSequence.collectAsState() // Collect the sequence

    // Collect the effectiveMaxDurationSeconds StateFlow from the ViewModel
    val effectiveMaxDuration by pomodoroViewModel.effectiveMaxDurationSeconds.collectAsState()
    val progress = if (effectiveMaxDuration > 0) {
        animateFloatAsState(
            targetValue = if (timeRemainingSeconds > 0) timeRemainingSeconds.toFloat() / effectiveMaxDuration.toFloat() else 0f,
            label = "progressAnimation"
        ).value.coerceIn(0f, 1f) // Ensure progress is between 0 and 1
    } else {
        // If effectiveMaxDuration is 0 (e.g., user set 0:00 manually),
        // show full or empty based on timeRemaining. If timeRemaining is also 0, show full (or empty).
        if (timeRemainingSeconds <= 0) 1f else 0f
    }


    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val sessionTypeTitle = when (currentSessionType) {
        PomodoroSessionType.WORK -> "Focus Time"
        PomodoroSessionType.SHORT_BREAK -> "Short Break"
        PomodoroSessionType.LONG_BREAK -> "Long Break"
    }

    var showEditDurationsDialog by remember { mutableStateOf(false) }
    var showSetTimeDialog by remember { mutableStateOf(false) } // State for new time picker dialog

    if (showEditDurationsDialog) {
        EditDurationsDialog(
            currentDurations = customDurations,
            onDismiss = { showEditDurationsDialog = false },
            onSave = { work, short, long ->
                pomodoroViewModel.updateCustomDurations(work, short, long)
                showEditDurationsDialog = false
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
                showEditDurationsDialog = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Durations Durations")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(horizontal = 16.dp, vertical = 16.dp), // Additional screen padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 32.dp,
                alignment = Alignment.Top
            )
        ) {
            Text(
                text = sessionTypeTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (upcomingSessions.isNotEmpty()) { // Only show if there's a sequence
                PomodoroSessionSequenceIndicator(
                    currentSessionType = currentSessionType,
                    upcomingSessions = upcomingSessions
                )
            } else {
                // Optional: Placeholder or small spacer if sequence is empty initially
                Spacer(modifier = Modifier.height(24.dp))
            }


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
                canReset = effectiveMaxDuration > 0 || timeRemainingSeconds == 0
            )
        }
    }
}

@Composable
fun PomodoroSessionSequenceIndicator(
    currentSessionType: PomodoroSessionType, // To highlight the current one
    upcomingSessions: List<PomodoroSessionType>,
    modifier: Modifier = Modifier
) {
    if (upcomingSessions.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.Center, // Center the sequence items
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display current session type prominently
        SessionIndicatorItem(
            sessionType = currentSessionType,
            isCurrent = true, // Special styling for the current session
            isNextInQueue = false // Not the "next" in sequence, but *the* current
        )

        Icon(
            painter = painterResource(id = R.drawable.arrowforward), // Replace with your forward arrow drawable
            contentDescription = "then",
            modifier = Modifier
                .size(20.dp)
                .padding(horizontal = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val itemsToShow = upcomingSessions.take(4)
        itemsToShow.forEachIndexed { index, sessionType ->
            // By passing a key that's relatively stable or changes predictably,
            // Compose can better manage animations. Here, index within the `itemsToShow`
            // list can serve as part of a key if needed, but the internal animations
            // of SessionIndicatorItem will do most of the work.
            key(sessionType.ordinal + index) { // Example key: combines type and position
                SessionIndicatorItem(
                    sessionType = sessionType,
                    isCurrent = false,
                    isNextInQueue = index == 0
                )
            }
            if (index < itemsToShow.size - 1) {
                AnimatedVisibility(visible = true) { // Arrow always visible between items
                    Icon(
                        painter = painterResource(id = R.drawable.arrowforward),
                        contentDescription = "then",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(horizontal = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

}

@Composable
private fun SessionIndicatorItem(
    sessionType: PomodoroSessionType,
    isCurrent: Boolean,
    isNextInQueue: Boolean
) {
    val text = when (sessionType) {
        PomodoroSessionType.WORK -> "Focus"
        PomodoroSessionType.SHORT_BREAK -> "Short"
        PomodoroSessionType.LONG_BREAK -> "Long"
    }
    val iconPainter = painterResource(
        id = when (sessionType) {
            PomodoroSessionType.WORK -> R.drawable.power
            PomodoroSessionType.SHORT_BREAK -> R.drawable.coffee
            PomodoroSessionType.LONG_BREAK -> R.drawable.relax
        }
    )

    val targetBackgroundColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        isNextInQueue -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 500), label = "bgColorAnim"
    )

    val targetContentColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isNextInQueue -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val animatedContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 500), label = "contentColorAnim"
    )

    val targetSize = if (isCurrent) 48.dp else 40.dp
    val animatedSize by animateDpAsState(
        targetValue = targetSize,
        animationSpec = tween(durationMillis = 300), label = "sizeAnim"
    )

    val targetIconSize = if (isCurrent) 24.dp else 20.dp
    val animatedIconSize by animateDpAsState(
        targetValue = targetIconSize,
        animationSpec = tween(durationMillis = 300), label = "iconSizeAnim"
    )

    val targetBorderStroke =
        if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    // Animating BorderStroke itself is tricky directly.
    // A common way is to animate the border color's alpha or width.
    // For simplicity, we'll make the border appear/disappear.
    // If you need smoother border animation, you might animate its color alpha.
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 500), label = "borderColorAnim"
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isCurrent) 2.dp else 0.dp, // Animate width to 0 for no border
        animationSpec = tween(durationMillis = 500), label = "borderWidthAnim"
    )


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(animatedSize)
                .clip(CircleShape)
                .background(animatedBackgroundColor)
                .border(
                    width = animatedBorderWidth,
                    color = animatedBorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = text,
                modifier = Modifier.size(animatedIconSize),
                tint = animatedContentColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            style = if (isCurrent) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.labelSmall,
            color = animatedContentColor, // Use animated content color
            textAlign = TextAlign.Center
        )
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
                painter = painterResource(R.drawable.restarticon),
                contentDescription = "Reset Timer",
                modifier = Modifier.size(36.dp),
                tint = if (canReset) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)            )
        }

        FilledIconButton( // start/pause button
            onClick = onStartPause,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                painter = if (isRunning) painterResource(id = R.drawable.pauseicon) else painterResource(id = R.drawable.playicon),
                contentDescription = if (isRunning) "Pause Timer" else "Start Timer",
                modifier = Modifier.size(if (isRunning) 36.dp else 42.dp)
            )
        }

        IconButton(onClick = onSkip) {
            Icon(
                painter = painterResource(R.drawable.skipiconwhite),
                contentDescription = "Skip Session",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// --- Custom Scrollable Time Picker Dialog ---
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

    // Ensure initial values are within range to prevent LazyListState crash
    val validInitialMinutes = initialMinutes.coerceIn(minutesRange.first(), minutesRange.last())
    val validInitialSeconds = initialSeconds.coerceIn(secondsRange.first(), secondsRange.last())


    val minutesListState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinutes)
    val secondsListState = rememberLazyListState(initialFirstVisibleItemIndex = initialSeconds)

    // These states are now primarily for displaying the current selection
    // and for the LaunchedEffects to update them after scrolling stops for visual feedback.
    // The final value on "Set" will be read directly.
    var currentDisplayedMinutes by remember { mutableStateOf(validInitialMinutes) }
    var currentDisplayedSeconds by remember { mutableStateOf(validInitialSeconds) }

    // Update selected values when scroll stops (due to snapping)
    LaunchedEffect(minutesListState.isScrollInProgress) {
        if (!minutesListState.isScrollInProgress) {
            val centerIndex = calculateCenterIndex(minutesListState, minutesRange.size)
            if (centerIndex >= 0 && centerIndex < minutesRange.size) {
                currentDisplayedMinutes = minutesRange[centerIndex]
            }
        }
    }
    LaunchedEffect(secondsListState.isScrollInProgress) {
        if (!secondsListState.isScrollInProgress) {
            val centerIndex = calculateCenterIndex(secondsListState, secondsRange.size)
            if (centerIndex >= 0 && centerIndex < minutesRange.size) {
                currentDisplayedSeconds = secondsRange[centerIndex]
            }
        }
    }

    // Scroll to initial position after composition if needed (especially if list state changes)
    LaunchedEffect(Unit) {
        minutesListState.scrollToItem(validInitialMinutes)
        secondsListState.scrollToItem(validInitialSeconds)
    }


    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.inverseOnSurface,
                contentColor = MaterialTheme.colorScheme.onBackground,
                disabledContainerColor = MaterialTheme.colorScheme.inversePrimary,
                disabledContentColor = MaterialTheme.colorScheme.onSurface
            )
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
                        label = "Minutes",
                        range = minutesRange,
                        listState = minutesListState,
                        currentDisplayedValue = currentDisplayedMinutes, // Pass for styling
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    ScrollableNumberPicker(
                        label = "Seconds",
                        range = secondsRange,
                        listState = secondsListState,
                        currentDisplayedValue = currentDisplayedSeconds, // Pass for styling
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
                    Button(onClick = {
                        // Read the truly centered item directly when "Set" is clicked
                        val finalMinutesIndex = calculateCenterIndex(minutesListState, minutesRange.size)
                        val finalSecondsIndex = calculateCenterIndex(secondsListState, secondsRange.size)

                        val finalMinutes = if (finalMinutesIndex in minutesRange.indices) minutesRange[finalMinutesIndex] else initialMinutes
                        val finalSeconds = if (finalSecondsIndex in secondsRange.indices) secondsRange[finalSecondsIndex] else initialSeconds

                        onTimeSet(finalMinutes, finalSeconds)
                    }) {
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
    label: String,
    range: List<Int>,
    listState: LazyListState,
    currentDisplayedValue: Int, // Used for styling the centered item
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp, // Height of each item in the picker
    visibleItemsCount: Int = 3 // How many items are visible (e.g., center + one above + one below)
) {
    val centralItemTextStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.primary)
    val peripheralItemTextStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    Box(modifier = modifier
        .height(itemHeight * visibleItemsCount)
        .clip(
            RoundedCornerShape(50f)
        )
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1f))
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Padding items to allow first and last actual numbers to reach the center
            items(1) { Spacer(Modifier.height(itemHeight * (visibleItemsCount / 2))) }

            items(items = range, key = { it }) { itemValue -> // Use key for better performance
                // Determine if this item is the one visually centered
                // This is a bit more complex now that selection is decoupled.
                // We rely on currentDisplayedValue which is updated by LaunchedEffect after scroll stops.
                val isVisuallyCentered = itemValue == currentDisplayedValue // Heuristic for styling

                Text(
                    text = String.format("%02d", itemValue),
                    style = if (isVisuallyCentered) centralItemTextStyle else peripheralItemTextStyle,
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
                .offset(y = itemHeight * (visibleItemsCount / 2) - (itemHeight / 2) + 20.dp), // Adjust position
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(itemHeight * (visibleItemsCount / 2) - (itemHeight / 2) + 20.dp)), // Adjust position
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

// Helper to find the visually centered item index
private fun calculateCenterIndex(listState: LazyListState, rangeSize: Int): Int {
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) return -1

    val viewportCenterY = layoutInfo.viewportSize.height / 2
    val centerItem = layoutInfo.visibleItemsInfo.minByOrNull {
        abs((it.offset.toFloat() + it.size.toFloat() / 2) - viewportCenterY.toFloat())
    }
    // The index from visibleItemsInfo is the absolute index in the LazyColumn items list,
    // which includes the top padding item.
    // If the list has 1 padding item, then item at index 0 of our 'range' is at LazyColumn index 1.
    val actualDataIndex = centerItem?.index?.minus(1) // Adjust for the top padding item

    return if (actualDataIndex != null && actualDataIndex >= 0 && actualDataIndex < rangeSize) {
        actualDataIndex
    } else {
        // Fallback if calculation is off or items are not fully visible
        // This might happen during fast scrolls or edge cases.
        // A simple fallback could be the first visible item from the data range.
        val firstVisibleDataIndex = (listState.firstVisibleItemIndex - 1).coerceIn(0, rangeSize - 1)
        firstVisibleDataIndex
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDurationsDialog(
    currentDurations: PomodoroDurations,
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
            shape = MaterialTheme.shapes.large,
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.inverseOnSurface,
                contentColor = MaterialTheme.colorScheme.onBackground,
                disabledContainerColor = MaterialTheme.colorScheme.inversePrimary,
                disabledContentColor = MaterialTheme.colorScheme.onSurface
            )
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
                        val workSec = (workMinutes.toIntOrNull()
                            ?: (DEFAULT_WORK_DURATION_SECONDS / 60)) * 60
                        val shortSec = (shortBreakMinutes.toIntOrNull()
                            ?: (DEFAULT_SHORT_BREAK_DURATION_SECONDS / 60)) * 60
                        val longSec = (longBreakMinutes.toIntOrNull()
                            ?: (DEFAULT_LONG_BREAK_DURATION_SECONDS / 60)) * 60
                        onSave(workSec, shortSec, longSec)
                    }) {
                        Text("Set")
                    }
                }
            }
        }
    }
}
