package com.prove2break.ui.common.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = FocusPrimaryLight,
    secondary = SecondaryLight,
    tertiary = BreakAmberLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    error = ErrorLight,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = FocusPrimaryDark,
    secondary = SecondaryDark,
    tertiary = BreakAmberDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = ErrorDark,
    outline = OutlineDark,
)

/**
 * Prove2Break Material 3 theme.
 *
 * Uses a calm, purpose-built seed palette (see Color.kt) rather than a generic
 * chatbot look. Dynamic color is intentionally NOT used by default so the
 * focus/break color language stays consistent across devices — see UX-UI.md.
 */
@Composable
fun Prove2BreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Prove2BreakTypography,
        content = content,
    )
}
