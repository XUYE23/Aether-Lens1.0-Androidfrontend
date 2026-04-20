package com.aether.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AetherColorScheme = lightColorScheme(
    primary          = Ink900,
    onPrimary        = Cream50,
    primaryContainer = Cream100,
    secondary        = DawnEmber,
    onSecondary      = Cream50,
    background       = Cream50,
    onBackground     = Ink900,
    surface          = Cream100,
    onSurface        = Ink900,
    surfaceVariant   = Cream200,
    outline          = Ink200,
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AetherColorScheme,
        content = content
    )
}

// Spacing scale
object Spacing {
    val xs  = 4
    val sm  = 8
    val md  = 16
    val lg  = 24
    val xl  = 32
    val xxl = 48
    val xxxl = 64
}
