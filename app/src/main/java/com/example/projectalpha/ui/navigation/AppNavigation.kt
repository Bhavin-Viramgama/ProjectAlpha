package com.example.projectalpha.ui.navigation

import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
//import androidx.compose.material.icons.filled.FitnessCenter // Or a better habit icon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
//import androidx.compose.material.icons.filled.Timer // Or a Pomodoro icon
import androidx.compose.ui.graphics.vector.ImageVector

// Sealed class for defining navigation routes
sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    //object Home : Screen("home", "Home", Icons.Filled.Home)
    object ToDoList : Screen("todo", "To-Do", Icons.Filled.CheckCircle)
    object Pomodoro : Screen("pomodoro", "Pomodoro", Icons.Filled.Notifications)
    object Habits : Screen("habits", "Habits", Icons.Filled.Face)
    object Profile : Screen("profile_nav", "Profile") // No icon for bottom nav
}

// List of bottom navigation items
val bottomNavItems = listOf(
    //Screen.Home,
    Screen.ToDoList,
    Screen.Pomodoro,
    Screen.Habits
)
