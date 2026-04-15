package com.aether.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DualSpaceNavigator(
    leftSpace: @Composable () -> Unit,
    rightSpace: @Composable () -> Unit
) {
    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screenWidthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                    },
                    onDragEnd = {
                        scope.launch {
                            val threshold = screenWidthPx * 0.08f
                            when {
                                offsetX.value < -threshold -> offsetX.animateTo(-screenWidthPx, spring(0.8f, 400f))
                                offsetX.value > threshold -> offsetX.animateTo(0f, spring(0.8f, 400f))
                                else -> {
                                    if (abs(offsetX.value) > screenWidthPx / 3) {
                                        if (offsetX.value < 0) {
                                            offsetX.animateTo(-screenWidthPx, spring(0.8f, 400f))
                                        } else {
                                            offsetX.animateTo(0f, spring(0.8f, 400f))
                                        }
                                    } else {
                                        offsetX.animateTo(0f, spring(0.8f, 400f))
                                    }
                                }
                            }
                        }
                        totalDragX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        totalDragX += dragAmount

                        val isAlreadyNavigating = offsetX.value != 0f
                        val isDraggingLeft = dragAmount < 0
                        val hasSignificantHorizontalDrag = abs(totalDragX) > 80f

                        val shouldHandleGesture = isAlreadyNavigating || (isDraggingLeft && hasSignificantHorizontalDrag)

                        if (shouldHandleGesture) {
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(-screenWidthPx, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = offsetX.value }
        ) {
            leftSpace()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = offsetX.value + screenWidthPx }
        ) {
            rightSpace()
        }
    }
}
