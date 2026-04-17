# 工作台语音转写与卡片流转动效设计文档

**日期**：2026-04-17  
**状态**：已审阅，待实现  
**涉及模块**：WorkspaceScreen / AmbientHubScreen、语音识别、卡片动效

---

## 一、背景与目标

在现有工作台（`AmbientHubScreen`）的基础上，新增"按住说话"的语音输入能力，将识别结果转化为可编辑、可滑动的命令卡片（Ghost Card），并与现有的卡片堆栈滑动流程无缝衔接。

**核心体验目标**：按下即清场、松手即定型、说错可编辑、一滑即执行。

---

## 二、整体架构

```
AmbientHubScreen
├── remember { WorkspaceCardStateHolder(voiceManager, scope, isDangerMode) }
│
├── VoiceRecognitionManager (interface)
│   ├── SpeechRecognizerImpl          ← 生产：Android SpeechRecognizer
│   └── MockVoiceRecognitionManager   ← 测试 / Compose Preview
│
└── UI 渲染层（全部订阅 stateHolder 的 StateFlow）
    ├── Layer 0  背景
    ├── Layer 20 CardStack pile（小堆叠剪影）
    ├── Layer 30 next-up 槽位卡片（主卡落位后静止）
    ├── Layer 50 GhostVoiceCard / DangerVoiceGhost
    ├── Layer 100 飞行中的主卡（动画期间临时置顶）
    └── Layer 10 AuroraWaveHalo（底部）
```

**核心原则**：
- `WorkspaceCardStateHolder` 只发号施令（目标状态），不感知动画时长
- 动画进度值（Animatable）全部在 Compose UI 层持有和驱动
- `cardStack` 数组全程不变；语音草稿以独立 `draftVoiceCard` 字段叠加渲染

---

## 三、新建文件

### 3.1 `voice/VoiceRecognitionManager.kt`

```kotlin
interface VoiceRecognitionManager {
    val partialText: StateFlow<String>
    val finalText: StateFlow<String?>
    val rmsDb: StateFlow<Float>           // 归一化 0f..1f，驱动 halo 波形
    val state: StateFlow<RecognitionState>
    val errorCode: StateFlow<RecognitionError?>

    fun startListening(locale: String = "zh-CN")
    fun stopListening()
    fun cancel()
    fun release()
}

sealed class RecognitionState {
    object Idle : RecognitionState()
    object Preparing : RecognitionState()
    object Listening : RecognitionState()
    object Finalizing : RecognitionState()
    object Denied : RecognitionState()
    data class Error(val code: RecognitionError) : RecognitionState()
}

enum class RecognitionError {
    NETWORK, TIMEOUT, NO_MATCH, UNAVAILABLE, OTHER
}
```

### 3.2 `voice/SpeechRecognizerImpl.kt`

- 封装 `android.speech.SpeechRecognizer`
- 所有 API 调用须在主线程（`withContext(Dispatchers.Main)`）
- `onRmsChanged(rmsdB)` → 归一化公式：`((rmsdB + 2f) / 12f).coerceIn(0f, 1f)`
- 前置检查：`SpeechRecognizer.isRecognitionAvailable(context)`，不可用时 emit `Denied`
- `EXTRA_LANGUAGE = "zh-CN"`，`EXTRA_PARTIAL_RESULTS = true`

**错误码分类**：

| SpeechRecognizer 错误码 | 归类 | 处理路径 |
|---|---|---|
| `ERROR_INSUFFICIENT_PERMISSIONS` (9) | Denied | Toast + 回 Idle |
| `ERROR_NO_MATCH` (7) | Silent | → Dismissing（无红字） |
| `ERROR_SPEECH_TIMEOUT` (6) | Silent | → Dismissing（无红字） |
| `ERROR_NETWORK` (2) | Visible | → Error（红字） |
| `ERROR_NETWORK_TIMEOUT` (1) | Visible | → Error |
| `ERROR_SERVER` (4) | Visible | → Error |
| `ERROR_AUDIO` (3) | Visible | → Error |
| `ERROR_CLIENT` (5) | Visible | → Error |
| `ERROR_RECOGNIZER_BUSY` (8) | Visible | → Error |

