plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") // For Room or other KSP-based processors
    alias(libs.plugins.kotlin.compose) // Essential: Applies the Kotlin Compose Compiler plugin
}

android {
    namespace = "com.example.projectalpha"
    compileSdk = 35 // Or your target compile SDK

    defaultConfig {
        applicationId = "com.example.projectalpha"
        minSdk = 24
        targetSdk = 35 // Or your target SDK
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true // Enables Jetpack Compose for the module
    }
    // composeOptions { // Only needed if you have to override compiler version, usually handled by BOM/plugin
    //     kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() // If you define compiler version in TOML
    // }
}

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx) // General lifecycle runtime

    // Activity & ViewModel (with Compose integration)
    implementation(libs.androidx.activity.compose)      // For ComponentActivity + Compose
    implementation(libs.androidx.lifecycle.viewmodel.ktx)      // For core ViewModel functionality
    implementation(libs.androidx.lifecycle.viewmodel.compose)  // Specifically for viewModel() in Compose

    // Jetpack Compose BOM and Libraries
    implementation(platform(libs.androidx.compose.bom)) // BOM to manage Compose library versions
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Or libs.androidx.material if you are using Material 2

    // Room (as you had it before)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Core Library Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4") // Or latest version

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // For Compose testing
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
