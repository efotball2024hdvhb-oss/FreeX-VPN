# SMS Forwarder 4.0

Ultra-modern dark iOS/Telegram-inspired Android SMS inbox.

## Real background flow

1. Android delivers `SMS_RECEIVED` to the manifest receiver when SMS permission is granted.
2. `SmsReceiver` parses the SMS and saves it immediately.
3. A visible notification says `پیام جدید از ...`.
4. WorkManager queues delivery when network is available.
5. `EmailWorker` posts the SMS to `/v1/forward`.
6. The Python backend sends the email using SMTP.
7. The SMS is marked as delivered only after a successful 2xx response.

## Important

The Android app does not contain SMTP credentials.

You must deploy `backend/server.py`, expose it over HTTPS, and put its URL in Settings.

On Android, the user must grant SMS permissions. The app cannot bypass Android permission controls.

## Build

JDK 17, Gradle 8.13, Android SDK 36.
