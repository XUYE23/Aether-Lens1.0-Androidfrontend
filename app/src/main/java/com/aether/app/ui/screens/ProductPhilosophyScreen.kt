package com.aether.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.app.ui.components.AetherMarkDawn
import com.aether.app.ui.theme.Cream50
import com.aether.app.ui.theme.Cream100
import com.aether.app.ui.theme.DawnDusk
import com.aether.app.ui.theme.DawnEmber
import com.aether.app.ui.theme.DawnGlow
import com.aether.app.ui.theme.DawnMauve
import com.aether.app.ui.theme.DawnPeach
import com.aether.app.ui.theme.FontCnBody
import com.aether.app.ui.theme.FontDisplay
import com.aether.app.ui.theme.FontMono
import com.aether.app.ui.theme.Ink300
import com.aether.app.ui.theme.Ink500
import com.aether.app.ui.theme.Ink900

private data class BeliefItem(
    val numeral: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
)

private data class DayMoment(
    val time: String,
    val did: String,
    val felt: String,
    val accent: Color,
)

@Composable
private fun PhilosophyReveal(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenHeightPx = remember(configuration.screenHeightDp) {
        configuration.screenHeightDp * 3f
    }
    var isVisible by remember { mutableStateOf(false) }
    val revealAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = delayMillis),
        label = "reveal_alpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = tween(durationMillis = 900, delayMillis = delayMillis),
        label = "reveal_translate"
    )

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                if (!isVisible && bounds.top < screenHeightPx * 0.9f && bounds.bottom > 0f) {
                    isVisible = true
                }
            }
            .graphicsLayer {
                alpha = revealAlpha
                translationY = translateY
            }
    ) {
        content()
    }
}

@Composable
fun ProductPhilosophyScreen(
    onBack: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream50)
            .verticalScroll(scrollState)
    ) {
        PhilosophyHeroSection(onBack = onBack)
        DiagnosisSection()
        BeliefsSection()
        ManifestoSection()
        DayWithAetherSection()
        ClosingSection()
    }
}

