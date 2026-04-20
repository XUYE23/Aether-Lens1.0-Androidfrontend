package com.aether.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aether.app.ui.theme.DawnDusk
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.DawnPeach

// Continuous single-line glasses — ported from screens-v2.jsx GlassesMark
// Source viewBox: 0 0 200 110; aspect ratio preserved (width × 0.55 height)

private fun buildGlassesPath(sx: Float, sy: Float): Path = Path().apply {
    // M 6 40
    moveTo(6 * sx, 40 * sy)
    // C 16 32, 26 30, 36 36
    cubicTo(16 * sx, 32 * sy, 26 * sx, 30 * sy, 36 * sx, 36 * sy)
    // C 46 42, 52 50, 56 62
    cubicTo(46 * sx, 42 * sy, 52 * sx, 50 * sy, 56 * sx, 62 * sy)
    // C 60 78, 52 90, 40 90
    cubicTo(60 * sx, 78 * sy, 52 * sx, 90 * sy, 40 * sx, 90 * sy)
    // C 26 90, 18 80, 20 66
    cubicTo(26 * sx, 90 * sy, 18 * sx, 80 * sy, 20 * sx, 66 * sy)
    // C 22 52, 36 44, 52 48
    cubicTo(22 * sx, 52 * sy, 36 * sx, 44 * sy, 52 * sx, 48 * sy)
    // C 70 52, 82 58, 100 58
    cubicTo(70 * sx, 52 * sy, 82 * sx, 58 * sy, 100 * sx, 58 * sy)
    // C 118 58, 130 52, 148 48
    cubicTo(118 * sx, 58 * sy, 130 * sx, 52 * sy, 148 * sx, 48 * sy)
    // C 164 44, 178 52, 180 66
    cubicTo(164 * sx, 44 * sy, 178 * sx, 52 * sy, 180 * sx, 66 * sy)
    // C 182 80, 174 90, 160 90
    cubicTo(182 * sx, 80 * sy, 174 * sx, 90 * sy, 160 * sx, 90 * sy)
    // C 148 90, 140 78, 144 62
    cubicTo(148 * sx, 90 * sy, 140 * sx, 78 * sy, 144 * sx, 62 * sy)
    // C 148 50, 154 42, 164 36
    cubicTo(148 * sx, 50 * sy, 154 * sx, 42 * sy, 164 * sx, 36 * sy)
    // C 174 30, 184 32, 194 40
    cubicTo(174 * sx, 30 * sy, 184 * sx, 32 * sy, 194 * sx, 40 * sy)
}

@Composable
fun GlassesMark(
    modifier: Modifier = Modifier,
    color: Color? = null,  // null = use dawn gradient
    strokeWidth: Float = 3.5f,
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 200f
        val sy = size.height / 110f
        val path = buildGlassesPath(sx, sy)
        val stroke = Stroke(
            width = strokeWidth * minOf(sx, sy),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        if (color != null) {
            drawPath(path = path, color = color, style = stroke)
        } else {
            val brush = Brush.linearGradient(
                colors = listOf(DawnDusk, DawnEmber, DawnPeach),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            drawPath(path = path, brush = brush, style = stroke)
        }
    }
}
