# Changelog

All notable changes to Opticast are documented here. This project adheres to
[Semantic Versioning](https://semver.org/) and the format of
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.1] - 2026-06-23

### Added
- Focus mode selector on the live screen: **Auto** (continuous autofocus),
  **Lock** (freeze focus at its current distance), and **Infinity** (lock focus
  at infinity for fixed mounts pointed at a far scene, so a near bright object
  can't steal focus). The chosen mode is persisted across sessions.
- Animated focus-square indicator at the tapped point on tap-to-focus.

### Fixed
- Tap-to-focus no longer fires on the end of a pinch-zoom or a drag; it now
  triggers only on a genuine single, stationary tap, so focus stops jumping to
  unintended spots.

## [1.0.0] - 2026-06-16

### Added
- Initial release. Captures the device camera and microphone and streams over
  RTMP or SRT to a user-specified server.
- H.264 / H.265 encoding; configurable resolution, frame rate and bitrate.
- Saved connection profiles, each with its own quality (presets 480p30-1080p60
  or an Advanced custom mode).
- Adaptive bitrate and automatic reconnection with capped backoff.
- Background streaming via a typed foreground service; optional on-screen preview
  (off by default).
- Tap-to-focus, pinch-zoom, camera switch, torch, mute.
- Stream credentials stored with Android Keystore-backed encryption.

[1.0.1]: https://github.com/teop23/opticast/releases/tag/v1.0.1
[1.0.0]: https://github.com/teop23/opticast/releases/tag/v1.0.0
