# 危险模式（Danger Mode）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在个人空间区域四新增"危险模式"开关，开启后全局边缘泛红、工作台卡片隐藏并显示发光大字、AuroraWaveHalo 充血变红。

**Architecture:** `isDangerModeActive` 存于 `PersonalSpaceUiState`（纯内存，不持久化），从 `MainViewModel` 向下传递至根容器（边缘红晕）和 `WorkspaceScreen`（内容切换）。`hasSeenDangerWarning` 通过 DataStore 持久化，仅控制首次警告弹窗是否出现。

**Tech Stack:** Kotlin、Jetpack Compose、DataStore Preferences、`animateColorAsState`、`animateFloatAsState`、`infiniteRepeatable`、`Modifier.drawWithContent`、`AnimatedVisibility`。

---

## 文件变更总览

| 操作 | 路径 |
|------|------|
| 修改 | `app/src/main/java/com/aether/app/data/UserPreferencesRepository.kt` |
| 修改 | `app/src/main/java/com/aether/app/MainViewModel.kt` |
| 新建 | `app/src/main/java/com/aether/app/ui/components/DangerModeCard.kt` |
| 修改 | `app/src/main/java/com/aether/app/ui/components/AuroraWaveHalo.kt` |
| 修改 | `app/src/main/java/com/aether/app/ui/screens/PersonalNexusScreen.kt` |
| 修改 | `app/src/main/java/com/aether/app/MainActivity.kt` |
| 修改 | `app/build.gradle.kts` |
| 新建 | `app/src/test/java/com/aether/app/MainViewModelDangerModeTest.kt` |

---

## Task 1：UserPreferencesRepository — 添加 hasSeenDangerWarning

**Files:**
- Modify: `app/src/main/java/com/aether/app/data/UserPreferencesRepository.kt`

- [ ] **Step 1：添加 DataStore Key 与 Flow**

在 `companion object` 中新增 key，并在类体中新增对应 Flow：

```kotlin
// companion object 内，紧接 KEY_API_CONFIGS_JSON 后
private val KEY_HAS_SEEN_DANGER_WARNING = booleanPreferencesKey("has_seen_danger_warning")
```

```kotlin
// 类体内，紧接 apiConfigsJson 属性后
val hasSeenDangerWarning: Flow<Boolean> = context.dataStore.data
    .map { prefs -> prefs[KEY_HAS_SEEN_DANGER_WARNING] ?: false }
```

- [ ] **Step 2：添加写入函数**

```kotlin
// 类体内，紧接 saveApiConfigs() 后
suspend fun saveHasSeenDangerWarning(value: Boolean) {
    context.dataStore.edit { it[KEY_HAS_SEEN_DANGER_WARNING] = value }
}
```

- [ ] **Step 3：编译验证**

```bash
./gradlew compileDebugKotlin
```

期望：BUILD SUCCESSFUL，无报错。

- [ ] **Step 4：Commit**

```bash
git add app/src/main/java/com/aether/app/data/UserPreferencesRepository.kt
git commit -m "feat: add hasSeenDangerWarning to DataStore"
```

---

## Task 2：MainViewModel — 危险模式状态与逻辑

**Files:**
- Modify: `app/src/main/java/com/aether/app/MainViewModel.kt`
- Modify: `app/build.gradle.kts`
- Create: `app/src/test/java/com/aether/app/MainViewModelDangerModeTest.kt`

- [ ] **Step 1：新增测试依赖**

