package com.aether.app.ui.screens

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aether.app.ui.components.AetherMark
import com.aether.app.ui.components.InkBlob
import com.aether.app.ui.components.Pill
import com.aether.app.ui.components.PillVariant
import com.aether.app.ui.theme.Cream100
import com.aether.app.ui.theme.Cream200
import com.aether.app.ui.theme.Cream50
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.DawnHaze
import com.aether.app.ui.theme.DawnPeach
import com.aether.app.ui.theme.FontBody
import com.aether.app.ui.theme.FontCnBody
import com.aether.app.ui.theme.FontDisplay
import com.aether.app.ui.theme.FontMono
import com.aether.app.ui.theme.Ink200
import com.aether.app.ui.theme.Ink300
import com.aether.app.ui.theme.Ink500
import com.aether.app.ui.theme.Ink700
import com.aether.app.ui.theme.Ink900
// ── Workspace states ─────────────────────────────────────────────────
private enum class SpeakState { Idle, Pressing, Speaking, Releasing }

data class SuggestionCard(
    val tag: String,
    val tagTime: String? = null,
    val timeAgo: String? = null,
    val headline: String,
    val body: String,
    val style: CardStyle = CardStyle.Filled,
    val actions: List<String> = emptyList(),
)

enum class CardStyle { Filled, Outlined, Ambient }

// ── Main workspace screen ─────────────────────────────────────────────

@Composable
fun AetherWorkspaceScreen(
    userName: String,
    avatarUriString: String? = null,
    suggestions: List<SuggestionCard> = defaultSuggestions(),
    onNavigateToPersonal: () -> Unit = {},
    onPressStart: () -> Unit = {},
    onPressEnd: () -> Unit = {},
    audioLevel: Float = 0f,
) {
    val avatarUri = remember(avatarUriString) { avatarUriString?.let(Uri::parse) }
    var speakState by remember { mutableStateOf(SpeakState.Idle) }
    var transcript by remember { mutableStateOf("") }
    val blobAmp = remember { Animatable(0.18f) }
    val speakOverlayAlpha = remember { Animatable(0f) }

    LaunchedEffect(speakState) {
        when (speakState) {
            SpeakState.Pressing -> {
                speakOverlayAlpha.animateTo(1f, tween(300))
                blobAmp.animateTo(0.18f, tween(200))
            }
            SpeakState.Speaking -> {
                blobAmp.animateTo(0.4f + audioLevel * 0.6f, spring(stiffness = Spring.StiffnessLow))
            }
            SpeakState.Releasing -> {
                blobAmp.animateTo(0.08f, tween(400))
                kotlinx.coroutines.delay(800)
                speakOverlayAlpha.animateTo(0f, tween(400))
                speakState = SpeakState.Idle
            }
            SpeakState.Idle -> {
                blobAmp.animateTo(0.18f, tween(300))
            }
        }
    }

    // Keep blob amp in sync with audio level while speaking
    LaunchedEffect(audioLevel) {
        if (speakState == SpeakState.Speaking) {
            blobAmp.animateTo(
                targetValue = 0.18f + audioLevel * 0.57f,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Idle view ───────────────────────────────────────────────
        WorkspaceIdle(
            userName = userName,
            avatarUri = avatarUri,
            suggestions = suggestions,
            onNavigateToPersonal = onNavigateToPersonal,
            modifier = Modifier.graphicsLayer { alpha = 1f - speakOverlayAlpha.value }
        )

        // ── Speak overlay ────────────────────────────────────────────
        if (speakOverlayAlpha.value > 0.01f) {
            SpeakOverlay(
                state = speakState,
                blobAmp = blobAmp.value,
                transcript = transcript,
                alpha = speakOverlayAlpha.value
            )
        }

        // ── Hold-to-speak dock (always visible) ──────────────────────
        HoldToSpeakDock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp, start = 24.dp, end = 24.dp),
            speakState = speakState,
            onPressStart = {
                speakState = SpeakState.Pressing
                onPressStart()
            },
            onPressEnd = {
                if (speakState == SpeakState.Speaking || speakState == SpeakState.Pressing) {
                    speakState = SpeakState.Releasing
                    onPressEnd()
                }
            }
        )
    }
}

// ── Idle view ─────────────────────────────────────────────────────────

