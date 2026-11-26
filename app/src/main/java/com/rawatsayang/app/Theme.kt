package com.rawatsayang.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val Green = Color(0xFF2F6F5E)
val GreenDark = Color(0xFF1B4438)
val GreenSoft = Color(0xFFDCEFE7)
val Ok = Color(0xFF1E8E5A)
val Warn = Color(0xFFD98E04)
val Bad = Color(0xFFD64545)

private val Light = lightColorScheme(
    primary = Green, onPrimary = Color.White,
    primaryContainer = GreenSoft, onPrimaryContainer = GreenDark,
    secondary = Green, onSecondary = Color.White,
    secondaryContainer = GreenSoft, onSecondaryContainer = GreenDark,
    background = Color(0xFFF3F7F5), surface = Color.White,
    surfaceVariant = Color(0xFFE6EEEA), onSurface = Color(0xFF16231F),
    onSurfaceVariant = Color(0xFF52625C), outline = Color(0xFFB9C7C1)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FCBB3), onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF1F4D41), onPrimaryContainer = GreenSoft,
    secondary = Color(0xFF7FCBB3), onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF1F4D41), onSecondaryContainer = GreenSoft,
    background = Color(0xFF0F1815), surface = Color(0xFF16221E),
    surfaceVariant = Color(0xFF22322D), onSurface = Color(0xFFE3EEE9),
    onSurfaceVariant = Color(0xFFA7B8B1), outline = Color(0xFF4A5D56)
)

@Composable
fun RawatSayangTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = Shapes(medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
        content = content
    )
}
