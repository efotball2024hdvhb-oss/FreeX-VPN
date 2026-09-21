# SMS Forwarder

Android application for user-authorized SMS receiving and forwarding.

## Current implementation
- Kotlin + Jetpack Compose
- Material 3 UI
- Permission request for SMS access
- Modern iOS-inspired visual language
- Forwarding on/off control
- Destination email UI
- SMS receiver foundation
- No email credentials hardcoded in the APK

## Important production step
Automatic email delivery should use a secured HTTPS backend or approved email service. The app should authenticate to that service without storing SMTP/API secrets in the APK.

The exact SMS permissions and distribution rules of Google Play must also be checked before publishing. This project is intended to be built and tested on an Android device/emulator where SMS reception is available.
