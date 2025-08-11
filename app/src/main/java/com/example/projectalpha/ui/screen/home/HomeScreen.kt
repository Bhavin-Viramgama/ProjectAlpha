package com.example.projectalpha.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AccessTime // Clock icon
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.ui.theme.*
import com.example.projectalpha.viewmodel.HomeViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(homeViewModel: HomeViewModel) {
    val username by homeViewModel.username.collectAsState()
    val monthlyTaskCount by homeViewModel.monthlyTaskCount.collectAsState()
    val streakEntity by homeViewModel.streakPoints.collectAsState()
    val todaysTasks by homeViewModel.todaysTasks.collectAsState()

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm a")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Screen padding
        ) {
            // Greeting
            Text(
                text = "Good Morning, $username",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Monthly Tasks: $monthlyTaskCount",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Streak is already in TopBar, but can be shown here too if desired
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar (Placeholder)
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO: Implement search logic */ },
                label = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Today's Tasks Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Today's Tasks", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { /* TODO: Navigate to To Do List screen */ }) {
                    Text("See all")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Today's Tasks List
            if (todaysTasks.isEmpty()) {
                Text(
                    "No tasks for today. Add some!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between items
                ) {
                    items(todaysTasks, key = { task -> task.id }) { task ->
                        TaskCard(task = task, timeFormatter = timeFormatter)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity, timeFormatter: DateTimeFormatter) {
    val cardColor = when (task.priority.lowercase()) {
        "high" -> HighPriorityColor.copy(alpha = 0.3f) // Lighter shade for background
        "medium" -> MediumPriorityColor.copy(alpha = 0.3f)
        "low" -> LowPriorityColor.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val priorityIndicatorColor = when (task.priority.lowercase()) {
        "high" -> HighPriorityColor // Solid color for indicator
        "medium" -> MediumPriorityColor
        "low" -> LowPriorityColor
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator (small colored circle or bar)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(priorityIndicatorColor)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                task.description?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2 // Limit description lines
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            task.deadline?.let { deadline ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Deadline time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = deadline.format(timeFormatter),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
