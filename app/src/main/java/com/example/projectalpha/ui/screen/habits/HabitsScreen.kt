package com.example.projectalpha.ui.screen.habits

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle // Consistent outlined icon for pending
// Material 3 imports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip // Explicit M3 FilterChip import
import androidx.compose.material3.FilterChipDefaults // Explicit M3 FilterChipDefaults import
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
// Compose Runtime imports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // For property delegation `val x by state`
import androidx.compose.runtime.setValue // For property delegation `var x by state`
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
// Other necessary imports
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.projectalpha.R // Your project's R class
import com.example.projectalpha.data.local.entity.HabitEntity
import com.example.projectalpha.viewmodel.HabitsViewModel
import java.time.DayOfWeek as JavaDayOfWeek // Alias
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material3.OutlinedButton // For toggle buttons
import androidx.compose.material3.TextButton // Already there
import androidx.compose.ui.unit.dp
import androidx.room.Update
import com.example.projectalpha.viewmodel.HabitFilterType // Import the enum
import java.time.LocalDate


// Extension function for toggling items in a MutableList
fun <T> MutableList<T>.toggle(item: T) {
    if (contains(item)) {
        remove(item)
    } else {
        add(item)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(habitsViewModel: HabitsViewModel) {
    // Observe the new habitsToDisplay and selectedFilterType
    val habitsToDisplay by habitsViewModel.habitsToDisplay.collectAsState()
    val currentFilterType by habitsViewModel.selectedFilterType.collectAsState()

    var showAddHabitDialog by rememberSaveable { mutableStateOf(false) }
    var habitToEdit by rememberSaveable { mutableStateOf<HabitEntity?>(null) } // Simplified key for rememberSaveable
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }

    // This effect ensures that if the screen becomes active and the date might have changed,
    // the daily reset logic is considered. ViewModel's init also calls it.
    LaunchedEffect(Unit) {
        habitsViewModel.performDailyResetIfNeeded()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    habitToEdit = null
                    showAddHabitDialog = true
                }
            ) {
                Icon(Icons.Filled.Add, "Add Habit")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply Scaffold padding
            // Screen-specific padding will be applied to child Column
        ) {
            // Filter Toggle Buttons Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) // Center the buttons
                ) {
                    HabitFilterButton(
                        text = "Today",
                        isSelected = currentFilterType == HabitFilterType.TODAY,
                        onClick = { habitsViewModel.setFilterType(HabitFilterType.TODAY) },
                        modifier = Modifier.weight(1f)
                    )
                    HabitFilterButton(
                        text = "All Habits",
                        isSelected = currentFilterType == HabitFilterType.ALL,
                        onClick = { habitsViewModel.setFilterType(HabitFilterType.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Title and List Column (with its own padding)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp) // Padding for list content below filter
            ) {
                Text(
                    text = if (currentFilterType == HabitFilterType.TODAY) "Today's Habits" else "All Habits",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )

                if (habitsToDisplay.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentFilterType == HabitFilterType.TODAY) "No habits scheduled for today." else "No habits found. Add one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp) // Padding at the bottom of the list
                    ) {
                        items(items = habitsToDisplay, key = { habit -> habit.id }) { habit ->
                            HabitItemCard(
                                habit = habit,
                                onToggleComplete = { habitsViewModel.toggleHabitCompletion(habit) },
                                onEdit = {
                                    habitToEdit = habit
                                    showAddHabitDialog = true
                                },
                                onDelete = { habitToDelete = habit }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddHabitDialog) {
        AddEditHabitDialog(
            habitToEdit = habitToEdit,
            onDismiss = {
                showAddHabitDialog = false
                habitToEdit = null
            },
            onConfirm = { name, days ->
                if (habitToEdit == null) {
                    habitsViewModel.addHabit(name, days)
                } else {
                    val updatedHabit = habitToEdit!!.copy(
                        name = name,
                        daysOfWeek = days
                        // Important: id, streakCount, isCompletedForToday, lastCompletedDate are preserved
                        // from the original habitToEdit unless explicitly changed by other logic.
                        // Here, we are only updating name and daysOfWeek.
                    )
                    habitsViewModel.updateExistingHabit(updatedHabit)
                }
                showAddHabitDialog = false
                habitToEdit = null
            }
        )
    }

    habitToDelete?.let { habit ->
        ConfirmDeleteDialog(
            habitName = habit.name,
            onDismiss = { habitToDelete = null },
            onConfirm = {
                habitsViewModel.deleteHabit(habit)
                habitToDelete = null
            }
        )
    }
}

@Composable
fun HabitFilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp), // Consistent height for buttons
        shape = RoundedCornerShape(8.dp), // More modern rounded corners
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
        ),
        border = ButtonDefaults.outlinedButtonBorder.takeIf { !isSelected } // No border if selected and filled
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitItemCard(
    habit: HabitEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardElevation by animateDpAsState(
        targetValue =if (habit.isCompletedForToday) 2.dp else 6.dp,
        label = "cardElevation"
        // Removed .value as 'by' delegate handles it
    )
    val backgroundColor = if (habit.isCompletedForToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    var showActions by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)) // Clipping the card itself
            .pointerInput(Unit) { // Apply pointerInput to the Card
                detectTapGestures(
                    //onLongPress = { showActions = true },
                    onTap = {
                            showActions = !showActions
                    }
                )
            },
        //elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedCompletionIcon(isCompleted = habit.isCompletedForToday, onToggleComplete = onToggleComplete)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (habit.isCompletedForToday) TextDecoration.LineThrough else null,
                            color = if (habit.isCompletedForToday) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val todayShortName = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
                        habit.daysOfWeek.map { it.take(3).uppercase() }.forEach { dayAbbreviation ->
                            val isToday = dayAbbreviation.equals(todayShortName, ignoreCase = true)
                            Text(
                                dayAbbreviation,
                                fontSize = 10.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                        CircleShape
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (habit.streakCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(id = R.drawable.firefill), // Use your project's R
                            contentDescription = "Streak",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = habit.streakCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (habit.isCompletedForToday && habit.streakCount > 0) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showActions) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                .copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onEdit(); showActions = false }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = { onDelete(); showActions = false }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCompletionIcon(isCompleted: Boolean, onToggleComplete: () -> Unit) {
    val icon = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
    val tint by animateColorAsState(
        targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(300), label = "iconTint"
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.15f else 0.05f,
        animationSpec = tween(300), label = "iconBackgroundAlpha"
    )

    IconButton(onClick = onToggleComplete, modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(tint.copy(alpha = backgroundAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isCompleted) "Mark Incomplete" else "Mark Complete",
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitDialog(
    habitToEdit: HabitEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, daysOfWeek: List<String>) -> Unit
) {
    var name by rememberSaveable(habitToEdit?.name) { mutableStateOf(habitToEdit?.name ?: "") }
    val allDays = remember { JavaDayOfWeek.entries.map { it.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase() } }
    val selectedDays = remember { mutableStateListOf<String>() }

    // State for the "Select All" checkbox
    var allDaysSelected by remember { mutableStateOf(false) }

    // Effect to initialize selectedDays and allDaysSelected checkbox
    LaunchedEffect(habitToEdit) {
        selectedDays.clear()
        if (habitToEdit != null) {
            selectedDays.addAll(habitToEdit.daysOfWeek.map { it.uppercase(Locale.ENGLISH) })
            allDaysSelected = selectedDays.size == allDays.size // Check if all days were initially selected
        } else {
            allDaysSelected = false // For new habit, default to not all selected
        }
    }

    // Effect to sync selectedDays list with allDaysSelected checkbox
    LaunchedEffect(allDaysSelected) {
        if (allDaysSelected) {
            selectedDays.clear()
            selectedDays.addAll(allDays)
        } else {
            // If unchecking "Select All", only clear if all were previously selected due to this checkbox.
            // This prevents unchecking "Select All" from clearing manually selected individual days
            // unless the user intends to deselect all. A bit nuanced, could be simpler if desired.
            // Simpler: if (!allDaysSelected) selectedDays.clear() - but this might be too aggressive.
            // Current: If allDaysSelected is false, it means either it was just unchecked OR
            // not all days were selected individually. We don't automatically clear here,
            // individual toggles will manage the list. The checkbox serves as a bulk add/check.
        }
    }

    // Effect to update "Select All" checkbox if all days are selected/deselected manually
    LaunchedEffect(selectedDays.toList()) { // Observe changes to the list content
        allDaysSelected = selectedDays.size == allDays.size && selectedDays.containsAll(allDays)
    }


    var nameError by remember { mutableStateOf<String?>(null) }
    var dayError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (habitToEdit == null) "Add New Habit" else "Edit Habit",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.isBlank()) "Name cannot be empty" else null
                    },
                    label = { Text("Habit Name*") },
                    isError = nameError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Repeat on:", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = allDaysSelected,
                            onCheckedChange = { checked ->
                                allDaysSelected = checked
                                if (checked) {
                                    selectedDays.clear()
                                    selectedDays.addAll(allDays)
                                } else {
                                    selectedDays.clear() // When "Select All" is unchecked, clear all days
                                }
                                dayError = if (selectedDays.isEmpty() && checked) "Select at least one day" // Should not happen if allDaysSelected true adds all
                                else if (selectedDays.isEmpty() && !checked) "Select at least one day"
                                else null
                            }
                        )
                        Text("All Days", style = MaterialTheme.typography.labelMedium)
                    }
                }
                FlowRow( // Use FlowRow for better chip layout
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allDays.forEach { day ->
                        DayChip(
                            dayName = day,
                            isSelected = selectedDays.contains(day),
                            onToggle = {
                                selectedDays.toggle(day) // Uses the extension function
                                // Update allDaysSelected checkbox based on individual selections
                                allDaysSelected = selectedDays.size == allDays.size && selectedDays.containsAll(allDays)

                                dayError = if (selectedDays.isEmpty()) "Select at least one day" else null
                            }
                        )
                    }
                }
                dayError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)) }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = {
                        val isNameValid = name.isNotBlank()
                        val areDaysSelected = selectedDays.isNotEmpty()
                        nameError = if (!isNameValid) "Name cannot be empty" else null
                        dayError = if (!areDaysSelected) "Select at least one day" else null

                        if (isNameValid && areDaysSelected) {
                            onConfirm(name, selectedDays.toList())
                        }
                    }) {
                        Text(if (habitToEdit == null) "Add" else "Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayChip(
    dayName: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = isSelected, // This is correctly passed
        onClick = onToggle,
        enabled = true,      // Explicitly providing, though it defaults to true for M3 FilterChip
        label = { Text(dayName.take(3)) },
        leadingIcon = if (isSelected) {
            { Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        shape = CircleShape,
        //this was the problem behind crashes while clicking on the add habit button.
//        border = FilterChipDefaults.filterChipBorder(
//            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isSelected) 0f else 0.5f),
//            selectedBorderColor = MaterialTheme.colorScheme.primary,
//            borderWidth = 1.dp,
//            selectedBorderWidth = 1.5.dp,
//            enabled = TODO(),
//            selected = TODO()
//        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, // More subtle unselected chip
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun ConfirmDeleteDialog(
    habitName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Habit?") },
        text = { Text("Are you sure you want to delete the habit \"$habitName\"? This will also affect its streak.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete", color = MaterialTheme.colorScheme.onError) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
