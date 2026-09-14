package com.nintech.budgment.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PineGreen,
    onPrimary = MintCream,
    secondary = JungleGreen,
    onSecondary = Night,
    tertiary = OrangePeel,
    onTertiary = Night,
    background = DarkGreen,
    onBackground = MintCream,
    surface = MintCream,
    onSurface = Night,
    surfaceVariant = MintCream,
    onSurfaceVariant = MutedLight,
    error = FaluRed,
    onError = MintCream,
    outline = MutedLight
)

private val DarkColors = darkColorScheme(
    primary = Aquamarine,
    onPrimary = Night,
    secondary = JungleGreen,
    onSecondary = Night,
    tertiary = HighlightDark,
    onTertiary = Night,
    background = CastletonGreen,
    onBackground = MintCream,
    surface = DarkGreen,
    onSurface = MintCream,
    surfaceVariant = CastletonGreen,
    onSurfaceVariant = MintCream,
    error = FaluRed,
    onError = MintCream,
    outline = Aquamarine
)


data class BudgmentExtraColors(
    val success: Color,
    val onSuccess: Color
)

private val LightExtraColors = BudgmentExtraColors(
    success = Success,
    onSuccess = MintCream
)

private val DarkExtraColors = BudgmentExtraColors(
    success = Success,
    onSuccess = Night
)

private val LocalBudgmentExtraColors = staticCompositionLocalOf {
    BudgmentExtraColors(success = Success, onSuccess = Color.White)
}

// Acceso: BudgmentTheme.extraColors.success
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

    CompositionLocalProvider(LocalBudgmentExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}