### 3.3 `voice/MockVoiceRecognitionManager.kt`

```kotlin
enum class MockScript { Success, Empty, NetworkError }

class MockVoiceRecognitionManager(
    private val script: MockScript = MockScript.Success
) : VoiceRecognitionManager {
    // Success：500ms 后开始按 60ms/字吐 partialText，
    //          3s 后 emit finalText = "提醒张三下午三点开会"
    // Empty：  1s 后 emit finalText = ""
    // NetworkError：800ms 后 emit RecognitionError.NETWORK
}
```

### 3.4 `data/DraftVoiceCard.kt`

```kotlin
data class DraftVoiceCard(
    val partialText: String = "",
    val finalText: String? = null,      // null = 仍在识别；非空 = 已定型
    val errorMessage: String? = null,   // 非空 = 识别失败
    val createdAt: Long = System.currentTimeMillis()
)
```

UI 层通过 null-性判断显示态：
- `finalText == null && errorMessage == null` → 打字机显示 `partialText`
- `finalText != null` → `BasicTextField` 可编辑
- `errorMessage != null` → 顶部红字闪现

### 3.5 `workspace/WorkspaceCardStateHolder.kt`

```kotlin
class WorkspaceCardStateHolder(
    private val voiceManager: VoiceRecognitionManager,
    private val scope: CoroutineScope,
    private val isDangerMode: StateFlow<Boolean>,
    initialCards: List<ActionCard> = MockData.getCardList()
) {
    // 公开状态
    val cardStack: StateFlow<List<ActionCard>>
    val currentIndex: StateFlow<Int>
    val draftVoiceCard: StateFlow<DraftVoiceCard?>
    val phase: StateFlow<VoicePhase>
    val liveAudioLevel: StateFlow<Float>   // = voiceManager.rmsDb

    // 事件入口
    fun onHaloPressStart()
    fun onHaloPressEnd()
    fun onDraftSwipedRight()              // executeCommand() + phase → Dismissing
    fun onDraftSwipedLeft()               // 丢弃 + phase → Dismissing
    fun onDraftTextEdited(newText: String)
    fun onSwipeCurrentCard()
    fun onDismissComplete()               // UI 动画结束后回调，phase → Idle

    fun dispose() { voiceManager.release() }
}

enum class VoicePhase {
    Idle,        // 无语音交互
    Listening,   // Ghost 在中心，partialText 实时刷新
    Editable,    // Ghost 已定型，BasicTextField 可用，可左/右滑
    Dismissing,  // Ghost 淡出，主卡倒放回中心
    Error        // 识别失败，红字闪现，1.5s 后自动 → Dismissing
}
```

**危险模式 fork（在 `onHaloPressEnd` 内）**：

```
普通模式：finalText 非空 → phase = Editable
危险模式：finalText 非空 → phase = Editable（行为路径相同）
          视觉差异：GhostVoiceCard 接收 isDangerMode=true，切换红色配色
```

> 预置卡片（飞书/日历）在危险模式下的直接执行行为由现有
> `AnimatedVisibility(!isDangerMode)` 处理，不在本次范围内。

### 3.6 `ui/components/GhostVoiceCard.kt`

