package com.phuc.synctask.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

// ─── Palette ───
private val IndigoPrimary      = Color(0xFF4B3FBE)
private val IndigoOnPrimary    = Color(0xFFFFFFFF)
private val IndigoPrimaryContainer = Color(0xFFE4DFFF)
private val IndigoOnPrimaryContainer = Color(0xFF120063)

private val GoldSecondary      = Color(0xFFB8860B)
private val GoldOnSecondary    = Color(0xFFFFFFFF)
private val GoldSecondaryContainer = Color(0xFFFFE082)
private val GoldOnSecondaryContainer = Color(0xFF3A2800)

// Light
private val LightColorScheme = lightColorScheme(
    primary              = IndigoPrimary,
    onPrimary            = IndigoOnPrimary,
    primaryContainer     = IndigoPrimaryContainer,
    onPrimaryContainer   = IndigoOnPrimaryContainer,
    secondary            = GoldSecondary,
    onSecondary          = GoldOnSecondary,
    secondaryContainer   = GoldSecondaryContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    background           = Color(0xFFF8FAFC),
    onBackground         = Color(0xFF0F172A),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF0F172A),
    surfaceVariant       = Color(0xFFEEEBFF),
    onSurfaceVariant     = Color(0xFF64748B),
    error                = Color(0xFFEF4444),
    onError              = Color(0xFFFFFFFF),
)

// Dark
private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF9E97FF),
    onPrimary            = Color(0xFF1A0E8F),
    primaryContainer     = Color(0xFF3328A5),
    onPrimaryContainer   = Color(0xFFE4DFFF),
    secondary            = Color(0xFFFFD54F),
    onSecondary          = Color(0xFF3A2800),
    secondaryContainer   = Color(0xFF5C4200),
    onSecondaryContainer = Color(0xFFFFE082),
    background           = Color(0xFF0F0F1A),
    onBackground         = Color(0xFFE8E6FF),
    surface              = Color(0xFF1C1B2E),
    onSurface            = Color(0xFFE8E6FF),
    surfaceVariant       = Color(0xFF2A2840),
    onSurfaceVariant     = Color(0xFF94A3B8),
    error                = Color(0xFFFF6B6B),
    onError              = Color(0xFF690005),
)

@Composable
fun SyncTaskTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Animate mỗi màu khi chuyển theme để tránh khựng
    val targetScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val primary              by animateColorAsState(targetScheme.primary,              tween(300), label = "primary")
    val onPrimary            by animateColorAsState(targetScheme.onPrimary,            tween(300), label = "onPrimary")
    val primaryContainer     by animateColorAsState(targetScheme.primaryContainer,     tween(300), label = "primaryContainer")
    val onPrimaryContainer   by animateColorAsState(targetScheme.onPrimaryContainer,   tween(300), label = "onPrimaryContainer")
    val secondary            by animateColorAsState(targetScheme.secondary,            tween(300), label = "secondary")
    val onSecondary          by animateColorAsState(targetScheme.onSecondary,          tween(300), label = "onSecondary")
    val secondaryContainer   by animateColorAsState(targetScheme.secondaryContainer,   tween(300), label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(targetScheme.onSecondaryContainer, tween(300), label = "onSecondaryContainer")
    val background           by animateColorAsState(targetScheme.background,           tween(300), label = "background")
    val onBackground         by animateColorAsState(targetScheme.onBackground,         tween(300), label = "onBackground")
    val surface              by animateColorAsState(targetScheme.surface,              tween(300), label = "surface")
    val onSurface            by animateColorAsState(targetScheme.onSurface,            tween(300), label = "onSurface")
    val surfaceVariant       by animateColorAsState(targetScheme.surfaceVariant,       tween(300), label = "surfaceVariant")
    val onSurfaceVariant     by animateColorAsState(targetScheme.onSurfaceVariant,     tween(300), label = "onSurfaceVariant")
    val error                by animateColorAsState(targetScheme.error,                tween(300), label = "error")
    val onError              by animateColorAsState(targetScheme.onError,              tween(300), label = "onError")

    val animatedScheme = targetScheme.copy(
        primary              = primary,
        onPrimary            = onPrimary,
        primaryContainer     = primaryContainer,
        onPrimaryContainer   = onPrimaryContainer,
        secondary            = secondary,
        onSecondary          = onSecondary,
        secondaryContainer   = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        background           = background,
        onBackground         = onBackground,
        surface              = surface,
        onSurface            = onSurface,
        surfaceVariant       = surfaceVariant,
        onSurfaceVariant     = onSurfaceVariant,
        error                = error,
        onError              = onError,
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography  = Typography(),
        content     = content
    )
}
