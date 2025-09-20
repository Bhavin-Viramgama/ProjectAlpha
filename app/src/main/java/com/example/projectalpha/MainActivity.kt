package com.example.projectalpha

import android.icu.lang.UCharacter.toUpperCase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.ui.theme.AppTypography
import com.example.projectalpha.ui.theme.HighPriorityFont
import com.example.projectalpha.ui.theme.LowPriorityFont
import com.example.projectalpha.ui.theme.MediumPriorityFont
import com.example.projectalpha.ui.theme.ProjectAlphaTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjectAlphaTheme {
                ProjectAlphaApp()
            }
        }
    }
}



@Composable
fun TaskCardPreview() {
    val dummyTask = TaskEntity(
        id = 1,
        title = "Finish Kotlin Project",
        description = "Complete the Jetpack Compose UI and test all features before the deadline.\nComplete the Jetpack Compose UI and test all features before the deadline.",
        deadline = LocalDateTime.now().plusHours(5), // 5 hours from now
        priority = "high",
        isCompleted = false,
        date = LocalDate.now()
    )

    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a") // Example format

    TaskItem(task = dummyTask, onToggleComplete = {}, onEdit = {}, onDelete = {})
//    LazyColumn(
//                    modifier = Modifier.fillMaxSize(),
//                    verticalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    items(tasks, key = { task -> task.id }) { task ->
//                        TaskItem(
//                            task = task,
//                            onToggleComplete = { toDoViewModel.toggleTaskCompletion(task) },
//                            onEdit = {
//                                taskToEdit = task
//                                showAddTaskDialog = true
//                            },
//                            onDelete = { taskToDelete = task }
//                        )
//                    }
//                }
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
        "high" -> HighPriorityFont // Solid color for indicator
        "medium" -> MediumPriorityFont
        "low" -> LowPriorityFont
        else -> Color.Transparent
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
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
                        modifier = Modifier.padding(top = 4.dp),
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



//@OptIn(ExperimentalMaterial3Api::class)
//@Preview(showBackground = true)
//@Composable
//fun Prev(){
//    ProjectAlphaTheme {
//            Scaffold(
//            ) { innerPadding ->
//                Surface (Modifier.padding(innerPadding)){
//                    Column(
//                        modifier = Modifier.fillMaxSize().padding(10.dp),
//                        verticalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        TaskCardPreview()
//                        TaskCardPreview()
//                    }
//
//                }
//
//            }
//    }
//}


