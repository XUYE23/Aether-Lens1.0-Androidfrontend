# 危险模式（Danger Mode）设计文档

**日期：** 2026-04-11  
**状态：** 已确认，待实现

---

## 1. 功能概述

在个人空间的区域四新增"危险模式"开关。开启后，App 进入高风险执行状态：工作台的任务卡片被隐藏，语音指令生成的 Action 将直接执行（跳过 UI 确认），全局边缘出现红色呼吸光晕。

---

## 2. 状态设计

### 2.1 状态归属

| 状态 | 类型 | 存储位置 | 说明 |
|------|------|----------|------|
| `isDangerModeActive` | `Boolean` | `PersonalSpaceUiState`（内存） | 重启后自动归 false，不持久化 |
| `showDangerWarningDialog` | `Boolean` | `PersonalSpaceUiState`（内存） | 控制首次警告弹窗的显示 |
| `hasSeenDangerWarning` | `StateFlow<Boolean>` | DataStore（持久化） | 记录用户是否已看过警告，只写一次 |

### 2.2 DataStore Key

```kotlin
// UserPreferencesRepository.kt
private val KEY_HAS_SEEN_DANGER_WARNING = booleanPreferencesKey("has_seen_danger_warning")
```

`hasSeenDangerWarning` 的 `stateIn` 初始值为 `false`（安全默认值：DataStore 未返回前视为未看过警告）。

### 2.3 开关逻辑流

```
用户点击 Switch ON → toggleDangerMode()
  ├─ hasSeenDangerWarning == false
  │   └─ showDangerWarningDialog = true（拦截，等待确认）
  └─ hasSeenDangerWarning == true
      └─ isDangerModeActive = true（直接激活）

用户点击"确认并开启" → confirmDangerMode()
  ├─ DataStore 写 hasSeenDangerWarning = true
  ├─ isDangerModeActive = true
  └─ showDangerWarningDialog = false

用户点击 Switch OFF（任何时候）→ dismissDangerMode()
  └─ isDangerModeActive = false（不重置 hasSeenDangerWarning）
```

### 2.4 状态传递路径

```
MainViewModel.isDangerModeActive (StateFlow)
  └─ MainActivity → AetherApp       // 驱动根容器边缘红晕 Modifier
  └─ MainScreen → HorizontalPager
      ├─ WorkspaceScreen(isDangerMode)   // 内容切换
      └─ PersonalNexusScreen(viewModel)  // 开关控制
```

---

## 3. UI 组件

### 3.1 新建文件：`ui/components/DangerModeCard.kt`

包含两个 Composable：

**`DangerModeCard`**
```kotlin
@Composable
fun DangerModeCard(
    isActive: Boolean,
    onToggle: () -> Unit  // 由 ViewModel 决策（可能弹窗拦截，非直接写状态）
)
```
- 卡片背景：`isActive` 为 true 时切换为淡红色（`animateColorAsState`）
- 标签文字："危险模式"
- 副标签：关闭时 "开启后将直接执行"，开启时 "⚡ 直接执行中"
- Switch 轨道颜色：关闭时默认灰色，开启时深红色 `Color(0xFF8B0000)`

**`DangerWarningDialog`**
```kotlin
@Composable
fun DangerWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
)
```
- 暗色背景 Surface（`tonalElevation = 6.dp`）
- 顶部 ⚠️ 图标 + 红色"警告"标题
- 文案："开启后，系统将直接发送消息或修改数据。您将放弃最终干预权，请谨慎评估当前场景的风险。"
- 按钮："取消"（灰色 TextButton）/ "确认并开启"（深红色 FilledButton）

### 3.2 修改：`PersonalNexusScreen.kt`

在 `PersonalNexusContent` 的区域四位置，将 `SectionPlaceholder(label = "区域 4")` 替换为：

```kotlin
DangerModeCard(
    isActive = uiState.isDangerModeActive,
    onToggle = onToggleDangerMode
)

if (uiState.showDangerWarningDialog) {
    DangerWarningDialog(
        onConfirm = onConfirmDangerMode,
        onDismiss = onDismissDangerWarning
    )
}
```

`PersonalNexusContent` 新增参数：
- `onToggleDangerMode: () -> Unit`
- `onConfirmDangerMode: () -> Unit`
- `onDismissDangerWarning: () -> Unit`

**`AuroraWaveHalo`（修改）**

新增 `isDangerMode: Boolean = false` 参数。内部三个颜色均通过 `animateColorAsState`（`tween(800)`）平滑过渡：

