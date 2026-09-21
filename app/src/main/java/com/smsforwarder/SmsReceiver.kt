package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val repo = SmsRepository(context)
        if (!repo.settings().third) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val now = System.currentTimeMillis()
        val items = messages.mapIndexed { index, sms ->
            SmsItem(
                id = now + index,
                sender = sms.displayOriginatingAddress ?: "Unknown",
                body = sms.messageBody ?: "",
                timestamp = now
            )
        }

        repo.add(items)
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<EmailWorker>()
                .setInitialDelay(1, TimeUnit.SECONDS)
                .build()
        )
    }
}