```kotlin
@Composable
fun GhostVoiceCard(
    draft: DraftVoiceCard,
    liveAudioLevel: Float,
    isDangerMode: Boolean = false,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

**视觉规格**：

| 元素 | 普通模式 | 危险模式 |
|---|---|---|
| 尺寸 | `fillMaxWidth(0.75f) × height(380.dp)` | 同左 |
| 圆角 | `RoundedCornerShape(24.dp)` | 同左 |
| 底色 | `Color(0xFFFFFFFF).copy(alpha = 0.02f)` | `Color(0xFFFF0000).copy(alpha = 0.04f)` |
| 流光边框宽度 | `2.dp` | `2.dp` |
| 流光色序 | DeepSeaBlue→Cyan→LightGreen→NeonPurple | `0xFF8B0000`→`0xFFFF0000`→`0xFFFF4444`→`0xFFCC0022` |
| 流光周期 | 3000ms Linear 无限循环 | 2500ms（略快，增加紧迫感） |
| 外光晕 | Cyan alpha 0.22，blur(12.dp) | Red alpha 0.28，blur(12.dp) |
| 光晕呼吸 | 2s Reverse EaseInOut | 1.4s Reverse（更快） |
| 顶部标签 | "飞书 · 语音输入中" / "飞书" | "⚠ 危险模式 · 语音指令" |
| 底部注释 | "发送给：团队频道" | "确认后自动执行" |
| 文字颜色 | GlassBorder | `Color(0xFFFFBBBB)` |
| 光标颜色 | NeonPurple | `Color(0xFFFF4444)` |

**错误态**（`draft.errorMessage != null`）：

顶部红字"识别失败"，keyframes：`0f at 0ms → 1f at 120ms → 1f at 720ms → 0.5f at 900ms`

**松手高光闪烁**（phase: Listening → Editable 时触发一次）：

白色遮罩 alpha `0 → 1 → 0`，总时长 500ms（`tween(200, EaseOutQuad)` + `tween(300, EaseInQuad)`）

---

## 四、修改文件

### 4.1 `MainActivity.kt`（`AmbientHubScreen` 函数）

**改动要点**：

1. 初始化 `voiceManager` 和 `stateHolder`：

```kotlin
val context = LocalContext.current
val scope = rememberCoroutineScope()
val voiceManager = remember { SpeechRecognizerImpl(context) }
val isDangerModeFlow = viewModel.uiState.map { it.isDangerModeActive }
    .stateIn(scope, SharingStarted.WhileSubscribed(), false)
val holder = remember { WorkspaceCardStateHolder(voiceManager, scope, isDangerModeFlow) }
DisposableEffect(Unit) { onDispose { holder.dispose() } }
```

2. 收集状态：

```kotlin
val phase by holder.phase.collectAsState()
val cardStack by holder.cardStack.collectAsState()
val currentIndex by holder.currentIndex.collectAsState()
val draft by holder.draftVoiceCard.collectAsState()
val audioLevel by holder.liveAudioLevel.collectAsState()
val isDangerMode by isDangerModeFlow.collectAsState(false)
```

3. UI 层持有的 Animatable（不在 stateHolder 里）：

```kotlin
val mainCardFlight = remember { Animatable(0f) }   // 0=中心，1=next-up 槽位
val ghostAlpha = remember { Animatable(0f) }

