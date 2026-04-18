package com.aether.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Audio-reactive wave halo shown at the bottom of AmbientHubScreen.
 *
 * [audioLevel]  0f..1f from voiceManager.rmsDb — drives wave amplitude.
 * [isListening] true = wave mode; false = breathing circle mode.
 * [onPressStart] called the moment the user's finger touches the halo.
 * [onPressEnd]   called when the finger is released (or gesture cancelled).
 */
@Composable
fun AuroraWaveHalo(
    audioLevel: Float = 0f,
    isListening: Boolean = false,
    onPressStart: () -> Unit = {},
    onPressEnd: () -> Unit = {},
    isDangerMode: Boolean = false
) {
    val morphProgress = remember { Animatable(0f) }

    LaunchedEffect(isListening) {
        val target = if (isListening) 1f else 0f
        morphProgress.animateTo(target, animationSpec = tween(400, easing = EaseInOut))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val wave1Phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "w1"
    )
    val wave2Phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "w2"
    )
    val wave3Phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "w3"
    )

    val deepBlue by animateColorAsState(
        targetValue = if (isDangerMode) Color(0xFF8B0000) else Color(0xFF0066FF),
        animationSpec = tween(800), label = "halo_deep"
    )
    val cyan by animateColorAsState(
        targetValue = if (isDangerMode) Color(0xFFFF0000) else Color(0xFF00DDFF),
        animationSpec = tween(800), label = "halo_cyan"
    )
    val lightGreen by animateColorAsState(
        targetValue = if (isDangerMode) Color(0xFFFF4444) else Color(0xFF88FFDD),
        animationSpec = tween(800), label = "halo_light"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .blur(20.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            try {
                                onPressStart()
                                tryAwaitRelease()
                            } finally {
                                onPressEnd()
                            }
                        }
                    )
                }
        ) {
            val centerX = size.width / 2
            val centerY = size.height
            val progress = morphProgress.value

            if (progress < 1f) {
                val circleAlpha = (1f - progress) * breathe
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cyan.copy(alpha = 0.9f * circleAlpha),
                            deepBlue.copy(alpha = 0.7f * circleAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = size.width * 0.5f
                    ),
                    center = Offset(centerX, centerY),
                    radius = size.width * 0.5f
                )
            }

            if (progress > 0f) {
                val waveAlpha = progress * breathe
                val baseAmplitude = 100f + audioLevel * 250f

                drawWaveLayer(size.width, size.height, wave1Phase, baseAmplitude * 1.5f, 2f, -150f, deepBlue, 0.95f * waveAlpha)
                drawWaveLayer(size.width, size.height, wave2Phase + 60f, baseAmplitude * 1.3f, 2.3f, -100f, cyan, 1f * waveAlpha)
                drawWaveLayer(size.width, size.height, wave3Phase + 120f, baseAmplitude * 1.1f, 2.7f, -50f, lightGreen, 0.9f * waveAlpha)
            }
        }

        if (!isListening) {
            Text(
                text = "按住 说话",
                fontSize = 20.sp,
                color = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveLayer(
    width: Float, height: Float, phase: Float, amplitude: Float,
    frequency: Float, yOffset: Float, color: Color, alpha: Float
) {
    val path = Path()
    val points = 100
    for (i in 0..points) {
        val x = (width / points) * i
        val normalizedX = i.toFloat() / points
        val wave = sin((normalizedX * frequency * 360 + phase) * Math.PI / 180).toFloat()
        val y = height + yOffset + wave * amplitude
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.lineTo(width, height + 200f)
    path.lineTo(0f, height + 200f)
    path.close()
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha), color.copy(alpha * 0.5f), Color.Transparent)
        )
    )
}