在 `app/build.gradle.kts` 的 `dependencies {}` 块末尾追加：

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("org.robolectric:robolectric:4.11.1")
testImplementation("app.cash.turbine:turbine:1.0.0")
```

同时在 `android {}` 块内的 `defaultConfig {}` 内追加（Robolectric 需要）：

```kotlin
testOptions {
    unitTests {
        isIncludeAndroidResources = true
    }
}
```

- [ ] **Step 2：写失败测试**

创建 `app/src/test/java/com/aether/app/MainViewModelDangerModeTest.kt`：

```kotlin
package com.aether.app

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.aether.app.data.UserPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelDangerModeTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: UserPreferencesRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = UserPreferencesRepository(ApplicationProvider.getApplicationContext())
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleDangerMode when hasSeenWarning is false shows dialog`() = runTest {
        // hasSeenDangerWarning 初始为 false（DataStore 无记录）
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isDangerModeActive)
            assertFalse(initial.showDangerWarningDialog)

            viewModel.toggleDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val afterToggle = awaitItem()
            assertFalse(afterToggle.isDangerModeActive)
            assertTrue(afterToggle.showDangerWarningDialog)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmDangerMode activates mode and dismisses dialog`() = runTest {
        viewModel.toggleDangerMode()  // 先触发弹窗
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1) // 消费弹窗状态
            viewModel.confirmDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.isDangerModeActive)
            assertFalse(state.showDangerWarningDialog)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDangerMode when hasSeenWarning is true activates directly`() = runTest {
        // 先 confirm 一次，写入 hasSeenDangerWarning = true
        viewModel.toggleDangerMode()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDangerMode()
        dispatcher.scheduler.advanceUntilIdle()
        // 关闭危险模式
        viewModel.dismissDangerMode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)
            // 第二次 toggle，hasSeenWarning 已为 true，应直接激活
            viewModel.toggleDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.isDangerModeActive)
            assertFalse(state.showDangerWarningDialog)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissDangerMode deactivates mode`() = runTest {
        viewModel.toggleDangerMode()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmDangerMode()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.dismissDangerMode()
            dispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isDangerModeActive)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 3：运行测试，确认全部失败**

```bash
./gradlew testDebugUnitTest --tests "com.aether.app.MainViewModelDangerModeTest"
```

期望：4 个测试全部 FAIL（函数尚未存在）。

- [ ] **Step 4：在 `PersonalSpaceUiState` 新增字段**

在 `MainViewModel.kt` 的 `PersonalSpaceUiState` 数据类中，在 `activeApiId` 字段后追加：

```kotlin
// ── 危险模式 ──────────────────────────────────────────────────────────────────
val isDangerModeActive: Boolean = false,
val showDangerWarningDialog: Boolean = false,
```

- [ ] **Step 5：在 `MainViewModel` 中新增 `hasSeenDangerWarning` StateFlow**

在 `avatarUriString` StateFlow 定义后追加：

```kotlin
/** 是否已看过危险模式警告（持久化）；初始值 false 为安全默认 */
val hasSeenDangerWarning: StateFlow<Boolean> = repository.hasSeenDangerWarning.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = false
)
```

- [ ] **Step 6：新增三个危险模式函数**

在 `saveUserName()` 函数后追加：

```kotlin
// ── 危险模式 ──────────────────────────────────────────────────────────────────

/**
 * Switch 点击入口：
 *   - 若已激活 → 关闭（同 dismissDangerMode）
 *   - 若未看过警告 → 显示弹窗
 *   - 若已看过警告 → 直接激活
 */
fun toggleDangerMode() {
    if (_uiState.value.isDangerModeActive) {
        dismissDangerMode()
        return
    }
    if (hasSeenDangerWarning.value) {
        _uiState.update { it.copy(isDangerModeActive = true) }
    } else {
        _uiState.update { it.copy(showDangerWarningDialog = true) }
    }
}

/** 用户在警告弹窗中点击"确认并开启" */
fun confirmDangerMode() {
    viewModelScope.launch {
        repository.saveHasSeenDangerWarning(true)
    }
    _uiState.update {
        it.copy(isDangerModeActive = true, showDangerWarningDialog = false)
    }
}

/** 关闭危险模式（Switch OFF 或弹窗取消） */
fun dismissDangerMode() {
    _uiState.update {
        it.copy(isDangerModeActive = false, showDangerWarningDialog = false)
    }
}
```

- [ ] **Step 7：运行测试，确认全部通过**

```bash
./gradlew testDebugUnitTest --tests "com.aether.app.MainViewModelDangerModeTest"
```

期望：4 个测试全部 PASS。

