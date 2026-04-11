# 设计规格：区域 3 "我的设备" (Aether My Devices)

**日期：** 2026-04-10  
**状态：** 已批准，进入实现  
**方案确认：** UI 方案 A（updateTransition 弹簧展开 + ShimmerSweep）/ Activity 作用域 ViewModel

---

## 1. 目标与范围

在 `PersonalNexusScreen` 的"区域 3"（宽 weight=1，高固定 240dp）中实现完整的蓝牙设备管理模块，替换现有 `SectionPlaceholder("区域 3")`。

**范围内：**
- 已绑定设备列表展示（DeviceCard 动效）
- 蓝牙扫描 BottomSheet（真实 BLE API + 权限流程）
- 模拟连接（2s 加载 → 接入预设 Lens 1.0）
- 冷启动自检（首次启动自动注入预设设备）
- 工作状态长按循环切换

**范围外：**
- 真实设备配对协议（GATT/Profile）
- 设备删除功能
- 多设备真实连接

---

## 2. 文件结构

```
app/src/main/java/com/aether/app/
├── data/
│   └── DeviceState.kt             ← DeviceState + ScannedDevice + WorkStatus enum
├── device/
│   ├── BluetoothScanner.kt        ← callbackFlow 封装 BLE startScan / stopScan
│   └── DeviceRepository.kt        ← DataStore 持久化 + 冷启动自检 + simulateConnect
├── DeviceViewModel.kt             ← DeviceUiState + 业务方法
├── DeviceViewModelFactory.kt      ← 手动 Factory（无 Hilt）
└── ui/components/
    └── MyDevicesSection.kt        ← 所有 UI 组件（含 BottomSheet）
```

`DeviceViewModel` 在 `MainActivity` 通过 `viewModels { DeviceViewModelFactory(repo) }` 创建，
传入 `PersonalNexusScreen(mainVm, deviceVm)` → `PersonalNexusContent(..., deviceVm)` → `MyDevicesSection(deviceVm)`。

---

## 3. 数据层

### 3.1 DeviceState.kt

```kotlin
enum class WorkStatus { WORKING, ORGANIZING, SLEEPING }

data class DeviceState(
    val id: String,
    val deviceName: String,
    val isConnected: Boolean,
    val batteryLevel: Int,       // 0–100
    val workStatus: WorkStatus,
    val isPredefined: Boolean    // true = Lens 1.0，不可删除
)

data class ScannedDevice(
    val name: String,
    val macAddress: String,
    val rssi: Int                // 降序排列，越大信号越强
)
```

**预设设备常量（DeviceRepository 内部）：**
```kotlin
val PREDEFINED_LENS = DeviceState(
    id = "predefined-lens-1",
    deviceName = "Lens 1.0",
    isConnected = true,
    batteryLevel = 68,
    workStatus = WorkStatus.WORKING,
    isPredefined = true
)
```

---

## 4. 逻辑层

### 4.1 BluetoothScanner

- 接收 `Context`，在 `scan()` 内获取 `BluetoothAdapter` 和 `BluetoothLeScanner`
- 权限检查：API ≥ 31 需要 `BLUETOOTH_SCAN + BLUETOOTH_CONNECT`；API < 31 需要 `BLUETOOTH + BLUETOOTH_ADMIN + ACCESS_FINE_LOCATION`
- 返回 `callbackFlow<List<ScannedDevice>>`，每次回调累加后去重（按 macAddress）并发出
- `awaitClose { leScanner.stopScan(callback) }` 保证 Flow 取消时自动停止扫描

### 4.2 DeviceRepository

- 使用独立 DataStore 文件 `device_preferences`，key = `"device_list_json"`，Gson 序列化 `List<DeviceState>`
- `getDevices(): Flow<List<DeviceState>>`
- `suspend fun saveDevices(list: List<DeviceState>)`
- **冷启动自检**：`init` 块协程检查，若列表为空则调用 `saveDevices(listOf(PREDEFINED_LENS))`
- `suspend fun simulateConnect(scanned: ScannedDevice): DeviceState`：`delay(2_000)` 后返回 `PREDEFINED_LENS.copy(id = UUID.randomUUID().toString(), isPredefined = false)`

---

## 5. 状态层

### 5.1 DeviceUiState

```kotlin
data class DeviceUiState(
    val boundDevices: List<DeviceState> = emptyList(),
    val isScanning: Boolean = false,
    val scannedDevices: List<ScannedDevice> = emptyList(), // 按 rssi 降序
    val connectingMac: String? = null,   // 驱动连接中加载动画
    val showScanSheet: Boolean = false,
    val permissionDenied: Boolean = false
)
```

### 5.2 DeviceViewModel 方法

| 方法 | 职责 |
|------|------|
| `startBluetoothScan()` | 启动扫描 Job，`withTimeoutOrNull(30_000L)` 自动超时停止 |
| `stopScan()` | 取消 scanJob，重置 isScanning=false |
| `connectDevice(scanned)` | 设置 connectingMac，调用 repo.simulateConnect，追加到 boundDevices，清除 connectingMac，关闭 BottomSheet |
| `cycleWorkStatus(id)` | WORKING→ORGANIZING→SLEEPING→WORKING 循环，更新 DataStore |
| `openScanSheet()` | showScanSheet=true，清空 scannedDevices |
| `dismissScanSheet()` | showScanSheet=false，stopScan() |

