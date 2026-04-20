package com.aether.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.ui.theme.Cream100
import com.aether.app.ui.theme.Cream200
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.FontBody
import com.aether.app.ui.theme.Ink900

enum class PillVariant { Primary, Secondary, Ghost }

@Composable
fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillVariant = PillVariant.Primary,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
) {
    val (bg, fg, borderColor) = when (variant) {
        PillVariant.Primary   -> Triple(Ink900, Color.White, Color.Transparent)
        PillVariant.Secondary -> Triple(Cream100, Ink900, Cream200)
        PillVariant.Ghost     -> Triple(Color.Transparent, DawnEmber, DawnEmber.copy(alpha = 0.35f))
    }

    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg.copy(alpha = if (enabled) 1f else 0.4f))
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontBody,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp,
                color = fg.copy(alpha = if (enabled) 1f else 0.5f)
            )
        )
    }
}
