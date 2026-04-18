package com.aether.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.data.DraftVoiceCard
import com.aether.app.ui.theme.GlassBorder
import com.aether.app.ui.theme.NeonPurple
import kotlinx.coroutines.delay

/**
 * The "Ghost" voice card shown in the centre of AmbientHubScreen during voice input.
 *
 * Display states driven by [DraftVoiceCard] null-ness:
 *   finalText == null && errorMessage == null  → Listening: partialText + animated border + cursor
 *   finalText != null                          → Editable: BasicTextField, border freezes
 *   errorMessage != null                       → Error: red text above body
 *
 * [isDangerMode] switches the colour palette to red without changing behaviour.
 */
@Composable
fun GhostVoiceCard(
    draft: DraftVoiceCard,
    isDangerMode: Boolean = false,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // ── Colour palette ─────────────────────────────────────────────────────────
    val borderColors = if (isDangerMode) listOf(
        Color(0xFF8B0000), Color(0xFFFF0000), Color(0xFFFF4444), Color(0xFFCC0022)
    ) else listOf(
        Color(0xFF0066FF), Color(0xFF00DDFF), Color(0xFF88FFDD), Color(0xFF9B6BFF)
    )
    val glowColor    = if (isDangerMode) Color(0xFFFF0000) else Color(0xFF00DDFF)
    val innerBg      = if (isDangerMode) Color(0xFFFF0000).copy(alpha = 0.04f)
                       else Color.White.copy(alpha = 0.02f)
    val textColor    = if (isDangerMode) Color(0xFFFFBBBB) else GlassBorder
    val cursorColor  = if (isDangerMode) Color(0xFFFF4444) else NeonPurple
    val cycleDuration  = if (isDangerMode) 2500 else 3000
    val breathDuration = if (isDangerMode) 1400 else 2000

    // ── Animations ─────────────────────────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "ghost")

    val borderShift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(cycleDuration, easing = LinearEasing)),
        label = "border_shift"
    )

    val glowAlpha by infinite.animateFloat(
        initialValue = 0.5f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(breathDuration, easing = EaseInOut), RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowScale by infinite.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            tween(breathDuration, easing = EaseInOut), RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    // One-shot white flash when finalText first arrives
    var showFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (showFlash) 1f else 0f,
        animationSpec = if (showFlash) tween(200) else tween(300),
        finishedListener = { if (showFlash) showFlash = false },
        label = "flash"
    )
    LaunchedEffect(draft.finalText) {
        if (draft.finalText != null) showFlash = true
    }

    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) { delay(500); cursorVisible = !cursorVisible }
    }

    // ── Layout ─────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth(0.75f)
            .height(380.dp)
            // Outer glow drawn directly on the root Box — no negative padding needed
            .graphicsLayer { alpha = glowAlpha; scaleX = glowScale; scaleY = glowScale }
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = maxOf(size.width, size.height) * 0.85f
                    ),
                    cornerRadius = CornerRadius(28.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Card surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(innerBg)
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val rx = 24.dp.toPx()
                    val shift = borderShift * size.width * 2f
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = borderColors + borderColors,
                            start = Offset(shift - size.width, 0f),
                            end = Offset(shift, size.height)
                        ),
                        cornerRadius = CornerRadius(rx),
                        style = Stroke(width = stroke)
                    )
                }
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top label
                Text(
                    text = when {
                        isDangerMode            -> "⚠ 危险模式 · 语音指令"
                        draft.finalText != null -> "飞书"
                        else                    -> "飞书 · 语音输入中"
                    },
                    fontSize = 14.sp,
                    color = if (isDangerMode) Color(0xFFFF6464) else GlassBorder,
                    fontWeight = FontWeight.Light
                )

                // Error message
                if (draft.errorMessage != null) {
                    Text(
                        text = draft.errorMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF6B6B),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Main text area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp)
                ) {
                    when {
                        draft.finalText != null -> {
                            var editText by remember(draft.finalText) {
                                mutableStateOf(draft.finalText)
                            }
                            BasicTextField(
                                value = editText,
                                onValueChange = { editText = it; onTextChange(it) },
                                modifier = Modifier.fillMaxSize(),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = textColor,
                                    lineHeight = 24.sp
                                ),
                                cursorBrush = SolidColor(cursorColor)
                            )
                        }
                        else -> {
                            if (draft.partialText.isEmpty()) {
                                Text(
                                    text = "正在聆听...",
                                    fontSize = 16.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor.copy(alpha = 0.45f)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = draft.partialText,
                                        fontSize = 16.sp,
                                        color = textColor,
                                        lineHeight = 24.sp
                                    )
                                    if (cursorVisible) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 2.dp)
                                                .width(1.5.dp)
                                                .height(16.dp)
                                                .background(cursorColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom label
                Text(
                    text = if (isDangerMode) "确认后自动执行" else "发送给：团队频道",
                    fontSize = 14.sp,
                    color = (if (isDangerMode) Color(0xFFFF6464) else NeonPurple).copy(alpha = 0.7f)
                )
            }
        }

        // White flash overlay
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0A0D14)
@Composable
private fun GhostVoiceCard_Listening_Normal_Preview() {
    GhostVoiceCard(
        draft = DraftVoiceCard(partialText = "提醒张三下午三点"),
        isDangerMode = false,
        onTextChange = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0D14)
@Composable
private fun GhostVoiceCard_Editable_Normal_Preview() {
    GhostVoiceCard(
        draft = DraftVoiceCard(finalText = "提醒张三下午三点开会"),
        isDangerMode = false,
        onTextChange = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0D14)
@Composable
private fun GhostVoiceCard_Listening_Danger_Preview() {
    GhostVoiceCard(
        draft = DraftVoiceCard(partialText = "提醒张三下午三点"),
        isDangerMode = true,
        onTextChange = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0D14)
@Composable
private fun GhostVoiceCard_Error_Preview() {
    GhostVoiceCard(
        draft = DraftVoiceCard(partialText = "提醒", errorMessage = "识别失败"),
        isDangerMode = false,
        onTextChange = {}
    )
}
