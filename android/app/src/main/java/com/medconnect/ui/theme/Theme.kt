package com.medconnect.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MedPrimary = Color(0xFF1565C0)
private val MedPrimaryContainer = Color(0xFFD1E4FF)
private val MedOnPrimaryContainer = Color(0xFF001D36)
private val MedSecondary = Color(0xFF0277BD)
private val MedBackground = Color(0xFFF8FAFC)
private val MedSurface = Color(0xFFFFFFFF)
private val MedSurfaceVariant = Color(0xFFEEF2F6)
private val MedOutline = Color(0xFFBFC8D4)

val MedConnectLightColors = lightColorScheme(
    primary = MedPrimary,
    onPrimary = Color.White,
    primaryContainer = MedPrimaryContainer,
    onPrimaryContainer = MedOnPrimaryContainer,
    secondary = MedSecondary,
    onSecondary = Color.White,
    background = MedBackground,
    onBackground = Color(0xFF1A1C1E),
    surface = MedSurface,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = MedSurfaceVariant,
    onSurfaceVariant = Color(0xFF5A6472),
    outline = MedOutline,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9),
    onTertiaryContainer = Color(0xFF1B5E20),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val MedConnectShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val MedConnectTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp),
)

@Composable
fun MedConnectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MedConnectLightColors,
        typography = MedConnectTypography,
        shapes = MedConnectShapes,
        content = content,
    )
}
