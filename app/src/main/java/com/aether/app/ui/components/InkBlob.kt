package com.aether.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aether.app.ui.theme.DawnGlow
import com.aether.app.ui.theme.DawnHaze
import com.aether.app.ui.theme.DawnMauve
import com.aether.app.ui.theme.DawnPeach
import com.aether.app.ui.theme.DawnEmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// Ported from screens-v2.jsx InkBlob
// 16-point Catmull-Rom spline, seed-based deterministic perturbation

private const val POINTS = 16
private const val BASE_R = 62f
private const val CX = 100f
private const val CY = 100f

private fun rand(i: Int, seed: Float): Float {
    val x = sin((i + 1) * seed * 9.13f + seed * 2.7f) * 43758.5453f
    return x - floor(x)
}

fun buildBlobPath(amp: Float, seed: Float, scale: Float): Path {
    val coords = Array(POINTS) { i ->
        val theta = (i.toFloat() / POINTS) * PI.toFloat() * 2f
        val wobble1 = sin(theta * 2f + seed) * 0.5f
        val wobble2 = sin(theta * 3f + seed * 1.7f) * 0.3f
        val noise = (rand(i, seed) - 0.5f) * 2f * amp
        val r = BASE_R * (1f + wobble1 * amp * 0.4f + wobble2 * amp * 0.3f + noise * 0.4f)
        floatArrayOf(CX + cos(theta) * r, CY + sin(theta) * r)
    }
    val n = POINTS

    fun ptX(idx: Int) = coords[((idx % n) + n) % n][0] * scale
    fun ptY(idx: Int) = coords[((idx % n) + n) % n][1] * scale

    return Path().apply {
        moveTo(ptX(0), ptY(0))
        for (i in 0 until n) {
            val x0 = ptX(i - 1); val y0 = ptY(i - 1)
            val x1 = ptX(i);     val y1 = ptY(i)
            val x2 = ptX(i + 1); val y2 = ptY(i + 1)
            val x3 = ptX(i + 2); val y3 = ptY(i + 2)
            val c1x = x1 + (x2 - x0) / 6f
            val c1y = y1 + (y2 - y0) / 6f
            val c2x = x2 - (x3 - x1) / 6f
            val c2y = y2 - (y3 - y1) / 6f
            cubicTo(c1x, c1y, c2x, c2y, x2, y2)
        }
        close()
    }
}

@Composable
fun InkBlob(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    amp: Float = 0.2f,
    seed: Float = 1f,
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 200f
        val path = buildBlobPath(amp, seed, scale)

        // Outer glow — painted via rect fill with radial gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    DawnPeach.copy(alpha = 0.35f),
                    DawnEmber.copy(alpha = 0.12f),
                    DawnEmber.copy(alpha = 0f),
                ),
                center = Offset(this.size.width * 0.5f, this.size.height * 0.5f),
                radius = this.size.width * 0.55f
            )
        )

        // Fill blob
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f    to DawnGlow,
                    0.35f to DawnPeach,
                    0.75f to DawnEmber,
                    1f    to DawnMauve,
                ),
                center = Offset(this.size.width * 0.42f, this.size.height * 0.40f),
                radius = this.size.width * 0.7f
            )
        )

        // Highlight rim
        drawPath(
            path = path,
            color = DawnHaze.copy(alpha = 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f * scale)
        )
    }
}
