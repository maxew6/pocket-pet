package com.pocketpet.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = MutedRose,
    onPrimary = CreamBase,
    primaryContainer = CreamSurfaceDim,
    onPrimaryContainer = Espresso,
    secondary = Honey,
    onSecondary = Espresso,
    secondaryContainer = CreamSurface,
    onSecondaryContainer = Espresso,
    tertiary = Sage,
    onTertiary = CreamBase,
    background = CreamBase,
    onBackground = Espresso,
    surface = CreamSurface,
    onSurface = Espresso,
    surfaceVariant = CreamSurfaceDim,
    onSurfaceVariant = EspressoMuted,
    error = SoftCoral,
    onError = CreamBase,
    outline = EspressoMuted,
)

private val DarkColors = darkColorScheme(
    primary = MutedRoseLight,
    onPrimary = DarkBase,
    primaryContainer = DarkSurfaceDim,
    onPrimaryContainer = CreamOnDark,
    secondary = HoneyLight,
    onSecondary = DarkBase,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = CreamOnDark,
    tertiary = SageLight,
    onTertiary = DarkBase,
    background = DarkBase,
    onBackground = CreamOnDark,
    surface = DarkSurface,
    onSurface = CreamOnDark,
    surfaceVariant = DarkSurfaceDim,
    onSurfaceVariant = CreamOnDarkMuted,
    error = SoftCoral,
    onError = DarkBase,
    outline = CreamOnDarkMuted,
)

/**
 * @param useDynamicColor When true (and on Android 12+), derives colors from the device wallpaper
 * instead of the fixed Pocket Pet palette above — off by default so the brand's warm cream
 * identity is what people see unless they've asked for dynamic color specifically.
 */
@Composable
fun PocketPetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PocketPetTypography,
        shapes = PocketPetShapes,
        content = content,
    )
}
