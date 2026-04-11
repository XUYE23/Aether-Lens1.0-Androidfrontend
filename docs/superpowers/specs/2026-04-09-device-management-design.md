# Aether 设备管理功能设计文档 (区域 3 - Lens 1.0)

## 项目概述

在 Aether App 的个人空间中实现区域 3 的设备管理功能，展示配套硬件 "Lens 1.0" 的连接状态、电量、工作状态，并支持真实蓝牙扫描 + 预设数据的混合方案。

**设计日期**：2026-04-09  
**目标版本**：v1.1  
**负责模块**：个人空间 - 区域 3

---

## 一、核心需求

### 1.1 功能目标

- 展示已配对的 Aether 设备列表（当前仅 Lens 1.0，架构支持未来多设备）
- 真实蓝牙扫描 + 模拟连接流程
- 动态展示设备状态：连接状态、电量、工作模式
- 冷启动自检：确保演示环境始终就绪
- 数据持久化：设备列表通过 DataStore 保存

### 1.2 交互要求

- 点击"+"按钮触发真实蓝牙权限请求和扫描
- ModalBottomSheet 展示扫描结果，符合 Aether 品牌风格
- 连接成功后触发"交错式流体展开"动画
- 长按设备卡片可切换工作状态（临时模拟触发器）

---

## 二、数据模型

### 2.1 DeviceState（设备状态）

```kotlin
data class DeviceState(
    val id: String = UUID.randomUUID().toString(),
    val deviceName: String,
    val macAddress: String? = null,
    val isConnected: Boolean,
    val batteryLevel: Int = 0,  // 0-100
    val workStatus: WorkStatus = WorkStatus.IDLE,
    val lastConnectedTime: Long = System.currentTimeMillis(),
    val isPredefined: Boolean = false  // 标记预设演示设备
)

enum class WorkStatus {
    WORKING,    // 工作
    ORGANIZING, // 整理
    IDLE        // 休眠
}
```

**字段说明**：
- `isPredefined`：区分预设 Lens 1.0 和真实扫描设备，便于冷启动自检逻辑

### 2.2 ScannedDevice（扫描设备）

```kotlin
data class ScannedDevice(
    val name: String,
    val macAddress: String,
    val rssi: Int  // 信号强度，用于排序
)
```

### 2.3 DeviceUiState（ViewModel 状态）

```kotlin
data class DeviceUiState(
    val pairedDevices: List<DeviceState> = emptyList(),
    val isScanning: Boolean = false,
    val scannedDevices: List<ScannedDevice> = emptyList(),
    val isConnecting: Boolean = false,
    val connectingDeviceName: String? = null,
    val showScanSheet: Boolean = false,
    val showBluetoothPrompt: Boolean = false
)
```

---

## 三、架构设计

### 3.1 文件结构

```
app/src/main/java/com/aether/app/
├── data/
│   ├── DeviceState.kt          (新建)
│   ├── ScannedDevice.kt        (新建)
│   └── UserPreferencesRepository.kt (扩展)
├── device/
│   ├── DeviceRepository.kt     (新建)
│   └── BluetoothScanner.kt     (新建)
├── DeviceViewModel.kt          (新建)
└── ui/screens/
    ├── PersonalNexusScreen.kt  (修改)
    └── DeviceScanSheet.kt      (新建)
```

### 3.2 职责划分

#### BluetoothScanner
- 检查蓝牙权限（Android 12+ 适配）
- 启动/停止 BLE 扫描
- 返回 `Flow<List<ScannedDevice>>` 实时推送扫描结果
- 处理蓝牙未开启的异常

#### DeviceRepository
- 封装 DataStore 持久化逻辑
- 管理 BluetoothScanner 生命周期
- 提供 `pairedDevices: Flow<List<DeviceState>>`
- 冷启动自检：`ensurePredefinedDevice()`
- 模拟连接：`connectDevice()` 返回预设 Lens 1.0 数据
- 预留接口：真实蓝牙连接、后端推送

#### DeviceViewModel
- 暴露 `uiState: StateFlow<DeviceUiState>`
- 管理扫描生命周期（30 秒自动超时）
- 处理设备连接流程
- 提供临时模拟触发器：`toggleWorkStatus()`
- `onCleared()` 确保扫描停止

#### MainActivity
- 注入 `applicationContext` 到 Repository（防止内存泄漏）
- 管理两个 Activity 作用域的 ViewModel：`MainViewModel` + `DeviceViewModel`

### 3.3 数据流

```
用户点击"+" 
  → DeviceViewModel.openScanSheet()
  → 检查权限（Composable 中的 ActivityResultLauncher）
  → DeviceViewModel.startBluetoothScan()
  → DeviceRepository.startScan()
  → BluetoothScanner.scan() 返回 Flow
  → 实时更新 scannedDevices
  → 用户点击设备
  → DeviceViewModel.connectDevice()
  → 立即关闭弹窗（让用户看到展开动画）
  → 模拟 2 秒连接
  → 返回预设 Lens 1.0 数据
  → 保存到 DataStore
  → 区域 3 触发"交错式流体展开"动画
```

---

## 四、UI 组件设计

