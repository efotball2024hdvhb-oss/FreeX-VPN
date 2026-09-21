package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        // Production email delivery should call a secured HTTPS backend here.
        // Never embed SMTP/API secrets in the APK.
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: "Unknown"
            val body = sms.messageBody ?: ""
            // Persist locally / enqueue a WorkManager job in the production backend-connected build.
            android.util.Log.i("SMSForwarder", "SMS received from $sender: $body")
        }
    }
}
