package com.example.restaurantpos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val FoodPOSDarkColorScheme = darkColorScheme(
    primary = BrandAmberGold,
    onPrimary = Color.Black,
    primaryContainer = BrandGoldDark,
    onPrimaryContainer = Color.Black,

    secondary = BrandGoldLight,
    onSecondary = Color.Black,

    background = DarkBase,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,

    error = AccentDanger,
    onError = Color.White,

    outline = BorderSubtle,
    outlineVariant = BorderHighlight,
)

@Composable
fun FoodPOSTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = FoodPOSDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBase.toArgb()
            window.navigationBarColor = DarkBase.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