### 4.1 区域 3 整体布局

```
┌─────────────────────────────────┐
│ 我的设备                    [+] │  ← SectionLabel + IconButton
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ Lens 1.0                    │ │  ← DeviceCard (未连接：80dp)
│ │ ● 未连接                    │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Lens 1.0                    │ │  ← DeviceCard (已连接：120dp)
│ │ ● 已连接                    │ │
│ │ [电池图标] 68%              │ │  ← 交错淡入 (0ms)
│ │ ● 工作                      │ │  ← 交错淡入 (100ms)
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

**容器特性**：
- 固定高度 240.dp
- 内部 LazyColumn 可滚动（支持未来多设备）
- 空状态显示 Icon + "暂无绑定设备，请添加"

### 4.2 DeviceCard 动画方案："交错式流体展开"

#### 第一层：容器扩张
- 使用 `updateTransition` 统一管理状态
- 高度从 80.dp → 120.dp
- Spring 物理动画：`dampingRatio = 0.75f, stiffness = 200f`

#### 第二层：内容交错载入
- 电量信息：0ms 延迟，`fadeIn() + slideInVertically()`
- 工作状态：100ms 延迟，同样动画
- 使用 `AnimatedVisibility` 包裹

#### 第三层：视觉反馈
- **光效扫过**：白色半透明渐变斜向 45° 扫过卡片（800ms）
- **电量填充动画**：`animateFloatAsState` 从 0% 涨到 68%（800ms）
- **工作状态切换**：`Crossfade` 平滑过渡（300ms）

### 4.3 自定义电池图标（Canvas 绘制）

```kotlin
Canvas(modifier = Modifier.size(width = 24.dp, height = 12.dp)) {
    // 1. 电池轮廓（圆角矩形 + 描边）
    // 2. 右侧电极突出（小圆角矩形）
    // 3. 内部填充（动态宽度 = batteryWidth * level）
    // 4. 颜色阈值：level > 20% → 绿色，≤ 20% → 红色
}
```

### 4.4 连接状态指示器

- 已连接：蓝绿光圆点 `#00D9FF` + "已连接"
- 未连接：灰色圆点 `#666666` + "未连接"

---

## 五、蓝牙扫描弹窗设计

### 5.1 DeviceScanSheet（ModalBottomSheet）

**品牌风格**：
- 磨砂玻璃效果：`containerColor.copy(alpha = 0.95f)` + `Modifier.blur(20.dp)`（Android 12+）
- 圆角：`topStart = 24.dp, topEnd = 24.dp`
- 背景遮罩：`scrimColor = Color.Black.copy(alpha = 0.6f)`

**内容结构**：
1. 标题栏："扫描设备" + 关闭按钮
2. 扫描指示器：波纹动画 + "正在扫描附近设备..."
3. 设备列表：LazyColumn + `animateItemPlacement()`（滑入动画）
4. 空状态：Icon + "未发现设备" + "开始扫描"按钮

### 5.2 ScannedDeviceCard

```
┌─────────────────────────────────┐
│ [O] Galaxy Buds                 │  ← LetterAvatar + 设备名
│     AA:BB:CC:DD:EE:FF      [|||]│  ← MAC 地址 + 信号强度
└─────────────────────────────────┘
```

**交互**：
- 点击触发连接（显示 CircularProgressIndicator）
- 连接中禁用点击
- 按 RSSI 降序排列

### 5.3 扫描波纹动画

- 中心蓝牙图标 + 外圈波纹
- `scale: 0.8f ↔ 1.2f`（1000ms 往复）
- `alpha: 0.6f ↔ 0.2f`（1000ms 往复）
- 使用 `rememberInfiniteTransition`

### 5.4 信号强度图标

- 3 格竖条，高度递增：8dp / 12dp / 16dp
- RSSI 映射：≥ -50 (3格) / ≥ -70 (2格) / ≥ -85 (1格) / 其他 (0格)
- 激活格：NeonPurple，未激活格：灰色半透明

---

## 六、蓝牙实现逻辑

### 6.1 权限处理

**Android 12+ (API 31+)**：
- `BLUETOOTH_SCAN`（带 `neverForLocation` 标志）
- `BLUETOOTH_CONNECT`

