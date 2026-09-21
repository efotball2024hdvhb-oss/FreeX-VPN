
package com.freex.vpn.data.repository

import android.content.Context
import com.freex.vpn.data.local.SettingsStore
import com.freex.vpn.domain.model.ConnectionState
import com.freex.vpn.domain.model.Server
import com.freex.vpn.domain.model.VpnSnapshot
import com.freex.vpn.domain.repository.VpnRepositoryContract
import com.freex.vpn.vpn.VpnNotificationService
import com.freex.vpn.vpn.OpenVpnEngine
import com.freex.vpn.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

class VpnRepository(
    private val engine: OpenVpnEngine,
    private val settings: SettingsStore
) : VpnRepositoryContract {
    private val appContext: Context = engine.context()
    private val _snapshot = MutableStateFlow(VpnSnapshot())
    override val snapshot: StateFlow<VpnSnapshot> = _snapshot
    private val ipClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .build()

    override fun preparePermission(context: Context) = engine.preparePermission()

    override suspend fun connect(server: Server): Result<Unit> {
        _snapshot.value = VpnSnapshot(ConnectionState.CONNECTING, server = server)
        val result = runCatching {
            val config = com.freex.vpn.data.api.ServerApi(ipClient).downloadConfig(server).getOrThrow()
            engine.connect("FreeX-${server.id.take(10)}", config).getOrThrow()
        }
        return result.onSuccess {
            val publicIp = fetchPublicIp()
            _snapshot.value = VpnSnapshot(
                state = ConnectionState.CONNECTED,
                server = server,
                connectedAtMillis = System.currentTimeMillis(),
                publicIp = publicIp
            )
            VpnNotificationService.start(appContext, "${server.city}, ${server.country}")
        }.onFailure {
            _snapshot.value = VpnSnapshot(
                ConnectionState.ERROR,
                server = server,
                error = it.message ?: "Connection failed"
            )
            VpnNotificationService.stop(appContext)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        _snapshot.value = _snapshot.value.copy(state = ConnectionState.DISCONNECTING, error = null)
        return engine.disconnect().onSuccess {
            _snapshot.value = VpnSnapshot()
            VpnNotificationService.stop(appContext)
        }.onFailure {
            _snapshot.value = _snapshot.value.copy(
                state = ConnectionState.ERROR,
                error = it.message ?: "Disconnect failed"
            )
        }
    }
    private suspend fun fetchPublicIp(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = URI(BuildConfig.PUBLIC_IP_URL)
            require(uri.scheme.equals("https", true)) { "Public IP endpoint must use HTTPS." }
            val request = Request.Builder().url(uri.toURL()).get().build()
            ipClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body.string()
                JSONObject(body).optString("ip").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

}
