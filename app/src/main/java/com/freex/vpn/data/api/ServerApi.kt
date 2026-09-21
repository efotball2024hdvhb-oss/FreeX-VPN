package com.freex.vpn.data.api

import com.freex.vpn.BuildConfig
import com.freex.vpn.domain.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ServerApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val baseUrl: String = BuildConfig.API_BASE_URL

    suspend fun fetchServers(): Result<List<Server>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl}servers")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(body)
                val servers = mutableListOf<Server>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    servers.add(
                        Server(
                            id = obj.optString("id", i.toString()),
                            name = obj.optString("name", "Server $i"),
                            country = obj.optString("country", "US"),
                            countryCode = obj.optString("countryCode", "US"),
                            endpoint = obj.optString("endpoint", "127.0.0.1"),
                            port = obj.optInt("port", 51820),
                            publicKey = obj.optString("publicKey", ""),
                            protocol = obj.optString("protocol", "wireguard")
                        )
                    )
                }
                Result.success(servers)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