- [ ] **Step 8：Commit**

```bash
git add app/build.gradle.kts \
        app/src/main/java/com/aether/app/MainViewModel.kt \
        app/src/test/java/com/aether/app/MainViewModelDangerModeTest.kt
git commit -m "feat: add danger mode state and logic to MainViewModel"
```

---

## Task 3：DangerModeCard.kt — 新建组件文件

**Files:**
- Create: `app/src/main/java/com/aether/app/ui/components/DangerModeCard.kt`

- [ ] **Step 1：创建文件，写入三个 Composable**

```kotlin
package com.aether.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ══════════════════════════════════════════════════════════════════════════════
//  区域四 — 危险模式开关卡片
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun DangerModeCard(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBackground by animateColorAsState(
        targetValue = if (isActive) Color(0xFFFFEEEE) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(600),
        label = "danger_card_bg"
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Surface(color = cardBackground) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "危险模式",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFFCC0000)
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isActive) "⚡ 直接执行中" else "开启后将直接执行",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) Color(0xFFCC6666)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF8B0000),
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  首次开启警告弹窗
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun DangerWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "⚠️", fontSize = 32.sp)
                Text(
                    text = "警告",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4444)
                )
                Text(
                    text = "开启后，系统将直接发送消息或修改数据。您将放弃最终干预权，请谨慎评估当前场景的风险。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消", color = Color(0xFF888888))
                    }
                    FilledTonalButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF8B0000),
                            contentColor = Color.White
                        )
                    ) {
                        Text("确认并开启", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  工作台中央覆盖层 — "危险模式"呼吸大字
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun DangerModeOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "danger_overlay")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "danger_text_alpha"
    )
    Text(
        text = "危险模式",
        fontSize = 40.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFFF2222).copy(alpha = alpha),
        letterSpacing = 10.sp,
        style = TextStyle(
            shadow = Shadow(
                color = Color.Red.copy(alpha = 0.8f),
                blurRadius = 20f
            )
        )
    )
}
```

- [ ] **Step 2：编译验证**

```bash
./gradlew compileDebugKotlin
```