@Composable
private fun PhilosophyHeroSection(onBack: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "hero_clover")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_halo_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3A2A3A),
                        Ink900,
                        Color(0xFF0B0908)
                    ),
                    center = Offset(540f, 240f),
                    radius = 1300f
                )
            )
            .padding(horizontal = 28.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhilosophyBackButton(onClick = onBack)
                Spacer(modifier = Modifier.weight(1f))
                PhilosophyReveal(delayMillis = 80) {
                    Text(
                        text = "PRODUCT PHILOSOPHY",
                        style = TextStyle(
                            fontFamily = FontMono,
                            fontSize = 10.sp,
                            letterSpacing = 0.2.sp,
                            color = Cream100.copy(alpha = 0.66f)
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(42.dp))
            }

            Spacer(modifier = Modifier.height(72.dp))

            PhilosophyReveal(delayMillis = 140) {
                Box(
                    modifier = Modifier.size(244.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(haloAlpha)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    DawnPeach.copy(alpha = 0.34f),
                                    DawnEmber.copy(alpha = 0.22f),
                                    DawnDusk.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width / 2f
                            ),
                            radius = size.width / 2f
                        )
                    }
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                    ) {
                        AetherMarkDawn(size = 146.dp, strokeWidth = 5.5f)
                    }
                }
            }

            Spacer(modifier = Modifier.height(52.dp))

            PhilosophyReveal(delayMillis = 220) {
                Text(
                    text = "在这个时代，我们需要的\n不是更好的工具。",
                    style = TextStyle(
                        fontFamily = FontDisplay,
                        fontWeight = FontWeight.Light,
                        fontStyle = FontStyle.Italic,
                        fontSize = 30.sp,
                        lineHeight = 45.sp,
                        color = DawnGlow
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            PhilosophyReveal(delayMillis = 340) {
                Text(
                    text = "而是被工具真正\n托住的自由。",
                    style = TextStyle(
                        fontFamily = FontDisplay,
                        fontWeight = FontWeight.Light,
                        fontStyle = FontStyle.Italic,
                        fontSize = 30.sp,
                        lineHeight = 45.sp,
                        color = DawnPeach
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            PhilosophyReveal(delayMillis = 420) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCROLL",
                        style = TextStyle(
                            fontFamily = FontMono,
                            fontSize = 10.sp,
                            letterSpacing = 0.22.sp,
                            color = Cream100.copy(alpha = 0.48f)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Cream100.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun DiagnosisSection() {
    val leftItems = remember {
        listOf("回复那条消息", "收件箱又满了", "优化下个季度", "排进日历里", "永不下线", "再检查一次通知")
    }
    val rightItems = remember {
        listOf("会议已经开始了。", "她想起了女儿的钢琴课。", "今晚的饭，他尝出了姜的味道。", "雨停的那一刻，你抬了头。", "你只是在场。", "手机安静地躺在口袋里。")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream50)
            .padding(horizontal = 28.dp, vertical = 72.dp)
    ) {
        PhilosophyReveal(delayMillis = 40) {
            SectionMark(number = "02", title = "Diagnosis")
        }
        Spacer(modifier = Modifier.height(24.dp))
        PhilosophyReveal(delayMillis = 120) {
            IntroText(text = "我们把一天切成了通知，\n把生活切成了日程。")
        }
        Spacer(modifier = Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PhilosophyReveal(modifier = Modifier.weight(1f), delayMillis = 200) {
                Column {
                Text(
                    text = "Today",
                    style = TextStyle(
                        fontFamily = FontDisplay,
                        fontWeight = FontWeight.Light,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        color = Ink500
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
                leftItems.forEach {
                    Text(
                        text = it,
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontWeight = FontWeight.Light,
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp,
                            color = Ink300
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                }
            }
            PhilosophyReveal(modifier = Modifier.weight(1f), delayMillis = 320) {
                Column {
                Text(
                    text = "With Aether",
                    style = TextStyle(
                        fontFamily = FontDisplay,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = Ink900
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
                rightItems.forEach {
                    Text(
                        text = it,
                        style = TextStyle(
                            fontFamily = FontDisplay,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.5.sp,
                            lineHeight = 22.sp,
                            color = Ink900
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun BeliefsSection() {
    val items = remember {
        listOf(
            BeliefItem("I", "被理解，不是被观察。", "记忆是陪伴，不是监控。", DawnEmber),
            BeliefItem("II", "被记住，不是被追踪。", "三到五年的陪伴，感觉像友谊。", DawnMauve),
            BeliefItem("III", "被陪伴，不是被打扰。", "主动，意味着在场，而不是推送。", DawnPeach),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream100)
            .padding(horizontal = 28.dp, vertical = 72.dp)
    ) {
        PhilosophyReveal(delayMillis = 40) {
            SectionMark(number = "03", title = "Three Beliefs")
        }
        Spacer(modifier = Modifier.height(24.dp))
        PhilosophyReveal(delayMillis = 120) {
            IntroText(text = "我们反复回到\n这三句话上。")
        }
        Spacer(modifier = Modifier.height(40.dp))
        items.forEachIndexed { index, item ->
            PhilosophyReveal(delayMillis = 180 + index * 120) {
                BeliefPanel(item = item)
            }
            if (index != items.lastIndex) {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ManifestoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink900)
            .padding(horizontal = 34.dp, vertical = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhilosophyReveal(delayMillis = 40) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DawnPeach.copy(alpha = 0.22f),
                                DawnEmber.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(540f, 300f),
                            radius = 780f
                        )
                    )
            )
        }
        PhilosophyReveal(delayMillis = 140) {
            Text(
                text = "效率是我们给这个时代的妥协，",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 26.sp,
                    lineHeight = 45.sp,
                    color = Cream50
                ),
                textAlign = TextAlign.Center
            )
        }
        PhilosophyReveal(delayMillis = 240) {
            Text(
                text = "Aether 是我们对自己的承诺。",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 26.sp,
                    lineHeight = 45.sp,
                    color = Color(0xFFF4DDC2)
                ),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        PhilosophyReveal(delayMillis = 340) {
            Text(
                text = "外接大脑处理一切，",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 26.sp,
                    lineHeight = 45.sp,
                    color = Color(0xFFEDC29F)
                ),
                textAlign = TextAlign.Center
            )
        }
        PhilosophyReveal(delayMillis = 440) {
            Text(
                text = "让你重新用心脏去感受。",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 26.sp,
                    lineHeight = 45.sp,
                    color = DawnPeach
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayWithAetherSection() {
    val moments = remember {
        listOf(
            DayMoment("07:12", "Aether 把今天的三件事轻声读给你听。", "你没有打开手机。", DawnDusk),
            DayMoment("09:40", "它提醒你，同事上周说起过他父亲的手术。", "你问了一句，她笑了。", DawnMauve),
            DayMoment("13:05", "会议前两分钟，它给你看了对方孩子的名字。", "寒暄变成了对话。", DawnEmber),
            DayMoment("17:30", "它什么都没说。", "窗外的光，正在变金。", DawnEmber),
            DayMoment("20:18", "它记得你妈妈生日前会紧张。它已经订好花了。", "你想起来，该打个电话。", DawnPeach),
            DayMoment("22:47", "Aether 安静地退到了背景里。", "你睡着了。", DawnGlow),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream50)
            .padding(horizontal = 28.dp, vertical = 72.dp)
    ) {
        PhilosophyReveal(delayMillis = 40) {
            SectionMark(number = "05", title = "A Day With Aether")
        }
        Spacer(modifier = Modifier.height(24.dp))
        PhilosophyReveal(delayMillis = 120) {
            IntroText(text = "不是功能清单。\n是一天里被轻轻托住的六个时刻。")
        }
        Spacer(modifier = Modifier.height(44.dp))
        Row {
            PhilosophyReveal(delayMillis = 180) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, end = 20.dp)
                        .width(30.dp)
                        .height(540.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DawnDusk,
                                        DawnMauve,
                                        DawnEmber,
                                        DawnPeach,
                                        DawnGlow
                                    )
                                ),
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                moments.forEachIndexed { index, moment ->
                    PhilosophyReveal(delayMillis = 220 + index * 100) {
                        DayMomentRow(moment = moment)
                    }
                    if (index != moments.lastIndex) {
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClosingSection() {
    val transition = rememberInfiniteTransition(label = "closing_clover")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "closing_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Cream50,
                        Cream100,
                        Ink500,
                        Ink900
                    )
                )
            )
            .padding(horizontal = 34.dp, vertical = 140.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhilosophyReveal(delayMillis = 80) {
            Text(
                text = "你已经够努力了。\n让我们替你记住剩下的一切。",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 26.sp,
                    lineHeight = 42.sp,
                    color = Ink900
                ),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(64.dp))
        PhilosophyReveal(delayMillis = 220) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                AetherMarkDawn(size = 104.dp, strokeWidth = 5.5f)
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        PhilosophyReveal(delayMillis = 320) {
            Text(
                text = "Aether",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.sp,
                    letterSpacing = 0.35.sp,
                    color = Cream50.copy(alpha = 0.56f)
                )
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        PhilosophyReveal(delayMillis = 420) {
            Text(
                text = "A letter, not a landing page.",
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.28.sp,
                    color = Cream50.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionMark(
    number: String,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(DawnEmber)
        )
        Text(
            text = "$number    $title",
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.sp,
                letterSpacing = 0.28.sp,
                color = Ink300
            )
        )
    }
}

@Composable
private fun IntroText(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontDisplay,
            fontWeight = FontWeight.Light,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            lineHeight = 32.sp,
            color = Ink900
        )
    )
}

@Composable
private fun BeliefPanel(item: BeliefItem) {
    Row {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(122.dp)
                .background(item.accent)
        )
        Column(modifier = Modifier.padding(start = 20.dp)) {
            Text(
                text = item.numeral,
                style = TextStyle(
                    fontFamily = FontMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.22.sp,
                    color = Ink500
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Normal,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.subtitle,
                style = TextStyle(
                    fontFamily = FontCnBody,
                    fontWeight = FontWeight.Light,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Ink500
                )
            )
        }
    }
}

@Composable
private fun DayMomentRow(moment: DayMoment) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = moment.time,
            style = TextStyle(
                fontFamily = FontMono,
                fontSize = 10.5.sp,
                color = Ink300
            ),
            modifier = Modifier.width(54.dp)
        )
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Cream50)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = moment.accent,
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
                Spacer(modifier = Modifier.width(18.dp))
                Column {
                    Text(
                        text = moment.did,
                        style = TextStyle(
                            fontFamily = FontCnBody,
                            fontWeight = FontWeight.Light,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Ink500
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = moment.felt,
                        style = TextStyle(
                            fontFamily = FontDisplay,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.5.sp,
                            lineHeight = 23.sp,
                            color = if (moment.accent == DawnGlow) Color(0xFF6A4B3F) else Ink900
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PhilosophyBackButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Cream100.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(width = 14.dp, height = 10.dp)) {
            val stroke = 1.7.dp.toPx()
            drawLine(
                color = Cream50,
                start = Offset(size.width, size.height / 2f),
                end = Offset(3.dp.toPx(), size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Cream50,
                start = Offset(3.dp.toPx(), size.height / 2f),
                end = Offset(8.dp.toPx(), 1.2.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Cream50,
                start = Offset(3.dp.toPx(), size.height / 2f),
                end = Offset(8.dp.toPx(), size.height - 1.2.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
