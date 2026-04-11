package com.aether.app.data

import kotlinx.coroutines.flow.Flow

/**
 * UserPreferencesRepository 的抽象接口，
 * 方便在生产代码中使用 DataStore 实现，在测试中使用纯内存 Fake 实现。
 */
interface IUserPreferencesRepository {
    val userName: Flow<String>
    val avatarUriString: Flow<String?>
    val activeApiId: Flow<String?>
    val apiConfigsJson: Flow<String?>
    val hasSeenDangerWarning: Flow<Boolean>

    suspend fun saveUserName(name: String)
    suspend fun saveAvatarUri(uriString: String)
    suspend fun saveActiveApiId(id: String)
    suspend fun saveApiConfigs(configs: List<ApiConfig>)
    suspend fun saveHasSeenDangerWarning(value: Boolean)
    fun deserializeConfigs(json: String): List<ApiConfig>

    companion object {
        const val DEFAULT_USER_NAME = "江南少"
    }
}
