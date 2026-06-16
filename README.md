# Opticast

> **GPL-3.0** · Android 8.0+ (API 26) · built on [RootEncoder](https://github.com/pedroSG94/RootEncoder)

*optic + cast* — an open-source, activity-agnostic **Android camera broadcaster**: capture your phone's camera + mic and stream to **any server you control** over RTMP or SRT. No watermark, no time limit, no account, **no telemetry**, no third-party servers in the path.

A libre alternative to apps like Larix Broadcaster — whose watermark/time-limit are pure client-side monetization, not a technical necessity (the stream goes straight to your server either way).

<p>
  <img src="docs/screenshots/live.png" width="260" alt="Live screen" />
  <img src="docs/screenshots/editor.png" width="260" alt="Quality editor" />
</p>

## Features

- Stream the phone camera + mic to any server over **RTMP or SRT**
- **H.264 or H.265** encoding
- **Connection profiles** — saved targets with **quality presets** (480p30 → 1080p60) and an **Advanced** editor (resolution / fps / bitrate / codec) with soft validation that warns but lets you proceed
- **Adaptive bitrate** + **auto-reconnect** with capped backoff — built for unreliable cellular
- **Background / screen-off streaming** (foreground service + wake lock) — preview is **off by default to save battery**, toggle it on when you need it
- Tap-to-focus, pinch-zoom, camera flip, torch, mute
- Encrypted credential storage, ongoing notification with a stop action
- OLED-dark Material 3 UI

**Non-goals (by design):** cloud accounts, analytics/telemetry, a hosted relay, ad/tracking SDKs. Bring-your-own-server, fully local, fully private — see [PRIVACY.md](PRIVACY.md).

## Install

- **GitHub Releases** — grab the signed APK from the [latest release](https://github.com/teop23/opticast/releases) and sideload it.
- **[Obtainium](https://github.com/ImranR98/Obtainium)** — add `https://github.com/teop23/opticast` for auto-updates.

You'll also need a server to stream to (e.g. [MediaMTX](https://github.com/bluenviron/mediamtx)) and a player/OBS to view it.

## Build

```bash
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleDebug          # debug APK
./gradlew installDebug           # install to a connected device
./gradlew assembleRelease        # signed release (needs keystore.properties, see below)
```

**Toolchain:** AGP 8.11.1, Gradle 8.13, Kotlin 2.3.21, compileSdk 36, JDK 17+.

Release signing is read from a `keystore.properties` at the repo root (git-ignored):

```properties
storeFile=opticast-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, `assembleRelease` still builds (unsigned).

## Security

Stream keys/passphrases are stored in **Android Keystore-backed encrypted storage**; the editor warns when a target would send credentials/video unencrypted to a remote host; service/receiver components are non-exported; `allowBackup=false`. Asks for **camera, mic, and network only**.

## License

GPL-3.0 — see [LICENSE](LICENSE). Built on RootEncoder (Apache-2.0).
