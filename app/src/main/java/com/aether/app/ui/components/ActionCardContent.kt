package com.aether.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.data.ActionCard
import com.aether.app.ui.theme.*

@Composable
fun ActionCardContent(card: ActionCard) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(DeepSeaBlue, NeonPurple)
    )
    var editableContent by remember { mutableStateOf(card.content) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(380.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(GlassSurface)
            .border(1.dp, LightBorder, RoundedCornerShape(24.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "飞书",
                fontSize = 14.sp,
                color = GlassBorder,
                fontWeight = FontWeight.Light
            )

            BasicTextField(
                value = editableContent,
                onValueChange = { editableContent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = GlassBorder,
                    lineHeight = 24.sp
                )
            )

            Text(
                text = "发送给：团队频道",
                fontSize = 14.sp,
                color = NeonPurple.copy(alpha = 0.7f)
            )
        }
    }
}
