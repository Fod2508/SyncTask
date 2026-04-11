package com.phuc.synctask.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4B3FBE),
    secondary = androidx.compose.ui.graphics.Color(0xFF80CBC4),
    tertiary = androidx.compose.ui.graphics.Color(0xFFCE93D8)
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4B3FBE),
    secondary = androidx.compose.ui.graphics.Color(0xFF00897B),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7B1FA2),
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F)
)

@Composable
fun SyncTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
