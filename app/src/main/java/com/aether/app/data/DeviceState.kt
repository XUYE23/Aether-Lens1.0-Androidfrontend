package com.aether.app.data

import com.google.gson.annotations.SerializedName

enum class WorkStatus {
    @SerializedName("WORKING") WORKING,
    @SerializedName("ORGANIZING") ORGANIZING,
    @SerializedName("SLEEPING") SLEEPING
}

data class DeviceState(
    val id: String,             // 内部唯一标识（UUID），非 MAC 地址；MAC 地址存于 ScannedDevice
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
