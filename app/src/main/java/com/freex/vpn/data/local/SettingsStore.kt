package com.freex.vpn.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "freex_settings")

class SettingsStore(private val context: Context) {
    private val KEY_KILL_SWITCH = booleanPreferencesKey("kill_switch")
    private val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
    private val KEY_SELECTED_SERVER = stringPreferencesKey("selected_server_id")

    val killSwitchFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_KILL_SWITCH] ?: false }
    val autoConnectFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_CONNECT] ?: false }
    val selectedServerFlow: Flow<String?> = context.dataStore.data.map { it[KEY_SELECTED_SERVER] }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KILL_SWITCH] = enabled }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CONNECT] = enabled }
    }

    suspend fun setSelectedServer(id: String) {
        context.dataStore.edit { it[KEY_SELECTED_SERVER] = id }
    }
}
