package com.aether.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.ui.theme.ConfirmBlue
import com.aether.app.ui.theme.RejectRed
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeCard(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    content: @Composable () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val swipeThreshold = screenWidth * 0.3f
    val view = LocalView.current

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val glowAlpha = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
    val glowColor = if (offsetX.value > 0) ConfirmBlue else RejectRed
    val showIcon = abs(offsetX.value) > 50f

    Box(
        modifier = Modifier.graphicsLayer(
            translationX = offsetX.value,
            translationY = offsetY.value,
            rotationZ = rotation.value,
            scaleX = scale.value,
            scaleY = scale.value
        )
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 2.dp,
                    color = glowColor.copy(alpha = glowAlpha),
                    shape = RoundedCornerShape(24.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            scope.launch { scale.animateTo(0.95f) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                                rotation.snapTo(offsetX.value / 20f)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > swipeThreshold -> {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        scale.animateTo(1f)
                                        offsetX.animateTo(2000f, spring(0.7f, 300f))
                                        rotation.animateTo(30f, spring(0.7f, 300f))
                                        onSwipeRight()
                                    }
                                    offsetX.value < -swipeThreshold -> {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        scale.animateTo(1f)
                                        offsetX.animateTo(-2000f, spring(0.7f, 300f))
                                        rotation.animateTo(-30f, spring(0.7f, 300f))
                                        onSwipeLeft()
                                    }
                                    else -> {
                                        scale.animateTo(1f)
                                        offsetX.animateTo(0f, spring(0.6f, 400f))
                                        offsetY.animateTo(0f, spring(0.6f, 400f))
                                        rotation.animateTo(0f, spring(0.6f, 400f))
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }

        if (showIcon) {
            Text(
                text = if (offsetX.value > 0) "✓" else "✕",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = if (offsetX.value > 0) ConfirmBlue.copy(alpha = glowAlpha) else RejectRed.copy(alpha = glowAlpha),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
            )
        }
    }
}
