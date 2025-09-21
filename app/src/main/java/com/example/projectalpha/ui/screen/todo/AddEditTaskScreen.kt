package com.example.projectalpha.ui.screen.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.layout.Arrangement // Already imported by *
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.BasicAlertDialog
// import androidx.compose.material3.BasicAlertDialog // Replaced with AlertDialog for consistency
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
// import androidx.compose.runtime.getValue // Already imported by *
// import androidx.compose.runtime.setValue // Already imported by *
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.ui.theme.AppTypography
import com.example.projectalpha.viewmodel.ToDoViewModel
import kotlinx.coroutines.flow.filterNotNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    navController: NavHostController,
    toDoViewModel: ToDoViewModel,
    taskId: Int?
) {
    val screenTitle = if (taskId == null) "Add New Task" else "Edit Task"
    var loadedTaskForEdit by remember { mutableStateOf<TaskEntity?>(null) }

    var title by rememberSaveable { mutableStateOf("") }
    var taskDateForScreen by rememberSaveable { mutableStateOf(toDoViewModel.selectedDate.value) }
    val priorities = listOf("High", "Medium", "Low")
    var priority by rememberSaveable { mutableStateOf("Medium") }
    var priorityExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    var showAdvancedOptions by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf("") }
    var deadlineDatePart by rememberSaveable { mutableStateOf(taskDateForScreen) }
    var deadlineTimePart by rememberSaveable { mutableStateOf(LocalTime.NOON) }
    var hasDeadline by rememberSaveable { mutableStateOf(false) } // This state controls if deadline fields are conceptually active

    var showTaskDatePickerDialog by remember { mutableStateOf(false) }
    var showDeadlineDatePickerDialog by remember { mutableStateOf(false) }
    var showDeadlineTimePickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId != null && taskId != -1) {
            toDoViewModel.getTaskById(taskId)
                .filterNotNull()
                .collect { task ->
                    loadedTaskForEdit = task
                    title = task.title
                    taskDateForScreen = task.date
                    priority = task.priority
                    description = task.description ?: ""
                    task.deadline?.let { dl ->
                        hasDeadline = true // Set hasDeadline based on loaded task
                        deadlineDatePart = dl.toLocalDate()
                        deadlineTimePart = dl.toLocalTime()
                    } ?: run {
                        hasDeadline = false
                        deadlineDatePart = task.date
                        deadlineTimePart = LocalTime.NOON
                    }
                    showAdvancedOptions = description.isNotBlank() || hasDeadline
                }
        } else {
            title = ""
            taskDateForScreen = toDoViewModel.selectedDate.value
            priority = "Medium"
            description = ""
            hasDeadline = false // Reset for new task
            deadlineDatePart = taskDateForScreen
            deadlineTimePart = LocalTime.NOON
            showAdvancedOptions = false
            loadedTaskForEdit = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        // Main content column that includes the button at the bottom
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            // Scrollable section for input fields
            Column(
                modifier = Modifier
                    .weight(1f) // Takes available space, pushing button to bottom
                    .padding(all = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Core Fields ---
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Task Title*") },
                    isError = titleError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                titleError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Spacing between date and priority
                    verticalAlignment = Alignment.CenterVertically // Align items vertically
                ) {
                    Box(modifier = Modifier.weight(1f)) { // Box for priority dropdown to control its width
                        ExposedDropdownMenuBox(
                            expanded = priorityExpanded,
                            onExpandedChange = { priorityExpanded = !priorityExpanded },
                            // modifier = Modifier.fillMaxWidth() // Let the Box control width
                        ) {
                            OutlinedTextField(
                                value = priority,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Priority") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                //.padding(start = 4.dp), // Padding was a bit much here
                                shape = MaterialTheme.shapes.medium,
                                singleLine = true // Ensure priority label fits well
                            )
                            ExposedDropdownMenu(
                                expanded = priorityExpanded,
                                onDismissRequest = { priorityExpanded = false }) {
                                priorities.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            priority = selectionOption
                                            priorityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showTaskDatePickerDialog = true },
                        modifier = Modifier
                            .weight(1f) // Date takes more space
                        //.padding(vertical = 10.dp, horizontal = 4.dp), // Padding was a bit much here
                        ,shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = "Select Task Date",
                            modifier = Modifier.size(ButtonDefaults.IconSize) // Standard icon size
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            "Date: ${taskDateForScreen.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))}",
                            maxLines = 1, overflow = TextOverflow.Ellipsis // Prevent text wrapping
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedOptions = !showAdvancedOptions }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = showAdvancedOptions, onCheckedChange = { showAdvancedOptions = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Advanced Options", style = MaterialTheme.typography.titleMedium) // Made titleMedium
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = if (showAdvancedOptions) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (showAdvancedOptions) "Hide Advanced Options" else "Show Advanced Options"
                    )
                }

                AnimatedVisibility(
                    visible = showAdvancedOptions,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            shape = MaterialTheme.shapes.medium
                        )

                        OutlinedButton(
                            onClick = { showDeadlineTimePickerDialog = true
                                      hasDeadline = true},
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = "Select Deadline Time", modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Deadline Time: ${deadlineTimePart.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}")
                        }

                        /*
                        // --- Re-add the "Set Specific Deadline" Checkbox ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = hasDeadline, // Controlled by this state
                                onCheckedChange = { hasDeadline = it }
                            )
                            Text("Set Specific Deadline")
                        }
                        // --- End Re-added Checkbox ---


                        // Deadline Date and Time Pickers (only visible if hasDeadline is true *AND* advanced options are shown)
                        AnimatedVisibility(visible = hasDeadline && showAdvancedOptions) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                //Deadline Date Picker
                                OutlinedButton(
                                    onClick = { showDeadlineDatePickerDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Filled.DateRange, contentDescription = "Select Deadline Date", modifier = Modifier.size(ButtonDefaults.IconSize))
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Deadline Date: ${deadlineDatePart.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                                }

                                OutlinedButton(
                                    onClick = { showDeadlineTimePickerDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Select Deadline Time", modifier = Modifier.size(ButtonDefaults.IconSize))
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Deadline Time: ${deadlineTimePart.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}")
                                }
                            }
                        }

                         */
                        // Spacer for visual separation if needed before next section or button
                        // Spacer(Modifier.height(8.dp)) // This was causing button to not be at bottom
                    }
                }
            } // End of scrollable Column

            // Save/Add Button (outside the scrollable Column, but inside the main Column with paddingValues)
            // This button will now be at the bottom of the screen.
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Title cannot be empty"
                        return@Button
                    }
                    // Only consider deadline if advanced options are shown AND hasDeadline is checked
                    val finalDeadline = if (showAdvancedOptions && hasDeadline) LocalDateTime.of(deadlineDatePart, deadlineTimePart) else null
                    // Only consider description if advanced options are shown
                    val finalDescription = if (showAdvancedOptions) description.takeIf { it.isNotBlank() } else null

                    if (taskId == null || taskId == -1) {
                        toDoViewModel.addTask(
                            title = title,
                            description = finalDescription,
                            taskDate = taskDateForScreen,
                            deadline = finalDeadline,
                            priority = priority
                        )
                    } else {
                        loadedTaskForEdit?.let {
                            toDoViewModel.updateTask(
                                it.copy(
                                    title = title,
                                    description = finalDescription,
                                    date = taskDateForScreen,
                                    deadline = finalDeadline,
                                    priority = priority
                                )
                            )
                        }
                    }
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) // Apply padding around the button
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    if (taskId == null || taskId == -1) "Add Task" else "Save Changes",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } // End of main screen Column
    } // End of Scaffold

    // --- DatePickerDialogs and TimePickerDialog (implementations remain the same) ---
    if (showTaskDatePickerDialog) {
        val currentTaskDateMillis = remember(taskDateForScreen) {
            taskDateForScreen.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentTaskDateMillis)
        DatePickerDialog(
            onDismissRequest = { showTaskDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTaskDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newSelectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            taskDateForScreen = newSelectedDate
                            if (!hasDeadline || deadlineDatePart.isBefore(newSelectedDate) || (loadedTaskForEdit?.deadline == null && taskId != null)) {
                                deadlineDatePart = newSelectedDate
                            }
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTaskDatePickerDialog = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeadlineDatePickerDialog && showAdvancedOptions && hasDeadline) {
        val currentDeadlineDateMillis = remember(deadlineDatePart) {
            deadlineDatePart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentDeadlineDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDeadlineDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeadlineDatePickerDialog = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        deadlineDatePart = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDeadlineDatePickerDialog = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeadlineTimePickerDialog && showAdvancedOptions && hasDeadline) {
        val timePickerState = rememberTimePickerState(
            initialHour = deadlineTimePart.hour,
            initialMinute = deadlineTimePart.minute,
            is24Hour = false
        )
        BasicAlertDialog(// Changed from BasicAlertDialog for standard M3 appearance
            onDismissRequest = { showDeadlineTimePickerDialog = false
                hasDeadline = false}
        ) {
            Surface( // Wrap content in a Surface for theming and shape
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation // Standard dialog elevation
            ) {
                Column(
                    modifier = Modifier
                        //.background(MaterialTheme.colorScheme.surface) // Surface handles this
                        .padding(24.dp), // Standard dialog padding
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Select Deadline Time",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showDeadlineTimePickerDialog = false
                            hasDeadline = false
                        }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                showDeadlineTimePickerDialog = false
                                deadlineTimePart =
                                    LocalTime.of(timePickerState.hour, timePickerState.minute)
                            }
                        ) { Text("OK") }
                    }
                }
            }
        }
    }
}

