package com.example.projectalpha


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
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
//import com.example.projectalpha.ui.theme.HighPriorityFont1
import com.example.projectalpha.ui.navigation.bottomNavItems
import com.example.projectalpha.ui.screen.habits.HabitsScreen
//import com.example.projectalpha.ui.screen.home.HomeScreen
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
import androidx.compose.runtime.SideEffect
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.projectalpha.ui.screen.todo.AddEditTaskScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController

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
    // --- System UI Controller Setup ---
    val systemUiController = rememberSystemUiController()
    val desiredStatusBarColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Set your desired status bar color here

    SideEffect {
        systemUiController.setStatusBarColor(
            color = desiredStatusBarColor,
        )
       /* // You can also control the navigation bar color if needed:
        // systemUiController.setNavigationBarColor(
        //    color = Color.Black, // Or your desired nav bar color
        //    darkIcons = false // If nav bar is dark, icons should be light
        // )

        */
    }
    // --- End System UI Controller Setup ---



    val streakEntity by homeViewModel.streakPoints.collectAsState() // Observe from HomeViewModel
    val streakPointsDisplay = streakEntity?.points ?: 0

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    /*val currentScreenTitle = bottomNavItems.find { it.route == currentDestination?.route }?.title
        ?: Screen.Profile.title.takeIf { currentDestination?.route == Screen.Profile.route }
        ?: Screen.AddEditTask.title.takeIf { currentDestination?.route == Screen.AddEditTask.route }
        ?: "Project Alpha" // Default title

     */

    val currentScreenTitle = when (currentDestination?.route) {
        Screen.ToDoList.route -> Screen.ToDoList.title
        Screen.Pomodoro.route -> Screen.Pomodoro.title
        Screen.Habits.route -> Screen.Habits.title
        Screen.Profile.route -> Screen.Profile.title
        // Check for AddEditTask route (base route without arguments)
        // Or if you want to be more specific, you can check if route starts with Screen.AddEditTask.route
        Screen.AddEditTask.route, Screen.AddEditTask.PushedTasks() -> { // Catches route with or without default args
            // Dynamic title for Add/Edit Task Screen
            val taskId = navBackStackEntry?.arguments?.getInt(Screen.AddEditTask.ARG_TASK_ID)
            if (taskId == null || taskId == -1) "Add New Task" else "Edit Task"
        }
        else -> {
            // Check against bottom nav items if it's one of them (handles dynamic titles if Screen object has one)
            bottomNavItems.find { currentDestination?.hierarchy?.any { dest -> dest.route == it.route } == true }?.title
                ?: "Project Alpha" // Default title
        }
    } ?: "Project Alpha" // Fallback if title is null


    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // Determine if TopAppBar should be shown
    val shouldShowTopAppBar = currentDestination?.route != Screen.AddEditTask.route &&
            !currentDestination?.route.orEmpty().startsWith(Screen.AddEditTask.route + "?")

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (shouldShowTopAppBar){
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            currentScreenTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), // Example: Default M3 TopAppBar color
                    ),
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF212121))
                                .padding(4.dp)
                        ) {
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
            }
        },
        bottomBar = {
            val isBottomBarVisible = bottomNavItems.any { it.route == currentDestination?.route }
            if (isBottomBarVisible) {
                AppBottomNavigationBar(navController = navController, items = bottomNavItems)
            }
        },

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
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), // background
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
            ToDoScreen(toDoViewModel = toDoViewModel,
                navController= navController) // Pass the ToDoViewModel
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

        // New route for Add/Edit Task Screen
        composable(
            route = Screen.AddEditTask.route + "?${Screen.AddEditTask.ARG_TASK_ID}={${Screen.AddEditTask.ARG_TASK_ID}}",
            arguments = listOf(
                navArgument(Screen.AddEditTask.ARG_TASK_ID) {
                    type = NavType.IntType
                    defaultValue = -1 // Default if no ID is passed (for adding new task)
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt(Screen.AddEditTask.ARG_TASK_ID)
            AddEditTaskScreen(
                navController = navController,
                toDoViewModel = toDoViewModel,
                taskId = if (taskId == -1) null else taskId // Pass null for new task
            )
        }
    }
}