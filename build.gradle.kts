// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false // This will correctly use Kotlin 2.0.21
    alias(libs.plugins.kotlin.compose) apply false // This will correctly use Compose plugin for Kotlin 2.0.21
    // Correct KSP version for Kotlin 2.0.21 - YOU MUST VERIFY THIS VERSION
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false // EXAMPLE - VERIFY!
}