| 颜色变量 | 正常模式 | 危险模式 |
|---------|---------|---------|
| `deepBlue` | `Color(0xFF0066FF)` | `Color(0xFF8B0000)` |
| `cyan` | `Color(0xFF00DDFF)` | `Color(0xFFFF0000)` |
| `lightGreen` | `Color(0xFF88FFDD)` | `Color(0xFFFF4444)` |

切换时机与 `isDangerModeActive` 同步，确保按下"确认并开启"时 Halo 与边缘红晕、大字同步"充血"。

---

### 3.3 修改：`MainActivity.kt`

**`AetherApp`**：根 Box 挂载边缘红晕 Modifier（见第 4 节）。

**`MainScreen`**：从 ViewModel 收集 `isDangerModeActive`，传入 `WorkspaceScreen`：
```kotlin
val isDangerMode by viewModel.uiState.map { it.isDangerModeActive }.collectAsState(false)
// 传入 WorkspaceScreen(isDangerMode = isDangerMode)
```

**`WorkspaceScreen` / `AmbientHubScreen`**：新增 `isDangerMode: Boolean` 参数，结构变更如下：

```kotlin
Box {
    // ① 卡片堆栈 — AnimatedVisibility 包裹（500ms 淡入淡出）
    AnimatedVisibility(
        visible = !isDangerMode,
        enter = fadeIn(animationSpec = tween(500)),
        exit  = fadeOut(animationSpec = tween(500))
    ) {
        // SwipeCard / 次卡 / CardStack（原有代码不动）
    }

    // ② 危险模式中央覆盖层
    AnimatedVisibility(
        visible = isDangerMode,
        enter = fadeIn(animationSpec = tween(500)),
        exit  = fadeOut(animationSpec = tween(500))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DangerModeOverlay()  // 见第 4 节
        }
    }

    // ③ 右上角用户名 — 始终保留（原有代码不动）

    // ④ AuroraWaveHalo — 始终保留，新增 isDangerMode 参数
    AuroraWaveHalo(isDangerMode = isDangerMode, ...)
}
```

---

## 4. 视觉效果

### 4.1 边缘红晕（根容器，方案 A：drawWithContent + RadialGradient）

挂载位置：`AetherApp` 根 `Box` 的 `Modifier`。

```kotlin
val glowAlpha by animateFloatAsState(
    targetValue = if (isDangerMode) 1f else 0f,
    animationSpec = tween(600)
)
val infiniteTransition = rememberInfiniteTransition()
val breathAlpha by infiniteTransition.animateFloat(
    initialValue = 0.15f,
    targetValue  = 0.35f,
    animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse)
)

Modifier.drawWithContent {
    drawContent()
    if (glowAlpha > 0f) {
        val brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Red.copy(alpha = breathAlpha * glowAlpha)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = maxOf(size.width, size.height) * 0.85f
        )
        drawRect(brush = brush)
    }
}
```

### 4.2 "危险模式"大字（工作台中央覆盖层）

位置：`DangerModeCard.kt`（与 `DangerModeCard` 同文件，内部私有 Composable）。

```kotlin
@Composable
fun DangerModeOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    Text(
        text = "危险模式",
        fontSize = 40.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFFF2222).copy(alpha = alpha),
        letterSpacing = 10.sp,
        style = LocalTextStyle.current.copy(
            shadow = Shadow(color = Color.Red.copy(alpha = 0.8f), blurRadius = 20f)
        )
    )
}
```

样式：粗体（`FontWeight.Black`）、字号 40sp、字间距 10sp、红色带 Shadow 发光、透明度呼吸动画（0.5f ↔ 1f，1200ms）。

---

## 5. 文件变更清单

| 操作 | 文件 |
|------|------|
| 修改 | `app/src/main/java/com/aether/app/ui/components/AuroraWaveHalo.kt` |
| 新建 | `app/src/main/java/com/aether/app/ui/components/DangerModeCard.kt` |
| 修改 | `app/src/main/java/com/aether/app/data/UserPreferencesRepository.kt` |
| 修改 | `app/src/main/java/com/aether/app/MainViewModel.kt` |
| 修改 | `app/src/main/java/com/aether/app/MainActivity.kt` |
| 修改 | `app/src/main/java/com/aether/app/ui/screens/PersonalNexusScreen.kt` |

---

## 6. 不在本次范围内

- 语音指令"直接执行"的后端逻辑（仅预留 UI 状态，实际执行逻辑待后续迭代）
- 区域五的实现
- 危险模式下的 Action 执行审计日志
