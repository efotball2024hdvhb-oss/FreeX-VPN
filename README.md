# FreeX VPN

FreeX VPN is a native Kotlin + Jetpack Compose Android client that uses the real OpenVPN for Android engine and Android `VpnService`. It does not invent server IPs or fake a connected state.

## Real server source: PublicVPNList

The project is wired to the documented PublicVPNList v1 endpoint:

`https://publicvpnlist.com/api/v1/servers?protocol=openvpn&status=online&sort=score&order=desc&per_page=100`

PublicVPNList currently publishes live-checked OpenVPN records. Its current WireGuard catalog is empty, which is why this build uses OpenVPN instead. The API requires a free short-lived Bearer key. See https://publicvpnlist.com/api/ for the access flow.

The app reads the documented `data[]` records and uses `config_download_url` to obtain the current `.ovpn` profile over HTTPS. It does not embed third-party server credentials in the APK.

## One required setup

The API provider issues a free 24-hour key. Put your key in the project root `gradle.properties`:

```properties
PUBLICVPNLIST_API_KEY=YOUR_24_HOUR_KEY
```

Do not commit your personal key to GitHub. Prefer a local `~/.gradle/gradle.properties` entry for private builds.

The key is required because the provider changed API access policy in September 2026; an anonymous URL alone no longer works.

## Connection path

`FreeX UI -> PublicVPNList HTTPS API -> online OpenVPN record -> HTTPS config download -> OpenVPN engine -> Android VpnService -> VPN tunnel`

The app requests Android VPN consent before starting the tunnel. It only moves to its own CONNECTED state after the OpenVPN engine reports an active VPN state.

## Important trust warning

PublicVPNList does not operate the listed third-party VPN servers and its technical checks do not establish operator trust, privacy, or logging practices. Do not use unknown public VPN infrastructure for banking, sensitive work, passwords, or private source code.

## Build

Requirements: Android Studio current stable, JDK 17, Android SDK 37, and internet access for Gradle dependencies.

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Notes

- Server count is not hardcoded; the API page size is currently 100 and can be paginated/extended later.
- Search, favorites, recent servers and latency/country sorting remain local.
- Android system Always-on VPN + Block connections without VPN is the supported strict kill-switch mechanism.
- No login, registration, analytics SDK, advertising SDK or tracking SDK is included.

## Licensing

The OpenVPN for Android dependency is GPLv2 with additional terms. If you distribute FreeX VPN, review and comply with its license and the licenses of all bundled dependencies.
