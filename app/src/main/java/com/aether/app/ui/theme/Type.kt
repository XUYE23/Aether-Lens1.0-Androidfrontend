package com.aether.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.aether.app.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val FrauncesFont = GoogleFont("Fraunces")
val InterFont = GoogleFont("Inter")
val NotoSerifSCFont = GoogleFont("Noto Serif SC")
val NotoSansSCFont = GoogleFont("Noto Sans SC")
val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

val FontDisplay = FontFamily(
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(googleFont = FrauncesFont, fontProvider = provider, weight = FontWeight.Light, style = FontStyle.Italic),
    Font(googleFont = NotoSerifSCFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = NotoSerifSCFont, fontProvider = provider, weight = FontWeight.Normal),
)

val FontBody = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = NotoSansSCFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = NotoSansSCFont, fontProvider = provider, weight = FontWeight.Medium),
)

val FontMono = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Medium),
)

val FontCnDisplay = FontFamily(
    Font(googleFont = NotoSerifSCFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = NotoSerifSCFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = NotoSerifSCFont, fontProvider = provider, weight = FontWeight.Medium),
)

val FontCnBody = FontFamily(
    Font(googleFont = NotoSansSCFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = NotoSansSCFont, fontProvider = provider, weight = FontWeight.Medium),
)
