package com.freex.vpn

import android.app.Application
import com.freex.vpn.data.api.ServerApi
import com.freex.vpn.data.local.SettingsStore
import com.freex.vpn.data.repository.ServerRepository
import com.freex.vpn.data.repository.VpnRepository
import com.freex.vpn.vpn.OpenVpnEngine
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class FreeXVpnApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    val settings = SettingsStore(app)
    val api = ServerApi(httpClient)
    val serverRepository = ServerRepository(api, settings)
    val vpnEngine = OpenVpnEngine(app)
    val vpnRepository = VpnRepository(vpnEngine, settings)
}
