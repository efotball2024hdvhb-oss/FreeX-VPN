
package com.freex.vpn.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freex.vpn.data.local.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val autoConnect: Boolean = false,
    val autoSelect: Boolean = true,
    val reconnect: Boolean = true
)

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    val state: StateFlow<SettingsState> =
        kotlinx.coroutines.flow.combine(
            store.autoConnect,
            store.autoSelect,
            store.reconnect
        ) { autoConnect, autoSelect, reconnect ->
            SettingsState(autoConnect, autoSelect, reconnect)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setAutoConnect(value: Boolean) = viewModelScope.launch { store.setAutoConnect(value) }
    fun setAutoSelect(value: Boolean) = viewModelScope.launch { store.setAutoSelect(value) }
    fun setReconnect(value: Boolean) = viewModelScope.launch { store.setReconnect(value) }
}
