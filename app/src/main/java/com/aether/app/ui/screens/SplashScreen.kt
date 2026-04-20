package com.aether.app.ui.screens

import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aether.app.ui.components.AetherMarkDawn
import com.aether.app.ui.theme.Cream50
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.DawnGlow
import com.aether.app.ui.theme.DawnHaze
import com.aether.app.ui.theme.DawnPeach
import com.aether.app.ui.theme.Ink200
import com.aether.app.ui.theme.Ink900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 3-phase launch animation:
// Phase 0 (0–1200ms): ink trace drawing in
// Phase 1 (1200–2600ms): full mark with breathing dawn halo
// Phase 2 (2600–3400ms): dissolve + translate up, callback fires at end

private enum class SplashPhase { Drawing, Breathing, Dissolving }

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(SplashPhase.Drawing) }

    val drawProgress = remember { Animatable(0f) }
    val haloAlpha    = remember { Animatable(0f) }
    val dissolveAlpha = remember { Animatable(1f) }
    val markTranslateY = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 0: draw the stroke over 1200ms
        drawProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        phase = SplashPhase.Breathing

        // Phase 1: breathing halo fades in over 400ms, holds for ~1s
        haloAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        delay(900)
        phase = SplashPhase.Dissolving

        // Phase 2: dissolve — mark moves up and fades simultaneously
        kotlinx.coroutines.coroutineScope {
            launch { dissolveAlpha.animateTo(0f, tween(700, easing = FastOutSlowInEasing)) }
            launch { markTranslateY.animateTo(-80f, tween(700, easing = FastOutSlowInEasing)) }
        }
        onComplete()
    }

    // Breathing pulse for phase 1
    val breathTransition = rememberInfiniteTransition(label = "halo_breath")
    val breathScale by breathTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream50)
            .graphicsLayer { alpha = dissolveAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        // Dawn halo (phases 1+2)
        if (phase != SplashPhase.Drawing) {
            Box(
                modifier = Modifier
                    .size(380.dp)
                    .offset(y = markTranslateY.value.dp)
                    .graphicsLayer {
                        alpha = haloAlpha.value * dissolveAlpha.value
                        scaleX = breathScale
                        scaleY = breathScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Outer halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DawnPeach.copy(alpha = 0.18f),
                                DawnPeach.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width / 2f
                        ),
                        radius = size.width / 2f
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(y = markTranslateY.value.dp)
                    .graphicsLayer {
                        alpha = haloAlpha.value * dissolveAlpha.value
                        scaleX = if (breathScale > 1f) breathScale * 0.96f else breathScale
                        scaleY = if (breathScale > 1f) breathScale * 0.96f else breathScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DawnEmber.copy(alpha = 0.22f),
                                DawnEmber.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width / 2f
                        ),
                        radius = size.width / 2f
                    )
                }
            }
        }

        // Logo mark
        Box(
            modifier = Modifier
                .offset(y = markTranslateY.value.dp)
                .graphicsLayer { alpha = dissolveAlpha.value }
        ) {
            when (phase) {
                SplashPhase.Drawing -> {
                    AnimatedMarkTrace(progress = drawProgress.value)
                }
                SplashPhase.Breathing, SplashPhase.Dissolving -> {
                    AetherMarkDawn(size = 170.dp, strokeWidth = 4.8f)
                }
            }
        }
    }
}

// Ink trace — draws the mark path with PathMeasure trimming
@Composable
private fun AnimatedMarkTrace(progress: Float) {
    Canvas(modifier = Modifier.size(170.dp)) {
        val scale = size.width / 100f

        // Ghost outline (fully drawn, dim)
        val ghostPath = buildFullMarkPath(scale)
        drawPath(
            path = ghostPath,
            color = Ink200.copy(alpha = 0.4f),
            style = Stroke(width = 4.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Animated partial path using PathMeasure
        val measure = PathMeasure(ghostPath.asAndroidPath(), false)
        val partialPath = AndroidPath()
        measure.getSegment(0f, measure.length * progress, partialPath, true)
        drawPath(
            path = partialPath.asComposePath(),
            color = Ink900,
            style = Stroke(width = 4.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Pen tip dot at current draw position
        if (progress > 0.01f && progress < 1f) {
            val pos = FloatArray(2)
            val tan = FloatArray(2)
            measure.getPosTan(measure.length * progress, pos, tan)
            drawCircle(color = DawnEmber, radius = 3.5f * scale, center = Offset(pos[0], pos[1]))
        }
    }
}

private fun buildFullMarkPath(scale: Float): Path = Path().apply {
    moveTo(20 * scale, 20 * scale)
    cubicTo(35 * scale, 8 * scale, 45 * scale, 8 * scale, 50 * scale, 30 * scale)
    cubicTo(55 * scale, 8 * scale, 65 * scale, 8 * scale, 80 * scale, 20 * scale)
    cubicTo(92 * scale, 35 * scale, 92 * scale, 45 * scale, 70 * scale, 50 * scale)
    cubicTo(92 * scale, 55 * scale, 92 * scale, 65 * scale, 80 * scale, 80 * scale)
    cubicTo(65 * scale, 92 * scale, 55 * scale, 92 * scale, 50 * scale, 70 * scale)
    cubicTo(45 * scale, 92 * scale, 35 * scale, 92 * scale, 20 * scale, 80 * scale)
    cubicTo(8 * scale, 65 * scale, 8 * scale, 55 * scale, 30 * scale, 50 * scale)
    cubicTo(8 * scale, 45 * scale, 8 * scale, 35 * scale, 20 * scale, 20 * scale)
    close()
}
