package com.aether.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) : IUserPreferencesRepository {

    companion object {
        private val KEY_USER_NAME       = stringPreferencesKey("user_name")
        private val KEY_AVATAR_URI      = stringPreferencesKey("avatar_uri")
        private val KEY_ACTIVE_API_ID   = stringPreferencesKey("active_api_id")
        private val KEY_API_CONFIGS_JSON = stringPreferencesKey("api_configs_json")
        private val KEY_HAS_SEEN_DANGER_WARNING = booleanPreferencesKey("has_seen_danger_warning")

        private val gson = Gson()
        private val apiConfigListType = object : TypeToken<List<ApiConfig>>() {}.type
    }

    override val userName: Flow<String> = dataStore.data
        .map { prefs -> prefs[KEY_USER_NAME] ?: IUserPreferencesRepository.DEFAULT_USER_NAME }

    override val avatarUriString: Flow<String?> = dataStore.data
        .map { prefs -> prefs[KEY_AVATAR_URI] }

    override val activeApiId: Flow<String?> = dataStore.data
        .map { prefs -> prefs[KEY_ACTIVE_API_ID] }

    override val apiConfigsJson: Flow<String?> = dataStore.data
        .map { prefs -> prefs[KEY_API_CONFIGS_JSON] }

    override val hasSeenDangerWarning: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_HAS_SEEN_DANGER_WARNING] ?: false }

    override suspend fun saveUserName(name: String) {
        dataStore.edit { it[KEY_USER_NAME] = name }
    }

    override suspend fun saveAvatarUri(uriString: String) {
        dataStore.edit { it[KEY_AVATAR_URI] = uriString }
    }

    override suspend fun saveActiveApiId(id: String) {
        dataStore.edit { it[KEY_ACTIVE_API_ID] = id }
    }

    override suspend fun saveApiConfigs(configs: List<ApiConfig>) {
        dataStore.edit { it[KEY_API_CONFIGS_JSON] = gson.toJson(configs, apiConfigListType) }
    }

    override suspend fun saveHasSeenDangerWarning(value: Boolean) {
        dataStore.edit { it[KEY_HAS_SEEN_DANGER_WARNING] = value }
    }

    override fun deserializeConfigs(json: String): List<ApiConfig> =
        gson.fromJson(json, apiConfigListType)
}