期望：BUILD SUCCESSFUL。

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/aether/app/ui/components/DangerModeCard.kt
git commit -m "feat: add DangerModeCard, DangerWarningDialog, DangerModeOverlay composables"
```

---

## Task 4：AuroraWaveHalo — 危险模式颜色动画

**Files:**
- Modify: `app/src/main/java/com/aether/app/ui/components/AuroraWaveHalo.kt`

- [ ] **Step 1：新增 `isDangerMode` 参数并动画化三个颜色**

将函数签名从：
```kotlin
fun AuroraWaveHalo(
    audioLevel: Float = 0f,
    onLongPress: () -> Unit = {}
)
```
改为：
```kotlin
fun AuroraWaveHalo(
    audioLevel: Float = 0f,
    onLongPress: () -> Unit = {},
    isDangerMode: Boolean = false
)
```

在函数体内，找到三个硬编码颜色的定义（位于 `Canvas` 的 `drawScope` lambda 内）：

```kotlin
val deepBlue = Color(0xFF0066FF)
val cyan = Color(0xFF00DDFF)
val lightGreen = Color(0xFF88FFDD)
```

将这三行**删除**，并在 `Box(...)` 之前（`Canvas` 之外）改为动画颜色：

```kotlin
val deepBlue by animateColorAsState(
    targetValue = if (isDangerMode) Color(0xFF8B0000) else Color(0xFF0066FF),
    animationSpec = tween(800),
    label = "halo_deep"
)
val cyan by animateColorAsState(
    targetValue = if (isDangerMode) Color(0xFFFF0000) else Color(0xFF00DDFF),
    animationSpec = tween(800),
    label = "halo_cyan"
)
val lightGreen by animateColorAsState(
    targetValue = if (isDangerMode) Color(0xFFFF4444) else Color(0xFF88FFDD),
    animationSpec = tween(800),
    label = "halo_light"
)
```

同时在文件顶部的 import 列表中补充（如果尚未存在）：
```kotlin
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Color
```

- [ ] **Step 2：编译验证**

```bash
./gradlew compileDebugKotlin
```

期望：BUILD SUCCESSFUL。

- [ ] **Step 3：Commit**

```bash
git add app/src/main/java/com/aether/app/ui/components/AuroraWaveHalo.kt
git commit -m "feat: AuroraWaveHalo animates to red palette in danger mode"
```

---

## Task 5：PersonalNexusScreen — 接入 DangerModeCard

**Files:**
- Modify: `app/src/main/java/com/aether/app/ui/screens/PersonalNexusScreen.kt`

- [ ] **Step 1：在 `PersonalNexusContent` 函数签名中新增三个回调参数**

当前签名末尾有 `onNavigateToApiSelection: () -> Unit`，在其后追加：

```kotlin
onToggleDangerMode: () -> Unit,
onConfirmDangerMode: () -> Unit,
onDismissDangerWarning: () -> Unit,
```

- [ ] **Step 2：替换区域四占位符**

找到：
```kotlin
SectionPlaceholder(label = "区域 4", minHeight = 112)
```
替换为：
```kotlin
DangerModeCard(
    isActive = uiState.isDangerModeActive,
    onToggle = onToggleDangerMode
)
```

- [ ] **Step 3：在 `Box` 末尾添加警告弹窗**

在 `PersonalNexusContent` 的根 `Box` 内，紧接 `EditNameDialog` 的 `if` 块之后追加：

```kotlin
if (uiState.showDangerWarningDialog) {
    DangerWarningDialog(
        onConfirm = onConfirmDangerMode,
        onDismiss = onDismissDangerWarning
    )
}
```

- [ ] **Step 4：在调用处 `PersonalNexusScreen` 中补充实参**

找到 `PersonalNexusContent(...)` 的调用（在 `PersonalNexusScreen` 函数内），在 `onNavigateToApiSelection = ...` 之后追加：

```kotlin
onToggleDangerMode   = viewModel::toggleDangerMode,
onConfirmDangerMode  = viewModel::confirmDangerMode,
onDismissDangerWarning = viewModel::dismissDangerMode,
```

- [ ] **Step 5：补充 import**

在文件顶部的 import 区域补充：

```kotlin
import com.aether.app.ui.components.DangerModeCard
import com.aether.app.ui.components.DangerWarningDialog
```

- [ ] **Step 6：编译验证**

```bash
./gradlew compileDebugKotlin
```

期望：BUILD SUCCESSFUL。

- [ ] **Step 7：Commit**

```bash
git add app/src/main/java/com/aether/app/ui/screens/PersonalNexusScreen.kt
git commit -m "feat: replace zone 4 placeholder with DangerModeCard"
```

---

## Task 6：MainActivity — 全局联动（边缘红晕 + 工作台切换）

**Files:**
- Modify: `app/src/main/java/com/aether/app/MainActivity.kt`

- [ ] **Step 1：在 `AetherApp` 中收集危险模式状态并挂载边缘红晕 Modifier**

找到 `AetherApp` 函数，在 `var showAwakening` 声明后追加：

```kotlin
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
```

将根 `Box` 的 `modifier` 从：
```kotlin
modifier = Modifier
    .fillMaxSize()
    .background(PureWhite)
```
改为：
```kotlin
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
```

- [ ] **Step 2：在 `MainScreen` 中收集 `isDangerMode` 并传入 `WorkspaceScreen`**

在 `MainScreen` 函数内，找到 `val userName by viewModel.userName.collectAsState()` 这行，在其后追加：

```kotlin
val isDangerMode by viewModel.uiState
    .map { it.isDangerModeActive }
    .collectAsState(initial = false)
