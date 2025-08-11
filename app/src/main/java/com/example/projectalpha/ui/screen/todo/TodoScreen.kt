package com.example.projectalpha.ui.screen.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
// Checkbox does not need a specific icon import for its visual state
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.ui.theme.AppTypography
import com.example.projectalpha.viewmodel.ToDoViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoScreen(toDoViewModel: ToDoViewModel) {
    val tasks by toDoViewModel.tasksForSelectedDate.collectAsState()
    val isLoading by toDoViewModel.isLoading.collectAsState()
    val error by toDoViewModel.error.collectAsState()
    val selectedDate by toDoViewModel.selectedDate.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

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
                Button(onClick = { toDoViewModel.selectDate(selectedDate.minusDays(1)) }) {
                    Text("<")
                }
                Text(
                    selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    style = AppTypography.titleLarge
                )
                Button(onClick = { toDoViewModel.selectDate(selectedDate.plusDays(1)) }) {
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
                        TaskItem(
                            task = task,
                            onToggleComplete = { toDoViewModel.toggleTaskCompletion(task) },
                            onEdit = {
                                taskToEdit = task
                                showAddTaskDialog = true
                            },
                            onDelete = { taskToDelete = task }
                        )
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

    taskToDelete?.let { task ->
        ConfirmDeleteDialog(
            taskTitle = task.title,
            onDismiss = { taskToDelete = null },
            onConfirm = {
                toDoViewModel.deleteTask(task)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
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

            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) { // Add slight start padding to text content
                Text(
                    text = task.title,
                    style = AppTypography.titleMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                    ),
                    fontWeight = FontWeight.Bold
                )
                task.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = AppTypography.bodySmall.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                task.deadline?.let {
                    Text(
                        text = "Deadline: ${it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))}",
                        style = AppTypography.bodySmall.copy(
                            color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
                Text(
                    text = "Priority: ${task.priority}",
                    style = AppTypography.bodySmall.copy(
                        color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.secondary
                    )
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, "Edit Task", tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete Task", tint = MaterialTheme.colorScheme.error)
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
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var taskDate by remember { mutableStateOf(taskToEdit?.date ?: selectedDate) }
    var deadlineDate by remember { mutableStateOf(taskToEdit?.deadline?.toLocalDate() ?: selectedDate) }
    var deadlineTime by remember { mutableStateOf(taskToEdit?.deadline?.toLocalTime() ?: LocalTime.NOON) }
    var hasDeadline by remember { mutableStateOf(taskToEdit?.deadline != null) }
    val priorities = listOf("High", "Medium", "Low")
    var priority by remember { mutableStateOf(taskToEdit?.priority ?: "Medium") }
    var priorityExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) { // Consider using Surface for dialog content background
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (taskToEdit == null) "Add New Task" else "Edit Task", style = AppTypography.headlineSmall)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Title*") },
                    isError = titleError != null,
                    singleLine = true
                )
                titleError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.bodySmall) }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.heightIn(min = 80.dp)
                )

                Text("Task Date: ${taskDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}")
                // TODO: Add Button to launch DatePickerDialog for 'taskDate'

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
                    Text("Set Specific Deadline Time")
                }

                if (hasDeadline) {
                    Text("Deadline: ${deadlineDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))} ${deadlineTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))}")
                    // TODO: Add Buttons to launch DatePickerDialog for 'deadlineDate' and TimePickerDialog for 'deadlineTime'
                }

                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded }
                ) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.menuAnchor()
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
                        val finalDeadline = if (hasDeadline) LocalDateTime.of(deadlineDate, deadlineTime) else null
                        onConfirm(title, description.takeIf { it.isNotBlank() }, taskDate, finalDeadline, priority)
                    }) {
                        Text(if (taskToEdit == null) "Add" else "Save")
                    }
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
