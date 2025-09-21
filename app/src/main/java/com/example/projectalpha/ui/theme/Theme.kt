package com.example.projectalpha.ui.theme

import android.app.Activity
import android.os.Build
import android.util.Log
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

/* Old Dark and Light Theme Color Schemes
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

 */

private val AppDarkColorScheme = darkColorScheme(
    primary = DeepBluePrimary,
    onPrimary = OnDeepBluePrimary,
    primaryContainer = DeepBluePrimaryContainer,
    onPrimaryContainer = OnDeepBluePrimaryContainer,

    secondary = TealSecondary,
    onSecondary = OnTealSecondary,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = OnTealSecondaryContainer,

    tertiary = PinkTertiary,
    onTertiary = OnPinkTertiary,
    tertiaryContainer = PinkTertiaryContainer,
    onTertiaryContainer = OnPinkTertiaryContainer,

    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = DarkBackground,
    onBackground = OnDarkBackground,

    surface = DarkSurface,
    onSurface = OnDarkSurface,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,

    outline = OutlineGray,

    inverseOnSurface = InverseOnSurfaceDark,
    inverseSurface = InverseSurfaceDark,
    inversePrimary = InversePrimaryDark,

    surfaceTint = SurfaceTintBlue
)

private val AppLightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = OnBluePrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnBluePrimaryContainer,

    secondary = GraySecondary,
    onSecondary = OnGraySecondary,
    secondaryContainer = GraySecondaryContainer,
    onSecondaryContainer = OnGraySecondaryContainer,

    tertiary = VioletTertiary,
    onTertiary = OnVioletTertiary,
    tertiaryContainer = VioletTertiaryContainer,
    onTertiaryContainer = OnVioletTertiaryContainer,

    error = ErrorRedLight,
    onError = OnErrorRedLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    background = LightBackground,
    onBackground = OnLightBackground,

    surface = LightSurface,
    onSurface = OnLightSurface,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,

    outline = OutlineGrayLight,

    inverseOnSurface = InverseOnSurfaceLight,
    inverseSurface = InverseSurfaceLight,
    inversePrimary = InversePrimaryLight,

    surfaceTint = SurfaceTintBlueLight
)

@Composable
fun ProjectAlphaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false if you want to strictly use your custom theme
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
    /* To Find ColorCode of dynamic Colors
    Log.d("Colors", "primary = ${colorScheme.primary}")
    Log.d("Colors", "onPrimary = ${colorScheme.onPrimary}")
    Log.d("Colors", "primaryContainer = ${colorScheme.primaryContainer}")
    Log.d("Colors", "onPrimaryContainer = ${colorScheme.onPrimaryContainer}")
    Log.d("Colors", "secondary = ${colorScheme.secondary}")
    Log.d("Colors", "onSecondary = ${colorScheme.onSecondary}")
    Log.d("Colors", "secondaryContainer = ${colorScheme.secondaryContainer}")
    Log.d("Colors", "onSecondaryContainer = ${colorScheme.onSecondaryContainer}")
    Log.d("Colors", "tertiary = ${colorScheme.tertiary}")
    Log.d("Colors", "onTertiary = ${colorScheme.onTertiary}")
    Log.d("Colors", "tertiaryContainer = ${colorScheme.tertiaryContainer}")
    Log.d("Colors", "onTertiaryContainer = ${colorScheme.onTertiaryContainer}")
    Log.d("Colors", "error = ${colorScheme.error}")
    Log.d("Colors", "onError = ${colorScheme.onError}")
    Log.d("Colors", "errorContainer = ${colorScheme.errorContainer}")
    Log.d("Colors", "onErrorContainer = ${colorScheme.onErrorContainer}")
    Log.d("Colors", "background = ${colorScheme.background}")
    Log.d("Colors", "onBackground = ${colorScheme.onBackground}")
    Log.d("Colors", "surface = ${colorScheme.surface}")
    Log.d("Colors", "onSurface = ${colorScheme.onSurface}")
    Log.d("Colors", "surfaceVariant = ${colorScheme.surfaceVariant}")
    Log.d("Colors", "onSurfaceVariant = ${colorScheme.onSurfaceVariant}")
    Log.d("Colors", "outline = ${colorScheme.outline}")
    Log.d("Colors", "inverseOnSurface = ${colorScheme.inverseOnSurface}")
    Log.d("Colors", "inverseSurface = ${colorScheme.inverseSurface}")
    Log.d("Colors", "inversePrimary = ${colorScheme.inversePrimary}")
    Log.d("Colors", "surfaceTint = ${colorScheme.surfaceTint}")
     */

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
