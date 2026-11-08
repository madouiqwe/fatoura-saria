package com.madoui.fatoura.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Teal = Color(0xFF00796B)
val TealDark = Color(0xFF005B52)
val TealSoft = Color(0xFFDDF2EF)
val Green = Color(0xFF2E7D32)
val GreenSoft = Color(0xFFE3F2E5)
val Blue = Color(0xFF1E88E5)
val BlueSoft = Color(0xFFE3F0FC)
val Amber = Color(0xFFF9A825)
val AmberSoft = Color(0xFFFFF6DC)

val Background = Color(0xFFF6F7F9)
val SurfaceWhite = Color.White
val TextPrimary = Color(0xFF1F2933)
val TextMuted = Color(0xFF6B7280)
val LineColor = Color(0xFFE6E8EC)
val Danger = Color(0xFFD32F2F)
val DangerSoft = Color(0xFFFDE8E8)
val StatBackground = Color(0xFFF1F0F6)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealSoft,
    onPrimaryContainer = TealDark,
    secondary = Green,
    onSecondary = Color.White,
    secondaryContainer = GreenSoft,
    onSecondaryContainer = Color(0xFF1B4D1F),
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = TextMuted,
    outline = LineColor,
    outlineVariant = LineColor,
    error = Danger,
    onError = Color.White
)

@Composable
fun FatouraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
