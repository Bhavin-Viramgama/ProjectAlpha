package com.example.projectalpha.ui.screen.todo

import kotlinx.coroutines.launch
import android.icu.lang.UCharacter.toUpperCase
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
// Checkbox does not need a specific icon import for its visual state
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.ui.theme.AppTypography
import com.example.projectalpha.ui.theme.HighPriorityFont
import com.example.projectalpha.ui.theme.HighPriorityFont1
import com.example.projectalpha.ui.theme.LowPriorityFont
import com.example.projectalpha.ui.theme.LowPriorityFont1
import com.example.projectalpha.ui.theme.MediumPriorityFont
import com.example.projectalpha.ui.theme.MediumPriorityFont1
import com.example.projectalpha.viewmodel.ToDoViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ToDoScreen(toDoViewModel: ToDoViewModel) {
    val tasks by toDoViewModel.tasksForSelectedDate.collectAsState()
    val isLoading by toDoViewModel.isLoading.collectAsState()
    val error by toDoViewModel.error.collectAsState()
    val selectedDate by toDoViewModel.selectedDate.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    //Code for animation-----------------------------------------------
    val visibleStates = remember { mutableStateMapOf<Int, Boolean>() }

    // Trigger staggered animation on load or when tasks change
    LaunchedEffect(tasks) {
        visibleStates.clear()
        tasks.forEachIndexed { index, task ->
            //delay(index * 50L) // delay for staggered animation Like Home Screen
            visibleStates[task.id] = true
        }
    }
    //-----------------------------------------------Code for animation

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                taskToEdit = null // Ensure we are adding, not editing
                showAddTaskDialog = true
            }) {
                Icon(Icons.Filled.Add, "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(horizontal = 16.dp), // Screen specific horizontal padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Date Picker / Selector (Simplified for now)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Button(onClick = {
                    toDoViewModel.selectDate(selectedDate.minusDays(1))
                    //toDoViewModel.loadTasksForDate12(selectedDate)
                }) {
                    Text("<")
                }
                Text(
                    selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    style = AppTypography.titleLarge
                )
                Button(onClick = {
                    toDoViewModel.selectDate(selectedDate.plusDays(1))
                    //toDoViewModel.loadTasksForDate12(selectedDate)
                }) {
                    Text(">")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            error?.let {
                Text(
                    "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = { toDoViewModel.clearError() }) {
                    Text("Dismiss Error")
                }
            }

            if (!isLoading && tasks.isEmpty() && error == null) {
                Text(
                    "No tasks for this date. Add one!",
                    style = AppTypography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (!isLoading && error == null) {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { task -> task.id }) { task ->

                        val visible = visibleStates[task.id] ?: false
                        //For Animated Items
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    slideInVertically(initialOffsetY = { it / 2 }),
                            exit = fadeOut(animationSpec = tween(300)) +
                                    slideOutVertically(targetOffsetY = { it / 2 }),
                            modifier = Modifier.animateItem()
                        ) {
                            TaskItem(
                                task = task,
                                onToggleComplete = { toDoViewModel.toggleTaskCompletion(task) },
                                onEdit = {
                                    taskToEdit = task
                                    showAddTaskDialog = true
                                },
                                onDelete = {
                                        taskToDelete = task
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            taskToEdit = taskToEdit,
            selectedDate = selectedDate,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, description, taskDate, deadline, priority ->
                if (taskToEdit == null) {
                    toDoViewModel.addTask(title, description, taskDate, deadline, priority)
                } else {
                    toDoViewModel.updateTask(
                        taskToEdit!!.copy(
                            title = title,
                            description = description,
                            date = taskDate,
                            deadline = deadline,
                            priority = priority
                        )
                    )
                }
                showAddTaskDialog = false
                taskToEdit = null
            }
        )
    }

    val coroutineScope = rememberCoroutineScope()
    taskToDelete?.let { task ->
        ConfirmDeleteDialog(
            taskTitle = task.title,
            onDismiss = { taskToDelete = null },
            onConfirm = {
                // Wait until animation finishes before removing from list
                coroutineScope.launch {
                    delay(100) //TODO Here is a bug, if user deletes a task and if he switches tab then coroutine will be destroyed and task will not be deleted!
                    visibleStates[task.id] = false
                    delay(300)
                    toDoViewModel.deleteTask(task)
                }
                taskToDelete = null
            }
        )
    }
}

@Composable
fun TaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // We keep track if the message is expanded or not in this
    // variable
    var isExpanded by remember { mutableStateOf(false) }

    val surfaceColor by animateColorAsState(
        if(isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    )
    val priorityFontColor = when (task.priority.lowercase()) {
        "high" -> HighPriorityFont1 // Solid color for indicator
        "medium" -> MediumPriorityFont1
        "low" -> LowPriorityFont1
        else -> Color.Transparent
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, top = 8.dp, end = 8.dp, bottom = 8.dp), // Adjust padding for Checkbox
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Use Checkbox for toggling completion
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }, // ViewModel handles the actual state change
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant // Or a more subtle color
                ),
                modifier = Modifier.padding(horizontal = 4.dp) // Add some padding around checkbox
            )
            // Removed Spacer as Checkbox has its own padding/touch target considerations



            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)) { // Add slight start padding to text content

                Text(
                    text = "${toUpperCase(task.priority)} PRIORITY",
                    style = AppTypography.bodySmall.copy(
                        color = if (task.isCompleted) Color.Gray else priorityFontColor
                    ),
                    modifier = Modifier.padding(top = 2.dp),
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.W400
                )

                Text(
                    text = task.title,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = AppTypography.titleMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                    ),
                    fontWeight = FontWeight.Bold
                )
                task.deadline?.let {
                    Text(
                        text = "Deadline: ${it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))}",
                        modifier = Modifier.padding(top = 0.dp),
                        style = AppTypography.bodySmall.copy(
                            color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
                task.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = AppTypography.bodySmall.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(top = 4.dp).animateContentSize(),
                        maxLines = if(isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }


            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete Task", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, "Edit Task", tint = MaterialTheme.colorScheme.secondary)
            }



        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    taskToEdit: TaskEntity? = null,
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, taskDate: LocalDate, deadline: LocalDateTime?, priority: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(taskToEdit?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(taskToEdit?.description ?: "") }
    var taskDateForDialog by rememberSaveable { mutableStateOf(taskToEdit?.date ?: selectedDate) }

    // Deadline states
    var deadlineDatePart by rememberSaveable { mutableStateOf(taskToEdit?.deadline?.toLocalDate() ?: selectedDate) }
    var deadlineTimePart by rememberSaveable { mutableStateOf(taskToEdit?.deadline?.toLocalTime() ?: LocalTime.NOON) }
    var hasDeadline by rememberSaveable { mutableStateOf(taskToEdit?.deadline != null) }

    //Subtasks!
    var hasSubTasks by rememberSaveable { mutableStateOf(false) }

    val priorities = listOf("High", "Medium", "Low")
    var priority by rememberSaveable { mutableStateOf(taskToEdit?.priority ?: "Medium") }
    var priorityExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    // Dialog visibility states
    var showTaskDatePickerDialog by remember { mutableStateOf(false) }
    var showDeadlineDatePickerDialog by remember { mutableStateOf(false) }
    var showDeadlineTimePickerDialog by remember { mutableStateOf(false) }


    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()) // <-- enables scrolling
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (taskToEdit == null) "Add New Task" else "Edit Task", style = AppTypography.headlineSmall)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Title*") },
                    isError = titleError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                titleError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                )

                //Column(modifier = Modifier.animateContentSize()) {} //For Smooth animation of the subtask wala part
