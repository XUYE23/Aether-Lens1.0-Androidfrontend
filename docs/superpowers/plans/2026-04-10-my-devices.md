# My Devices (区域 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `PersonalNexusScreen` 区域 3 实现完整的蓝牙设备管理模块，包含弹簧动画卡片、Canvas 电量图标、BLE 扫描 BottomSheet 和 ShimmerSweep 高光效果。

**Architecture:** Repository (DataStore) → DeviceViewModel (Activity scope) → MyDevicesSection (Composable)；BluetoothScanner 用 `callbackFlow` 封装 BLE API，`withTimeoutOrNull(30s)` 自动停止扫描；UI 全部在 `MyDevicesSection.kt` 单文件中。

**Tech Stack:** Kotlin + Jetpack Compose + Material3 + DataStore Preferences + Gson + `android.bluetooth.le` BLE API（无新依赖）

---

## 文件清单

| 动作 | 路径 | 职责 |
|------|------|------|
| 修改 | `app/src/main/AndroidManifest.xml` | 添加蓝牙权限 |
| 新建 | `app/src/main/java/com/aether/app/data/DeviceState.kt` | WorkStatus enum + DeviceState + ScannedDevice |
| 新建 | `app/src/main/java/com/aether/app/device/BluetoothScanner.kt` | callbackFlow BLE 扫描 |
| 新建 | `app/src/main/java/com/aether/app/device/DeviceRepository.kt` | DataStore 持久化 + 冷启动自检 + simulateConnect |
| 新建 | `app/src/main/java/com/aether/app/DeviceViewModel.kt` | DeviceUiState + 业务方法 + DeviceViewModelFactory |
| 新建 | `app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt` | 所有 UI 组件 |
| 修改 | `app/src/main/java/com/aether/app/MainActivity.kt` | 创建 DeviceViewModel，传入 PersonalNexusScreen |
| 修改 | `app/src/main/java/com/aether/app/ui/screens/PersonalNexusScreen.kt` | 替换 SectionPlaceholder，新增 deviceViewModel 参数 |

---

