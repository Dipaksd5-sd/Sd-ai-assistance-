package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sd_assistant_memory")

data class AssistantPreferences(
    val assistantName: String = "SD",
    val userName: String = "Boss",
    val isWakeWordEnabled: Boolean = true,
    val isBackgroundServiceEnabled: Boolean = false,
    val preferredVoice: String = "Puck",
    val customApiKey: String = ""
)

class LocalMemoryDataSource(private val context: Context) {

    private object Keys {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val USER_NAME = stringPreferencesKey("user_name")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("bg_service_enabled")
        val PREFERRED_VOICE = stringPreferencesKey("preferred_voice")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
    }

    val preferencesFlow: Flow<AssistantPreferences> = context.dataStore.data.map { prefs ->
        AssistantPreferences(
            assistantName = prefs[Keys.ASSISTANT_NAME] ?: "SD",
            userName = prefs[Keys.USER_NAME] ?: "Boss",
            isWakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: true,
            isBackgroundServiceEnabled = prefs[Keys.BACKGROUND_SERVICE_ENABLED] ?: false,
            preferredVoice = prefs[Keys.PREFERRED_VOICE] ?: "Puck",
            customApiKey = prefs[Keys.CUSTOM_API_KEY] ?: ""
        )
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setBackgroundServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_SERVICE_ENABLED] = enabled }
    }

    suspend fun setPreferredVoice(voice: String) {
        context.dataStore.edit { it[Keys.PREFERRED_VOICE] = voice }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { it[Keys.CUSTOM_API_KEY] = key.trim() }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim() }
    }

    suspend fun clearMemory() {
        context.dataStore.edit { it.clear() }
    }
}
