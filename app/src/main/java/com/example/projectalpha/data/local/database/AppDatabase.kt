package com.example.projectalpha.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.projectalpha.data.local.dao.HabitDao
import com.example.projectalpha.data.local.dao.StreakDao
import com.example.projectalpha.data.local.dao.TaskDao
import com.example.projectalpha.data.local.dao.UserDao
import com.example.projectalpha.data.local.entity.HabitEntity
import com.example.projectalpha.data.local.entity.StreakEntity
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Database(
    entities = [TaskEntity::class, HabitEntity::class, StreakEntity::class, UserEntity::class],
    version = 2, // Keep your current version
    exportSchema = false // Set to true if you plan to inspect schemas or add migrations later
)
@TypeConverters(Converters::class) // Ensure your Converters class is correctly implemented
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun streakDao(): StreakDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "project_alpha_db"

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(AppDatabaseCallback(context, scope)) // Pass context to callback
                    .fallbackToDestructiveMigration() // For now, during development. Replace with migrations for release.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context, // Added context
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    Log.d("AppDatabaseCallback", "Database created. Populating initial data.")
                    populateInitialData(database)
                }
            }
        }

        // Removed onOpen as it's not strictly needed for this pre-population logic

        private suspend fun populateInitialData(database: AppDatabase) {
            val taskDao = database.taskDao()
            val habitDao = database.habitDao()
            val streakDao = database.streakDao()
            // val userDao = database.userDao() // If you need to populate default user

            // --- Idempotency Check (Optional but good for development) ---
            // This prevents re-populating if the app is stopped right after DB creation
            // and restarted, potentially calling onCreate again in some edge cases
            // before the first population finishes.
            // Requires synchronous methods in DAOs for the check.
//            if (taskDao.getAllTasksList().isNotEmpty() || habitDao.getAllHabitsList().isNotEmpty()) {
//                Log.d("AppDatabaseCallback", "Database already contains data. Skipping pre-population.")
//                return
//            }
//            Log.d("AppDatabaseCallback", "Starting data population...")

            // Add initial streak points
            // Assuming StreakEntity ID 0 is the single row for the app's streak
            streakDao.insertOrUpdateStreak(StreakEntity(id = 0, points = 10))

            // Add sample tasks
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            taskDao.insertTask(
                TaskEntity(
                    title = "Morning Jog",
                    description = "Run for 30 minutes in the park.",
                    deadline = LocalDateTime.of(today, LocalTime.of(7, 0)),
                    priority = "Medium",
                    isCompleted = false,
                    date = today
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Project Proposal",
                    description = "Finalize and send the project proposal.",
                    deadline = LocalDateTime.of(today, LocalTime.of(17, 0)),
                    priority = "High",
                    isCompleted = false,
                    date = today
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Read a chapter",
                    description = "Read one chapter of 'Atomic Habits'.",
                    deadline = LocalDateTime.of(today, LocalTime.of(21, 0)), // Kept deadline for today
                    priority = "Low",
                    isCompleted = true, // Example of a task completed on a previous day
                    date = today.minusDays(1)
                )
            )
            taskDao.insertTask(
                TaskEntity(
                    title = "Grocery Shopping",
                    description = "Buy groceries for the week.",
                    deadline = LocalDateTime.of(tomorrow, LocalTime.of(10, 0)),
                    priority = "Medium",
                    isCompleted = false,
                    date = tomorrow
                )
            )
            Log.d("AppDatabaseCallback", "Sample tasks inserted.")

            Log.d("AppDatabaseCallback", "Sample habits inserted.")
            Log.d("AppDatabaseCallback", "Data population finished.")
        }
    }
}
