# Opticast

**Turn your Android phone into a live camera.** Point it at any server you control and Opticast streams your camera and mic straight there — over RTMP or SRT. No account, no watermark, no time limit, nothing phoning home.

It's the broadcaster app behind a motorcycle helmet-cam project, but it works for anything: a webcam for your PC, a second angle for a stream, a security cam, a baby monitor, a field camera for an event.

<p>
  <img src="docs/screenshots/live.png" width="250" alt="Opticast — live screen" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/editor.png" width="250" alt="Opticast — add a target" />
</p>

## How to use it

**1. Have somewhere to send the stream.** Opticast doesn't host anything — it pushes to a server *you* pick. The easiest is your own [MediaMTX](https://github.com/bluenviron/mediamtx) (one small binary on your PC), but any RTMP/SRT endpoint works (YouTube Live, Twitch, a streaming box, etc.).

**2. Add a target.** Tap **Targets → New**, then:
- choose **RTMP** or **SRT**,
- enter the **host**, **port**, and **path** of your server (e.g. for MediaMTX on your PC: RTMP, host = your PC's IP, port `1935`, path `live/stream`),
- add a password/passphrase if your server needs one,
- pick a **quality preset** (or open **Advanced** for custom resolution / fps / bitrate / codec).

Tap the target to select it.

**3. Go live.** Hit **GO LIVE**. The chip up top shows `LIVE` with your bitrate and uptime. Open the same path in OBS, VLC, or any player and you'll see your phone's camera.

**4. While streaming.** Toggle the **preview** (it's off by default to save battery and stay cool), **flip** the camera, **mute**, or turn on the **torch**. Tap-to-focus and pinch-to-zoom work on the preview. Lock the screen and it keeps streaming. Stop from the big button or straight from the notification.

## Features

- Stream to any **RTMP or SRT** server, in **H.264 or H.265**
- **Saved targets** — name them, switch between them, each with its own quality (presets from 480p30 to 1080p60, or a custom advanced setup)
- **Adapts to your connection** — drops bitrate when the link is weak and **reconnects automatically** if it drops (great for cellular / on the move)
- **Keeps streaming with the screen off** — the preview is optional, so it sips battery
- Tap-to-focus, pinch-zoom, camera flip, torch, mute
- Your stream passwords are stored **encrypted on the device** — and Opticast never sends anything to anyone except the server you chose

## Install

- **APK** — download the latest from [Releases](https://github.com/teop23/opticast/releases) and install it (you may need to allow installing from unknown sources).
- **Auto-updates** — add `https://github.com/teop23/opticast` to [Obtainium](https://github.com/ImranR98/Obtainium).

Requires Android 8.0 (Oreo) or newer.

## Privacy

Opticast collects nothing and contains no ads, analytics, or trackers. Your video and credentials go only to the server you enter. Full details: [PRIVACY.md](PRIVACY.md).

## Building it yourself

```bash
./gradlew installDebug      # build & install a debug build to a connected device
./gradlew testDebugUnitTest # run the tests
./gradlew assembleRelease   # signed release (needs a keystore.properties at the repo root)
```

Built with Jetpack Compose on top of [RootEncoder](https://github.com/pedroSG94/RootEncoder).

## License

[GPL-3.0](LICENSE). Free to use, study, share, and modify — and it stays that way.
