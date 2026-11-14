package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ChessSightDarkColorScheme = darkColorScheme(
    primary = TournamentEmerald,
    onPrimary = ObsidianBackground,
    primaryContainer = EmeraldDim.copy(alpha = 0.25f),
    onPrimaryContainer = TournamentEmerald,
    secondary = TacticalBlue,
    onSecondary = ObsidianBackground,
    secondaryContainer = TacticalBlue.copy(alpha = 0.16f),
    onSecondaryContainer = TacticalBlue,
    tertiary = AmberNotice,
    onTertiary = ObsidianBackground,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder.copy(alpha = 0.55f),
    error = CoralError,
    onError = ObsidianBackground,
    errorContainer = CoralError.copy(alpha = 0.16f),
    onErrorContainer = CoralError
)

private val ChessSightShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun ChessSightTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChessSightDarkColorScheme,
        typography = ChessSightTypography,
        shapes = ChessSightShapes,
        content = content
    )
}
