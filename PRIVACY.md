# Privacy Policy

Opticast collects **nothing**.

- **No analytics, no tracking, no telemetry, no ads, no accounts.**
- The app makes **no network connections except to the streaming server you configure.** Your video/audio goes only to the RTMP/SRT endpoint you enter — never to the developer or any third party.
- **Permissions** are used only for their obvious purpose:
  - **Camera / Microphone** — to capture the stream you choose to broadcast.
  - **Internet / Network state** — to send that stream to your server and adapt to your connection.
  - **Foreground service / wake lock** — to keep streaming while the screen is off.
  - **Notifications** — the ongoing "streaming" notification.
  - **Ignore battery optimizations** (optional) — so the OS doesn't kill the stream mid-broadcast.
- **Stream credentials** (passwords / SRT passphrases) are stored **encrypted on your device** (Android Keystore) and are never transmitted anywhere except, as part of the protocol, to the server you configured.
- Opticast requests **no location, storage, contacts, or phone permissions.**

Opticast is free and open source (GPLv3). There is no backend operated by the developer.
