package com.freex.vpn.data.repository

import android.content.Context
import com.freex.vpn.BuildConfig
import com.freex.vpn.domain.model.ConnectionState
import com.freex.vpn.domain.model.Server
import com.freex.vpn.domain.model.VpnSnapshot
import com.freex.vpn.domain.repository.VpnRepositoryContract
import com.freex.vpn.vpn.WireGuardVpnEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request

class VpnRepository(
    private val context: Context,
    private val wireGuardEngine: WireGuardVpnEngine = WireGuardVpnEngine(context)
) : VpnRepositoryContract {

    private val _snapshot = MutableStateFlow(
        VpnSnapshot(
            state = ConnectionState.DISCONNECTED,
            activeServer = null,
            bytesIn = 0L,
            bytesOut = 0L,
            pingMs = 0L
        )
    )
    override val snapshot: StateFlow<VpnSnapshot> = _snapshot.asStateFlow()

    override suspend fun connect(server: Server): Result<Unit> {
        _snapshot.value = _snapshot.value.copy(state = ConnectionState.CONNECTING, activeServer = server)
        val res = wireGuardEngine.connect(server)
        return if (res.isSuccess) {
            _snapshot.value = _snapshot.value.copy(state = ConnectionState.CONNECTED)
            Result.success(Unit)
        } else {
            _snapshot.value = _snapshot.value.copy(state = ConnectionState.ERROR)
            Result.failure(res.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        _snapshot.value = _snapshot.value.copy(state = ConnectionState.DISCONNECTING)
        wireGuardEngine.disconnect()
        _snapshot.value = _snapshot.value.copy(state = ConnectionState.DISCONNECTED, activeServer = null)
        return Result.success(Unit)
    }

    override suspend fun pingActiveServer(): Result<Long> {
        val start = System.currentTimeMillis()
        return try {
            val req = Request.Builder().url(BuildConfig.API_BASE_URL).head().build()
            OkHttpClient().newCall(req).execute().use {
                val ping = System.currentTimeMillis() - start
                _snapshot.value = _snapshot.value.copy(pingMs = ping)
                Result.success(ping)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
