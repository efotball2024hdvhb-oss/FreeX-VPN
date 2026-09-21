
package com.freex.vpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WireGuardVpnEngine(private val context: Context) {
    private val backend = GoBackend(context)
    private var activeTunnel: WireGuardTunnel? = null

    fun preparePermission(): Intent? = VpnService.prepare(context)

    suspend fun connect(name: String, rawConfig: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(VpnService.prepare(context) == null) { "VPN permission has not been granted." }
            val config = Config.parse(ByteArrayInputStream(rawConfig.toByteArray(StandardCharsets.UTF_8)))
            require(config.peers.isNotEmpty()) { "WireGuard configuration has no peers." }

            activeTunnel?.let { runCatching { backend.setState(it, Tunnel.State.DOWN, null) } }

            val tunnel = WireGuardTunnel(name) {}
            backend.setState(tunnel, Tunnel.State.UP, config)
            activeTunnel = tunnel
        }
    }

    suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val tunnel = activeTunnel ?: return@runCatching
            backend.setState(tunnel, Tunnel.State.DOWN, null)
            activeTunnel = null
        }
    }

    fun isConnected(): Boolean = activeTunnel != null
    fun context(): Context = context
}
