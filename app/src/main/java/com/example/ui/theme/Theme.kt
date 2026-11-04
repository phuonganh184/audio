package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Hệ màu Dark Mode chủ đạo: Đen - Tím - Trắng
private val DarkColorScheme = darkColorScheme(
    primary = AmethystVibrant,
    onPrimary = Color.White,
    primaryContainer = AmethystContainerDark,
    onPrimaryContainer = AmethystLight,
    secondary = AmethystLight,
    onSecondary = ObsidianBlack,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = MoonlightWhite,
    tertiary = LanternAmber,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = MoonlightWhite,
    surface = ObsidianSurface,
    onSurface = MoonlightWhite,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = SilverMist,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianElevated,
    surfaceContainerHighest = ObsidianOutline,
    outline = ObsidianOutline,
    error = RoseError
)

// Hệ màu Light Mode thanh lịch: Trắng Ngọc Trai - Tím Hoàng Gia
private val LightColorScheme = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = Color.White,
    primaryContainer = RoyalPurpleContainer,
    onPrimaryContainer = AmethystDeep,
    secondary = AmethystVibrant,
    onSecondary = Color.White,
    secondaryContainer = PearlElevated,
    onSecondaryContainer = InkBlack,
    tertiary = AmberDeep,
    onTertiary = Color.White,
    background = PearlBackground,
    onBackground = InkBlack,
    surface = PearlSurface,
    onSurface = InkBlack,
    surfaceVariant = PearlCard,
    onSurfaceVariant = SlatePurple,
    surfaceContainerLowest = PearlSurface,
    surfaceContainerLow = PearlBackground,
    surfaceContainer = PearlCard,
    surfaceContainerHigh = PearlElevated,
    surfaceContainerHighest = PearlOutline,
    outline = PearlOutline,
    error = Color(0xFFE11D48)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BachNhatAnhAudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
