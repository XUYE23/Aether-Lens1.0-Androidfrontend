package com.aether.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_USER_NAME       = stringPreferencesKey("user_name")
        private val KEY_AVATAR_URI      = stringPreferencesKey("avatar_uri")
        private val KEY_ACTIVE_API_ID   = stringPreferencesKey("active_api_id")
        private val KEY_API_CONFIGS_JSON = stringPreferencesKey("api_configs_json")
        private val KEY_HAS_SEEN_DANGER_WARNING = booleanPreferencesKey("has_seen_danger_warning")

        const val DEFAULT_USER_NAME = "江南少"

        private val gson = Gson()
        private val apiConfigListType = object : TypeToken<List<ApiConfig>>() {}.type
    }

    val userName: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_USER_NAME] ?: DEFAULT_USER_NAME }

    val avatarUriString: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_AVATAR_URI] }

    val activeApiId: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_ACTIVE_API_ID] }

    val apiConfigsJson: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_API_CONFIGS_JSON] }

    val hasSeenDangerWarning: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_HAS_SEEN_DANGER_WARNING] ?: false }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun saveAvatarUri(uriString: String) {
        context.dataStore.edit { it[KEY_AVATAR_URI] = uriString }
    }

    suspend fun saveActiveApiId(id: String) {
        context.dataStore.edit { it[KEY_ACTIVE_API_ID] = id }
    }

    suspend fun saveApiConfigs(configs: List<ApiConfig>) {
        context.dataStore.edit { it[KEY_API_CONFIGS_JSON] = gson.toJson(configs, apiConfigListType) }
    }

    suspend fun saveHasSeenDangerWarning(value: Boolean) {
        context.dataStore.edit { it[KEY_HAS_SEEN_DANGER_WARNING] = value }
    }

    fun deserializeConfigs(json: String): List<ApiConfig> =
        gson.fromJson(json, apiConfigListType)
}
