# Design — Opticast (Android Camera Broadcaster)

Status: approved design, pre-implementation
Date: 2026-06-15
Name: **Opticast** (optic + cast)
License: **GPLv3** — copyleft so the app can never be re-closed/watermarked (the exact thing it reacts against); compatible with the Apache-2.0 RootEncoder dependency (one-way into GPLv3).

## Summary

A general-purpose, activity-agnostic Android **camera broadcaster**: captures the phone's camera + mic and streams to any user-specified server over **RTMP or SRT**, with no watermark, no time limit, no accounts, and no telemetry. A libre alternative to Larix Broadcaster. The MotoLive motorcycle-streaming use case is one consumer of this app, not its identity. Built on the Apache-2.0 [RootEncoder](https://github.com/pedroSG94/RootEncoder) library.

## Distribution

GPLv3 open-source repo. Ship order (effort ladder):
1. **GitHub repo + signed APK on Releases** (baseline — makes the app usable).
2. **Obtainium-friendly + IzzyOnDroid** repo for auto-updates with near-zero maintenance overhead.
3. **F-Droid proper** once stable (the ideological home; build-from-source/reproducible).
4. Google Play deferred/optional — `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + typed camera/mic FGS need policy justification there; not worth it for the niche audience initially.

## Scope

**In scope (v1 MVP):** broadcast *this phone's* camera/mic to a server.

**Out of scope (v1):** ingesting/bridging an external camera (RTSP/UVC pull-and-republish) — that is a separate, MotoLive-specific concern with different permissions (USB host, no sensor) and audience. It gets its own repo/module later.

## Feature set

**Core (v1):**
- Live camera preview; switch front/back camera
- Stream to a server via RTMP or SRT
- Connection profiles: save/edit/select named destinations (name, URL, protocol, secret, video/audio params)
- Video config: resolution, fps, bitrate, codec (H.264 baseline)
- Adaptive bitrate (drop/raise bitrate as the link changes) — critical for cellular
- Auto-reconnect with backoff
- Background / screen-off streaming (foreground service + wake lock)
- Audio capture + mute toggle (video-only fallback if mic denied)
- Basic camera controls: tap-to-focus, pinch-zoom, torch
- Connection state (idle / connecting / live / reconnecting / error) + stats (bitrate, fps, dropped frames, uptime)

**Deferred (post-v1):** simultaneous multi-destination streaming, local MP4 recording, HEVC/AV1, RTSP output, on-screen overlays (text/GPS/time), network bonding (cellular+wifi).

**Non-goals (by design, never):** cloud accounts, analytics/telemetry, a hosted relay, ad/tracking SDKs, anything that routes the stream through a third-party server. Bring-your-own-server, fully local, fully private.

## Protocols

- **RTMP** — universal compatibility (any server/platform). TCP; stalls under cellular packet loss.
- **SRT** — packet-loss/jitter resilient, tunable latency, AES encryption. The right protocol for the moving-bike-on-cellular case. MediaMTX accepts it on UDP 8890.

RTSP output deferred (adds little for the broadcast direction once on a public relay).

## Permissions

**Runtime:** `CAMERA`, `RECORD_AUDIO`, `POST_NOTIFICATIONS` (Android 13+, for the FGS notification).

**Install-time / normal:** `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CAMERA` + `FOREGROUND_SERVICE_MICROPHONE` (Android 14+ typed FGS), `WAKE_LOCK`.

**Optional, recommended:** `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — to survive aggressive OEM task-killing (HyperOS/MIUI). Caveat: flagged by Google Play review; a non-issue for F-Droid/direct-APK.

**Deliberately NOT requested:** location, storage/media, contacts, phone state, accounts. The "camera + mic + network, nothing else" story is both a trust pitch and attack-surface reduction; document it in the README.

## Architecture

Layered, with the streaming engine behind an interface so everything above it is testable without hardware.

