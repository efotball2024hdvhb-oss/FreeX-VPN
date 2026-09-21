# SMS Forwarder

Real Android/Kotlin project for user-authorized SMS reception, local message history, background delivery, and secure email forwarding through a backend.

## Android
- Kotlin + Jetpack Compose + Material 3
- First-launch SMS permission request
- Receives SMS with BroadcastReceiver
- Stores up to 200 recent messages locally
- Shows real received messages in the app
- WorkManager queues background email delivery and marks successfully delivered messages
- Configurable destination email and backend URL
- No SMTP credentials inside the APK

## Backend
`backend/server.py` provides `/v1/forward` and sends mail using server-side SMTP environment variables.

## Security
Run the backend behind HTTPS and configure authentication before exposing it to the internet. Never commit passwords, SMTP credentials, or tokens.

## Important
Only process SMS on a device where the owner has explicitly authorized the application. Review current Google Play SMS permission and distribution requirements before publishing.
