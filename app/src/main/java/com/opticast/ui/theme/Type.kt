package com.opticast.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.opticast.R

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

val OpticastTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = SpaceGrotesk),
        displayMedium = displayMedium.copy(fontFamily = SpaceGrotesk),
        displaySmall = displaySmall.copy(fontFamily = SpaceGrotesk),
        headlineLarge = headlineLarge.copy(fontFamily = SpaceGrotesk),
        headlineMedium = headlineMedium.copy(fontFamily = SpaceGrotesk),
        headlineSmall = headlineSmall.copy(fontFamily = SpaceGrotesk),
        titleLarge = titleLarge.copy(fontFamily = SpaceGrotesk),
        titleMedium = titleMedium.copy(fontFamily = SpaceGrotesk),
        titleSmall = titleSmall.copy(fontFamily = SpaceGrotesk),
        bodyLarge = bodyLarge.copy(fontFamily = SpaceGrotesk),
        bodyMedium = bodyMedium.copy(fontFamily = SpaceGrotesk),
        bodySmall = bodySmall.copy(fontFamily = SpaceGrotesk),
        labelLarge = labelLarge.copy(fontFamily = SpaceGrotesk),
        labelMedium = labelMedium.copy(fontFamily = SpaceGrotesk),
        labelSmall = labelSmall.copy(fontFamily = SpaceGrotesk),
    )
}

/** Monospace, tabular-feeling style for live numeric readouts (bitrate / fps / uptime). */
val MonoStat = TextStyle(
    fontFamily = SpaceMono,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.sp,
)