- **`Broadcaster` (interface) + `RootEncoderBroadcaster` (impl)** — the only code that touches RootEncoder. Wraps its Camera2 stream builder + RTMP/SRT clients. Surface: `start(profile)`, `stop()`, `attachPreview(view)`/`detachPreview()`, `switchCamera()`, `setBitrate()`, `toggleTorch()`, `toggleMute()`; emits state/stats/error events. Isolates library internals from the rest of the app.
- **`StreamingService` (foreground service)** — owns the `Broadcaster`, wake lock, and notification. The lifecycle owner so streaming survives screen-off/backgrounding. UI binds to it; it has no UI dependency. `android:exported="false"`.
- **`AdaptiveBitrateController`** — subscribes to the library's bandwidth/connection callbacks, nudges `Broadcaster.setBitrate()` within configured bounds. Pure logic, unit-testable with a fake broadcaster.
- **`ReconnectController`** — on disconnect callback + network-state change, retries with backoff (1→2→5→max). Pure logic, unit-testable.
- **`ConnectionRepository` + `Connection` model** — saved destination profiles, persisted (DataStore for non-secret fields; Keystore-backed encrypted store for secrets). Includes URL validation/parsing. Plain Kotlin, unit-testable.
- **UI (Jetpack Compose) + `StreamViewModel`** — screens: **Live** (preview via `AndroidView`, Go-Live, state chip, stats, focus/zoom/torch/flip), **Connections** (list + editor), **Settings** (default video/audio params). ViewModel holds `StateFlow<UiState>`, talks to the bound service, never to RootEncoder directly.

### Data flow

camera + mic → `Broadcaster` (encode H.264/AAC) → RTMP/SRT → user's server. Library callbacks → state/stats → `StreamViewModel` → UI. `AdaptiveBitrateController` reads the bandwidth signal → adjusts encoder bitrate. `StreamingService` keeps the pipeline alive.

### Screen-off handling (highest-risk integration point)

With the screen off / app backgrounded there is no preview `Surface`. `attachPreview`/`detachPreview` must let the camera→encoder pipeline keep running while only *rendering* detaches — locking the screen drops the preview, never the stream. RootEncoder supports preview-less background streaming (context instead of surface view); this path gets explicit integration testing against a local MediaMTX.

### Error handling

- Connection-failed / auth-error / disconnect callbacks → `ReconnectController` or a clear UI error state.
- Mic permission denied → graceful video-only stream.
- Camera unavailable/in-use → surfaced as error, not a crash.

## Security

Threat model: protect the user's stream keys at rest and in transit, prevent other apps from hijacking the camera/service, and ship a verifiable minimal-permission binary. Out of scope: a stream pointed at a misconfigured/open server (the user's relay is responsible for its own auth).

- **Credentials at rest** — secret fields (RTMP `user:pass`, SRT passphrase, stream keys) stored in Android Keystore-backed encrypted storage, never plaintext. Non-secret fields in plain DataStore.
- **Transport** — prefer SRT-with-passphrase (AES) or RTMPS over public internet. App shows a visible "credentials + video unencrypted" warning when a profile uses plain RTMP / passphrase-less SRT to a non-localhost host. Warn, don't block (LAN use is legitimate).
- **No secret leakage in logs** — redact credentials/keys from all logcat output and error messages. No analytics/crash SDK.
- **Locked-down components** — `StreamingService` and any receivers `android:exported="false"`; `android:allowBackup="false"` (or backup rules excluding the secrets store) so encrypted creds can't be pulled via `adb backup`.
- **Least privilege** — camera/mic/network-only permission set is itself attack-surface reduction.
- **Supply chain** — pin RootEncoder + transitive versions, minimal dependency tree, aim for F-Droid reproducible builds so users can verify APK == source. No backend, no secrets in the repo.

## Testing strategy

- **Unit (JVM, no hardware):** `ConnectionRepository`, URL validation, `AdaptiveBitrateController`, `ReconnectController`, `StreamViewModel` state logic — all against a fake `Broadcaster`.
- **Integration / manual:** `RootEncoderBroadcaster` verified against a local MediaMTX (the working setup already in place), including the screen-off preview-detach path and reconnect under simulated network drop.

## Open implementation questions (resolve during planning)

1. Exact RootEncoder entry classes/version (Camera2 stream builder vs unified stream API) — confirm against the current release at plan time.
2. Min/target SDK (target Android 14+ for typed FGS; min likely 26–29).
3. Keystore-backed storage choice (Jetpack Security `EncryptedSharedPreferences` vs Tink vs Keystore-direct), given Jetpack Security's maintenance status.
