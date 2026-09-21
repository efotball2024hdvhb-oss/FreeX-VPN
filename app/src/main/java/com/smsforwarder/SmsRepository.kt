package com.smsforwarder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SmsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("sms_store", Context.MODE_PRIVATE)

    fun load(): List<SmsItem> {
        val raw = prefs.getString("messages", "[]") ?: "[]"
        val json = JSONArray(raw)
        return buildList {
            for (i in 0 until json.length()) {
                val o = json.getJSONObject(i)
                add(SmsItem(
                    id = o.getLong("id"),
                    sender = o.optString("sender"),
                    body = o.optString("body"),
                    timestamp = o.getLong("timestamp"),
                    forwarded = o.optBoolean("forwarded", false)
                ))
            }
        }.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun add(items: List<SmsItem>) {
        val all = (items + load()).distinctBy { it.id }.take(200)
        saveMessages(all)
    }

    @Synchronized
    fun markForwarded(ids: Set<Long>) {
        saveMessages(load().map { if (it.id in ids) it.copy(forwarded = true) else it })
    }

    private fun saveMessages(items: List<SmsItem>) {
        val json = JSONArray()
        items.forEach {
            json.put(JSONObject().apply {
                put("id", it.id)
                put("sender", it.sender)
                put("body", it.body)
                put("timestamp", it.timestamp)
                put("forwarded", it.forwarded)
            })
        }
        prefs.edit().putString("messages", json.toString()).apply()
    }

    fun settings(): Triple<String, String, Boolean> =
        Triple(
            prefs.getString("destination", "efotball2024hdvhb@gmail.com")!!,
            prefs.getString("backendUrl", "")!!,
            prefs.getBoolean("enabled", true)
        )

    fun saveSettings(destination: String, backendUrl: String, enabled: Boolean) {
        prefs.edit()
            .putString("destination", destination.trim())
            .putString("backendUrl", backendUrl.trim().removeSuffix("/"))
            .putBoolean("enabled", enabled)
            .apply()
    }
}
