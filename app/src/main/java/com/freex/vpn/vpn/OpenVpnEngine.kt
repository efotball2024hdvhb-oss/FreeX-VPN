package com.freex.vpn.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.StringReader

class OpenVpnEngine(private val context: Context) {
    fun preparePermission(): Intent? = VpnService.prepare(context)

    suspend fun connect(name: String, config: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(VpnService.prepare(context) == null) { "VPN permission has not been granted." }
            val parser = ConfigParser()
            parser.parseConfig(StringReader(config))
            val profile: VpnProfile = parser.convertProfile()
            profile.mName = name
            val check = profile.checkProfile(context)
            require(check == de.blinkt.openvpn.R.string.no_error_found) { "OpenVPN profile validation failed." }
            VPNLaunchHelper.startOpenVpn(profile, context)
            var connected = false
            repeat(30) {
                delay(500)
                val status = VpnStatus.getLastCleanLogMessage(context).orEmpty()
                if (VpnStatus.isVPNActive()) { connected = true; return@repeat }
                if (status.contains("AUTH_FAILED", true) || status.contains("TLS Error", true) || status.contains("Connection refused", true)) return@repeat
            }
            require(connected) { "OpenVPN did not report an active VPN tunnel." }
        }
    }

    suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.stopService(Intent(context, Class.forName("de.blinkt.openvpn.core.OpenVPNService")))
        }
    }

    fun isConnected(): Boolean = VpnStatus.isVPNActive()
    fun context(): Context = context
}
