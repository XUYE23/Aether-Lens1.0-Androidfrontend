package com.aether.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.aether.app.data.UserPreferencesRepository
import com.aether.app.device.DeviceRepository
import com.aether.app.device.BluetoothScanner
import com.aether.app.ui.components.ActionCardContent
import com.aether.app.ui.components.AuroraWaveHalo
import com.aether.app.ui.components.CardStack
import com.aether.app.ui.components.DangerModeOverlay
import com.aether.app.ui.components.SwipeCard
import com.aether.app.ui.screens.AwakeningScreen
import com.aether.app.ui.screens.PersonalNexusScreen
import com.aether.app.ui.theme.AetherTheme
import com.aether.app.ui.theme.GlassBorder
import com.aether.app.ui.theme.PureWhite
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.DisposableEffect
import com.aether.app.ui.components.GhostVoiceCard
import com.aether.app.voice.SpeechRecognizerImpl
import com.aether.app.workspace.VoicePhase
import com.aether.app.workspace.WorkspaceCardStateHolder
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Repository 在 Activity 创建时初始化，与 Activity 生命周期一致
    private val repository by lazy { UserPreferencesRepository(applicationContext) }

    // MainViewModel 以 Activity 为作用域，两个 Tab 共享同一实例
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository)
    }

    private val deviceRepository by lazy { DeviceRepository(applicationContext) }
    private val bluetoothScanner by lazy { BluetoothScanner(applicationContext) }
    private val deviceViewModel: DeviceViewModel by viewModels {
        DeviceViewModelFactory(deviceRepository, bluetoothScanner)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AetherTheme {
                AetherApp(
                    context = this,
                    viewModel = mainViewModel,
                    deviceViewModel = deviceViewModel
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  应用根节点
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AetherApp(context: Context, viewModel: MainViewModel, deviceViewModel: DeviceViewModel) {
    val prefs = remember { context.getSharedPreferences("aether", Context.MODE_PRIVATE) }
    var showAwakening by remember { mutableStateOf(!prefs.getBoolean("awakened", false)) }

    val isDangerMode by viewModel.uiState
        .map { it.isDangerModeActive }
        .collectAsState(initial = false)

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDangerMode) 1f else 0f,
        animationSpec = tween(600),
        label = "danger_glow_alpha"
    )
    val dangerTransition = rememberInfiniteTransition(label = "danger_breath")
    val breathAlpha by dangerTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "danger_breath_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .drawWithContent {
                drawContent()
                if (glowAlpha > 0f) {
                    val brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Red.copy(alpha = breathAlpha * glowAlpha)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = maxOf(size.width, size.height) * 0.85f
                    )
                    drawRect(brush = brush)
                }
            }
    ) {
        if (showAwakening) {
            AwakeningScreen {
                prefs.edit().putBoolean("awakened", true).apply()
                showAwakening = false
            }
        } else {
            MainScreen(viewModel = viewModel, deviceViewModel = deviceViewModel)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  主容器：TabRow + HorizontalPager
//  userName 由 MainViewModel（DataStore）提供，工作台和个人空间共享同一数据源
// ══════════════════════════════════════════════════════════════════════════════

private val TAB_TITLES = listOf("工作台", "个人空间")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel, deviceViewModel: DeviceViewModel) {
    val pagerState     = rememberPagerState(initialPage = 0) { TAB_TITLES.size }
    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex by remember { derivedStateOf { pagerState.currentPage } }

    // 从 DataStore 驱动的 StateFlow 中收集用户昵称
    val userName by viewModel.userName.collectAsState()

    val isDangerMode by viewModel.uiState
        .map { it.isDangerModeActive }
        .collectAsState(initial = false)

    Column(modifier = Modifier.fillMaxSize()) {

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor   = MaterialTheme.colorScheme.background,
            contentColor     = MaterialTheme.colorScheme.primary,
            indicator        = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color    = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            TAB_TITLES.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick  = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text     = {
                        Text(
                            text       = title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold
                                         else FontWeight.Normal,
                            color      = if (selectedTabIndex == index)
                                             MaterialTheme.colorScheme.primary
                                         else
                                             MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state                = pagerState,
            modifier             = Modifier
                .fillMaxSize()
                .weight(1f),
            beyondBoundsPageCount = 1
        ) { page ->
            when (page) {
                // userName 实时从 DataStore 流读取，工作台始终与个人空间同步
                0 -> WorkspaceScreen(userName = userName, isDangerMode = isDangerMode)
                1 -> PersonalNexusScreen(viewModel = viewModel, deviceViewModel = deviceViewModel)
                else -> Unit
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Tab 0 — 工作台
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun WorkspaceScreen(userName: String, isDangerMode: Boolean = false) {
    AmbientHubScreen(userName = userName, isDangerMode = isDangerMode)
}

@Composable
fun AmbientHubScreen(userName: String, isDangerMode: Boolean = false) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val voiceManager = remember { SpeechRecognizerImpl(context) }
    val holder = remember {
        WorkspaceCardStateHolder(
            voiceManager = voiceManager,
            scope = scope,
            onExecuteCommand = { text ->
                android.util.Log.d("AmbientHub", "Execute command: $text")
            }
        )
    }
    DisposableEffect(Unit) { onDispose { holder.dispose() } }

    val phase        by holder.phase.collectAsState()
    val cardStack    by holder.cardStack.collectAsState()
    val currentIndex by holder.currentIndex.collectAsState()
    val draft        by holder.draftVoiceCard.collectAsState()
    val audioLevel   by holder.liveAudioLevel.collectAsState()

    // UI-owned animation values — never stored in the state holder
    val mainCardFlight = remember { Animatable(0f) }  // 0 = centre, 1 = stack slot
    val ghostAlpha     = remember { Animatable(0f) }

    LaunchedEffect(phase) {
        when (phase) {
            VoicePhase.Listening -> {
                if (!isDangerMode) {
                    launch {
                        mainCardFlight.animateTo(
                            1f, spring(dampingRatio = 0.8f, stiffness = 200f)
                        )
                    }
                }
                launch {
                    if (!isDangerMode) delay(150)
                    ghostAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
                }
            }
            VoicePhase.Dismissing -> {
                launch {
                    ghostAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                }
                if (!isDangerMode) {
                    mainCardFlight.animateTo(
                        0f, spring(dampingRatio = 0.8f, stiffness = 200f)
                    )
                }
                holder.onDismissComplete()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Pre-existing cards (hidden in danger mode) ─────────────────────────
        AnimatedVisibility(
            visible = !isDangerMode,
            enter = fadeIn(animationSpec = tween(500)),
            exit  = fadeOut(animationSpec = tween(500))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Next-up card (currentIndex + 1)
                if (currentIndex + 1 < cardStack.size) {
                    val transitionProgress = remember { Animatable(0f) }
                    val scale  = 0.28f + (0.72f * transitionProgress.value)
                    val tX     = 24f + (0f  - 24f) * transitionProgress.value
                    val tY     = 40f + (80f - 40f) * transitionProgress.value
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = tX.dp, y = tY.dp)
                                .graphicsLayer {
                                    scaleX = scale; scaleY = scale
                                    transformOrigin =
                                        androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                                }
                                .zIndex(20f)
                        ) {
                            SwipeCard(onSwipeLeft = {}, onSwipeRight = {}) {
                                ActionCardContent(cardStack[currentIndex + 1])
                            }
                        }
                    }
                }

                // CardStack pile (currentIndex + 2 and beyond)
                if (currentIndex + 2 < cardStack.size) {
                    Box(
                        modifier = Modifier
                            .padding(top = 40.dp, start = 24.dp)
                            .align(Alignment.TopStart)
                    ) {
                        CardStack(
                            remainingCount = cardStack.size - currentIndex - 2,
                            nextCard       = cardStack.getOrNull(currentIndex + 2)
                        )
                    }
                }
            }
        }

        // ── Danger mode overlay ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = isDangerMode,
            enter = fadeIn(animationSpec = tween(500)),
            exit  = fadeOut(animationSpec = tween(500))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 280.dp),
                contentAlignment = Alignment.Center
            ) {
                DangerModeOverlay()
            }
        }

        // ── Main card — spring flight between centre and stack slot ───────────
        if (currentIndex < cardStack.size) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                with(density) {
                    val screenW = constraints.maxWidth.toFloat()
                    val screenH = constraints.maxHeight.toFloat()
                    val cardW   = screenW * 0.75f
                    val cardH   = 380.dp.toPx()
                    val topPad  = 80.dp.toPx()
                    val stackX  = 24.dp.toPx()
                    val stackY  = 40.dp.toPx()

                    val centreX = (screenW - cardW) / 2f
                    val centreY = topPad + (screenH - topPad - cardH) / 2f

                    val p     = mainCardFlight.value
                    val curX  = lerp(centreX, stackX, p)
                    val curY  = lerp(centreY, stackY, p)
                    val scale = lerp(1f, 0.28f, p)

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(curX.roundToInt(), curY.roundToInt()) }
                            .graphicsLayer {
                                scaleX = scale; scaleY = scale
                                transformOrigin =
                                    androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                            }
                            .zIndex(if (mainCardFlight.isRunning) 100f else 30f)
                    ) {
                        key(currentIndex) {
                            if (phase == VoicePhase.Idle) {
                                SwipeCard(
                                    onSwipeLeft  = { holder.onSwipeCurrentCard() },
                                    onSwipeRight = { holder.onSwipeCurrentCard() }
                                ) {
                                    ActionCardContent(cardStack[currentIndex])
                                }
                            } else {
                                ActionCardContent(cardStack[currentIndex])
                            }
                        }
                    }
                }
            }
        }

        // ── Ghost voice card ─────────────────────────────────────────────────
        if (draft != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
                    .graphicsLayer { alpha = ghostAlpha.value }
                    .zIndex(50f),
                contentAlignment = Alignment.Center
            ) {
                if (phase == VoicePhase.Editable || phase == VoicePhase.Error) {
                    SwipeCard(
                        onSwipeLeft  = { holder.onDraftSwipedLeft() },
                        onSwipeRight = { holder.onDraftSwipedRight() }
                    ) {
                        GhostVoiceCard(
                            draft          = draft!!,
                            liveAudioLevel = audioLevel,
                            isDangerMode   = isDangerMode,
                            onTextChange   = holder::onDraftTextEdited
                        )
                    }
                } else {
                    GhostVoiceCard(
                        draft          = draft!!,
                        liveAudioLevel = audioLevel,
                        isDangerMode   = isDangerMode,
                        onTextChange   = holder::onDraftTextEdited
                    )
                }
            }
        }

        // ── AuroraWaveHalo ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            AuroraWaveHalo(
                audioLevel   = audioLevel,
                isListening  = phase in setOf(VoicePhase.Listening, VoicePhase.Editable),
                onPressStart = { holder.onHaloPressStart() },
                onPressEnd   = { holder.onHaloPressEnd() },
                isDangerMode = isDangerMode
            )
        }

        // ── Username (top-right) ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 24.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = userName,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color      = GlassBorder
                )
                Text(
                    text          = "的工作台",
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Normal,
                    color         = GlassBorder.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
