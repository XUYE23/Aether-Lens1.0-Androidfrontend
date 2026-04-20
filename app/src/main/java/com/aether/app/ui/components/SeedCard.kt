package com.aether.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.ui.theme.Cream100
import com.aether.app.ui.theme.Cream200
import com.aether.app.ui.theme.Cream50
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.FontBody
import com.aether.app.ui.theme.FontCnBody
import com.aether.app.ui.theme.FontDisplay
import com.aether.app.ui.theme.FontMono
import com.aether.app.ui.theme.Ink200
import com.aether.app.ui.theme.Ink300
import com.aether.app.ui.theme.Ink500
import com.aether.app.ui.theme.Ink700
import com.aether.app.ui.theme.Ink900

// Core portrait tag card — ported from screens-v2.jsx SeedCard
@Composable
fun SeedCard(
    category: String,
    text: String,
    modifier: Modifier = Modifier,
    tail: String? = null,
    rotation: Float = 0f,
    onDelete: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .rotate(rotation)
            .widthIn(min = 140.dp, max = 220.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0xFF2D2030).copy(alpha = 0.18f))
            .clip(RoundedCornerShape(14.dp))
            .background(Cream50)
            .border(width = 1.dp, color = Ink900.copy(alpha = 0.06f), shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.uppercase(),
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 9.sp,
                        letterSpacing = 0.14.sp,
                        color = DawnEmber,
                        fontWeight = FontWeight.Normal
                    )
                )
                if (onDelete != null) {
                    Text(
                        text = "×",
                        style = TextStyle(
                            fontFamily = FontBody,
                            fontSize = 11.sp,
                            color = Ink300
                        ),
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.07).sp,
                    color = Ink900
                )
            )
            if (tail != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tail,
                    style = TextStyle(
                        fontFamily = FontCnBody,
                        fontSize = 11.sp,
                        color = Ink500
                    )
                )
            }
        }
    }
}

// Category selector chip for core portrait input
@Composable
fun CategoryBubble(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (active) Ink900 else Cream100
    val fg = if (active) Cream50 else Ink700

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(
                width = if (active) 0.dp else 1.dp,
                color = if (active) Color.Transparent else Ink200,
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "＋ $label",
            style = TextStyle(
                fontFamily = FontCnBody,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = fg
            )
        )
    }
}
