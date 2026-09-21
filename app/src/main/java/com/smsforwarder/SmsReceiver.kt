package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isEmpty()) return

        val sender = parts.firstOrNull()?.displayOriginatingAddress ?: "Unknown"
        val body = parts.joinToString("") { it.messageBody.orEmpty() }
        val timestamp = parts.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()

        SmsRepository.add(context, SmsItem(timestamp, sender, body, timestamp, false))

        if (SmsRepository.isEnabled(context) && SmsRepository.getBackend(context).isNotBlank()) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<EmailWorker>().build())
        }
    }
}
