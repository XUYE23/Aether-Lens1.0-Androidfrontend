package com.aether.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aether.app.ui.theme.DawnDusk
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.DawnPeach
import com.aether.app.ui.theme.Ink900

// Single-stroke double-loop logo mark — ported from mark.jsx
// viewBox 0 0 100 100, path translated to Compose Path API

private fun buildMarkPath(scale: Float): Path {
    val s = scale
    return Path().apply {
        // M20 20 C 35 8, 45 8, 50 30
        moveTo(20 * s, 20 * s)
        cubicTo(35 * s, 8 * s, 45 * s, 8 * s, 50 * s, 30 * s)
        // C 55 8, 65 8, 80 20
        cubicTo(55 * s, 8 * s, 65 * s, 8 * s, 80 * s, 20 * s)
        // C 92 35, 92 45, 70 50
        cubicTo(92 * s, 35 * s, 92 * s, 45 * s, 70 * s, 50 * s)
        // C 92 55, 92 65, 80 80
        cubicTo(92 * s, 55 * s, 92 * s, 65 * s, 80 * s, 80 * s)
        // C 65 92, 55 92, 50 70
        cubicTo(65 * s, 92 * s, 55 * s, 92 * s, 50 * s, 70 * s)
        // C 45 92, 35 92, 20 80
        cubicTo(45 * s, 92 * s, 35 * s, 92 * s, 20 * s, 80 * s)
        // C 8 65, 8 55, 30 50
        cubicTo(8 * s, 65 * s, 8 * s, 55 * s, 30 * s, 50 * s)
        // C 8 45, 8 35, 20 20 Z
        cubicTo(8 * s, 45 * s, 8 * s, 35 * s, 20 * s, 20 * s)
        close()
    }
}

@Composable
fun AetherMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Ink900,
    strokeWidth: Float = 4.5f,
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 100f
        val path = buildMarkPath(scale)
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun AetherMarkDawn(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Float = 4.5f,
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 100f
        val path = buildMarkPath(scale)
        val brush = Brush.linearGradient(
            colors = listOf(DawnDusk, DawnEmber, DawnPeach),
            start = Offset(0f, 0f),
            end = Offset(this.size.width, this.size.height)
        )
        drawPath(
            path = path,
            brush = brush,
            style = Stroke(
                width = strokeWidth * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