```

找到 `0 -> WorkspaceScreen(userName = userName)` 这行，改为：

```kotlin
0 -> WorkspaceScreen(userName = userName, isDangerMode = isDangerMode)
```

- [ ] **Step 3：更新 `WorkspaceScreen` 和 `AmbientHubScreen` 签名**

将：
```kotlin
fun WorkspaceScreen(userName: String) {
    AmbientHubScreen(userName = userName)
}
```
改为：
```kotlin
fun WorkspaceScreen(userName: String, isDangerMode: Boolean = false) {
    AmbientHubScreen(userName = userName, isDangerMode = isDangerMode)
}
```

将：
```kotlin
fun AmbientHubScreen(userName: String) {
```
改为：
```kotlin
fun AmbientHubScreen(userName: String, isDangerMode: Boolean = false) {
```

- [ ] **Step 4：在 `AmbientHubScreen` 的根 `Box` 内用 `AnimatedVisibility` 包裹卡片区并添加覆盖层**

找到根 `Box` 内第一段卡片代码：

```kotlin
if (currentIndex < cardList.size) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        ...
    ) { ... }
}

if (currentIndex + 1 < cardList.size) { ... }

if (currentIndex + 2 < cardList.size) { ... }
```

用 `AnimatedVisibility` 将**这三个 `if` 块整体**包裹（不动其内部代码）：

```kotlin
AnimatedVisibility(
    visible = !isDangerMode,
    enter = fadeIn(animationSpec = tween(500)),
    exit  = fadeOut(animationSpec = tween(500))
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (currentIndex < cardList.size) {
            // ... 原有代码，保持不变
        }
        if (currentIndex + 1 < cardList.size) {
            // ... 原有代码，保持不变
        }
        if (currentIndex + 2 < cardList.size) {
            // ... 原有代码，保持不变
        }
    }
}
```

紧接在这个 `AnimatedVisibility` 之后，追加危险模式覆盖层：

```kotlin
AnimatedVisibility(
    visible = isDangerMode,
    enter = fadeIn(animationSpec = tween(500)),
    exit  = fadeOut(animationSpec = tween(500))
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 280.dp), // 为底部 Halo 留出空间
        contentAlignment = Alignment.Center
    ) {
        DangerModeOverlay()
    }
}
```

- [ ] **Step 5：将 `AuroraWaveHalo` 调用传入 `isDangerMode`**

找到：
```kotlin
AuroraWaveHalo(audioLevel = 0.5f, onLongPress = { /* TODO: Voice input */ })
```
改为：
```kotlin
AuroraWaveHalo(
    audioLevel = 0.5f,
    onLongPress = { /* TODO: Voice input */ },
    isDangerMode = isDangerMode
)
```

- [ ] **Step 6：补充 import**

在 `MainActivity.kt` 顶部 import 区域补充（如尚未存在）：

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aether.app.ui.components.DangerModeOverlay
import kotlinx.coroutines.flow.map
```

- [ ] **Step 7：完整编译验证**

```bash
./gradlew assembleDebug
```

期望：BUILD SUCCESSFUL。

- [ ] **Step 8：运行全部测试**

```bash
./gradlew testDebugUnitTest
```

期望：所有测试 PASS。

- [ ] **Step 9：Commit**

```bash
git add app/src/main/java/com/aether/app/MainActivity.kt
git commit -m "feat: wire up global danger mode — edge glow, workspace overlay, halo sync"
```

---

## 手动验证清单

完成所有 Task 后，在设备/模拟器上验证以下场景：

- [ ] 首次点击"危险模式" Switch → 警告弹窗弹出
- [ ] 点击"取消" → 弹窗关闭，Switch 保持 OFF
- [ ] 再次点击 Switch → 弹窗再次出现（首次未确认不写 DataStore）
- [ ] 点击"确认并开启" → 弹窗关闭，Switch 变红，区域四卡片背景变淡红
- [ ] 切换到工作台 → 卡片淡出，"危险模式"大字淡入，Halo 渐变为红色，屏幕边缘出现红晕呼吸
- [ ] 切回个人空间 → 区域四开关仍可见且为开启状态
- [ ] 点击 Switch OFF → 所有效果平滑恢复正常
- [ ] 杀掉 App 重启 → 危险模式默认关闭（但再次开启时无弹窗，因 hasSeenDangerWarning 已为 true）
