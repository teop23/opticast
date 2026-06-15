# Opticast

> **License: GPLv3.** Distribution: GitHub Releases (signed APK) + Obtainium/IzzyOnDroid; F-Droid later.

*optic + cast* — an open-source, activity-agnostic **Android camera broadcaster**: capture this phone's camera + mic and stream to **any server you control** over RTMP or SRT. No watermark, no time limit, no account, no telemetry, no third-party servers in the path.

A libre alternative to apps like Larix Broadcaster — whose watermark/time-limit are pure client-side monetization, not a technical necessity (the stream goes straight to your server either way).

This started as the streaming component for [MotoLive](../README.md) (motorcycle helmet-POV streaming) but is intentionally general-purpose. MotoLive is just one consumer.

## Status

**Design approved, pre-implementation.** See [design spec](docs/superpowers/specs/2026-06-15-camera-broadcaster-design.md). Implementation plan is the next step.

## Features (v1 MVP)

- Stream the phone camera + mic to a server via **RTMP or SRT**
- **Connection profiles** — saved named destinations (URL, protocol, secret, video/audio params)
- **Adaptive bitrate** — adjusts to the link; built for unreliable cellular
- **Auto-reconnect** with backoff
- **Background / screen-off streaming** — keeps running when the screen is off or you switch apps (foreground service + wake lock)
- Front/back camera switch, tap-to-focus, pinch-zoom, torch, audio mute
- Clear connection state + live stats (bitrate, fps, dropped frames, uptime)

**Deferred:** simultaneous multi-destination, local MP4 recording, HEVC/AV1, RTSP output, on-screen overlays, network bonding.

**Non-goals (by design):** cloud accounts, analytics/telemetry, a hosted relay, ad/tracking SDKs — anything that routes your stream through someone else's server.

## Permissions

Asks for **camera, microphone, and network — nothing else.** No location, no storage, no contacts. (Plus the normal install-time foreground-service/wake-lock declarations needed to stream with the screen off, and an optional battery-optimization exemption to survive aggressive OEM task-killers.) See the spec for the full list and rationale.

## Built on

[RootEncoder](https://github.com/pedroSG94/RootEncoder) (Apache-2.0) — the camera→RTMP/RTSP/SRT encoder library. Kotlin + Jetpack Compose.

## Security posture

Stream keys/passphrases stored in Android Keystore-backed encrypted storage; visible warning on unencrypted transports; locked-down (non-exported) components; minimal dependency tree aiming for F-Droid reproducible builds. Full threat model in the spec.
