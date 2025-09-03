package com.example.projectalpha.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import android.icu.lang.UCharacter.toUpperCase
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
//import androidx.compose.material.icons.filled.AccessTime // Clock icon
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.projectalpha.AppNavHost
import com.example.projectalpha.R
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.ui.navigation.Screen
import com.example.projectalpha.ui.theme.*
import com.example.projectalpha.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import kotlin.collections.set

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavController,homeViewModel: HomeViewModel) {
    val username by homeViewModel.username.collectAsState()
    val todaysTaskCount by homeViewModel.todaysTaskCount.collectAsState()
    //val streakEntity by homeViewModel.streakPoints.collectAsState()
    val todaysTasks by homeViewModel.todaysTasks.collectAsState()
    //val navController = rememberNavController()

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
                text = "Good Morning, $username!",
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
                    "Today's Total Tasks: $todaysTaskCount",
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
                label = {
                    Text(text = "Search tasks",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 10.dp),
                singleLine = true,
                shape = RoundedCornerShape(24),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface, //CotainerBG
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color(0xFF3F51B5), //Border
                    unfocusedIndicatorColor = Color.LightGray,
                    cursorColor = Color(0xFF3F51B5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.DarkGray,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                trailingIcon = {
                    IconButton(onClick = { /*TODO: Clear Search*/ }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }

            )
            Spacer(modifier = Modifier.height(16.dp))

            // Today's Tasks Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Today's Tasks", style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = { navController.navigate(Screen.ToDoList.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }/* TODO: Navigate to To Do List screen */ },
                    /*For Border
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    contentPadding = PaddingValues(0.dp)
                     */
                ) {
                    Text(text = "See all", style = MaterialTheme.typography.labelMedium)
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

                        val visibleStates = remember { mutableStateMapOf<Int, Boolean>() }

                        // Trigger animation when this item enters composition
                        LaunchedEffect(todaysTasks) {
                            visibleStates.clear()
                            todaysTasks.forEachIndexed { index, task ->
                                delay(index * 80L) // delay for staggered animation
                                visibleStates[task.id] = true
                            }
                        }
                        val visible = visibleStates[task.id] ?: false

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    slideInVertically(initialOffsetY = { it / 2 }),
                            exit = fadeOut(animationSpec = tween(300)) +
                                    slideOutVertically(targetOffsetY = { it / 2 }),
                            modifier = Modifier.animateItem()
                        )  {
                            TaskCard(task = task, timeFormatter = timeFormatter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: TaskEntity, timeFormatter: DateTimeFormatter) {

    val cardColor = when (task.priority.lowercase()) {
        "high" -> HighPriorityColor.copy(alpha = 0.2f) // Lighter shade for background
        "medium" -> MediumPriorityColor.copy(alpha = 0.2f)
        "low" -> LowPriorityColor.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val priorityIndicatorColor = when (task.priority.lowercase()) {
        "high" -> HighPriorityIndicator // Solid color for indicator
        "medium" -> MediumPriorityIndicator
        "low" -> LowPriorityIndicator
        else -> Color.Transparent
    }
    val priorityFontColor = when (task.priority.lowercase()) {
        "high" -> HighPriorityFont
        "medium" -> MediumPriorityFont
        "low" -> LowPriorityFont
        else -> Color.Transparent
    }
    val priorityFontColor2 = when (task.priority.lowercase()) {
        "high" -> HighPriorityFont2
        "medium" -> MediumPriorityFont2
        "low" -> LowPriorityFont2
        else -> Color.Transparent
    }
    val priorityFontColor1 = when (task.priority.lowercase()) {
        "high" -> HighPriorityFont1
        "medium" -> MediumPriorityFont1
        "low" -> LowPriorityFont1
        else -> Color.Transparent
    }

    //For Clickable Expansion of the card
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            .clickable{isExpanded = !isExpanded},
        shape = RoundedCornerShape(12.dp),
        //border = BorderStroke(1.dp,priorityFontColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {


        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.Center

        ){
            Text(
                text = "${toUpperCase(task.priority)} PRIORITY",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.W400,
                color = priorityFontColor1
            )
            Row(Modifier.padding(top=8.dp),
                horizontalArrangement = Arrangement.Center) {
                // Priority Indicator (small colored circle or bar)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(priorityFontColor2)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

            }

        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                task.description?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            maxLines = if(isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis // Shows "..." if text exceeds
                        )
                    }
                }

                task.deadline?.let { deadline ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.timeline),
                            contentDescription = "Deadline time",
                            modifier = Modifier.size(20.dp),
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
}
