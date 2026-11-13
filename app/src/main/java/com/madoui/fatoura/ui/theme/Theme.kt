package com.madoui.fatoura.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madoui.fatoura.R

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

/** خط عربي عصري واضح الأرقام، يُستخدم في كل شاشات التطبيق وفي الإيصال المطبوع. */
val AppFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold)
)

/** تدرّج طباعي موحّد: عناوين واضحة ونصوص مقروءة وأرقام بارزة. */
private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 15.sp
    )
)

@Composable
fun FatouraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
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
