package com.aether.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Ink & Cream ──────────────────────────────────────────────────────────────
val Ink900   = Color(0xFF1A1614)
val Ink700   = Color(0xFF3D342E)
val Ink500   = Color(0xFF6B5F57)
val Ink300   = Color(0xFFA89B90)
val Ink200   = Color(0xFFC9BEB4)

val Cream50  = Color(0xFFFAF6F0)
val Cream100 = Color(0xFFF3ECE2)
val Cream200 = Color(0xFFE8DDCE)
val Cream300 = Color(0xFFD9CBB6)

// ── Dawn Gradient ─────────────────────────────────────────────────────────────
val DawnDusk  = Color(0xFF2E2438)
val DawnMauve = Color(0xFF7C5A6B)
val DawnEmber = Color(0xFFB85C3C)
val DawnPeach = Color(0xFFE8A57A)
val DawnHaze  = Color(0xFFF2D6B3)
val DawnGlow  = Color(0xFFFAEBD0)

// ── Legacy aliases (pre-redesign components that haven't been updated yet) ──
val GlassSurface = Cream100.copy(alpha = 0.92f)
val GlassBorder  = Ink700
val LightBorder  = Ink200.copy(alpha = 0.8f)
val PureWhite    = Cream50
val NeonPurple   = DawnMauve
val DeepSeaBlue  = DawnDusk
val ConfirmBlue  = Color(0xFF4A90D9)
val RejectRed    = Color(0xFFD94A4A)
