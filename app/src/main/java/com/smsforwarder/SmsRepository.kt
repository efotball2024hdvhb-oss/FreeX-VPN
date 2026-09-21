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

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = prefs(c).getBoolean(KEY_ENABLED, true)
    fun setEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_ENABLED, v).apply()
    fun getEmail(c: Context) = prefs(c).getString(KEY_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL
    fun setEmail(c: Context, v: String) = prefs(c).edit().putString(KEY_EMAIL, v.trim()).apply()
    fun getBackend(c: Context) = prefs(c).getString(KEY_BACKEND, "") ?: ""
    fun setBackend(c: Context, v: String) = prefs(c).edit().putString(KEY_BACKEND, v.trim().trimEnd('/')).apply()

    @Synchronized fun add(c: Context, item: SmsItem) {
        save(c, (getAll(c).toMutableList().apply { add(0, item) }).take(200))
    }

    @Synchronized fun getAll(c: Context): List<SmsItem> {
        return try {
            val a = JSONArray(prefs(c).getString(KEY_MESSAGES, "[]") ?: "[]")
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(SmsItem(o.getLong("id"), o.getString("sender"), o.getString("body"),
                        o.getLong("timestamp"), o.optBoolean("forwarded", false)))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    fun pending(c: Context) = getAll(c).filter { !it.forwarded }

    @Synchronized fun markForwarded(c: Context, id: Long) {
        save(c, getAll(c).map { if (it.id == id) it.copy(forwarded = true) else it })
    }

    private fun save(c: Context, list: List<SmsItem>) {
        val a = JSONArray()
        list.forEach { x ->
            a.put(JSONObject().apply {
                put("id", x.id); put("sender", x.sender); put("body", x.body)
                put("timestamp", x.timestamp); put("forwarded", x.forwarded)
            })
        }
        prefs(c).edit().putString(KEY_MESSAGES, a.toString()).apply()
    }
}
