package com.freex.vpn.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("freex_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val favorites = stringSetPreferencesKey("favorites")
        val recent = stringSetPreferencesKey("recent")
        val autoConnect = booleanPreferencesKey("auto_connect")
        val autoSelect = booleanPreferencesKey("auto_select")
        val reconnect = booleanPreferencesKey("reconnect")
        val dns = androidx.datastore.preferences.core.stringPreferencesKey("dns")
        val protocol = androidx.datastore.preferences.core.stringPreferencesKey("protocol")
        val theme = androidx.datastore.preferences.core.stringPreferencesKey("theme")
        val maxRecent = intPreferencesKey("max_recent")
    }

    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.favorites] ?: emptySet() }
    val recent: Flow<List<String>> = context.dataStore.data.map { (it[Keys.recent] ?: emptySet()).toList() }

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[Keys.autoConnect] ?: false }
    val autoSelect: Flow<Boolean> = context.dataStore.data.map { it[Keys.autoSelect] ?: true }
    val reconnect: Flow<Boolean> = context.dataStore.data.map { it[Keys.reconnect] ?: true }
    val dns: Flow<String> = context.dataStore.data.map { it[Keys.dns] ?: "1.1.1.1" }
    val protocol: Flow<String> = context.dataStore.data.map { it[Keys.protocol] ?: "OpenVPN" }
    val theme: Flow<String> = context.dataStore.data.map { it[Keys.theme] ?: "AMOLED" }

    suspend fun setFavorite(id: String, value: Boolean) {
        context.dataStore.edit { p ->
            val current = (p[Keys.favorites] ?: emptySet()).toMutableSet()
            if (value) current += id else current -= id
            p[Keys.favorites] = current
        }
    }

    suspend fun markRecent(id: String) {
        context.dataStore.edit { p ->
            val current = (p[Keys.recent] ?: emptySet()).toMutableList()
            current.remove(id)
            current.add(0, id)
            p[Keys.recent] = current.take(50).toSet()
        }
    }

    suspend fun setAutoConnect(value: Boolean) { context.dataStore.edit { it[Keys.autoConnect] = value } }
    suspend fun setAutoSelect(value: Boolean) { context.dataStore.edit { it[Keys.autoSelect] = value } }
    suspend fun setReconnect(value: Boolean) { context.dataStore.edit { it[Keys.reconnect] = value } }
    suspend fun setDns(value: String) { context.dataStore.edit { it[Keys.dns] = value } }
}
