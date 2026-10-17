package com.gurnoor.locationchanger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gurnoor.locationchanger.R

// High-contrast, text-paired colors. Green = location changed, blue = real location, red = stop.
val BrandBlue = Color(0xFF0B5FA5)
val ActiveGreen = Color(0xFF1B7F3B)
val StopRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    background = Color(0xFFF4F6F8),
    onBackground = Color(0xFF111111),
    surfaceVariant = Color(0xFFE2E8EF),
    onSurfaceVariant = Color(0xFF26323C),
    outline = Color(0xFF4A5A66),
)

// Kept high-contrast on purpose; followed only if the phone is in dark mode.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB6E6),
    onPrimary = Color(0xFF00111F),
    surface = Color(0xFF14191D),
    onSurface = Color(0xFFF2F4F6),
    background = Color(0xFF0C0F12),
    onBackground = Color(0xFFF2F4F6),
    surfaceVariant = Color(0xFF263038),
    onSurfaceVariant = Color(0xFFE6ECF0),
    outline = Color(0xFF9BB0BF),
)

// Baloo Paaji 2: one friendly, rounded, highly legible family covering both Latin and Gurmukhi.
// It's a variable font (wght axis), so each weight is the same file at a different weight setting.
private fun baloo(weight: Int) = Font(
    R.font.baloo_paaji2,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Baloo = FontFamily(baloo(400), baloo(500), baloo(600), baloo(700))

// Big type with generous line height so Gurmukhi matras are never clipped.
private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Baloo, fontSize = 30.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = Baloo, fontSize = 26.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = Baloo, fontSize = 24.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = Baloo, fontSize = 22.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = Baloo, fontSize = 20.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontFamily = Baloo, fontSize = 20.sp, lineHeight = 32.sp),
    labelLarge = TextStyle(fontFamily = Baloo, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun LocationChangerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
