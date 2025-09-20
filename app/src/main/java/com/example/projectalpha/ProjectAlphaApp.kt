package com.example.projectalpha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projectalpha.ui.navigation.Screen
import com.example.projectalpha.ui.theme.HighPriorityFont1
import com.example.projectalpha.ui.navigation.bottomNavItems
import com.example.projectalpha.ui.screen.habits.HabitsScreen
import com.example.projectalpha.ui.screen.home.HomeScreen
import com.example.projectalpha.ui.screen.pomodoro.PomodoroScreen
import com.example.projectalpha.ui.screen.profile.ProfileScreen
import com.example.projectalpha.ui.screen.todo.ToDoScreen
import com.example.projectalpha.viewmodel.HabitsViewModel
import com.example.projectalpha.viewmodel.HabitsViewModelFactory
import com.example.projectalpha.viewmodel.HomeViewModel
import com.example.projectalpha.viewmodel.HomeViewModelFactory
import com.example.projectalpha.viewmodel.PomodoroViewModel
import com.example.projectalpha.viewmodel.PomodoroViewModelFactory
import com.example.projectalpha.viewmodel.ProfileViewModel
import com.example.projectalpha.viewmodel.ProfileViewModelFactory
import com.example.projectalpha.viewmodel.ToDoViewModel
import com.example.projectalpha.viewmodel.ToDoViewModelFactory
import kotlin.collections.forEach

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
        factory = PomodoroViewModelFactory()
    )
    val habitsViewModel: HabitsViewModel = viewModel(
        factory = HabitsViewModelFactory(
            application.habitRepository,
            application.streakRepository,
            application // Pass Application instance
        )
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
        ?: "Project Alpha" // Default title

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
//                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
//                        Icon(Icons.Filled.AccountCircle, "Profile")
//                    }
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
            //homeViewModel = homeViewModel,
            toDoViewModel = toDoViewModel,
            pomodoroViewModel = pomodoroViewModel,
            habitsViewModel = habitsViewModel,
            profileViewModel = profileViewModel // Pass the ProfileViewModel
        )
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavHostController, items: List<Screen>) {
    NavigationBar(
        containerColor = Color(0xFF1E1E1E), // background
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination


        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    screen.icon?.let { icon ->
                        Icon(icon, contentDescription = screen.title, tint = if (selected) Color(0xFF9B4DFF) else Color.Gray)
                    }
                },
                label = { Text(screen.title ?: "") },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF9B4DFF).copy(alpha = 0.15f),
                    selectedIconColor = Color(0xFF9B4DFF),
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color(0xFF9B4DFF),
                    unselectedTextColor = Color.Gray
                )

            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    //homeViewModel: HomeViewModel,
    toDoViewModel: ToDoViewModel,
    pomodoroViewModel: PomodoroViewModel,
    habitsViewModel: HabitsViewModel,
    profileViewModel: ProfileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Pomodoro.route,
        modifier = modifier
    ) {
        /*
        //This is the old component for Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController, //for navigation to todoScreen on press see all button
                homeViewModel = homeViewModel
            )
        }

         */

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