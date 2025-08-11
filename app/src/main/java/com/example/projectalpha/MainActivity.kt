package com.example.projectalpha

import androidx.lifecycle.viewmodel.compose.viewModel // For viewModel()
import com.example.projectalpha.viewmodel.HomeViewModel
import com.example.projectalpha.viewmodel.HomeViewModelFactory
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import androidx.compose.material3.ExperimentalMaterial3Api
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projectalpha.ui.navigation.Screen
import com.example.projectalpha.ui.navigation.bottomNavItems
import com.example.projectalpha.ui.screen.habits.HabitsScreen
import com.example.projectalpha.ui.screen.home.HomeScreen
import com.example.projectalpha.ui.screen.pomodoro.PomodoroScreen
import com.example.projectalpha.ui.screen.profile.ProfileScreen
import com.example.projectalpha.ui.screen.todo.ToDoScreen
import com.example.projectalpha.ui.theme.ProjectAlphaTheme
import com.example.projectalpha.viewmodel.HabitsViewModel
import com.example.projectalpha.viewmodel.HabitsViewModelFactory
import com.example.projectalpha.viewmodel.PomodoroViewModel
import com.example.projectalpha.viewmodel.PomodoroViewModelFactory
import com.example.projectalpha.viewmodel.ToDoViewModel
import com.example.projectalpha.viewmodel.ToDoViewModelFactory
import com.example.projectalpha.viewmodel.* // Import all your ViewModels and Factories


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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectAlphaApp() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as ProjectAlphaApplication

    // ViewModel Instantiations
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(application.taskRepository, application.streakRepository)
    )
    val toDoViewModel: ToDoViewModel = viewModel(
        factory = ToDoViewModelFactory(application.taskRepository)
    )
    val pomodoroViewModel: PomodoroViewModel = viewModel(
        factory = PomodoroViewModelFactory(application.streakRepository)
    )
    val habitsViewModel: HabitsViewModel = viewModel(
        factory = HabitsViewModelFactory(application.habitRepository)
    )
    // Assuming you have UserRepository and ProfileViewModelFactory defined
    // For now, let's placeholder it or create a simple one if not ready
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            application.userRepository, // Make sure userRepository is in Application
            application.streakRepository,
            application.taskRepository
        )
    )
    // ---

    val streakEntity by homeViewModel.streakPoints.collectAsState() // Observe from HomeViewModel
    val streakPointsDisplay = streakEntity?.points ?: 0

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentScreenTitle = bottomNavItems.find { it.route == currentDestination?.route }?.title
        ?: Screen.Profile.title.takeIf { currentDestination?.route == Screen.Profile.route }
        ?: Screen.Home.title ?: "Project Alpha" // Default title

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(currentScreenTitle, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF212121))
                            .padding(4.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.firefill),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Streak Points",
                            tint = Color.Unspecified
                        )
                        Text(
                            text = streakPointsDisplay.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Filled.AccountCircle, "Profile")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val isBottomBarVisible = bottomNavItems.any { it.route == currentDestination?.route }
            if (isBottomBarVisible) {
                AppBottomNavigationBar(navController = navController, items = bottomNavItems)
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            homeViewModel = homeViewModel,
            toDoViewModel = toDoViewModel,
            pomodoroViewModel = pomodoroViewModel,
            habitsViewModel = habitsViewModel,
            profileViewModel = profileViewModel // Pass the ProfileViewModel
        )
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController, items: List<Screen>) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    screen.icon?.let { icon ->
                        Icon(icon, contentDescription = screen.title)
                    }
                },
                label = { Text(screen.title ?: "") },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel,
    toDoViewModel: ToDoViewModel,       // Add ToDoViewModel as a parameter
    pomodoroViewModel: PomodoroViewModel, // Add PomodoroViewModel
    habitsViewModel: HabitsViewModel,   // Add HabitsViewModel
    profileViewModel: ProfileViewModel  // Add ProfileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(homeViewModel = homeViewModel)
        }
        composable(Screen.ToDoList.route) {
            ToDoScreen(toDoViewModel = toDoViewModel) // Pass the ToDoViewModel
        }
        composable(Screen.Pomodoro.route) {
            PomodoroScreen(pomodoroViewModel = pomodoroViewModel) // Pass the PomodoroViewModel
        }
        composable(Screen.Habits.route) {
            HabitsScreen(habitsViewModel = habitsViewModel) // Pass the HabitsViewModel
        }
        composable(Screen.Profile.route) {
            ProfileScreen(profileViewModel = profileViewModel) // Pass the ProfileViewModel
        }
    }
}

/*@Composable
fun TaskCardPreview() {
    val dummyTask = TaskEntity(
        id = 1,
        title = "Finish Kotlin Project",
        description = "Complete the Jetpack Compose UI and test all features before the deadline.",
        deadline = LocalDateTime.now().plusHours(5), // 5 hours from now
        priority = "high",
        isCompleted = false,
        date = LocalDate.now()
    )

    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a") // Example format

    TaskCard(task = dummyTask, timeFormatter = timeFormatter)
}



@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun prev(){
    ProjectAlphaTheme {
            Scaffold(
            ) { innerPadding ->
                Surface(Modifier.padding(innerPadding)) {
                    Column(Modifier.padding(16.dp)){
                        TaskCardPreview()
                    }
                }
            }
    }
}

 */