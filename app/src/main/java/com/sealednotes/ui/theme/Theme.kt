package com.sealednotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBBC7FF),
    onPrimary = Color(0xFF1A2678),
    primaryContainer = Color(0xFF323F90),
    onPrimaryContainer = Color(0xFFDDE1FF),

    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434659),
    onSecondaryContainer = Color(0xFFDFE1F9),

    surface = Color(0xFF121316),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF1E1F23),
    onSurfaceVariant = Color(0xFFC6C5CA),

    surfaceContainerLowest = Color(0xFF0E0E11),
    surfaceContainerLow = Color(0xFF1A1B1E),
    surfaceContainer = Color(0xFF1E1F23),
    surfaceContainerHigh = Color(0xFF292A2D),
    surfaceContainerHighest = Color(0xFF343538),

    outline = Color(0xFF8F9099),
    outlineVariant = Color(0xFF44464F),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),

    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E2E6),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun SealedNotesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