**Android 11 及以下**：
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`

**权限请求**：
- 在 Composable 中使用 `rememberLauncherForActivityResult(RequestMultiplePermissions())`
- 点击"+"按钮时检查权限，未授予则弹出系统权限对话框

### 6.2 蓝牙未开启引导

```kotlin
if (!bluetoothAdapter.isEnabled) {
    throw BluetoothDisabledException()
}
```

- ViewModel 捕获异常后设置 `showBluetoothPrompt = true`
- UI 显示 AlertDialog："需要开启蓝牙" + "去开启"按钮
- 点击后启动 `Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)`

### 6.3 扫描超时机制

```kotlin
launch {
    delay(30_000)  // 30 秒
    if (_uiState.value.isScanning) {
        stopScan()
    }
}
```

### 6.4 模拟连接流程

```kotlin
suspend fun connectDevice(scannedDevice: ScannedDevice): DeviceState {
    delay(2000)  // 模拟连接延迟
    
    // ⚠️ 预留接口：此处未来接入真实蓝牙连接逻辑
    // TODO: 调用 BluetoothDevice.connectGatt() 建立真实连接
    // TODO: 通过 GATT 特征值读取真实电量和工作状态
    
    return DeviceState(
        deviceName = "Lens 1.0",
        macAddress = scannedDevice.macAddress,
        isConnected = true,
        batteryLevel = 68,
        workStatus = WorkStatus.WORKING,
        isPredefined = false
    )
}
```

### 6.5 冷启动自检

```kotlin
suspend fun ensurePredefinedDevice() {
    val current = pairedDevices.first()
    if (current.none { it.isPredefined }) {
        val predefinedLens = DeviceState(
            deviceName = "Lens 1.0",
            isConnected = true,
            batteryLevel = 68,
            workStatus = WorkStatus.WORKING,
            isPredefined = true
        )
        savePairedDevices(current + predefinedLens)
    }
}
```

**触发时机**：`DeviceViewModel.init` 块中自动执行

---

## 七、数据持久化

### 7.1 DataStore 字段

在 `UserPreferencesRepository` 中新增：

```kotlin
private val KEY_PAIRED_DEVICES_JSON = stringPreferencesKey("paired_devices_json")

val pairedDevicesJson: Flow<String?> = context.dataStore.data
    .map { prefs -> prefs[KEY_PAIRED_DEVICES_JSON] }

suspend fun savePairedDevices(devices: List<DeviceState>) {
    val json = gson.toJson(devices, deviceListType)
    context.dataStore.edit { it[KEY_PAIRED_DEVICES_JSON] = json }
}
```

### 7.2 持久化时机

- 连接新设备成功后
- 更新设备工作状态后
- 冷启动自检添加预设设备后

---

## 八、预留接口与扩展性

### 8.1 真实蓝牙连接（未来实现）

```kotlin
// DeviceRepository.connectDevice() 中
// TODO: 调用 BluetoothDevice.connectGatt() 建立 GATT 连接
// TODO: 读取 Battery Service (0x180F) 获取真实电量
// TODO: 读取自定义 Service 获取工作状态
```

### 8.2 后端推送（未来实现）

```kotlin
// DeviceRepository.updateWorkStatus() 中
// TODO: 通过 WebSocket/SSE 接收后端推送的状态变化
// TODO: 或通过蓝牙 GATT Notification 监听硬件状态变化
```

### 8.3 多设备支持

- 当前架构已支持 `List<DeviceState>`
- 区域 3 使用 LazyColumn，自动支持滚动
- 未来只需扩展扫描逻辑和连接逻辑，UI 无需改动

---

## 九、技术细节

### 9.1 生命周期管理

- `DeviceViewModel.onCleared()` 调用 `repository.stopScan()`
- 扫描 Job 在 ViewModel 销毁时自动取消
- BluetoothScanner 使用 `callbackFlow` + `awaitClose` 确保资源释放

### 9.2 内存泄漏防护

- MainActivity 注入 `applicationContext` 而非 Activity 实例
- Repository 持有 Application Context，不持有 Activity 引用

### 9.3 权限兼容性

- 使用 `Build.VERSION.SDK_INT` 判断 API 级别
- Android 12+ 使用新权限，11 及以下使用旧权限
- `@SuppressLint("MissingPermission")` 仅在运行时检查通过后调用

### 9.4 动画性能

- 所有动画使用 `spring()` 物理规格，符合 Aether 设计规范
- `updateTransition` 统一管理状态，避免动画冲突
- Canvas 绘制电池图标，避免多层嵌套布局

---

## 十、测试要点

### 10.1 功能测试

- [ ] 冷启动后区域 3 显示预设 Lens 1.0（已连接）
- [ ] 点击"+"按钮触发权限请求
- [ ] 扫描弹窗显示真实蓝牙设备
- [ ] 点击设备后弹窗关闭，2 秒后区域 3 展开动画
- [ ] 长按设备卡片切换工作状态（工作 → 整理 → 休眠）
- [ ] 杀进程重启后设备列表保持

### 10.2 边界测试

- [ ] 蓝牙未开启时显示引导对话框
- [ ] 权限被拒绝后扫描不启动
- [ ] 扫描 30 秒后自动停止
- [ ] 扫描中切换 Tab 不崩溃
- [ ] 连接中关闭 App 不泄漏

### 10.3 动画测试

- [ ] 连接成功后电量从 0% 涨到 68%
- [ ] 光效扫过仅触发一次
- [ ] 工作状态切换使用 Crossfade
- [ ] 扫描列表新设备滑入动画流畅

---

## 十一、成功标准

- ✅ 区域 3 UI 符合 Aether 品牌风格
- ✅ 真实蓝牙扫描功能正常
- ✅ 动画流畅（60fps），符合物理直觉
- ✅ 数据持久化正常，重启后状态保持
- ✅ 代码架构清晰，职责分离
- ✅ 预留接口注释完整，便于后续扩展

---

**文档版本**：v1.0  
**创建日期**：2026-04-09  
**作者**：Claude (Brainstorming Phase)
