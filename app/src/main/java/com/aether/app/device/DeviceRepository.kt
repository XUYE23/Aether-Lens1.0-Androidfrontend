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
            deviceName = scanned.name,  // 使用实际扫描到的设备名
            isPredefined = false
        )
    }
}
