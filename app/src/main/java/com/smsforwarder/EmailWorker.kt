package com.smsforwarder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class EmailWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result {
        val backend = SmsRepository.getBackend(applicationContext)
        if (backend.isBlank()) return Result.success()

        for (sms in SmsRepository.pending(applicationContext)) {
            try {
                val conn = (URL("$backend/v1/forward").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 20000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val payload = JSONObject().apply {
                    put("to", SmsRepository.getEmail(applicationContext))
                    put("sender", sms.sender)
                    put("body", sms.body)
                    put("timestamp", sms.timestamp)
                }.toString()
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()

                when {
                    code in 200..299 -> SmsRepository.markForwarded(applicationContext, sms.id)
                    code >= 500 -> return Result.retry()
                    else -> return Result.failure()
                }
            } catch (_: Exception) {
                return Result.retry()
            }
        }
        return Result.success()
    }
}
