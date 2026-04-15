package com.aether.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun BreathingHalo(onLongPress: () -> Unit = {}) {
    var isActive by remember { mutableStateOf(false) }
    val morphProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isActive) {
        scope.launch {
            if (isActive) {
                morphProgress.animateTo(1f, animationSpec = tween(300, easing = EaseInOut))
            } else {
                morphProgress.animateTo(0f, animationSpec = tween(300, easing = EaseInOut))
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "halo")

    val breathe = infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val wavePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            isActive = true
                            onLongPress()
                        },
                        onPress = {
                            isActive = true
                            tryAwaitRelease()
                            isActive = false
                        }
                    )
                }
        ) {
            val centerX = size.width / 2
            val centerY = size.height
            val progress = morphProgress.value

            val brightBlue = Color(0xFF00D9FF)
            val lightPurple = Color(0xFFB794F6)

            if (progress < 1f) {
                val circleAlpha = (1f - progress) * breathe.value
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lightPurple.copy(alpha = 0.6f * circleAlpha),
                            brightBlue.copy(alpha = 0.4f * circleAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = size.width * 0.6f
                    ),
                    center = Offset(centerX, centerY),
                    radius = size.width * 0.6f
                )
            }

            if (progress > 0f) {
                val waveAlpha = progress
                val barHeight = 200f
                val waveAmplitude = 35f

                for (i in 0..60) {
                    val x = (size.width / 60) * i
                    val wave = sin((i * 6 + wavePhase.value) * Math.PI / 180) * waveAmplitude
                    val y = centerY - barHeight / 2 + wave.toFloat()

                    val distanceFromCenter = kotlin.math.abs(x - centerX) / centerX
                    val alpha = (0.7f - distanceFromCenter * 0.5f).coerceIn(0.2f, 0.7f) * waveAlpha

                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lightPurple.copy(alpha = alpha),
                                brightBlue.copy(alpha = alpha * 0.8f),
                                Color.Transparent
                            )
                        ),
                        radius = 30f,
                        center = Offset(x, y)
                    )
                }
            }
        }

        if (!isActive) {
            Text(
                text = "按住 说话",
                fontSize = 16.sp,
                color = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