@Composable
private fun WorkspaceIdle(
    userName: String,
    avatarUri: Uri?,
    suggestions: List<SuggestionCard>,
    onNavigateToPersonal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Cream50)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AetherMark(size = 24.dp, color = Ink900, strokeWidth = 6f)
                Text(
                    text = "Aether",
                    style = TextStyle(
                        fontFamily = FontDisplay, fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = (-0.2).sp, color = Ink900
                    )
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Cream100)
                    .clickable(onClick = onNavigateToPersonal)
                    .semantics { contentDescription = "Open personal space" }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(DawnEmber.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "$userName avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "A",
                            style = TextStyle(
                                fontFamily = FontMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DawnEmber
                            )
                        )
                    }
                }
                Text(
                    text = "YOUR SPACE",
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 10.sp,
                        letterSpacing = 0.14.sp, color = Ink700
                    )
                )
                Text(
                    text = "›",
                    style = TextStyle(
                        fontFamily = FontMono,
                        fontSize = 12.sp,
                        color = Ink500
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "THURSDAY · 9:14",
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 10.sp,
                    letterSpacing = 0.16.sp, color = Ink500
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Morning, ",
                style = TextStyle(
                    fontFamily = FontDisplay, fontSize = 36.sp,
                    fontWeight = FontWeight.Light, lineHeight = 40.sp,
                    letterSpacing = (-0.9).sp, color = Ink900
                )
            )
            Row {
                Text(
                    text = userName,
                    style = TextStyle(
                        fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                        fontSize = 36.sp, fontWeight = FontWeight.Light,
                        lineHeight = 40.sp, letterSpacing = (-0.9).sp, color = DawnEmber
                    )
                )
                Text(
                    text = ".",
                    style = TextStyle(
                        fontFamily = FontDisplay, fontSize = 36.sp,
                        fontWeight = FontWeight.Light, lineHeight = 40.sp, color = Ink900
                    )
                )
            }
            Text(
                text = "Three quiet things.",
                style = TextStyle(
                    fontFamily = FontDisplay, fontSize = 36.sp,
                    fontWeight = FontWeight.Light, lineHeight = 40.sp,
                    letterSpacing = (-0.9).sp, color = Ink900
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Suggestion cards
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            suggestions.forEach { card ->
                SuggestionCardView(card = card)
            }
        }

        // Bottom padding for dock
        Spacer(modifier = Modifier.height(124.dp))
    }
}

