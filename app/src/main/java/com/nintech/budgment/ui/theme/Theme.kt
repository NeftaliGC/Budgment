package com.nintech.budgment.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Se definen todos los roles para que ningún componente caiga en la paleta base (lavanda) de Material 3.
private val LightColors = lightColorScheme(
    primary = PineGreen,
    onPrimary = MintCream,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = DarkGreen,
    inversePrimary = Aquamarine,
    secondary = JungleGreen,
    onSecondary = Night,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = DarkGreen,
    tertiary = BurntOrange,
    onTertiary = MintCream,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = MintCream,
    onBackground = Night,
    surface = MintCream,
    onSurface = Night,
    surfaceVariant = CardBgLight,
    onSurfaceVariant = MutedTextLight,
    surfaceTint = PineGreen,
    inverseSurface = DarkGreen,
    inverseOnSurface = MintCream,
    error = FaluRed,
    onError = MintCream,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = MutedLight,
    outlineVariant = OutlineVariantLight,
    surfaceBright = MintCream,
    surfaceDim = SurfaceContainerHighestLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val DarkColors = darkColorScheme(
    primary = Aquamarine,
    onPrimary = Night,
    primaryContainer = CastletonGreen,
    onPrimaryContainer = MintCream,
    inversePrimary = PineGreen,
    secondary = JungleGreen,
    onSecondary = Night,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = MintCream,
    tertiary = HighlightDark,
    onTertiary = Night,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = TertiaryContainerLight,
    background = Night,
    onBackground = MintCream,
    surface = DarkGreen,
    onSurface = MintCream,
    surfaceVariant = CardBgDark,
    onSurfaceVariant = MutedTextDark,
    surfaceTint = Aquamarine,
    inverseSurface = MintCream,
    inverseOnSurface = Night,
    error = ErrorDark,
    onError = Night,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight,
    outline = Aquamarine,
    outlineVariant = OutlineVariantDark,
    surfaceBright = SurfaceContainerHighestDark,
    surfaceDim = Night,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

data class BudgmentExtraColors(
    val success: Color,
    val onSuccess: Color
)

private val LightExtraColors = BudgmentExtraColors(success = SuccessDark, onSuccess = MintCream)
private val DarkExtraColors = BudgmentExtraColors(success = Success, onSuccess = Night)

private val LocalBudgmentExtraColors = staticCompositionLocalOf {
    BudgmentExtraColors(success = Success, onSuccess = Color.White)
}

/**
 * Si la app se está mostrando en oscuro. Puede no coincidir con el teléfono: el usuario puede
 * fijar el tema en Configuración.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

object BudgmentTheme {
    val extraColors: BudgmentExtraColors
        @Composable
        get() = LocalBudgmentExtraColors.current
}

@Composable
fun BudgmentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalBudgmentExtraColors provides extraColors, LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}