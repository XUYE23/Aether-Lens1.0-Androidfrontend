package com.aether.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxScope
import com.aether.app.ui.components.AetherMark
import com.aether.app.ui.components.AetherToggle
import com.aether.app.ui.components.CategoryBubble
import com.aether.app.ui.components.GlassesMark
import com.aether.app.ui.components.SeedCard
import com.aether.app.ui.theme.Cream100
import com.aether.app.ui.theme.Cream200
import com.aether.app.ui.theme.Cream300
import com.aether.app.ui.theme.Cream50
import com.aether.app.ui.theme.DawnDusk
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.DawnGlow
import com.aether.app.ui.theme.DawnHaze
import com.aether.app.ui.theme.DawnPeach
import com.aether.app.ui.theme.FontBody
import com.aether.app.ui.theme.FontCnBody
import com.aether.app.ui.theme.FontCnDisplay
import com.aether.app.ui.theme.FontDisplay
import com.aether.app.ui.theme.FontMono
import com.aether.app.ui.theme.Ink200
import com.aether.app.ui.theme.Ink300
import com.aether.app.ui.theme.Ink500
import com.aether.app.ui.theme.Ink700
import com.aether.app.ui.theme.Ink900

// ── Shared onboarding scaffold ───────────────────────────────────────

@Composable
private fun OnboardScaffold(
    step: Int,
    totalSteps: Int = 6,
    required: Boolean,
    onSkip: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream50)
    ) {
        // Step progress bar
        Row(
            modifier = Modifier
                .padding(top = 70.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(totalSteps) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < step) Ink900 else Cream300)
                )
            }
        }

        // Step label + required/optional
        Row(
            modifier = Modifier
                .padding(top = 86.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Step ${step.toString().padStart(2, '0')} · ${totalSteps.toString().padStart(2, '0')}",
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 11.sp,
                    letterSpacing = 0.12.sp, color = Ink500
                )
            )
            if (required) {
                Text(
                    text = if (step == totalSteps) "Required · last step" else "Required",
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 11.sp,
                        letterSpacing = 0.12.sp, color = DawnEmber
                    )
                )
            } else {
                Text(
                    text = "Optional",
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 11.sp,
                        letterSpacing = 0.12.sp, color = Ink500
                    )
                )
            }
        }

        // Skip link
        if (onSkip != null && !required) {
            Text(
                text = "SKIP",
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 11.sp,
                    letterSpacing = 0.14.sp, color = Ink500,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 24.dp)
                    .clickable { onSkip() }
            )
        }

        content()
    }
}

// ── Primary CTA button (ink pill with arrow) ─────────────────────────