**扫描超时模式：**
```kotlin
private var scanJob: Job? = null
const val SCAN_TIMEOUT_MS = 30_000L

fun startBluetoothScan() {
    scanJob?.cancel()
    scanJob = viewModelScope.launch {
        _uiState.update { it.copy(isScanning = true, scannedDevices = emptyList()) }
        withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            scanner.scan().collect { devices ->
                _uiState.update { it.copy(scannedDevices = devices) }
            }
        }
        _uiState.update { it.copy(isScanning = false) }
        scanJob = null
    }
}
```

---

## 6. UI 组件

### 6.1 MyDevicesSection

- 顶部 Row：`SectionLabel("我的设备")` + `IconButton(onClick = vm::openScanSheet)` (Icons.Filled.Add)
- `LazyColumn`，`Modifier.height(240.dp - headerHeight).clip()`（实际内容区约 196dp）
- 若 `boundDevices.isEmpty()` 显示 `EmptyDeviceState`
- 否则 `items(boundDevices)` 渲染 `DeviceCard`

### 6.2 EmptyDeviceState

- 居中 Column：设备图标（灰色）+ 说明文字 + `FilledTonalButton("添加设备", onClick = openScanSheet)`
- 按钮与标题栏 "+" 触发同一 `openScanSheet()` 方法

### 6.3 DeviceCard

**高度动画（核心）：**
```kotlin
val transition = updateTransition(device.isConnected, label = "card_expand")
val cardHeight by transition.animateDp(
    transitionSpec = { spring(stiffness = 300f, dampingRatio = 0.75f) },
    label = "card_height"
) { connected -> if (connected) 120.dp else 80.dp }
```

**ShimmerSweepEffect：**
- `LaunchedEffect(device.isConnected)` 监听 isConnected 变为 true 的瞬间
- 启动 `Animatable(0f) → animateTo(1f, tween(200))`
- Canvas 绘制一条白色半透明斜向渐变线从左向右扫过卡片
- 完成后 Animatable 重置，不循环

**内容交错显现（展开态）：**
```kotlin
// 电量行：立即显示
AnimatedVisibility(visible = isConnected, enter = fadeIn())

// 工作状态行：延迟 100ms
var showStatus by remember { mutableStateOf(false) }
LaunchedEffect(isConnected) {
    if (isConnected) { delay(100); showStatus = true } else showStatus = false
}
AnimatedVisibility(showStatus, enter = slideInVertically { it/2 } + fadeIn())
```

**长按切换状态：**
```kotlin
Modifier.combinedClickable(
    onClick = { /* 无操作 */ },
    onLongClick = { vm.cycleWorkStatus(device.id) }
)
```

### 6.4 BatteryIcon (Canvas)

- 尺寸 `28×16dp`，圆角 `3dp` 的电池轮廓 + 右侧电极凸起（`4×8dp`）
- 填充色：`batteryLevel > 20` → 绿色 `#22c55e`，`≤ 20` → 红色 `#ef4444`
- **涨潮动画**：`val animatedLevel by animateFloatAsState(batteryLevel / 100f, tween(800, easing = FastOutSlowInEasing))`
- 绘制填充宽度 = `内部宽度 × animatedLevel`

### 6.5 BluetoothScanBottomSheet

- `ExperimentalMaterial3Api ModalBottomSheet`
- **顶部脉冲波纹**：
  ```kotlin
  val pulse = rememberInfiniteTransition()
  val scale by pulse.animateFloat(1f, 2.2f, infiniteRepeatable(tween(1200), RepeatMode.Restart))
  val alpha by pulse.animateFloat(0.6f, 0f, infiniteRepeatable(tween(1200)))
  // Canvas 绘制 3 圈 NeonPurple 圆环，phase 各延迟 400ms
  ```
- 扫描状态行：`CircularProgressIndicator` (20dp) + "扫描中…" / 超时后显示"扫描完成"
- `LazyColumn`：`ScannedDeviceItem` = `LetterAvatar(name.first())` + 设备名/MAC + `SignalIcon(rssi)`
- 点击项：若 `connectingMac == mac` 显示加载圆圈；否则触发 `vm.connectDevice(item)`

### 6.6 SignalIcon (Canvas)

3 格竖条，高度递增（40%、65%、100%），按 rssi 阈值着色：
- `rssi > -60` → 3 格亮色
- `-75 < rssi ≤ -60` → 2 格亮色
- `-90 < rssi ≤ -75` → 1 格亮色
- `rssi ≤ -90` → 全灰

---

## 7. Manifest 权限

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

运行时申请：`MyDevicesSection` 内 `rememberLauncherForActivityResult(RequestMultiplePermissions)`，
拒绝时 `vm.onPermissionDenied()`，UI 显示引导 Snackbar。

---

## 8. 集成点

`PersonalNexusContent` 的 Row 中，将：
```kotlin
SectionPlaceholder(label = "区域 3", minHeight = 240, modifier = Modifier.weight(1f))
```
替换为：
```kotlin
MyDevicesSection(
    viewModel = deviceViewModel,
    modifier = Modifier.weight(1f)
)
```

`PersonalNexusContent` 增加参数 `deviceViewModel: DeviceViewModel`。  
`PersonalNexusScreen` 接收 `deviceViewModel: DeviceViewModel` 并透传。  
`MainActivity` 创建并传入两个 ViewModel。

---

## 9. 不需要的新依赖

BLE API 位于 `android.bluetooth.*`，已通过 SDK 提供。  
DataStore、Gson、Compose、Material3 均已在 `build.gradle.kts` 中声明。
