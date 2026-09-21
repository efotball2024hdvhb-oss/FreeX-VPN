package com.freex.vpn.data.api

import com.freex.vpn.BuildConfig
import com.freex.vpn.domain.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class ServerApi(private val client: OkHttpClient) {
    suspend fun fetchServers(): Result<List<Server>> = withContext(Dispatchers.IO) {
        runCatching {
            require(BuildConfig.SERVER_API_KEY.isNotBlank()) {
                "PublicVPNList API key is missing. Put PUBLICVPNLIST_API_KEY=... in gradle.properties."
            }
            val uri = URI(BuildConfig.SERVER_API_URL)
            require(uri.scheme.equals("https", true)) { "Server API must use HTTPS." }
            val request = Request.Builder()
                .url(uri.toURL())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer ${BuildConfig.SERVER_API_KEY}")
                .build()
            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) {
                    if (response.code == 401) "PublicVPNList API key is missing or expired."
                    else if (response.code == 429) "PublicVPNList rate limit reached. Try again later."
                    else "Server API returned HTTP ${response.code}."
                }
                parse(response.body.string())
            }
        }
    }

    suspend fun downloadConfig(server: Server): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = URI(server.configDownloadUrl)
            require(uri.scheme.equals("https", true)) { "Config URL must use HTTPS." }
            val request = Request.Builder()
                .url(uri.toURL())
                .header("Accept", "application/octet-stream,text/plain,*/*")
                .header("Authorization", "Bearer ${BuildConfig.SERVER_API_KEY}")
                .build()
            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "VPN profile download failed: HTTP ${response.code}." }
                val body = response.body.string()
                require(body.contains("client", true)) { "Downloaded profile is not an OpenVPN client configuration." }
                require(body.contains("remote ", true)) { "OpenVPN profile has no remote endpoint." }
                body
            }
        }
    }

    private fun parse(body: String): List<Server> {
        val root = JSONObject(body)
        val array = root.optJSONArray("data") ?: root.optJSONArray("servers") ?: JSONArray()
        val result = ArrayList<Server>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val protocol = o.optString("protocol").lowercase()
            val host = o.optString("hostname").ifBlank { o.optString("ip") }.trim()
            val port = o.optInt("port", -1)
            val url = o.optString("config_download_url").trim()
            if (o.optString("id").isBlank() || protocol != "openvpn" || host.isBlank() || port !in 1..65535 || !url.startsWith("https://")) continue
            result += Server(
                id = o.optString("id"),
                country = o.optString("country_name", "Unknown"),
                countryCode = o.optString("country_code", "UN").uppercase(),
                city = o.optString("city", "Unknown").ifBlank { "Unknown" },
                host = host,
                port = port,
                protocol = protocol,
                latencyMs = o.optLong("latency_ms", -1).takeIf { it >= 0 },
                online = o.optString("availability_status", "online").lowercase() == "online",
                loadPercent = null,
                lastCheckedEpochSeconds = o.optString("last_checked_at").toEpochSeconds(),
                speedMbps = o.optDouble("speed_mbps", Double.NaN).takeIf { !it.isNaN() },
                score = o.optDouble("technical_quality_score", Double.NaN).takeIf { !it.isNaN() },
                configDownloadUrl = url
            )
        }
        return result.filter { it.online }
    }

    private fun String.toEpochSeconds(): Long? = runCatching {
        java.time.Instant.parse(this).epochSecond
    }.getOrNull()
}
