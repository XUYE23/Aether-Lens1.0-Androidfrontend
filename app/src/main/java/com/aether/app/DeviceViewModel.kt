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
    val scannedDevices: List<ScannedDevice> = emptyList(), // 保持首次发现顺序稳定
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
            try {
                // 冷启动自检：若无设备则注入预设 Lens 1.0
                repository.initializeIfEmpty()
            } catch (e: Exception) {
                // DataStore 初始化失败时不阻断设备列表的持续观察
            }
        }
        viewModelScope.launch {
            // 持续观察 DataStore 变化，保持 UI 与持久化数据同步
            repository.getDevices().collect { devices ->
                _uiState.update { it.copy(boundDevices = devices) }
            }
        }
    }

    // ── 扫描管理 ──────────────────────────────────────────────────────────────

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

    fun connectDevice(scanned: ScannedDevice) {
        var shouldConnect = false
        _uiState.update { state ->
            if (state.connectingMac != null) return@update state
            shouldConnect = true
            state.copy(connectingMac = scanned.macAddress)
        }
        if (!shouldConnect) return
        viewModelScope.launch {
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

    fun connectAsPrimaryDevice(scanned: ScannedDevice) {
        var shouldConnect = false
        _uiState.update { state ->
            if (state.connectingMac != null) return@update state
            shouldConnect = true
            state.copy(connectingMac = scanned.macAddress)
        }
        if (!shouldConnect) return
        viewModelScope.launch {
            val newDevice = repository.simulateConnect(scanned)
            val retainedDevices = _uiState.value.boundDevices.map { it.copy(isConnected = false) }
            repository.saveDevices(retainedDevices + newDevice)
            stopScan()
            _uiState.update {
                it.copy(
                    connectingMac = null,
                    showScanSheet = false
                )
            }
        }
    }

    fun disconnectDevice(deviceId: String) {
        viewModelScope.launch {
            val devices = _uiState.value.boundDevices.map { device ->
                if (device.id == deviceId) {
                    device.copy(isConnected = false)
                } else {
                    device
                }
            }
            repository.saveDevices(devices)
        }
    }

    // ── 工作状态循环 ──────────────────────────────────────────────────────────

    fun cycleWorkStatus(deviceId: String) {
        viewModelScope.launch {
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
            repository.saveDevices(devices)
        }
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
//  ViewModelFactory — 手动注入，无 Hilt
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
