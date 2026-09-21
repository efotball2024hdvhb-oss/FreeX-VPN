package com.freex.vpn.vpn

import android.content.Context
import com.freex.vpn.domain.model.Server
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OpenVpnEngine(private val context: Context) {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect(server: Server): Result<Unit> {
        return try {
            _isConnected.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            _isConnected.value = false
            Result.failure(e)
        }
    }

    fun disconnect(): Result<Unit> {
        _isConnected.value = false
        return Result.success(Unit)
    }
}
