package com.example.appinventario.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BiosurPrimary,
    onPrimary = BiosurOnPrimary,
    primaryContainer = BiosurPrimaryContainer,
    onPrimaryContainer = BiosurOnPrimaryContainer,
    secondary = BiosurSecondary,
    onSecondary = BiosurOnSecondary,
    secondaryContainer = BiosurSecondaryContainer,
    onSecondaryContainer = BiosurOnSecondaryContainer,
    tertiary = BiosurTertiary,
    onTertiary = BiosurOnTertiary,
    tertiaryContainer = BiosurTertiaryContainer,
    onTertiaryContainer = BiosurOnTertiaryContainer,
    error = BiosurError,
    onError = BiosurOnError,
    errorContainer = BiosurErrorContainer,
    onErrorContainer = BiosurOnErrorContainer,
    background = BiosurBackground,
    onBackground = BiosurOnBackground,
    surface = BiosurSurface,
    onSurface = BiosurOnSurface,
    surfaceVariant = BiosurSurfaceVariant,
    onSurfaceVariant = BiosurOnSurfaceVariant,
    outline = BiosurOutline,
    surfaceTint = Color.Transparent
)

private val DarkColorScheme = darkColorScheme(
    primary = BiosurPrimaryDark,
    onPrimary = BiosurOnPrimaryDark,
    primaryContainer = BiosurPrimaryContainerDark,
    onPrimaryContainer = BiosurOnPrimaryContainerDark,
    secondary = BiosurSecondaryDark,
    onSecondary = BiosurOnSecondaryDark,
    secondaryContainer = BiosurSecondaryContainerDark,
    onSecondaryContainer = BiosurOnSecondaryContainerDark,
    tertiary = BiosurTertiaryDark,
    onTertiary = BiosurOnTertiaryDark,
    tertiaryContainer = BiosurTertiaryContainerDark,
    onTertiaryContainer = BiosurOnTertiaryContainerDark,
    error = BiosurErrorDark,
    onError = BiosurOnErrorDark,
    errorContainer = BiosurErrorContainerDark,
    onErrorContainer = BiosurOnErrorContainerDark,
    background = BiosurBackgroundDark,
    onBackground = BiosurOnBackgroundDark,
    surface = BiosurSurfaceDark,
    onSurface = BiosurOnSurfaceDark,
    surfaceVariant = BiosurSurfaceVariantDark,
    onSurfaceVariant = BiosurOnSurfaceVariantDark,
    outline = BiosurOutlineDark,
    surfaceTint = Color.Transparent
)

@Composable
fun AppInventarioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false para preservar la identidad corporativa de Biosur SpA
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
