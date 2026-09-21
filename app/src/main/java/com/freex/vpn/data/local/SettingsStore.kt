package com.freex.vpn.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "freex_settings")

class SettingsStore(private val context: Context) {
    private val KEY_AUTO_CONNECT = booleanPreferencesKey("auto_connect")
    private val KEY_AUTO_SELECT = booleanPreferencesKey("auto_select")
    private val KEY_RECONNECT = booleanPreferencesKey("reconnect")
    private val KEY_SELECTED_SERVER = stringPreferencesKey("selected_server_id")
    private val KEY_FAVORITES = stringSetPreferencesKey("favorites")
    private val KEY_RECENT = stringSetPreferencesKey("recent")

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_CONNECT] ?: false }
    val autoSelect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_SELECT] ?: false }
    val reconnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_RECONNECT] ?: false }
    val selectedServer: Flow<String?> = context.dataStore.data.map { it[KEY_SELECTED_SERVER] }
    val favorites: Flow<List<String>> = context.dataStore.data.map { (it[KEY_FAVORITES] ?: emptySet()).toList() }
    val recent: Flow<List<String>> = context.dataStore.data.map { (it[KEY_RECENT] ?: emptySet()).toList() }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CONNECT] = enabled }
    }

    suspend fun setAutoSelect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SELECT] = enabled }
    }

    suspend fun setReconnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_RECONNECT] = enabled }
    }

    suspend fun setSelectedServer(id: String) {
        context.dataStore.edit { it[KEY_SELECTED_SERVER] = id }
    }

    suspend fun setFavorite(serverId: String, isFavorite: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (isFavorite) current.add(serverId) else current.remove(serverId)
            prefs[KEY_FAVORITES] = current
        }
    }

    suspend fun markRecent(serverId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RECENT]?.toMutableSet() ?: mutableSetOf()
            current.add(serverId)
            prefs[KEY_RECENT] = current
        }
    }
}
