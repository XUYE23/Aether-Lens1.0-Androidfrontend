package com.aether.app.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.audio.AudioCaptureManager
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun AuroraWaveHalo(
    audioLevel: Float = 0f,
    onLongPress: () -> Unit = {},
    isDangerMode: Boolean = false
) {
    val context = LocalContext.current
    var isActive by remember { mutableStateOf(false) }
    var currentAudioLevel by remember { mutableFloatStateOf(0f) }
    val audioCaptureManager = remember { AudioCaptureManager() }
    val morphProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioCaptureManager.startCapture { level ->
                currentAudioLevel = level
            }
        }
    }

    DisposableEffect(isActive) {
        if (isActive) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            audioCaptureManager.stopCapture()
            currentAudioLevel = 0f
        }

        onDispose {
            audioCaptureManager.stopCapture()
        }
    }

    LaunchedEffect(isActive) {
        scope.launch {
            if (isActive) {
                morphProgress.animateTo(1f, animationSpec = tween(400, easing = EaseInOut))
            } else {
                morphProgress.animateTo(0f, animationSpec = tween(400, easing = EaseInOut))
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val breathe = infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val wave1Phase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2Phase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3Phase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    val deepBlue by animateColorAsState(
        targetValue = if (isDangerMode) Color(0xFF8B0000) else Color(0xFF0066FF),
        animationSpec = tween(800),
        label = "halo_deep"
    )
    val cyan by animateColorAsState(
        targetValue = if (isDangerMode) Color(0xFFFF0000) else Color(0xFF00DDFF),
        animationSpec = tween(800),
        label = "halo_cyan"
    )
    val lightGreen by animateColorAsState(
        targetValue = if (isDangerMode) Color(0xFFFF4444) else Color(0xFF88FFDD),
        animationSpec = tween(800),
        label = "halo_light"
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

            if (progress < 1f) {
                val circleAlpha = (1f - progress) * breathe.value
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
                val waveAlpha = progress * breathe.value
                val baseAmplitude = 100f + currentAudioLevel * 250f

                drawWaveLayer(
                    width = size.width,
                    height = size.height,
                    phase = wave1Phase.value,
                    amplitude = baseAmplitude * 1.5f,
                    frequency = 2f,
                    yOffset = -150f,
                    color = deepBlue,
                    alpha = 0.95f * waveAlpha
                )

                drawWaveLayer(
                    width = size.width,
                    height = size.height,
                    phase = wave2Phase.value + 60f,
                    amplitude = baseAmplitude * 1.3f,
                    frequency = 2.3f,
                    yOffset = -100f,
                    color = cyan,
                    alpha = 1f * waveAlpha
                )

                drawWaveLayer(
                    width = size.width,
                    height = size.height,
                    phase = wave3Phase.value + 120f,
                    amplitude = baseAmplitude * 1.1f,
                    frequency = 2.7f,
                    yOffset = -50f,
                    color = lightGreen,
                    alpha = 0.9f * waveAlpha
                )
            }
        }

        if (!isActive) {
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
    width: Float,
    height: Float,
    phase: Float,
    amplitude: Float,
    frequency: Float,
    yOffset: Float,
    color: Color,
    alpha: Float
) {
    val path = Path()
    val points = 100

    for (i in 0..points) {
        val x = (width / points) * i
        val normalizedX = i.toFloat() / points
        val wave = sin((normalizedX * frequency * 360 + phase) * Math.PI / 180).toFloat()
        val y = height + yOffset + wave * amplitude

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.lineTo(width, height + 200f)
    path.lineTo(0f, height + 200f)
    path.close()

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.5f),
                Color.Transparent
            )
        )
    )
}