LaunchedEffect(phase) {
    when (phase) {
        VoicePhase.Listening -> {
            launch { mainCardFlight.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 200f)) }
            launch {
                delay(150)
                ghostAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
            }
        }
        VoicePhase.Dismissing -> {
            launch { ghostAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
            mainCardFlight.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 200f))
            holder.onDismissComplete()
        }
        else -> Unit
    }
}
```

4. 主卡插值（用 `mainCardFlight.value` 驱动位移 + 缩放）：

```kotlin
// 目标位置：(24dp, 40dp)，scale 0.28，transformOrigin=(0,0)
val p = mainCardFlight.value
Box(
    modifier = Modifier
        .offset(x = lerp(0f, 24f, p).dp, y = lerp(80f, 40f, p).dp)
        .graphicsLayer {
            scaleX = lerp(1f, 0.28f, p)
            scaleY = lerp(1f, 0.28f, p)
            transformOrigin = TransformOrigin(0f, 0f)
        }
        .zIndex(if (mainCardFlight.isRunning) 100f else 30f)
) {
    SwipeCard(onSwipeLeft = { holder.onSwipeCurrentCard() },
              onSwipeRight = { holder.onSwipeCurrentCard() }) {
        ActionCardContent(cardStack[currentIndex])
    }
}
```

5. Ghost 层（在 `isDangerMode` AnimatedVisibility 之外，独立渲染）：

```kotlin
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
                onSwipeLeft = { holder.onDraftSwipedLeft() },
                onSwipeRight = { holder.onDraftSwipedRight() }
            ) {
                GhostVoiceCard(draft!!, audioLevel, isDangerMode, holder::onDraftTextEdited)
            }
        } else {
            GhostVoiceCard(draft!!, audioLevel, isDangerMode, holder::onDraftTextEdited)
        }
    }
}
```

### 4.2 `ui/components/AuroraWaveHalo.kt`

**新签名**：

```kotlin
@Composable
fun AuroraWaveHalo(
    audioLevel: Float,
    isListening: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    isDangerMode: Boolean = false
)
```

**改动**：
- 删除内部 `AudioCaptureManager` 实例化和 `permissionLauncher`（权限改由 `SpeechRecognizerImpl` 管理）
- `isActive` 替换为参数 `isListening`，`morphProgress` 目标值由 `isListening` 驱动
- `detectTapGestures` 改为：

```kotlin
detectTapGestures(
    onPress = {
        try {
            onPressStart()
            tryAwaitRelease()
        } finally {
            onPressEnd()   // 手势取消也能触发
        }
    }
)
```

### 4.3 `ui/components/SwipeCard.kt`

无逻辑改动。确认其 `onSwipeLeft` / `onSwipeRight` 回调在 Editable 阶段正确接入 `holder.onDraftSwipedLeft/Right()`（由调用方传入，无需修改组件本身）。

### 4.4 `AndroidManifest.xml`

核查 `RECORD_AUDIO` 已声明（现有已有），无需新增。

---

## 五、状态机完整转移表

| From | Event / 条件 | To | StateHolder 动作 |
|---|---|---|---|
| Idle | `onHaloPressStart()` | Listening | `voiceManager.startListening("zh-CN")`；创建空 `DraftVoiceCard` |
| Listening | `partialText` emit | Listening | 更新 `draftVoiceCard.partialText` |
| Listening | `onHaloPressEnd()` + finalText 非空 | Editable | `draftVoiceCard.finalText = finalText` |
| Listening | `onHaloPressEnd()` + 空结果 / `NO_MATCH` / `SPEECH_TIMEOUT` | Dismissing | 清空草稿 |
| Listening | `voiceManager.error`（Visible 类） | Error | `draftVoiceCard.errorMessage = "识别失败"` |
| Listening | `voiceManager.error`（Denied） | Idle | Toast "请授予麦克风权限" |
| Error | 进入后 1.5s | Dismissing | 自动转移 |
| Editable | `onDraftSwipedRight()` | Dismissing | `executeCommand(finalText)`（本期实现：Log + Toast 展示文本；后续接入飞书发送 API） |
| Editable | `onDraftSwipedLeft()` | Dismissing | 纯丢弃 |
| Editable | `onDraftTextEdited(newText)` | Editable | 更新 `draftVoiceCard.finalText` |
| Dismissing | `onDismissComplete()`（UI 回调） | Idle | `draftVoiceCard = null` |

---

## 六、动画参数汇总

| 动画 | API | 参数 | 备注 |
|---|---|---|---|
| 主卡弹射（去程） | `spring` | `dampingRatio=0.8f, stiffness=200f` | ~350ms |
| 主卡倒放（回程） | 同上 | 同上 | 同一 `Animatable` 反向 |
| Ghost 淡入 | `tween` | `400ms, FastOutSlowInEasing` | 延迟 150ms 启动（交错相遇） |
| Ghost 淡出 | `tween` | `300ms, FastOutSlowInEasing` | 与主卡回程并行 |
| 流光边框循环 | `rememberInfiniteTransition` | `3000ms Linear`（危险 2500ms） | 仅 Listening 时运行 |
| 松手高光闪烁 | `keyframes` | `0→1(200ms EaseOutQuad)→0(300ms EaseInQuad)` | Listening→Editable 触发一次 |
| Error 红字 | `keyframes` | `0f@0ms, 1f@120ms, 1f@720ms, 0.5f@900ms` | phase=Error 时触发 |
| 外光晕呼吸 | `infiniteRepeatable` | `2000ms EaseInOut Reverse`（危险 1400ms） | 常驻 |
| Z-Index 飞行期 | `Modifier.zIndex(100f)` | — | `mainCardFlight.isRunning` 时生效 |

---

## 七、Z-Index 层级

```
100  飞行中的主卡（mainCardFlight.isRunning 时临时置顶）
 50  Ghost 层 Box（内部含 GhostVoiceCard，Editable 时被 SwipeCard 包裹；
     SwipeCard 作为 Box 的子节点，不需要独立 zIndex）
 30  next-up 槽位的主卡（落位后静止，mainCardFlight.isRunning=false 时）
 20  CardStack pile（CardStack 组件）
 10  AuroraWaveHalo
  0  背景
