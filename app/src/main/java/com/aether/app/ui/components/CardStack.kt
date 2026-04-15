package com.aether.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aether.app.data.ActionCard
import com.aether.app.ui.theme.GlassBorder

@Composable
fun CardStack(
    remainingCount: Int,
    nextCard: ActionCard? = null,
    animationProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val visibleCards = minOf(remainingCount, 3)

        for (i in 0 until visibleCards) {
            val baseOffsetX = -(i * 6).dp
            val baseOffsetY = -(i * 8).dp

            val animatedOffsetX = baseOffsetX + (6 * animationProgress).dp
            val animatedOffsetY = baseOffsetY + (8 * animationProgress).dp

            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(130.dp)
                    .offset(
                        x = animatedOffsetX,
                        y = animatedOffsetY
                    )
                    .zIndex((visibleCards - i).toFloat())
                    .scale(1f - i * 0.05f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                if (i == 0 && nextCard != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = nextCard.title.substringBefore("消息").substringBefore("审批").substringBefore("提醒").substringBefore("审查"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = nextCard.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "刚刚",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .offset(x = 10.dp, y = (-10).dp)
                .zIndex(100f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0066FF))
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            if (remainingCount > 0) {
                Text(
                    text = remainingCount.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
