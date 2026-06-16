package com.opticast.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val OpticastColors = darkColorScheme(
    primary = Lime,
    onPrimary = OnLime,
    secondary = LimeDim,
    onSecondary = OnLime,
    tertiary = Amber,
    onTertiary = Color(0xFF231A00),
    background = Background,
    onBackground = TextHigh,
    surface = Surface,
    onSurface = TextHigh,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = Outline,
    outlineVariant = Outline,
    error = LiveRed,
    onError = Color.White,
)

private val OpticastShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun OpticastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OpticastColors,
        typography = OpticastTypography,
        shapes = OpticastShapes,
        content = content,
    )
}