```

---

## 八、危险模式完整矩阵

| 卡片来源 | 普通模式 | 危险模式 |
|---|---|---|
| 预置卡片（飞书/日历，`cardStack`） | 居中展示，手动左/右滑 | 隐藏（现有 `AnimatedVisibility` 处理），DangerModeOverlay 覆盖 |
| 语音 Ghost 卡（`draftVoiceCard`） | 蓝/青流光边框，Editable 可编辑/滑动 | **红色流光边框**，同样保留 Editable，可二次修改后滑动确认 |

> Ghost 卡在 `AnimatedVisibility(!isDangerMode)` 之外独立渲染，危险模式下自然可见。

---

## 九、测试策略

### 单元测试（`WorkspaceCardStateHolderTest.kt`）

使用 Turbine 收集 `StateFlow`，注入 `MockVoiceRecognitionManager`：

| 测试用例 | Mock 脚本 | 断言 |
|---|---|---|
| Happy Path | Success | phase: Idle→Listening→Editable；finalText 非空 |
| 空识别倒放 | Empty | phase: Idle→Listening→Dismissing；cardStack 不变 |
| 网络错误 | NetworkError | phase: Listening→Error→（1.5s）→Dismissing |
| 右滑确认 | Success + swipeRight | `executeCommand` 被调；phase→Dismissing |
| 左滑作废 | Success + swipeLeft | `executeCommand` 不被调；phase→Dismissing |
| 危险模式颜色 | Success + isDangerMode=true | phase 路径不变；UI 侧 `isDangerMode` 为 true |
| Dismiss 回 Idle | 任意 + `onDismissComplete()` | phase→Idle；draftVoiceCard=null |

### Compose Preview

```kotlin
// 4 个关键 Preview
GhostVoiceCard_Listening_Normal
GhostVoiceCard_Editable_Normal
GhostVoiceCard_Listening_Danger
GhostVoiceCard_Error
```

---

## 十、关键风险

| 风险 | 缓解措施 |
|---|---|
| 国内定制 ROM 无 `SpeechRecognizer` | `isRecognitionAvailable()` 前置检查，不可用→Denied→Toast |
| 部分机型依赖 Google 服务（中文识别） | 同上 |
| `onRmsChanged` dB 范围设备差异大 | 归一化：`((rmsdB + 2f) / 12f).coerceIn(0f, 1f)` |
| `SpeechRecognizer` 必须主线程调用 | `withContext(Dispatchers.Main)` 包裹所有 API 调用 |
| `tryAwaitRelease()` 因手势冲突中断 | `onPressEnd` 在 `finally` 块里调用 |