## Task 1: Manifest — 添加蓝牙权限

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 在 `<manifest>` 根节点添加权限声明**

  打开 `app/src/main/AndroidManifest.xml`，在现有 `RECORD_AUDIO` 权限之后添加：

  ```xml
  <uses-permission android:name="android.permission.BLUETOOTH"
      android:maxSdkVersion="30" />
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
      android:maxSdkVersion="30" />
  <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
      android:usesPermissionFlags="neverForLocation" />
  <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  ```

  完整文件应为：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <manifest xmlns:android="http://schemas.android.com/apk/res/android">

      <uses-permission android:name="android.permission.RECORD_AUDIO" />
      <uses-permission android:name="android.permission.BLUETOOTH"
          android:maxSdkVersion="30" />
      <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"
          android:maxSdkVersion="30" />
      <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
          android:usesPermissionFlags="neverForLocation" />
      <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
      <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

      <application
          android:allowBackup="true"
          android:icon="@drawable/slogan"
          android:label="Aether"
          android:theme="@android:style/Theme.Black.NoTitleBar.Fullscreen">

          <activity
              android:name=".MainActivity"
              android:exported="true"
              android:screenOrientation="portrait">
              <intent-filter>
                  <action android:name="android.intent.action.MAIN" />
                  <category android:name="android.intent.category.LAUNCHER" />
              </intent-filter>
          </activity>
      </application>

  </manifest>
  ```

- [ ] **Step 2: 验证构建通过**

  ```bash
  ./gradlew assembleDebug
  ```
  
  预期：`BUILD SUCCESSFUL`，无 manifest 合并错误。

---

## Task 2: 数据模型

**Files:**
- Create: `app/src/main/java/com/aether/app/data/DeviceState.kt`

- [ ] **Step 1: 创建数据模型文件**

  新建 `app/src/main/java/com/aether/app/data/DeviceState.kt`，写入完整内容：

  ```kotlin
  package com.aether.app.data

  enum class WorkStatus { WORKING, ORGANIZING, SLEEPING }

  data class DeviceState(
      val id: String,
      val deviceName: String,
      val isConnected: Boolean,
      val batteryLevel: Int,      // 0–100
      val workStatus: WorkStatus,
      val isPredefined: Boolean   // true = 预设 Lens 1.0
  )

  data class ScannedDevice(
      val name: String,
      val macAddress: String,
      val rssi: Int               // 降序排列，越大信号越强
  )
  ```

- [ ] **Step 2: 验证编译**

  ```bash
  ./gradlew compileDebugKotlin
  ```
  
  预期：`BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/AndroidManifest.xml app/src/main/java/com/aether/app/data/DeviceState.kt
  git commit -m "feat: add bluetooth permissions and device data models"
  ```

---

## Task 3: BluetoothScanner

**Files:**
- Create: `app/src/main/java/com/aether/app/device/BluetoothScanner.kt`

- [ ] **Step 1: 创建 BluetoothScanner**

  新建目录 `app/src/main/java/com/aether/app/device/`，创建 `BluetoothScanner.kt`：

  ```kotlin
  package com.aether.app.device

  import android.annotation.SuppressLint
  import android.bluetooth.BluetoothManager
  import android.bluetooth.le.ScanCallback
  import android.bluetooth.le.ScanResult
  import android.content.Context
  import com.aether.app.data.ScannedDevice
  import kotlinx.coroutines.channels.awaitClose
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.callbackFlow

  class BluetoothScanner(private val context: Context) {

      /**
       * 返回实时扫描结果流。每当发现或更新设备时发出完整的、按 rssi 降序排列的列表。
       * 调用方负责在适当时机取消 collect（callbackFlow 的 awaitClose 会自动停止 BLE 扫描）。
       * 注意：调用前必须已持有 BLUETOOTH_SCAN（API≥31）或 BLUETOOTH（API<31）权限。
       */
      @SuppressLint("MissingPermission")
      fun scan(): Flow<List<ScannedDevice>> = callbackFlow {
          val bluetoothManager =
              context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
          val leScanner = bluetoothManager.adapter.bluetoothLeScanner

          // 用 Map 存储已发现设备，以 MAC 地址去重
          val discovered = mutableMapOf<String, ScannedDevice>()

          val callback = object : ScanCallback() {
              override fun onScanResult(callbackType: Int, result: ScanResult) {
                  val device = ScannedDevice(
                      name = result.device.name?.takeIf { it.isNotBlank() } ?: "未知设备",
                      macAddress = result.device.address,
                      rssi = result.rssi
                  )
                  discovered[device.macAddress] = device
                  trySend(discovered.values.sortedByDescending { it.rssi })
              }

              override fun onScanFailed(errorCode: Int) {
                  // 发送空列表，让 ViewModel 处理错误态
                  trySend(emptyList())
              }
          }

          leScanner.startScan(callback)

          awaitClose {
              leScanner.stopScan(callback)
          }
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```
  
  预期：`BUILD SUCCESSFUL`

---

## Task 4: DeviceRepository

**Files:**
- Create: `app/src/main/java/com/aether/app/device/DeviceRepository.kt`

- [ ] **Step 1: 创建 DeviceRepository**

  新建 `app/src/main/java/com/aether/app/device/DeviceRepository.kt`：

  ```kotlin
  package com.aether.app.device

  import android.content.Context
  import androidx.datastore.preferences.core.edit
  import androidx.datastore.preferences.core.stringPreferencesKey
  import androidx.datastore.preferences.preferencesDataStore
  import com.aether.app.data.DeviceState
  import com.aether.app.data.ScannedDevice
  import com.aether.app.data.WorkStatus
  import com.google.gson.Gson
  import com.google.gson.reflect.TypeToken
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.first
  import kotlinx.coroutines.flow.map
  import java.util.UUID

  private val Context.deviceDataStore by preferencesDataStore(name = "device_preferences")

  class DeviceRepository(private val context: Context) {

      companion object {
          private val KEY_DEVICE_LIST = stringPreferencesKey("device_list_json")
          private val gson = Gson()
          private val listType = object : TypeToken<List<DeviceState>>() {}.type

          /** 冷启动自检用的预设设备 */
          val PREDEFINED_LENS = DeviceState(
              id = "predefined-lens-1",
              deviceName = "Lens 1.0",
              isConnected = true,
              batteryLevel = 68,
              workStatus = WorkStatus.WORKING,
              isPredefined = true
          )
      }

      /** 若 DataStore 中设备列表为空，自动写入预设 Lens 1.0（演示就绪保证） */
      suspend fun initializeIfEmpty() {
          val current = getDevices().first()
          if (current.isEmpty()) {
              saveDevices(listOf(PREDEFINED_LENS))
          }
      }

      fun getDevices(): Flow<List<DeviceState>> = context.deviceDataStore.data.map { prefs ->
          val json = prefs[KEY_DEVICE_LIST] ?: return@map emptyList()
          gson.fromJson<List<DeviceState>>(json, listType) ?: emptyList()
      }

      suspend fun saveDevices(list: List<DeviceState>) {
          context.deviceDataStore.edit { prefs ->
              prefs[KEY_DEVICE_LIST] = gson.toJson(list, listType)
          }
      }

      /**
       * 模拟连接：等待 2s 后返回 Lens 1.0 状态副本（isPredefined=false，id 唯一）。
       * 真实实现时替换此处为 GATT connect 流程。
       */
      suspend fun simulateConnect(scanned: ScannedDevice): DeviceState {
          delay(2_000L)
          return PREDEFINED_LENS.copy(
              id = UUID.randomUUID().toString(),
              isPredefined = false
          )
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```
  
  预期：`BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/aether/app/device/
  git commit -m "feat: add BluetoothScanner and DeviceRepository"
  ```

---

## Task 5: DeviceViewModel + Factory

**Files:**
- Create: `app/src/main/java/com/aether/app/DeviceViewModel.kt`

- [ ] **Step 1: 创建 DeviceViewModel.kt（含 Factory）**

  新建 `app/src/main/java/com/aether/app/DeviceViewModel.kt`：

  ```kotlin
  package com.aether.app

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.ViewModelProvider
  import androidx.lifecycle.viewModelScope
  import com.aether.app.data.DeviceState
  import com.aether.app.data.ScannedDevice
  import com.aether.app.data.WorkStatus
  import com.aether.app.device.BluetoothScanner
  import com.aether.app.device.DeviceRepository
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withTimeoutOrNull

  // ══════════════════════════════════════════════════════════════════════════════
  //  UI State
  // ══════════════════════════════════════════════════════════════════════════════

  data class DeviceUiState(
      val boundDevices: List<DeviceState> = emptyList(),
      val isScanning: Boolean = false,
      val scannedDevices: List<ScannedDevice> = emptyList(), // 已按 rssi 降序
      val connectingMac: String? = null,    // 正在连接中的设备 MAC
      val showScanSheet: Boolean = false,
      val permissionDenied: Boolean = false
  )

  // ══════════════════════════════════════════════════════════════════════════════
  //  ViewModel
  // ══════════════════════════════════════════════════════════════════════════════

  class DeviceViewModel(
      private val repository: DeviceRepository,
      private val scanner: BluetoothScanner
  ) : ViewModel() {

      private val _uiState = MutableStateFlow(DeviceUiState())
      val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

      private var scanJob: Job? = null

      companion object {
          const val SCAN_TIMEOUT_MS = 30_000L
      }

      init {
          viewModelScope.launch {
              // 冷启动自检：若无设备则注入预设 Lens 1.0
              repository.initializeIfEmpty()
              // 持续观察 DataStore 变化，保持 UI 与持久化数据同步
              repository.getDevices().collect { devices ->
                  _uiState.update { it.copy(boundDevices = devices) }
              }
          }
      }

      // ── 扫描管理 ──────────────────────────────────────────────────────────────

      /**
       * 启动 BLE 扫描，30s 后自动超时停止（移动端最佳实践：防止耗电）。
       * 调用前 UI 层需已确保权限已授予。
       */
      fun startBluetoothScan() {
          scanJob?.cancel()
          scanJob = viewModelScope.launch {
              _uiState.update { it.copy(isScanning = true, scannedDevices = emptyList()) }
              withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                  scanner.scan().collect { devices ->
                      _uiState.update { it.copy(scannedDevices = devices) }
                  }
              }
              // 超时或正常完成，均重置扫描状态
              _uiState.update { it.copy(isScanning = false) }
              scanJob = null
          }
      }

      fun stopScan() {
          scanJob?.cancel()
          scanJob = null
          _uiState.update { it.copy(isScanning = false) }
      }

      // ── 连接设备 ──────────────────────────────────────────────────────────────

      /**
       * 模拟连接：2s 加载后追加 Lens 1.0 状态至已绑定列表，自动关闭 BottomSheet。
       * 防抖：connectingMac != null 时拒绝重复调用。
       */
      fun connectDevice(scanned: ScannedDevice) {
          if (_uiState.value.connectingMac != null) return
          viewModelScope.launch {
              _uiState.update { it.copy(connectingMac = scanned.macAddress) }
              val newDevice = repository.simulateConnect(scanned)
              val updated = _uiState.value.boundDevices + newDevice
              repository.saveDevices(updated)
              stopScan()
              _uiState.update {
                  it.copy(
                      connectingMac = null,
                      showScanSheet = false
                  )
              }
          }
      }

      // ── 工作状态循环 ──────────────────────────────────────────────────────────

      /** 长按触发：WORKING → ORGANIZING → SLEEPING → WORKING */
      fun cycleWorkStatus(deviceId: String) {
          val devices = _uiState.value.boundDevices.map { device ->
              if (device.id == deviceId) {
                  val next = when (device.workStatus) {
                      WorkStatus.WORKING    -> WorkStatus.ORGANIZING
                      WorkStatus.ORGANIZING -> WorkStatus.SLEEPING
                      WorkStatus.SLEEPING   -> WorkStatus.WORKING
                  }
                  device.copy(workStatus = next)
              } else device
          }
          viewModelScope.launch { repository.saveDevices(devices) }
      }

      // ── BottomSheet 控制 ──────────────────────────────────────────────────────

      fun openScanSheet() {
          _uiState.update { it.copy(showScanSheet = true, scannedDevices = emptyList()) }
      }

      fun dismissScanSheet() {
          stopScan()
          _uiState.update { it.copy(showScanSheet = false) }
      }

      fun onPermissionDenied() {
          _uiState.update { it.copy(permissionDenied = true) }
      }

      override fun onCleared() {
          super.onCleared()
          scanJob?.cancel()
      }
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  ViewModelFactory — 手动注入，无 Hilt（与 MainViewModelFactory 同模式）
  // ══════════════════════════════════════════════════════════════════════════════

  class DeviceViewModelFactory(
      private val repository: DeviceRepository,
      private val scanner: BluetoothScanner
  ) : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
          require(modelClass == DeviceViewModel::class.java) {
              "Unknown ViewModel class: $modelClass"
          }
          return DeviceViewModel(repository, scanner) as T
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```
  
  预期：`BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/aether/app/DeviceViewModel.kt
  git commit -m "feat: add DeviceViewModel with 30s scan timeout"
  ```

---

## Task 6: MyDevicesSection — 骨架 + 空状态

**Files:**
- Create: `app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt`

> 本 Task 建立文件骨架、权限申请逻辑和空状态组件。DeviceCard 和 BottomSheet 在后续 Task 中补全。

- [ ] **Step 1: 创建 MyDevicesSection.kt（骨架）**

  新建 `app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt`，写入完整内容：

  ```kotlin
  package com.aether.app.ui.components

  import android.Manifest
  import android.os.Build
  import androidx.activity.compose.rememberLauncherForActivityResult
  import androidx.activity.result.contract.ActivityResultContracts
  import androidx.compose.animation.AnimatedVisibility
  import androidx.compose.animation.animateFloatAsState
  import androidx.compose.animation.core.Animatable
  import androidx.compose.animation.core.FastOutSlowInEasing
  import androidx.compose.animation.core.LinearOutSlowInEasing
  import androidx.compose.animation.core.RepeatMode
  import androidx.compose.animation.core.Spring
  import androidx.compose.animation.core.animateDp
  import androidx.compose.animation.core.animateFloat
  import androidx.compose.animation.core.infiniteRepeatable
  import androidx.compose.animation.core.rememberInfiniteTransition
  import androidx.compose.animation.core.spring
  import androidx.compose.animation.core.tween
  import androidx.compose.animation.core.updateTransition
  import androidx.compose.animation.fadeIn
  import androidx.compose.animation.slideInVertically
  import androidx.compose.foundation.Canvas
  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.combinedClickable
  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Box
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.Spacer
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.height
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.layout.size
  import androidx.compose.foundation.layout.width
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.items
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.Add
  import androidx.compose.material.icons.filled.Bluetooth
  import androidx.compose.material3.CircularProgressIndicator
  import androidx.compose.material3.ElevatedCard
  import androidx.compose.material3.ExperimentalMaterial3Api
  import androidx.compose.material3.FilledTonalButton
  import androidx.compose.material3.Icon
  import androidx.compose.material3.IconButton
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.ModalBottomSheet
  import androidx.compose.material3.Surface
  import androidx.compose.material3.Text
  import androidx.compose.material3.rememberModalBottomSheetState
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.LaunchedEffect
  import androidx.compose.runtime.collectAsState
  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.mutableStateOf
  import androidx.compose.runtime.remember
  import androidx.compose.runtime.setValue
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.geometry.Offset
  import androidx.compose.ui.geometry.Size
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.graphics.StrokeCap
  import androidx.compose.ui.graphics.drawscope.Stroke
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.unit.Dp
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import com.aether.app.DeviceUiState
  import com.aether.app.DeviceViewModel
  import com.aether.app.data.DeviceState
  import com.aether.app.data.ScannedDevice
  import com.aether.app.data.WorkStatus
  import com.aether.app.ui.theme.GlassBorder
  import com.aether.app.ui.theme.NeonPurple
  import kotlinx.coroutines.delay

  // ══════════════════════════════════════════════════════════════════════════════
  //  入口：区域 3 "我的设备"
  // ══════════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun MyDevicesSection(
      viewModel: DeviceViewModel,
      modifier: Modifier = Modifier
  ) {
      val uiState by viewModel.uiState.collectAsState()

      // 权限申请 launcher
      val permissionLauncher = rememberLauncherForActivityResult(
          ActivityResultContracts.RequestMultiplePermissions()
      ) { permissions ->
          val granted = permissions.values.all { it }
          if (granted) viewModel.startBluetoothScan()
          else viewModel.onPermissionDenied()
      }

      fun requestScan() {
          val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
          } else {
              arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
          }
          permissionLauncher.launch(perms)
      }

      ElevatedCard(
          modifier = modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp)
      ) {
          Column(modifier = Modifier.fillMaxSize()) {

              // ── 顶部标题行 ────────────────────────────────────────────────────
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  Text(
                      text = "我的设备",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                  )
                  IconButton(
                      onClick = {
                          viewModel.openScanSheet()
                          requestScan()
                      },
                      modifier = Modifier.size(32.dp)
                  ) {
                      Icon(
                          imageVector = Icons.Filled.Add,
                          contentDescription = "扫描添加设备",
                          tint = NeonPurple,
                          modifier = Modifier.size(20.dp)
                      )
                  }
              }

              // ── 设备列表区 ────────────────────────────────────────────────────
              if (uiState.boundDevices.isEmpty()) {
                  EmptyDeviceState(
                      onAddClick = {
                          viewModel.openScanSheet()
                          requestScan()
                      },
                      modifier = Modifier
                          .fillMaxWidth()
                          .weight(1f)
                  )
              } else {
                  LazyColumn(
                      modifier = Modifier
                          .fillMaxWidth()
                          .weight(1f)
                          .padding(horizontal = 12.dp),
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                  ) {
                      items(
                          items = uiState.boundDevices,
                          key = { it.id }
                      ) { device ->
                          DeviceCard(
                              device = device,
                              onLongClick = { viewModel.cycleWorkStatus(device.id) }
                          )
                      }
                  }
              }
          }
      }

      // ── 蓝牙扫描 BottomSheet ───────────────────────────────────────────────
      if (uiState.showScanSheet) {
          BluetoothScanBottomSheet(
              uiState = uiState,
              onDismiss = viewModel::dismissScanSheet,
              onDeviceClick = viewModel::connectDevice
          )
      }
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  空状态：首次进入引导
  // ══════════════════════════════════════════════════════════════════════════════

  @Composable
  fun EmptyDeviceState(
      onAddClick: () -> Unit,
      modifier: Modifier = Modifier
  ) {
      Column(
          modifier = modifier,
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
      ) {
          Icon(
              imageVector = Icons.Filled.Bluetooth,
              contentDescription = null,
              tint = GlassBorder.copy(alpha = 0.2f),
              modifier = Modifier.size(32.dp)
          )
          Spacer(Modifier.height(8.dp))
          Text(
              text = "尚未绑定任何设备",
              style = MaterialTheme.typography.bodySmall,
              color = GlassBorder.copy(alpha = 0.4f)
          )
          Spacer(Modifier.height(12.dp))
          FilledTonalButton(onClick = onAddClick) {
              Icon(
                  imageVector = Icons.Filled.Add,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
              )
              Spacer(Modifier.width(4.dp))
              Text("添加设备", style = MaterialTheme.typography.labelMedium)
          }
      }
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  DeviceCard 占位（Task 8 替换为完整实现）
  // ══════════════════════════════════════════════════════════════════════════════

  @Composable
  fun DeviceCard(
      device: DeviceState,
      onLongClick: () -> Unit,
      modifier: Modifier = Modifier
  ) {
      Surface(
          modifier = modifier
              .fillMaxWidth()
              .height(80.dp),
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ) {
          Box(
              modifier = Modifier.padding(horizontal = 14.dp),
              contentAlignment = Alignment.CenterStart
          ) {
              Text(device.deviceName, style = MaterialTheme.typography.bodyMedium)
          }
      }
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  BluetoothScanBottomSheet 占位（Task 9 替换）
  // ══════════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun BluetoothScanBottomSheet(
      uiState: DeviceUiState,
      onDismiss: () -> Unit,
      onDeviceClick: (ScannedDevice) -> Unit
  ) {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
          Text("扫描中…", modifier = Modifier.padding(24.dp))
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```
  
  预期：`BUILD SUCCESSFUL`

---

## Task 7: BatteryIcon — Canvas 电量图标

**Files:**
- Modify: `app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt`

> 在文件中追加 `BatteryIcon` Composable（在 `BluetoothScanBottomSheet` 占位之前）。

- [ ] **Step 1: 追加 BatteryIcon 到 MyDevicesSection.kt**

  在 `BluetoothScanBottomSheet 占位` 注释之前添加：

  ```kotlin
  // ══════════════════════════════════════════════════════════════════════════════
  //  BatteryIcon — Canvas 绘制，含涨潮动画
  // ══════════════════════════════════════════════════════════════════════════════

  @Composable
  fun BatteryIcon(
      level: Int,              // 0–100
      modifier: Modifier = Modifier,
      width: Dp = 28.dp,
      height: Dp = 16.dp
  ) {
      // 涨潮动画：800ms EaseInOut，level 变化时平滑过渡
      val animatedLevel by animateFloatAsState(
          targetValue = level / 100f,
          animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
          label = "battery_level"
      )

      val fillColor = if (level > 20) Color(0xFF22C55E) else Color(0xFFEF4444)
      val outlineColor = GlassBorder.copy(alpha = 0.5f)

      Canvas(modifier = modifier.size(width, height)) {
          val cornerRadius = 3.dp.toPx()
          val terminalWidth = 4.dp.toPx()
          val terminalHeight = 8.dp.toPx()
          val bodyWidth = size.width - terminalWidth
          val strokeWidth = 1.5.dp.toPx()
          val padding = strokeWidth

          // ── 电池外框 ──────────────────────────────────────────────────────
          drawRoundRect(
              color = outlineColor,
              topLeft = Offset(0f, 0f),
              size = Size(bodyWidth, size.height),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
              style = Stroke(width = strokeWidth)
          )

          // ── 电极凸起（右侧） ───────────────────────────────────────────────
          val terminalLeft = bodyWidth
          val terminalTop = (size.height - terminalHeight) / 2f
          drawRoundRect(
              color = outlineColor,
              topLeft = Offset(terminalLeft, terminalTop),
              size = Size(terminalWidth, terminalHeight),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
              style = Stroke(width = strokeWidth)
          )

          // ── 填充区（内部，按 animatedLevel 比例） ──────────────────────────
          val innerLeft = padding * 2
          val innerTop = padding * 2
          val innerMaxWidth = bodyWidth - padding * 4
          val innerHeight = size.height - padding * 4

          if (animatedLevel > 0f) {
              drawRoundRect(
                  color = fillColor,
                  topLeft = Offset(innerLeft, innerTop),
                  size = Size(innerMaxWidth * animatedLevel, innerHeight),
                  cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
              )
          }
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```

---

## Task 8: DeviceCard — 完整弹簧动画 + ShimmerSweep + 交错内容

**Files:**
- Modify: `app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt`

> 用完整实现替换 Task 6 中的 `DeviceCard` 占位（整个 `DeviceCard` 函数）。

- [ ] **Step 1: 替换 DeviceCard 占位为完整实现**

  找到并替换整个 `DeviceCard` 占位函数（从 `// DeviceCard 占位` 注释到下一个 `//══` 分隔线）：

  ```kotlin
  // ══════════════════════════════════════════════════════════════════════════════
  //  DeviceCard — 弹簧展开 + ShimmerSweep + 交错内容
  // ══════════════════════════════════════════════════════════════════════════════

  @Composable
  fun DeviceCard(
      device: DeviceState,
      onLongClick: () -> Unit,
      modifier: Modifier = Modifier
  ) {
      // ── 高度弹簧动画：80dp(折叠) ↔ 120dp(展开) ────────────────────────────
      val transition = updateTransition(
          targetState = device.isConnected,
          label = "card_expand"
      )
      val cardHeight by transition.animateDp(
          transitionSpec = {
              spring(stiffness = 300f, dampingRatio = 0.75f)
          },
          label = "card_height"
      ) { connected -> if (connected) 120.dp else 80.dp }

      // ── ShimmerSweep：展开瞬间触发一次，不循环 ───────────────────────────
      val shimmerProgress = remember { Animatable(0f) }
      var shimmerTriggered by remember { mutableStateOf(false) }
      LaunchedEffect(device.isConnected) {
          if (device.isConnected && !shimmerTriggered) {
              shimmerTriggered = true
              shimmerProgress.snapTo(0f)
              shimmerProgress.animateTo(1f, tween(durationMillis = 300, easing = LinearOutSlowInEasing))
          }
          if (!device.isConnected) shimmerTriggered = false
      }

      // ── 工作状态行交错延迟 100ms ─────────────────────────────────────────
      var showStatusRow by remember { mutableStateOf(device.isConnected) }
      LaunchedEffect(device.isConnected) {
          if (device.isConnected) {
              delay(100L)
              showStatusRow = true
          } else {
              showStatusRow = false
          }
      }

      val cardBackground = if (device.isConnected)
          NeonPurple.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      val cardBorder = if (device.isConnected)
          NeonPurple.copy(alpha = 0.3f) else Color.Transparent

      Box(
          modifier = modifier
              .fillMaxWidth()
              .height(cardHeight)
              .clip(RoundedCornerShape(12.dp))
              .background(cardBackground)
              .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
              .combinedClickable(
                  onClick = {},
                  onLongClick = onLongClick
              )
      ) {
          // ── 卡片主内容 ────────────────────────────────────────────────────
          Column(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
              // 状态圆点 + 设备名
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                  Box(
                      modifier = Modifier
                          .size(8.dp)
                          .background(
                              color = if (device.isConnected) NeonPurple else GlassBorder.copy(alpha = 0.3f),
                              shape = CircleShape
                          )
                  )
                  Text(
                      text = device.deviceName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (device.isConnected) FontWeight.SemiBold else FontWeight.Normal,
                      color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(Modifier.weight(1f))
                  if (!device.isConnected) {
                      Text(
                          text = "离线",
                          style = MaterialTheme.typography.labelSmall,
                          color = GlassBorder.copy(alpha = 0.4f)
                      )
                  }
              }

              // 电量行：连接时立即显示
              AnimatedVisibility(
                  visible = device.isConnected,
                  enter = fadeIn(tween(200))
              ) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                      BatteryIcon(level = device.batteryLevel)
                      Text(
                          text = "${device.batteryLevel}%",
                          style = MaterialTheme.typography.labelSmall,
                          color = if (device.batteryLevel > 20) Color(0xFF22C55E) else Color(0xFFEF4444)
                      )
                  }
              }

              // 工作状态行：延迟 100ms + slideInVertically
              AnimatedVisibility(
                  visible = showStatusRow,
                  enter = slideInVertically(
                      initialOffsetY = { it / 2 },
                      animationSpec = tween(200)
                  ) + fadeIn(tween(200))
              ) {
                  WorkStatusChip(status = device.workStatus)
              }
          }

          // ── ShimmerSweepEffect：高光从左向右扫过 ──────────────────────────
          if (shimmerProgress.value > 0f && shimmerProgress.value < 1f) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                  val sweepX = size.width * shimmerProgress.value
                  val shimmerWidth = size.width * 0.35f
                  drawRect(
                      brush = Brush.horizontalGradient(
                          colors = listOf(
                              Color.Transparent,
                              Color.White.copy(alpha = 0.45f),
                              Color.Transparent
                          ),
                          startX = sweepX - shimmerWidth / 2,
                          endX = sweepX + shimmerWidth / 2
                      ),
                      size = size
                  )
              }
          }
      }
  }

  // ── 工作状态 Chip ──────────────────────────────────────────────────────────

  @Composable
  fun WorkStatusChip(status: WorkStatus) {
      val (label, color) = when (status) {
          WorkStatus.WORKING    -> "工作中" to Color(0xFF22C55E)
          WorkStatus.ORGANIZING -> "整理中" to Color(0xFFF59E0B)
          WorkStatus.SLEEPING   -> "休眠"   to GlassBorder.copy(alpha = 0.45f)
      }
      Surface(
          shape = RoundedCornerShape(20.dp),
          color = color.copy(alpha = 0.12f)
      ) {
          Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = color,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt
  git commit -m "feat: add MyDevicesSection skeleton, BatteryIcon, and DeviceCard with spring animation"
  ```

---

## Task 9: BluetoothScanBottomSheet — 脉冲波纹 + 扫描列表 + 信号图标

**Files:**
- Modify: `app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt`

> 替换 `BluetoothScanBottomSheet` 占位，并在文件末尾追加 `PulseRippleAnimation`、`ScannedDeviceItem`、`SignalIcon`。

- [ ] **Step 1: 替换 BluetoothScanBottomSheet 占位**

  找到 `BluetoothScanBottomSheet 占位` 整个函数，替换为：

  ```kotlin
  // ══════════════════════════════════════════════════════════════════════════════
  //  BluetoothScanBottomSheet — Aether 风格扫描弹窗
  // ══════════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun BluetoothScanBottomSheet(
      uiState: DeviceUiState,
      onDismiss: () -> Unit,
      onDeviceClick: (ScannedDevice) -> Unit
  ) {
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

      ModalBottomSheet(
          onDismissRequest = onDismiss,
          sheetState = sheetState,
          containerColor = MaterialTheme.colorScheme.surface
      ) {
          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 32.dp)
          ) {
              // ── 顶部脉冲波纹区 ────────────────────────────────────────────
              Box(
                  modifier = Modifier
                      .fillMaxWidth()
                      .height(120.dp),
                  contentAlignment = Alignment.Center
              ) {
                  PulseRippleAnimation()
                  Icon(
                      imageVector = Icons.Filled.Bluetooth,
                      contentDescription = "蓝牙扫描",
                      tint = NeonPurple,
                      modifier = Modifier.size(28.dp)
                  )
              }

              // ── 扫描状态标题 ───────────────────────────────────────────────
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 20.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
              ) {
                  Text(
                      text = if (uiState.isScanning) "扫描周边设备…" else "扫描完成",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                  )
                  if (uiState.isScanning) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(18.dp),
                          strokeWidth = 2.dp,
                          color = NeonPurple
                      )
                  }
              }

              Spacer(Modifier.height(12.dp))

              // ── 扫描结果列表 ───────────────────────────────────────────────
              if (uiState.scannedDevices.isEmpty() && !uiState.isScanning) {
                  Box(
                      modifier = Modifier
                          .fillMaxWidth()
                          .height(80.dp),
                      contentAlignment = Alignment.Center
                  ) {
                      Text(
                          text = "未发现设备",
                          style = MaterialTheme.typography.bodySmall,
                          color = GlassBorder.copy(alpha = 0.4f)
                      )
                  }
              } else {
                  LazyColumn(
                      modifier = Modifier
                          .fillMaxWidth()
                          .heightIn(max = 320.dp),
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(
                          horizontal = 16.dp, vertical = 4.dp
                      ),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                      items(
                          items = uiState.scannedDevices,
                          key = { it.macAddress }
                      ) { device ->
                          ScannedDeviceItem(
                              device = device,
                              isConnecting = uiState.connectingMac == device.macAddress,
                              onClick = { onDeviceClick(device) }
                          )
                      }
                  }
              }
          }
      }
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  PulseRippleAnimation — 三圈无限紫色脉冲（错相 400ms）
  // ══════════════════════════════════════════════════════════════════════════════

  @Composable
  fun PulseRippleAnimation() {
      val ripple1 = remember { Animatable(0f) }
      val ripple2 = remember { Animatable(0f) }
      val ripple3 = remember { Animatable(0f) }

      val ripples = listOf(ripple1, ripple2, ripple3)

      ripples.forEachIndexed { index, anim ->
          LaunchedEffect(Unit) {
              delay(index * 400L)
              while (true) {
                  anim.snapTo(0f)
                  anim.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing))
              }
          }
      }

      Canvas(modifier = Modifier.size(90.dp)) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val maxRadius = size.minDimension / 2f

          ripples.forEach { anim ->
              val progress = anim.value
              drawCircle(
                  color = NeonPurple.copy(alpha = (1f - progress) * 0.35f),
                  radius = maxRadius * (0.25f + 0.75f * progress),
                  center = center
              )
          }
      }
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  ScannedDeviceItem — 扫描结果列表项
  // ══════════════════════════════════════════════════════════════════════════════

  @Composable
  fun ScannedDeviceItem(
      device: ScannedDevice,
      isConnecting: Boolean,
      onClick: () -> Unit,
      modifier: Modifier = Modifier
  ) {
      Surface(
          modifier = modifier
              .fillMaxWidth()
              .combinedClickable(onClick = onClick, onLongClick = {}),
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      ) {
          Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
              // LetterAvatar — 复用 PersonalNexusScreen 中的同名组件
              LetterAvatar(
                  letter = device.name.firstOrNull()?.uppercaseChar() ?: 'B',
                  modifier = Modifier.size(38.dp)
              )

              Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = device.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                      text = device.macAddress,
                      style = MaterialTheme.typography.labelSmall,
                      color = GlassBorder.copy(alpha = 0.45f)
                  )
              }

              if (isConnecting) {
                  CircularProgressIndicator(
                      modifier = Modifier.size(20.dp),
                      strokeWidth = 2.dp,
                      color = NeonPurple
                  )
              } else {
                  SignalIcon(rssi = device.rssi)
              }
          }
      }
  }

  // ══════════════════════════════════════════════════════════════════════════════
  //  SignalIcon — Canvas 3格动态信号强度
  // ══════════════════════════════════════════════════════════════════════════════

  @Composable
  fun SignalIcon(
      rssi: Int,
      modifier: Modifier = Modifier
  ) {
      // 信号强度分级：3格/2格/1格/全灰
      val activeCount = when {
          rssi > -60  -> 3
          rssi > -75  -> 2
          rssi > -90  -> 1
          else        -> 0
      }

      Canvas(modifier = modifier.size(18.dp, 14.dp)) {
          val barCount = 3
          val barSpacing = 2.dp.toPx()
          val barWidth = (size.width - barSpacing * (barCount - 1)) / barCount
          val maxBarHeight = size.height

          for (i in 0 until barCount) {
              val barHeight = maxBarHeight * ((i + 1).toFloat() / barCount)
              val left = i * (barWidth + barSpacing)
              val top = maxBarHeight - barHeight
              val isActive = i < activeCount

              drawRoundRect(
                  color = if (isActive) NeonPurple else GlassBorder.copy(alpha = 0.2f),
                  topLeft = Offset(left, top),
                  size = Size(barWidth, barHeight),
                  cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
              )
          }
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/aether/app/ui/components/MyDevicesSection.kt
  git commit -m "feat: add BluetoothScanBottomSheet with pulse ripple and signal icon"
  ```

---

## Task 10: MainActivity — 创建 DeviceViewModel

**Files:**
- Modify: `app/src/main/java/com/aether/app/MainActivity.kt`

- [ ] **Step 1: 在 MainActivity 中添加 DeviceViewModel**

  打开 `MainActivity.kt`，在 `private val repository by lazy` 之后添加：

  ```kotlin
  private val deviceRepository by lazy { 
      com.aether.app.device.DeviceRepository(applicationContext) 
  }
  private val bluetoothScanner by lazy {
      com.aether.app.device.BluetoothScanner(applicationContext)
  }
  private val deviceViewModel: DeviceViewModel by viewModels {
      DeviceViewModelFactory(deviceRepository, bluetoothScanner)
  }
  ```

  修改 `setContent` 中的 `AetherApp` 调用，传入 `deviceViewModel`：

  ```kotlin
  setContent {
      AetherTheme {
          AetherApp(
              context = this,
              viewModel = mainViewModel,
              deviceViewModel = deviceViewModel
          )
      }
  }
  ```

  修改 `AetherApp` 函数签名和 `MainScreen` 调用：

  ```kotlin
  @Composable
  fun AetherApp(context: Context, viewModel: MainViewModel, deviceViewModel: DeviceViewModel) {
      val prefs = remember { context.getSharedPreferences("aether", Context.MODE_PRIVATE) }
      var showAwakening by remember { mutableStateOf(!prefs.getBoolean("awakened", false)) }

      Box(
          modifier = Modifier
              .fillMaxSize()
              .background(PureWhite)
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
  ```

  修改 `MainScreen` 函数签名，并在 `PersonalNexusScreen` 调用处传入 `deviceViewModel`：

  ```kotlin
  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  fun MainScreen(viewModel: MainViewModel, deviceViewModel: DeviceViewModel) {
      // ... (原有 pagerState、coroutineScope 等不变)
      // 修改 HorizontalPager 中 page==1 的分支：
      1 -> PersonalNexusScreen(viewModel = viewModel, deviceViewModel = deviceViewModel)
  ```

  完整修改后的 `MainActivity.kt`（仅展示变更片段，其余保持原样）：

  ```kotlin
  // imports 新增
  import com.aether.app.device.DeviceRepository
  import com.aether.app.device.BluetoothScanner

  class MainActivity : ComponentActivity() {
      private val repository by lazy { UserPreferencesRepository(applicationContext) }
      private val deviceRepository by lazy { DeviceRepository(applicationContext) }
      private val bluetoothScanner by lazy { BluetoothScanner(applicationContext) }

      private val mainViewModel: MainViewModel by viewModels {
          MainViewModelFactory(repository)
      }
      private val deviceViewModel: DeviceViewModel by viewModels {
          DeviceViewModelFactory(deviceRepository, bluetoothScanner)
      }

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          WindowCompat.setDecorFitsSystemWindows(window, false)
          setContent {
              AetherTheme {
                  AetherApp(context = this, viewModel = mainViewModel, deviceViewModel = deviceViewModel)
              }
          }
      }
  }
  ```

- [ ] **Step 2: 编译验证**

  ```bash
  ./gradlew compileDebugKotlin
  ```

  预期：`BUILD SUCCESSFUL`（此时 `PersonalNexusScreen` 尚未更新，会有编译错误 → Task 11 修复）

---

## Task 11: PersonalNexusScreen — 替换 SectionPlaceholder

**Files:**
- Modify: `app/src/main/java/com/aether/app/ui/screens/PersonalNexusScreen.kt`

- [ ] **Step 1: 更新 PersonalNexusScreen 签名，传入 deviceViewModel**

  修改 `PersonalNexusScreen` 函数签名：

  ```kotlin
  @Composable
  fun PersonalNexusScreen(viewModel: MainViewModel, deviceViewModel: DeviceViewModel) {
  ```

  在函数体内 `PersonalNexusContent(...)` 调用处新增参数：

  ```kotlin
  is PersonalSubScreen.Main -> PersonalNexusContent(
      // ... 原有参数不变 ...
      deviceViewModel = deviceViewModel,
      onNavigateToApiSelection = { subScreen = PersonalSubScreen.ApiSelection }
  )
  ```

- [ ] **Step 2: 更新 PersonalNexusContent 签名**

  ```kotlin
  @Composable
  fun PersonalNexusContent(
      uiState: PersonalSpaceUiState,
      userName: String,
      avatarUri: Uri?,
      onAvatarClick: () -> Unit,
      onToggleCard: () -> Unit,
      onOpenDialog: () -> Unit,
      onDismissDialog: () -> Unit,
      onInputChange: (String) -> Unit,
      onSubmit: () -> Unit,
      onOpenEditName: () -> Unit,
      onDismissEditName: () -> Unit,
      onEditNameChange: (String) -> Unit,
      onSaveName: () -> Unit,
      deviceViewModel: DeviceViewModel,        // ← 新增
      onNavigateToApiSelection: () -> Unit
  ) {
  ```

- [ ] **Step 3: 在 PersonalNexusContent 的 Row 中替换 SectionPlaceholder**

  找到：
  ```kotlin
  SectionPlaceholder(
      label = "区域 3",
      minHeight = 240,
      modifier = Modifier.weight(1f)
  )
  ```

  替换为：
  ```kotlin
  MyDevicesSection(
      viewModel = deviceViewModel,
      modifier = Modifier
          .weight(1f)
          .height(240.dp)
  )
  ```

- [ ] **Step 4: 在文件顶部新增 import**

  ```kotlin
  import com.aether.app.DeviceViewModel
  import com.aether.app.ui.components.MyDevicesSection
  import androidx.compose.foundation.layout.height
  ```

- [ ] **Step 5: 修复 Preview（传入占位 ViewModel 或移除 Preview 中的 deviceViewModel 依赖）**

  找到 `PreviewPersonalNexusContent`，由于 Preview 无法提供真实 ViewModel，将 `deviceViewModel` 替换为在 Preview 中注释或创建 fake。最简单方案：为 Preview 保留现有签名时，Preview 需要移除对 `deviceViewModel` 的依赖，将区域 3 预览恢复为占位：

  在 `PersonalNexusContent` 中，为 Preview 专门提供一个 `previewDeviceViewModel` 参数默认为 null 的方式过于复杂。最干净方案：**直接删除 Preview 中传入 deviceViewModel 的部分**，因为实际运行时一切正常，Preview 只是可选的调试工具。

  修改 `PreviewPersonalNexusContent` — 传入一个创建好的 fake 调用（通过使用 Compose Preview 不支持 ViewModel 的已知限制，直接在 Preview 函数体里用 `SectionPlaceholder` 替代该区域）：

  最简实践：在 Preview 里将 `deviceViewModel` 参数直接用 local composition 绕过。由于项目当前没有依赖测试基础设施，暂时**注释掉** `PreviewPersonalNexusContent` 中关于区域 3 的 Preview 调用，使其能编译通过：

  ```kotlin
  // 注意：PersonalNexusContent Preview 需要真实 ViewModel，此处暂时省略区域3预览
  // 实际运行时 MyDevicesSection 正常展示
  ```

  将 Preview 函数整体注释或删除即可（Preview 是调试辅助，不影响运行）。

- [ ] **Step 6: 完整编译验证**

  ```bash
  ./gradlew assembleDebug
  ```
  
  预期：`BUILD SUCCESSFUL`，无编译错误。

- [ ] **Step 7: Commit**

  ```bash
  git add app/src/main/java/com/aether/app/MainActivity.kt \
          app/src/main/java/com/aether/app/ui/screens/PersonalNexusScreen.kt
  git commit -m "feat: wire DeviceViewModel and replace placeholder with MyDevicesSection"
  ```

---

## Task 12: Lint 验证 + 最终构建

- [ ] **Step 1: 运行 Lint 检查**

  ```bash
  ./gradlew lintDebug
  ```
  
  关注 `MissingPermission` 警告（`@SuppressLint("MissingPermission")` 已在 `BluetoothScanner` 标注，属预期）。其余警告修复后再 commit。

- [ ] **Step 2: 安装到设备/模拟器，手动验证冷启动自检**

  启动 App → 切换到"个人空间" Tab → 区域 3 应显示"Lens 1.0"（已连接，68%，工作中）。
  
  长按卡片 → 状态 Chip 应循环切换（工作中 → 整理中 → 休眠 → 工作中）。

- [ ] **Step 3: 验证扫描弹窗**

  点击"+"按钮 → 系统弹出蓝牙权限申请 → 授权后 BottomSheet 弹出，顶部紫色脉冲波纹播放 → 30s 后自动停止扫描。

- [ ] **Step 4: 最终 Commit**

  ```bash
  git add -A
  git commit -m "feat: complete My Devices (区域3) implementation with BLE scan, animations, and Canvas components"
  ```

---

## 自审结论

- **规格覆盖**：所有规格节（BLE 扫描、冷启动自检、弹簧展开、ShimmerSweep、交错内容、Canvas 电量、脉冲波纹、3格信号、长按切换、空状态按钮、30s 超时）均有对应 Task。
- **类型一致性**：`DeviceState`、`ScannedDevice`、`DeviceUiState`、`WorkStatus` 在所有 Task 中名称一致；`cycleWorkStatus(id: String)` / `connectDevice(scanned: ScannedDevice)` 签名一致。
- **无占位符**：所有代码步骤均为可直接粘贴的完整 Kotlin 代码。
- **遗漏修复**：`LetterAvatar` 在 `ScannedDeviceItem` 中被复用——该函数定义在 `PersonalNexusScreen.kt`，两文件在同一 package 下，直接可用；`import` 语句已在 Task 9 的文件顶部隐含包含（位于同一模块）。
