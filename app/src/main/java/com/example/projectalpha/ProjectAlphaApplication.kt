package com.example.projectalpha

import android.app.Application
import com.example.projectalpha.data.local.database.AppDatabase
import com.example.projectalpha.data.repository.HabitRepository
// Import your IUserRepository and its implementation (or placeholder)
import com.example.projectalpha.data.repository.IUserRepository
import com.example.projectalpha.data.repository.StreakRepository
import com.example.projectalpha.data.repository.TaskRepository
import com.example.projectalpha.data.repository.UserRepository // Assuming you have this concrete class
// If you create a placeholder UserRepository:
// import com.example.projectalpha.data.repository.UserRepositoryPlaceholder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// If using Hilt, you would annotate this with @HiltAndroidApp
// @HiltAndroidApp
class ProjectAlphaApplication : Application() {

    // Application scope.
    // While viewModelScope is preferred for UI-related coroutines,
    // an application-wide scope can be useful for background tasks
    // initiated from the Application class itself (like the streak initialization).
    // If NOT using Hilt and viewModelScope, this is reasonable.
    // With Hilt, most operations are scoped to ViewModels or other Hilt components.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Database and repositories instances using by lazy so the database and repositories
    // are only created when they're needed.
    // In a Hilt setup, these would be provided via @Provides methods in an @Module.

    // The database instance
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) } // Pass the applicationScope if AppDatabase needs it

    // Repositories
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
    val habitRepository by lazy { HabitRepository(database.habitDao(), database.habitCompletionLogDao()) }
    val streakRepository by lazy { StreakRepository(database.streakDao()) }

    // Correctly initialize UserRepository
    // Option 1: If you have a concrete UserRepository that takes UserDao
    // val userRepository: IUserRepository by lazy { UserRepository(database.userDao()) } // Assuming UserDao exists

    // Option 2: If you have a placeholder or a UserRepository that doesn't need a DAO directly (e.g., uses SharedPreferences)
    // For now, let's assume a UserRepository that might take the application context or no args for placeholder
    // Ensure you have a 'UserDao' in your AppDatabase if UserRepository needs it.
    // If your UserRepository doesn't need a DAO (e.g. uses SharedPreferences or is a placeholder):
//    val userRepository: IUserRepository by lazy {
//        // Replace with your actual UserRepository implementation
//        // If it needs database.userDao(), make sure userDao() is defined in AppDatabase
//        // For example:
//        // UserRepository(database.userDao())
//        // Or if it's a simple placeholder that doesn't need a DAO:
//        // Simple inline placeholder
//    }
    val userRepository: IUserRepository by lazy {
        UserRepository(database.userDao()) // If it needs the DAO
        // UserRepository() // If it doesn't need the DAO for its placeholder version
    }


    override fun onCreate() {
        super.onCreate()
        // Initialize ThreeTenABP if minSdk < 26 and you are using java.time classes
        // (Modern Android Gradle Plugin with desugaring might handle this automatically for API < 26)
        // com.jakewharton.threetenabp.AndroidThreeTen.init(this);

        // Ensure streak record exists when the app starts.
        // It's good to do this on a background thread.
        applicationScope.launch {
            streakRepository.ensureStreakExists()
        }
    }
}
