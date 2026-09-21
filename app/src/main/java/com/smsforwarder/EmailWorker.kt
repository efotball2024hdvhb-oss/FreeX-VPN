package com.smsforwarder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class EmailWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = SmsRepository(applicationContext)
        val settings = repo.settings()
        val destination = settings.first
        val backendUrl = settings.second
        val enabled = settings.third

        if (!enabled || backendUrl.isBlank() || destination.isBlank()) return Result.success()

        val pending = repo.load().filterNot { it.forwarded }.take(20)
        if (pending.isEmpty()) return Result.success()

        return try {
            val payload = JSONObject().apply {
                put("to", destination)
                put("messages", JSONArray().apply {
                    pending.forEach { sms ->
                        put(JSONObject().apply {
                            put("sender", sms.sender)
                            put("body", sms.body)
                            put("timestamp", sms.timestamp)
                        })
                    }
                })
            }

            val connection = (URL("$backendUrl/v1/forward").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            connection.disconnect()

            if (code in 200..299) {
                repo.markForwarded(pending.map { it.id }.toSet())
                Result.success()
            } else if (code in 400..499) {
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
