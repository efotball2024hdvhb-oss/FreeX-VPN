package com.freex.vpn.data.api

import com.freex.vpn.domain.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class ServerApi(private val client: OkHttpClient) {

    suspend fun fetchServers(): Result<List<Server>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/efotball2024hdvhb-oss/FreeX-VPN/main/server-api.example.json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(body)
                val list = mutableListOf<Server>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        Server(
                            id = obj.optString("id", "srv-$i"),
                            country = obj.optString("country", "Germany"),
                            countryCode = obj.optString("countryCode", "DE"),
                            city = obj.optString("city", "Frankfurt"),
                            host = obj.optString("host", "127.0.0.1"),
                            port = obj.optInt("port", 1194),
                            protocol = obj.optString("protocol", "openvpn"),
                            latencyMs = obj.optLong("latencyMs", 50L),
                            online = obj.optBoolean("online", true),
                            loadPercent = obj.optInt("loadPercent", 20),
                            lastCheckedEpochSeconds = System.currentTimeMillis() / 1000,
                            speedMbps = obj.optDouble("speedMbps", 100.0),
                            score = obj.optDouble("score", 9.5),
                            configDownloadUrl = obj.optString("configDownloadUrl", "")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
