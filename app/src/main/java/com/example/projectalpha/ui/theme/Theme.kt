package com.example.projectalpha.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppDarkColorScheme = darkColorScheme(
    primary = DeepBluePrimaryLight, // Lighter blue for primary in dark mode for better visibility
    onPrimary = NeutralGrayDark,    // Dark text on light blue
    primaryContainer = DeepBluePrimaryDark, // Darker blue for containers
    onPrimaryContainer = White,
    secondary = TealAccent,
    onSecondary = Black,
    secondaryContainer = TealAccentDark,
    onSecondaryContainer = White,
    tertiary = PinkAccent, // Keep a distinct tertiary if needed
    onTertiary = Black,
    tertiaryContainer = Color(0xFF7E002D),
    onTertiaryContainer = White,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = NeutralGrayDark,
    onBackground = TextPrimaryDark,
    surface = NeutralGraySurface, // Cards, dialogs background
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF45464F), // Slightly lighter than surface for subtle distinctions
    onSurfaceVariant = TextSecondaryDark,
    outline = NeutralGrayMedium, // For borders, dividers
    inverseOnSurface = NeutralGrayDark,
    inverseSurface = DeepBluePrimaryLight, // Example for SnackBar
    inversePrimary = DeepBluePrimary,
    surfaceTint = DeepBluePrimaryLight // Tint color for elevated surfaces
)

private val AppLightColorScheme = lightColorScheme(
    primary = DeepBluePrimary,
    onPrimary = White,
    primaryContainer = DeepBluePrimaryLight,
    onPrimaryContainer = DeepBluePrimaryDark, // Dark text on light blue container
    secondary = TealAccent,
    onSecondary = White,
    secondaryContainer = Color(0xFFB2DFDB), // Lighter Teal for container
    onSecondaryContainer = TealAccentDark,
    tertiary = AmberAccent, // Amber for a warm tertiary accent
    onTertiary = Black,
    tertiaryContainer = Color(0xFFFFECB3), // Light amber container
    onTertiaryContainer = Color(0xFF604500),
    error = Color(0xFFB00020),
    onError = White,
    errorContainer = Color(0xFFFDE7E9),
    onErrorContainer = Color(0xFF410002),
    background = NeutralGrayLight, // Light gray background
    onBackground = TextPrimaryLight,
    surface = CardBackgroundLightM3, // White for cards, dialogs
    onSurface = TextPrimaryLight,
    surfaceVariant = NeutralGrayMedium, // For dividers, disabled states, outlines on cards
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFF757575), // Slightly darker outline for better visibility
    inverseOnSurface = White,
    inverseSurface = NeutralGrayDark,
    inversePrimary = DeepBluePrimaryLight,
    surfaceTint = DeepBluePrimary // Tint color for elevated surfaces
)

@Composable
fun ProjectAlphaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // Set to false if you want to strictly use your custom theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AppDarkColorScheme
        else -> AppLightColorScheme
    }
    // to handle status bar color
//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        SideEffect {
//            val window = (view.context as Activity).window
//            // Set status bar color to be slightly darker/lighter version of the background or primary
//            // For a "GitHub" feel, the status bar is often part of the main app header or a subtle dark/light
//            window.statusBarColor = if (darkTheme) NeutralGrayDark.toArgb() else DeepBluePrimaryDark.toArgb() // Example: Dark blue status bar in light theme
//
//            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
//            // For older SDKs, you might need different status bar icon handling.
//        }
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Your existing typography
        shapes = Shapes,       // Add Shapes if you have custom ones (see below)
        content = content
    )
}