//                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
//                        Checkbox(checked = hasSubTasks, onCheckedChange = { hasSubTasks = it })
//                        Text("Set Subtasks")
//                    }
//
//                    if(hasSubTasks){
//                        //TextField(value = "TODO()",onValueChange = {TODO()})
//                            Text("Subtask1:")
//                    }



                OutlinedButton(
                    onClick = { showTaskDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Select Task Date", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Task Date: ${taskDateForDialog.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
                    Text("Set Specific Deadline")
                }

                if (hasDeadline) {
                    // Deadline Date Picker Button
//                    OutlinedButton(
//                        onClick = { showDeadlineDatePickerDialog = true },
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Icon(Icons.Filled.DateRange, contentDescription = "Select Deadline Date", modifier = Modifier.size(ButtonDefaults.IconSize))
//                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
//                        Text("Deadline Date: ${deadlineDatePart.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
//                    }

                    // Deadline Time Picker Button
                    OutlinedButton(
                        onClick = { showDeadlineTimePickerDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Select Deadline Time", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Deadline Time: ${deadlineTimePart.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}")
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (title.isBlank()) {
                            titleError = "Title cannot be empty"
                            return@Button
                        }
                        val finalDeadline = if (hasDeadline) LocalDateTime.of(deadlineDatePart, deadlineTimePart) else null
                        onConfirm(title, description.takeIf { it.isNotBlank() }, taskDateForDialog, finalDeadline, priority)
                    }) {
                        Text(if (taskToEdit == null) "Add" else "Save")
                    }
                }
            }
        }
    }

    // --- DatePickerDialog for Task Date ---
    if (showTaskDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = taskDateForDialog.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showTaskDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTaskDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            taskDateForDialog = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTaskDatePickerDialog = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    // --- DatePickerDialog for Deadline Date ---
    if (showDeadlineDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadlineDatePart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDeadlineDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeadlineDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            deadlineDatePart = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDeadlineDatePickerDialog = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }


    // --- TimePickerDialog for Deadline Time ---
    if (showDeadlineTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = deadlineTimePart.hour,
            initialMinute = deadlineTimePart.minute,
            is24Hour = false // Or true, depending on your preference/locale
        )
        // We need a wrapper Dialog for TimePicker in Material 3 if not using FullscreenTimePicker
        AlertDialog( // Using AlertDialog as a simple wrapper for TimePickerDialog content
            onDismissRequest = { showDeadlineTimePickerDialog = false },
            modifier = Modifier.fillMaxWidth() // Adjust width as needed
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface) // Use surface color
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDeadlineTimePickerDialog = false }) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showDeadlineTimePickerDialog = false
                            deadlineTimePart = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        }
                    ) { Text("OK") }
                }
            }
        }
    }
}



@Composable
fun ConfirmDeleteDialog(
    taskTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Task?") },
        text = { Text("Are you sure you want to delete \"$taskTitle\"? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
