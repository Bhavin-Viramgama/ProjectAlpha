package com.example.projectalpha.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.projectalpha.data.local.dao.HabitDao
import com.example.projectalpha.data.local.dao.StreakDao
import com.example.projectalpha.data.local.dao.TaskDao
import com.example.projectalpha.data.local.dao.UserDao
import com.example.projectalpha.data.local.dao.HabitCompletionLogDao
import com.example.projectalpha.data.local.entity.HabitEntity
import com.example.projectalpha.data.local.entity.StreakEntity
import com.example.projectalpha.data.local.entity.TaskEntity
import com.example.projectalpha.data.local.entity.UserEntity
import com.example.projectalpha.data.local.entity.HabitCompletionLogEntity
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
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        StreakEntity::class,
        UserEntity::class,
        HabitCompletionLogEntity::class],
    version = 4, // Keep your current version
    exportSchema = true // Set to true if you plan to inspect schemas or add migrations later
)
@TypeConverters(Converters::class) // Ensure your Converters class is correctly implemented
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun streakDao(): StreakDao
    abstract fun userDao(): UserDao
    abstract fun habitCompletionLogDao(): HabitCompletionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "project_alpha_db"

        // --- MIGRATION EXAMPLE ---
        // You'll need a migration from version 3 to 4 because you added a new table.
        // If your previous version didn't have exportSchema = true, Room can't auto-generate.
        // For now, if you are okay with losing data during development, fallbackToDestructiveMigration is fine.
        // Otherwise, you need a real migration.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQL to create the new habit_completion_logs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_completion_logs` (
                        `habitId` INTEGER NOT NULL, 
                        `dateCompleted` INTEGER NOT NULL, 
                        PRIMARY KEY(`habitId`, `dateCompleted`), 
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                // Create indices separately if not included in CREATE TABLE above by Room's schema generation
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completion_logs_habitId` ON `habit_completion_logs` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completion_logs_dateCompleted` ON `habit_completion_logs` (`dateCompleted`)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(AppDatabaseCallback(context, scope)) // Pass context to callback
                    // .fallbackToDestructiveMigration() // Use this if you don't want to write migrations yet
                    .addMigrations(MIGRATION_3_4) // <<< ADD MIGRATION
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

        // You might have an onOpen callback for different logic if needed.
        // override fun onOpen(db: SupportSQLiteDatabase) {
        // super.onOpen(db);
        // Log.d("AppDatabaseCallback", "Database OPENED.");
        // If you need to do something every time the DB opens (e.g., verify schema or run checks)
        // }


        private suspend fun populateInitialData(database: AppDatabase) {
            // ... (your existing task, habit, streak population logic) ...
            // You might want to add some sample HabitCompletionLogEntity entries
            // for testing, corresponding to your sample habits.
            Log.d("AppDatabaseCallback", "Populating initial data...")
            val taskDao = database.taskDao()
            val habitDao = database.habitDao()
            val streakDao = database.streakDao()
            val habitCompletionLogDao = database.habitCompletionLogDao()

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

            val todayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
            val dayAfterTomorrowName = today.plusDays(2).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()

            habitDao.insertHabit(
                HabitEntity(
                    name = "Morning Meditation",
                    daysOfWeek = listOf(todayName, "SATURDAY", "SUNDAY"),
                    streakCount = 2,
                    isCompletedForToday = false,
                    lastCompletedDate = today.minusDays(1) // Assuming it was completed yesterday
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    name = "Read for 30 Mins",
                    daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
                    streakCount = 5,
                    isCompletedForToday = false,
                    lastCompletedDate = null
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    name = "Weekend Jog",
                    daysOfWeek = listOf("SATURDAY", "SUNDAY"),
                    isCompletedForToday = false, // Reset correctly
                    lastCompletedDate = null
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    name = "Hydrate Well (Scheduled for today and day after tomorrow)",
                    daysOfWeek = listOf(todayName, dayAfterTomorrowName) // Ensure at least one habit is for "today"
                )
            )
            Log.d("AppDatabaseCallback", "Sample habits inserted.")

            // Example of populating Habit and its logs
            val medId = habitDao.insertHabit(
                HabitEntity(
                    name = "Morning Meditation",
                    daysOfWeek = listOf(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(), "SATURDAY", "SUNDAY"),
                    streakCount = 1, // Start with 1 if completed yesterday
                    isCompletedForToday = false,
                    lastCompletedDate = today.minusDays(1)
                )
            )
            // Log completion for yesterday
            habitCompletionLogDao.insertCompletionLog(HabitCompletionLogEntity(medId.toInt(), today.minusDays(1)))


            val readId = habitDao.insertHabit(
                HabitEntity(
                    name = "Read for 30 Mins",
                    daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
                    streakCount = 0, // No initial streak
                    isCompletedForToday = false,
                    lastCompletedDate = null
                )
            )
            // Example: completed 2 and 3 days ago
            habitCompletionLogDao.insertCompletionLog(HabitCompletionLogEntity(readId.toInt(), today.minusDays(2)))
            habitCompletionLogDao.insertCompletionLog(HabitCompletionLogEntity(readId.toInt(), today.minusDays(3)))


            habitDao.insertHabit(
                HabitEntity(
                    name = "Weekend Jog",
                    daysOfWeek = listOf("SATURDAY", "SUNDAY"),
                    isCompletedForToday = false,
                    lastCompletedDate = null
                )
            )
            habitDao.insertHabit(
                HabitEntity(
                    name = "Hydrate Well (Scheduled for today)",
                    daysOfWeek = listOf(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase())
                )
            )
            Log.d("AppDatabaseCallback", "Sample data populated.")
        }
    }
}
