package com.imrat.focuslock.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Status colours shared by every screen. */
val LockedRed = Color(0xFFFF6B6B)
val UnlockedGreen = Color(0xFF34D399)
val WarnAmber = Color(0xFFFBBF24)

private val Dark: ColorScheme = darkColorScheme(
    primary = Color(0xFF8B7CFF), onPrimary = Color(0xFF0B0830),
    primaryContainer = Color(0xFF2B2468), onPrimaryContainer = Color(0xFFE3DFFF),
    secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF00201B),
    background = Color(0xFF0B1020), onBackground = Color(0xFFEAEDF7),
    surface = Color(0xFF0B1020), onSurface = Color(0xFFEAEDF7),
    surfaceVariant = Color(0xFF171F38), onSurfaceVariant = Color(0xFF9BA6C7),
    surfaceContainer = Color(0xFF131A2E), surfaceContainerHigh = Color(0xFF1B2440),
    outline = Color(0xFF3A4670), outlineVariant = Color(0xFF232C4A),
    error = LockedRed, onError = Color(0xFF3B0000),
)

private val Light: ColorScheme = lightColorScheme(
    primary = Color(0xFF5B4BFF), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E2FF), onPrimaryContainer = Color(0xFF1B1465),
    secondary = Color(0xFF0F9D8A), onSecondary = Color.White,
    background = Color(0xFFF5F6FB), onBackground = Color(0xFF111628),
    surface = Color(0xFFF5F6FB), onSurface = Color(0xFF111628),
    surfaceVariant = Color(0xFFE9ECF8), onSurfaceVariant = Color(0xFF5A6486),
    surfaceContainer = Color.White, surfaceContainerHigh = Color(0xFFEDEFFA),
    outline = Color(0xFFB5BCD8), outlineVariant = Color(0xFFDCE0F0),
    error = Color(0xFFD93B3B), onError = Color.White,
)

private val Sans = FontFamily.SansSerif
private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 52.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun FocusLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
