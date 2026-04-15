package com.aether.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AetherColorScheme = lightColorScheme(
    primary = NeonPurple,
    secondary = DeepSeaBlue,
    background = PureWhite,
    surface = PureWhite,
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onBackground = GlassBorder,
    onSurface = GlassBorder
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AetherColorScheme,
        content = content
    )
}
