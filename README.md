# Opticast

Open-source Android camera broadcaster. It captures the device camera and microphone and streams them to a user-specified **RTMP or SRT** server. No account, no watermark, no time limit; nothing is sent anywhere except the stream you configure.

Originally built as the broadcaster for a motorcycle helmet-cam setup, but it's general-purpose — a phone webcam, a second stream angle, a field/IP camera, etc.

<p>
  <img src="docs/screenshots/live.png" width="240" alt="Live screen" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/editor.png" width="240" alt="Target editor" />
</p>

## Features

- RTMP and SRT output; H.264 or H.265 encoding.
- Saved connection profiles, each with its own quality — presets from 480p30 to 1080p60, or an Advanced mode for custom resolution, fps, bitrate and codec. The editor warns on unusual or unencrypted settings but doesn't block them.
- Adaptive bitrate and automatic reconnection with capped backoff (useful on cellular).
- Background streaming via a foreground service; the on-screen preview is optional (off by default) to cut battery/CPU use.
- Tap-to-focus, pinch-zoom, camera switch, torch, mute.
- Stream credentials stored with Android Keystore-backed encryption.
- No analytics, ads, or trackers; no network access beyond the configured server.

## Usage

1. Run an RTMP/SRT server you control — e.g. [MediaMTX](https://github.com/bluenviron/mediamtx) — or use any compatible endpoint (YouTube Live, Twitch, …).
2. **Targets → New**: pick RTMP or SRT, enter the host, port and path (plus a passphrase/credentials if the server needs them), choose a quality preset or open **Advanced**. Tap the profile to select it.
3. **Go Live.** The status chip shows the connection state with live bitrate and uptime. Play the same path in OBS, VLC, or any player.
4. While streaming: toggle the preview, flip the camera, mute, or enable the torch. The stream continues with the screen locked; stop it from the button or the notification.

## Install

- **APK:** download from [Releases](https://github.com/teop23/opticast/releases) and sideload (enable "install from unknown sources" if prompted).
- **Updates:** add `https://github.com/teop23/opticast` to [Obtainium](https://github.com/ImranR98/Obtainium).

Requires Android 8.0 (API 26) or newer.

## Building

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # debug APK
./gradlew installDebug        # install to a connected device
./gradlew assembleRelease     # signed release; needs a keystore.properties (see below)
```

Toolchain: AGP 8.11.1, Gradle 8.13, Kotlin 2.3.21, compileSdk 36, JDK 17+. UI is Jetpack Compose; streaming uses [RootEncoder](https://github.com/pedroSG94/RootEncoder) (Apache-2.0).

Release signing reads `keystore.properties` at the repo root (git-ignored): `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Without it, `assembleRelease` still builds, unsigned.

## Privacy

No data is collected; see [PRIVACY.md](PRIVACY.md).

## License

[GPL-3.0](LICENSE).
