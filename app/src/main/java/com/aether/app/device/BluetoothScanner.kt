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
        val adapter = bluetoothManager.adapter
        if (adapter == null) {
            close(IllegalStateException("设备不支持蓝牙"))
            return@callbackFlow
        }
        val leScanner = adapter.bluetoothLeScanner
        if (leScanner == null) {
            close(IllegalStateException("蓝牙未开启，请先开启蓝牙"))
            return@callbackFlow
        }

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
