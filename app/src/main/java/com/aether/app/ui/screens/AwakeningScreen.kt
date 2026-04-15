package com.aether.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AwakeningScreen(onComplete: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }

    val ringProgress = animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(1500, easing = EaseInOut),
        label = "ring"
    )

    val textAlpha = animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(1000),
        label = "text"
    )

    LaunchedEffect(Unit) {
        delay(500)
        phase = 1
        delay(2000)
        phase = 2
        delay(3000)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(DeepSeaBlue, NeonPurple)
                ),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                alpha = ringProgress.value
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 300.dp)
        ) {
            Text(
                text = "神经链路已建立",
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = GlassBorder.copy(alpha = textAlpha.value)
            )
            Text(
                text = "绑定完成，专属 AI 入驻",
                fontSize = 16.sp,
                color = NeonPurple.copy(alpha = textAlpha.value)
            )
        }
    }
}
