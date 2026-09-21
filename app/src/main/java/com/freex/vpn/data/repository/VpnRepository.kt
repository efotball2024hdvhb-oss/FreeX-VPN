package com.freex.vpn.data.repository

import android.content.Context
import android.content.Intent
import com.freex.vpn.data.local.SettingsStore
import com.freex.vpn.domain.model.ConnectionState
import com.freex.vpn.domain.model.Server
import com.freex.vpn.domain.model.VpnSnapshot
import com.freex.vpn.domain.repository.VpnRepositoryContract
import com.freex.vpn.vpn.OpenVpnEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VpnRepository(
    private val vpnEngine: OpenVpnEngine,
    private val settings: SettingsStore
) : VpnRepositoryContract {

    private val _snapshot = MutableStateFlow(VpnSnapshot())
    override val snapshot: StateFlow<VpnSnapshot> = _snapshot.asStateFlow()

    override suspend fun connect(server: Server): Result<Unit> {
        _snapshot.value = _snapshot.value.copy(
            state = ConnectionState.CONNECTING,
            server = server
        )
        val res = vpnEngine.connect(server)
        return if (res.isSuccess) {
            _snapshot.value = _snapshot.value.copy(
                state = ConnectionState.CONNECTED,
                connectedAtMillis = System.currentTimeMillis()
            )
            Result.success(Unit)
        } else {
            _snapshot.value = _snapshot.value.copy(
                state = ConnectionState.ERROR,
                error = res.exceptionOrNull()?.message
            )
            Result.failure(res.exceptionOrNull() ?: Exception("Failed to connect"))
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        _snapshot.value = _snapshot.value.copy(state = ConnectionState.DISCONNECTING)
        vpnEngine.disconnect()
        _snapshot.value = VpnSnapshot(state = ConnectionState.DISCONNECTED)
        return Result.success(Unit)
    }

    override fun preparePermission(androidContext: Context): Intent? {
        return null
    }
}