@Composable
private fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dawn: Boolean = false,
    footnote: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (dawn) Brush.linearGradient(
                        colors = listOf(DawnDusk, DawnEmber, DawnPeach),
                        start = Offset(0f, 0f), end = Offset(1000f, 0f)
                    ) else Brush.linearGradient(colors = listOf(Ink900, Ink900))
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = TextStyle(
                        fontFamily = FontBody, fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (dawn) DawnGlow else Cream50
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "→",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = if (dawn) DawnGlow else Cream50
                    )
                )
            }
        }
        if (footnote != null) {
            Text(
                text = footnote.uppercase(),
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 11.sp,
                    letterSpacing = 0.14.sp, color = Ink300
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Step 1 — Name entry (required)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun OnboardStep1Name(onNext: (name: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val blinkAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            blinkAlpha.animateTo(0f, tween(500))
            blinkAlpha.animateTo(1f, tween(500))
        }
    }

    OnboardScaffold(step = 1, required = true) {
        // Header
        Column(
            modifier = Modifier
                .padding(top = 156.dp, start = 28.dp, end = 28.dp)
        ) {
            Text(
                text = "What should\nI call you?",
                style = TextStyle(
                    fontFamily = FontDisplay, fontStyle = FontStyle.Normal,
                    fontSize = 46.sp, fontWeight = FontWeight.Light,
                    lineHeight = 50.sp, letterSpacing = (-1.2).sp, color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "I'll remember this for as long as we're together. You can change it anytime.",
                style = TextStyle(
                    fontFamily = FontBody, fontSize = 15.sp,
                    lineHeight = 23.sp, color = Ink500
                )
            )
        }

        // Name input
        Column(
            modifier = Modifier
                .padding(top = 360.dp, start = 28.dp, end = 28.dp)
        ) {
            Text(
                text = "YOUR NAME",
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 10.sp,
                    letterSpacing = 0.14.sp, color = Ink500
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp, color = Ink900,
                        shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                    .padding(bottom = 14.dp)
            ) {
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    textStyle = TextStyle(
                        fontFamily = FontDisplay, fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 38.sp, letterSpacing = (-0.76).sp, color = Ink900
                    ),
                    cursorBrush = SolidColor(DawnEmber),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        if (name.isNotBlank()) onNext(name.trim())
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${name.length} / 40",
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 12.sp,
                    letterSpacing = 0.06.sp, color = Ink300
                )
            )
        }

        // Aether's greeting preview
        if (name.isNotBlank()) {
            Row(
                modifier = Modifier
                    .padding(top = 540.dp, start = 28.dp, end = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Cream100)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AetherMark(size = 22.dp, color = Ink700, strokeWidth = 6f)
                Text(
                    text = "\"Nice to meet you, ",
                    style = TextStyle(
                        fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                        fontSize = 13.sp, lineHeight = 19.sp, color = Ink700
                    )
                )
                Text(
                    text = name,
                    style = TextStyle(
                        fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                        fontSize = 13.sp, lineHeight = 19.sp, color = DawnEmber
                    )
                )
                Text(
                    text = ".\"",
                    style = TextStyle(
                        fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                        fontSize = 13.sp, lineHeight = 19.sp, color = Ink700
                    )
                )
            }
        }

        // CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            PrimaryCta(
                text = "Continue",
                onClick = { if (name.isNotBlank()) onNext(name.trim()) },
                footnote = "Required to continue"
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Step 2 — Voiceprint (optional)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun OnboardStep2Voiceprint(onNext: () -> Unit, onSkip: () -> Unit) {
    val phrase = "Morning light. A quiet hum behind the thought."
    val words = phrase.split(" ")

    OnboardScaffold(step = 2, required = false, onSkip = onSkip) {
        Column(modifier = Modifier.padding(top = 140.dp, start = 28.dp, end = 28.dp)) {
            Text(
                text = "Read this aloud.",
                style = TextStyle(
                    fontFamily = FontDisplay, fontSize = 40.sp,
                    fontWeight = FontWeight.Light, lineHeight = 44.sp,
                    letterSpacing = (-1.0).sp, color = Ink900
                )
            )
            Text(
                text = "Softly.",
                style = TextStyle(
                    fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                    fontSize = 40.sp, fontWeight = FontWeight.Light,
                    lineHeight = 44.sp, letterSpacing = (-1.0).sp, color = DawnEmber
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "So I recognize your voice among the noise — and never answer to anyone else's.",
                style = TextStyle(
                    fontFamily = FontBody, fontSize = 14.sp,
                    lineHeight = 22.sp, color = Ink500
                )
            )
        }

        // Phrase card
        Box(
            modifier = Modifier
                .padding(top = 326.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Cream100)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Text(
                    text = "PHRASE 02 · 03",
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 10.sp,
                        letterSpacing = 0.14.sp, color = Ink500
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Word-level highlighting
                val spoken = 3
                val active = 4
                Row(horizontalArrangement = Arrangement.Absolute.Left) {
                    words.forEachIndexed { i, word ->
                        val color = when {
                            i < spoken -> Ink300
                            i == active -> DawnEmber
                            else -> Ink900
                        }
                        Text(
                            text = "$word ",
                            style = TextStyle(
                                fontFamily = FontDisplay, fontSize = 28.sp,
                                fontWeight = FontWeight.Light, lineHeight = 36.sp,
                                letterSpacing = (-0.42).sp, color = color,
                                fontStyle = if (i == active) FontStyle.Italic else FontStyle.Normal
                            )
                        )
                    }
                }
            }
        }

        // Waveform (static visual, records by holding button)
        Row(
            modifier = Modifier
                .padding(top = 556.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(48) { i ->
                val center = 24
                val d = kotlin.math.abs(i - center).toFloat()
                val envelope = maxOf(0f, 1f - d / 24f)
                val noise = 0.4f + kotlin.math.abs(kotlin.math.sin(i * 1.3).toFloat()) * 0.6f
                val h = maxOf(3f, envelope * noise * 64f)
                val active = i <= 28
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (active) DawnEmber.copy(alpha = 0.6f + envelope * 0.4f)
                            else Ink200.copy(alpha = 0.5f)
                        )
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
        }

        // Bottom controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0:08",
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 12.sp,
                            letterSpacing = 0.08.sp, color = Ink500
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Ink200))
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(DawnEmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LISTENING",
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 11.sp,
                            letterSpacing = 0.14.sp, color = DawnEmber
                        )
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, Ink200, RoundedCornerShape(28.dp))
                            .clickable(onClick = onSkip),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip for now",
                            style = TextStyle(fontFamily = FontBody, fontSize = 15.sp, color = Ink500)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Ink900)
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(DawnPeach)
                            )
                            Text(
                                text = "Finish reading",
                                style = TextStyle(
                                    fontFamily = FontBody, fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium, color = Cream50
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Step 3 — Core Portrait / seed garden (optional)
// ══════════════════════════════════════════════════════════════════════

data class SeedEntry(
    val category: String,
    val text: String,
    val tail: String? = null,
    val rotation: Float = 0f,
)

private val CATEGORIES = listOf("我是谁", "我的人", "我在意的", "我喜欢的", "我的节奏", "我的目标")

@Composable
fun OnboardStep3CorePortrait(onNext: () -> Unit, onSkip: () -> Unit) {
    var seeds by remember { mutableStateOf(listOf<SeedEntry>()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("") }

    OnboardScaffold(step = 3, required = false, onSkip = onSkip) {
        Column(
            modifier = Modifier
                .padding(top = 136.dp, start = 28.dp, end = 28.dp)
        ) {
            Text(
                text = "用几句话，\n告诉我你是谁。",
                style = TextStyle(
                    fontFamily = FontCnDisplay, fontSize = 28.sp,
                    fontWeight = FontWeight.Normal, lineHeight = 36.sp, color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "这是你的核心画像——Aether 与你同行三年五年，靠的就是这些种子。",
                style = TextStyle(
                    fontFamily = FontCnBody, fontSize = 13.sp,
                    lineHeight = 21.sp, color = Ink500
                )
            )
        }

        // Garden canvas
        Box(
            modifier = Modifier
                .padding(top = 280.dp, start = 16.dp, end = 16.dp, bottom = 260.dp)
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DawnHaze.copy(alpha = 0.55f),
                            DawnGlow.copy(alpha = 0.35f),
                            Cream100
                        ),
                        radius = 600f
                    )
                )
        ) {
            if (seeds.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "A blank garden,",
                        style = TextStyle(
                            fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                            fontSize = 20.sp, fontWeight = FontWeight.Light,
                            lineHeight = 27.sp, color = Ink700
                        )
                    )
                    Text(
                        text = "waiting.",
                        style = TextStyle(
                            fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                            fontSize = 20.sp, fontWeight = FontWeight.Light,
                            lineHeight = 27.sp, color = Ink700
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "点下面任意类别，种一颗种子。\n什么都不填也可以进入下一步。",
                        style = TextStyle(
                            fontFamily = FontCnBody, fontSize = 12.sp,
                            lineHeight = 19.sp, color = Ink500
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Show planted seeds
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    seeds.forEachIndexed { index, seed ->
                        val offX = ((index % 2) * 160 + (if (index % 3 == 0) 10 else 0)).dp
                        val offY = ((index / 2) * 100).dp
                        Box(modifier = Modifier.offset(x = offX, y = offY)) {
                            SeedCard(
                                category = seed.category,
                                text = seed.text,
                                tail = seed.tail,
                                rotation = seed.rotation,
                                onDelete = {
                                    seeds = seeds.toMutableList().also { it.removeAt(index) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Category chips
        Column(
            modifier = Modifier
                .padding(top = 570.dp, start = 0.dp)
        ) {
            Text(
                text = "CATEGORIES",
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 10.sp,
                    letterSpacing = 0.16.sp, color = Ink500
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES.size) { i ->
                    CategoryBubble(
                        label = CATEGORIES[i],
                        active = selectedCategory == CATEGORIES[i],
                        onClick = { selectedCategory = CATEGORIES[i] }
                    )
                }
                item {
                    CategoryBubble(
                        label = "自定义",
                        active = false,
                        onClick = { selectedCategory = "自定义" }
                    )
                }
            }
        }

        // Quick-add input when category is selected
        if (selectedCategory != null) {
            Row(
                modifier = Modifier
                    .padding(top = 660.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Cream100)
                    .border(1.dp, Ink200, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        fontFamily = FontDisplay, fontSize = 15.sp,
                        color = Ink900
                    ),
                    cursorBrush = SolidColor(DawnEmber),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (inputText.isNotBlank()) {
                            val rots = listOf(-3f, 2f, -1f, 3f, -2f, 1f)
                            seeds = seeds + SeedEntry(
                                category = selectedCategory!!,
                                text = inputText.trim(),
                                rotation = rots[seeds.size % rots.size]
                            )
                            inputText = ""
                            selectedCategory = null
                        }
                    })
                )
                if (inputText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Ink900)
                            .clickable {
                                val rots = listOf(-3f, 2f, -1f, 3f, -2f, 1f)
                                seeds = seeds + SeedEntry(
                                    category = selectedCategory!!,
                                    text = inputText.trim(),
                                    rotation = rots[seeds.size % rots.size]
                                )
                                inputText = ""
                                selectedCategory = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "＋", style = TextStyle(color = Cream50, fontSize = 16.sp))
                    }
                }
            }
        }

        // CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            PrimaryCta(
                text = "交给 Aether",
                onClick = onNext
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Step 4 — Bluetooth glasses pairing (optional)
// ══════════════════════════════════════════════════════════════════════

@Composable
fun OnboardStep4Bluetooth(onNext: () -> Unit, onSkip: () -> Unit) {
    var scanning by remember { mutableStateOf(true) }
    var found by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "scan_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    OnboardScaffold(step = 4, required = false, onSkip = onSkip) {
        Column(modifier = Modifier.padding(top = 136.dp, start = 28.dp, end = 28.dp)) {
            Text(
                text = "连接你的",
                style = TextStyle(
                    fontFamily = FontCnDisplay, fontSize = 28.sp,
                    fontWeight = FontWeight.Normal, lineHeight = 36.sp, color = Ink900
                )
            )
            Text(
                text = "Aether 眼镜",
                style = TextStyle(
                    fontFamily = FontCnDisplay, fontStyle = FontStyle.Italic,
                    fontSize = 28.sp, fontWeight = FontWeight.Normal,
                    lineHeight = 36.sp, color = DawnEmber
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "把眼镜从盒中取出。靠近手机，等它找到你。",
                style = TextStyle(
                    fontFamily = FontCnBody, fontSize = 14.sp,
                    lineHeight = 22.sp, color = Ink500
                )
            )
        }

        // Glasses + scanning halo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-30).dp),
            contentAlignment = Alignment.Center
        ) {
            // Scanning rings
            if (scanning && !found) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size((180 + i * 60).dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                DawnEmber.copy(alpha = (0.2f - i * 0.05f) * pulseScale),
                                CircleShape
                            )
                    )
                }
            }

            GlassesMark(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(200f / 110f),
                color = null
            )
        }

        // Scanning status text
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 80.dp)
        ) {
            if (!found) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DawnEmber))
                    Text(
                        text = "SCANNING",
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 11.sp,
                            letterSpacing = 0.14.sp, color = DawnEmber
                        )
                    )
                }
            }
        }

        // CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            if (found) {
                PrimaryCta(text = "配对并继续", onClick = onNext)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, Ink200, RoundedCornerShape(28.dp))
                            .clickable(onClick = onSkip),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "稍后配对",
                            style = TextStyle(fontFamily = FontCnBody, fontSize = 15.sp, color = Ink500)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Ink900)
                            .clickable { found = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "继续搜索",
                            style = TextStyle(
                                fontFamily = FontCnBody, fontSize = 16.sp,
                                fontWeight = FontWeight.Medium, color = Cream50
                            )
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Step 5 — API configuration (required)
// ══════════════════════════════════════════════════════════════════════

private val PROVIDERS = listOf(
    Triple("Anthropic", "Claude 3.5+", true),
    Triple("OpenAI", "GPT-4o+", false),
    Triple("Mistral", "Large", false),
    Triple("Local", "Ollama", false),
)

@Composable
fun OnboardStep5Api(onNext: (apiKey: String) -> Unit) {
    var selectedProvider by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf("") }

    OnboardScaffold(step = 5, totalSteps = 6, required = true) {
        Column(modifier = Modifier.padding(top = 136.dp, start = 28.dp, end = 28.dp)) {
            Text(
                text = "One last key.",
                style = TextStyle(
                    fontFamily = FontDisplay, fontSize = 38.sp,
                    fontWeight = FontWeight.Light, lineHeight = 40.sp,
                    letterSpacing = (-0.95).sp, color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your model provider powers the thinking. The key stays on your device — I never see it in plaintext.",
                style = TextStyle(
                    fontFamily = FontBody, fontSize = 14.sp,
                    lineHeight = 22.sp, color = Ink500
                )
            )
        }

        // Provider grid
        Column(modifier = Modifier.padding(top = 282.dp, start = 24.dp, end = 24.dp)) {
            Text(
                text = "PROVIDER",
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 10.sp,
                    letterSpacing = 0.14.sp, color = Ink500
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )
            val gridRows = PROVIDERS.chunked(2)
            gridRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (name, sub, _) ->
                        val idx = PROVIDERS.indexOfFirst { it.first == name }
                        val selected = selectedProvider == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) Ink900 else Cream100)
                                .border(
                                    1.dp,
                                    if (selected) Ink900 else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedProvider = idx }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .border(
                                                if (selected) 2.dp else 1.5.dp,
                                                if (selected) Cream50 else Ink300,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(DawnPeach)
                                            )
                                        }
                                    }
                                    Text(
                                        text = name,
                                        style = TextStyle(
                                            fontFamily = FontDisplay, fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            letterSpacing = (-0.16).sp,
                                            color = if (selected) Cream50 else Ink900
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sub,
                                    style = TextStyle(
                                        fontFamily = FontMono, fontSize = 10.sp,
                                        letterSpacing = 0.08.sp,
                                        color = if (selected) Cream200 else Ink300
                                    ),
                                    modifier = Modifier.padding(start = 22.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // API key input
        Column(modifier = Modifier.padding(top = 486.dp, start = 24.dp, end = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "API KEY",
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 10.sp,
                        letterSpacing = 0.14.sp, color = Ink500
                    )
                )
                if (apiKey.length > 10) {
                    Text(
                        text = "● VALID",
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 10.sp,
                            letterSpacing = 0.14.sp, color = DawnEmber
                        )
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ink900)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🔐", fontSize = 14.sp)
                BasicTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    textStyle = TextStyle(
                        fontFamily = FontMono, fontSize = 14.sp,
                        letterSpacing = 0.08.sp, color = Cream200
                    ),
                    cursorBrush = SolidColor(DawnPeach),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }

        // CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            PrimaryCta(
                text = "Begin",
                onClick = { if (apiKey.isNotBlank()) onNext(apiKey.trim()) },
                dawn = true,
                footnote = "Cannot skip · needed for Aether to think"
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Step 6 — Permissions (optional)
// ══════════════════════════════════════════════════════════════════════

private data class PermItem(
    val label: String,
    val labelEn: String,
    val why: String,
    val defaultOn: Boolean,
)

private val PERMISSIONS = listOf(
    PermItem("麦克风", "Microphone", "为了听见你。按住说话与唤醒词识别。", true),
    PermItem("蓝牙", "Bluetooth", "和你的眼镜保持同一呼吸。", true),
    PermItem("通知", "Notifications", "在合适的时刻轻声出现——不会更频繁。", true),
    PermItem("日历", "Calendar", "知道下一件事是谁、在哪、需要什么。可随时关闭。", false),
    PermItem("位置", "Location", "用来认得你常去的地方——从不持续追踪。", false),
)

@Composable
fun OnboardStep6Permissions(onNext: () -> Unit) {
    val permStates = remember { PERMISSIONS.map { mutableStateOf(it.defaultOn) } }

    OnboardScaffold(step = 6, totalSteps = 6, required = false, onSkip = onNext) {
        Column(modifier = Modifier.padding(top = 136.dp, start = 28.dp, end = 28.dp)) {
            Text(
                text = "Aether 需要你的",
                style = TextStyle(
                    fontFamily = FontCnDisplay, fontSize = 26.sp,
                    fontWeight = FontWeight.Normal, lineHeight = 32.sp, color = Ink900
                )
            )
            Text(
                text = "一些信任",
                style = TextStyle(
                    fontFamily = FontCnDisplay, fontStyle = FontStyle.Italic,
                    fontSize = 26.sp, fontWeight = FontWeight.Normal,
                    lineHeight = 32.sp, color = DawnEmber
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "每一项都可以稍后更改。每次只取用恰好够用的那一点。",
                style = TextStyle(
                    fontFamily = FontCnBody, fontSize = 13.sp,
                    lineHeight = 21.sp, color = Ink500
                )
            )
        }

        Column(modifier = Modifier.padding(top = 276.dp, start = 24.dp, end = 24.dp)) {
            PERMISSIONS.forEachIndexed { i, perm ->
                if (i > 0) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink200))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = perm.label,
                                style = TextStyle(
                                    fontFamily = FontCnDisplay, fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal, color = Ink900
                                )
                            )
                            Text(
                                text = perm.labelEn.uppercase(),
                                style = TextStyle(
                                    fontFamily = FontMono, fontSize = 9.sp,
                                    letterSpacing = 0.14.sp, color = Ink300
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = perm.why,
                            style = TextStyle(
                                fontFamily = FontCnBody, fontStyle = FontStyle.Italic,
                                fontSize = 12.sp, lineHeight = 19.sp, color = Ink500
                            )
                        )
                    }
                    AetherToggle(
                        checked = permStates[i].value,
                        onCheckedChange = { permStates[i].value = it },
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // CTA
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            val onCount = permStates.count { it.value }
            val offCount = permStates.size - onCount
            PrimaryCta(
                text = "进入 Aether",
                onClick = onNext,
                dawn = true,
                footnote = "$onCount allowed · $offCount off · change anytime"
            )
        }
    }
}
