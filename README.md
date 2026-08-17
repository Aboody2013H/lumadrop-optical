# LumaDrop Optical

<p align="center">
  <img src="branding/lumadrop-icon-v2-master.png" width="220" alt="LumaDrop Optical icon">
</p>

LumaDrop moves files between phones by turning one screen into a stream of animated QR codes and using the other phone's camera to catch them. No accounts, no pairing, no Bluetooth, and no shared Wi-Fi—just point one phone at the other.

It started as an experiment and grew into an offline Android version of [Decimen Optical Transfer](https://github.com/bashalarmistalt/decimen-optical-transfer), with a new interface and a bunch of Android-specific features added along the way.

## What it can do

- Send any kind of file up to 64 MB.
- Bundle several files into one ZIP and send them together.
- Open LumaDrop straight from Android's Share menu.
- Keep going when the camera misses or sees the same QR frame twice.
- Use either the rear or front camera.
- Zoom from 1x to 10x with the slider, a pinch gesture, or the mouse wheel.
- Turn the sender's brightness up during a transfer and put it back afterward.
- Save received files through Android's normal file picker.
- Send text snippets as well as files.
- Work completely offline once the app is installed.

Speed depends on both phones. Screen refresh rate, camera exposure, focus, QR density, and general device performance all make a difference, so 60 FPS should be treated as a target rather than a promise.

## Install it

Grab the newest APK from [GitHub Releases](https://github.com/Aboody2013H/lumadrop-optical/releases). Android may ask you to allow installs from your browser or file manager.

The current APK is release-signed and works on Android 6.0/API 23 or newer. The older builds on the Releases page are kept for the project's history and are debug-signed.

Already have one of those debug builds installed? Android may make you uninstall it before installing the release version because the signatures are different.

All twelve builds—from the original native prototype through `0.5.2-luma.11`—are still available. The [changelog](CHANGELOG.md) tells the story build by build.

## Build it yourself

You'll need:

- Node.js 24 or newer
- JDK 17
- Android SDK 36

Build and test the web app first:

```bash
cd decimen-web
npm install
npm test
npm run build
```

If you changed the web app, copy everything from `decimen-web/dist/` into `decimenApp/src/main/assets/web/`. Then build the Android wrapper from the repository root:

```bash
./gradlew :decimenApp:lintDebug :decimenApp:assembleDebug
```

You'll find the APK at `decimenApp/build/outputs/apk/debug/decimenApp-debug.apk`.

## What's where

- `decimen-web/` — the modified Decimen web app and optical-protocol tests.
- `decimenApp/` — the Android app people should actually use.
- `app/` — the earlier native Kotlin/Compose prototype, kept for reference.
- `branding/` — the LumaDrop icon and artwork.
- `releases/` — release notes and checksums. The APKs themselves live on GitHub Releases.

## Privacy and safety

Encoding and decoding happen on the phones. LumaDrop needs camera permission on the receiving side, but it does not need an account or a transfer server. As always, only open a received file when you trust where it came from.

## How it was made

LumaDrop was vibe-coded using Codex and ChatGPT. The app took shape through a lot of quick back-and-forth: try an idea, test it on real phones, fix what broke, and repeat.

## Credit and license

LumaDrop is built from Decimen Optical Transfer by Evan Crawley (Bash Alarmist). It remains licensed under GNU AGPL v3.0 or later, and the original copyright, contributor, codec, and third-party notices are all kept in [NOTICE.md](NOTICE.md), `decimen-web/NOTICE`, and the vendored codec files.

See [LICENSE](LICENSE) for the full license and [CHANGELOG.md](CHANGELOG.md) for the complete build history.