@Composable
private fun SuggestionCardView(card: SuggestionCard) {
    val (bg, borderColor) = when (card.style) {
        CardStyle.Filled   -> Pair(Cream100, Color.Transparent)
        CardStyle.Outlined -> Pair(Cream50, Ink200)
        CardStyle.Ambient  -> Pair(Color.Transparent, Ink200.copy(alpha = 0.5f))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            // Tag row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.tag.uppercase(),
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 10.sp,
                        letterSpacing = 0.14.sp, color = Ink500
                    )
                )
                if (card.tagTime != null) {
                    Text(
                        text = card.tagTime,
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 10.sp,
                            letterSpacing = 0.08.sp, color = Ink500
                        )
                    )
                }
                if (card.timeAgo != null) {
                    Text(
                        text = card.timeAgo,
                        style = TextStyle(
                            fontFamily = FontMono, fontSize = 10.sp,
                            color = Ink300
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headline
            Text(
                text = card.headline,
                style = TextStyle(
                    fontFamily = FontDisplay, fontSize = if (card.style == CardStyle.Outlined) 17.sp else 19.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = if (card.style == CardStyle.Outlined) FontStyle.Italic else FontStyle.Normal,
                    lineHeight = if (card.style == CardStyle.Outlined) 24.sp else 26.sp,
                    color = Ink900
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Body
            Text(
                text = card.body,
                style = TextStyle(
                    fontFamily = FontBody, fontSize = 13.sp,
                    lineHeight = 20.sp, color = Ink700
                )
            )

            // Action pills
            if (card.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    card.actions.forEachIndexed { i, action ->
                        Pill(
                            text = action,
                            onClick = {},
                            variant = if (i == 0) PillVariant.Secondary else PillVariant.Ghost,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 14.dp, vertical = 8.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ── Hold-to-speak dock ───────────────────────────────────────────────

@Composable
private fun HoldToSpeakDock(
    modifier: Modifier = Modifier,
    speakState: SpeakState,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
) {
    val dockLabel = when (speakState) {
        SpeakState.Idle      -> "Hold to speak. Or just think near me."
        SpeakState.Pressing  -> "Holding. Whenever you're ready."
        SpeakState.Speaking  -> "Holding. Take your time."
        SpeakState.Releasing -> "Heard. Let me think."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Ink900)
            .padding(start = 22.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = dockLabel,
            style = TextStyle(
                fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                fontSize = 15.sp, color = Cream200
            ),
            modifier = Modifier.weight(1f)
        )

        // Mic button — hold gesture
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(DawnEmber, DawnPeach)
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            val released = tryAwaitRelease()
                            if (released) onPressEnd()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎙",
                fontSize = 18.sp
            )
        }
    }
}

// ── Speak overlay (dark + InkBlob) ──────────────────────────────────

@Composable
private fun SpeakOverlay(
    state: SpeakState,
    blobAmp: Float,
    transcript: String,
    alpha: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink900)
            .graphicsLayer { this.alpha = alpha }
    ) {
        // Radial background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DawnEmber.copy(alpha = 0.35f),
                            DawnEmber.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        // Header
        Row(
            modifier = Modifier
                .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DawnPeach))
            Text(
                text = when (state) {
                    SpeakState.Pressing  -> "LISTENING"
                    SpeakState.Speaking  -> "HEARING YOU"
                    SpeakState.Releasing -> "THINKING"
                    else -> ""
                }.uppercase(),
                style = TextStyle(
                    fontFamily = FontMono, fontSize = 11.sp,
                    letterSpacing = 0.14.sp, color = DawnHaze
                )
            )
        }

        // Transcript
        if (transcript.isNotBlank() || state == SpeakState.Pressing) {
            Column(
                modifier = Modifier.padding(top = 128.dp, start = 28.dp, end = 28.dp)
            ) {
                Text(
                    text = if (state == SpeakState.Releasing) "SENT" else "TRANSCRIBING",
                    style = TextStyle(
                        fontFamily = FontMono, fontSize = 10.sp,
                        letterSpacing = 0.16.sp,
                        color = DawnHaze.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (transcript.isBlank() && state == SpeakState.Pressing) {
                    Text(
                        text = "I'm here. Take your time.",
                        style = TextStyle(
                            fontFamily = FontDisplay, fontStyle = FontStyle.Italic,
                            fontSize = 22.sp, lineHeight = 28.sp,
                            color = DawnHaze.copy(alpha = 0.45f)
                        )
                    )
                } else {
                    Text(
                        text = transcript,
                        style = TextStyle(
                            fontFamily = FontDisplay, fontSize = 26.sp,
                            fontWeight = FontWeight.Light, lineHeight = 32.sp,
                            letterSpacing = (-0.39).sp,
                            color = if (state == SpeakState.Releasing)
                                Cream50.copy(alpha = 0.55f) else Cream50
                        )
                    )
                }
            }
        }

        // InkBlob — centered, sized by state
        val blobSize = when (state) {
            SpeakState.Speaking  -> 320.dp
            SpeakState.Releasing -> 220.dp
            else -> 280.dp
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 60.dp)
        ) {
            InkBlob(
                size = blobSize,
                amp = blobAmp,
                seed = when (state) {
                    SpeakState.Speaking  -> 2.3f
                    SpeakState.Releasing -> 0.9f
                    else -> 1.1f
                }
            )
        }

        // Timer + status
        Text(
            text = when (state) {
                SpeakState.Pressing  -> "0:01  ·  Release to cancel"
                SpeakState.Speaking  -> "0:04  ·  Release to send"
                SpeakState.Releasing -> "0:04 sent  ·  I'll write back"
                else -> ""
            },
            style = TextStyle(
                fontFamily = FontMono, fontSize = 11.sp,
                letterSpacing = 0.14.sp,
                color = if (state == SpeakState.Speaking) DawnPeach
                        else DawnHaze.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 136.dp)
        )
    }
}

// ── Default suggestion cards ──────────────────────────────────────────

private fun defaultSuggestions() = listOf(
    SuggestionCard(
        tag = "Next · 9:15",
        timeAgo = "in 1 min",
        headline = "Mira's walking in.",
        body = "Last time she mentioned her daughter started piano. Ask how it's going.",
        style = CardStyle.Filled,
        actions = listOf("Remind me", "Not now"),
    ),
    SuggestionCard(
        tag = "Surfaced · from 2 weeks ago",
        headline = "\"I want to write to Dad more often — even just a line.\"",
        body = "Want me to draft something?",
        style = CardStyle.Outlined,
        actions = listOf("Draft it", "Later", "Forget this"),
    ),
    SuggestionCard(
        tag = "Ambient",
        headline = "Golden hour in 9h 22m",
        body = "You asked to be told.",
        style = CardStyle.Ambient,
    ),
)
