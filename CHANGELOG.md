# Changelog

LumaDrop moved fast, so there were ten Android builds in a pretty short stretch. Here's what actually changed in each one. The `luma.N` versions are based on Decimen 0.5.2.

> **A note about the old builds:** the GitHub repo was created after `luma.9`, so the exact source snapshot for each earlier APK was never committed. The APKs are the real archived builds, but their tags point at the consolidated public source instead of pretending the old source can be reproduced byte for byte. Unless you're curious about the history, download `luma.9`.

## 0.5.2-luma.9 — 2026-08-17

- Gave LumaDrop its neon QR-to-camera icon.
- Used the same artwork for Android, the web app, the PWA, the favicon, and Apple touch icons.
- Rebuilt the recommended APK with a dedicated release signature and a cleaner filename.
- Everything added in `luma.8` is still here.

## 0.5.2-luma.8 — 2026-08-17

- Made it possible to pick several files at once. LumaDrop puts them into a normal ZIP before sending.
- Added safe filenames, duplicate-name handling, CRC-32 records, and tests that check the ZIP contains the exact original bytes.
- Added Android Share support for one file or several files.
- Turned the sending phone's brightness up during a transfer and restored it afterward.
- Added the Android bridge that lets the bundled web sender read shared content URIs.

## 0.5.2-luma.7 — 2026-08-17

- Raised the receiver's zoom limit from 4x to 10x.
- Kept it safe on phones whose cameras do not support the same amount of hardware zoom.

## 0.5.2-luma.6 — 2026-08-17

- Added a button to switch between the rear and front cameras.
- Kept the zoom and receiver state sensible when the camera restarts.
- Added the new camera wording to every bundled language.

## 0.5.2-luma.5 — 2026-08-17

- Added a zoom slider to the receiver.
- Added pinch-to-zoom on phones and mouse-wheel zoom on desktop.
- Made the decoder scan the zoomed crop rather than only making the preview look zoomed.
- Added tests for the zoom behavior.

## 0.5.2-luma.4 — 2026-08-17

- Stopped tiny files and text snippets from being forced into an unnecessarily dense QR code.
- Added adaptive frame sizing, which made small transfers easier for cameras to read.
- Kept the chosen high-density settings for files that actually need them.

## 0.5.2-luma.3 — 2026-08-17

- Fixed arbitrary-file sending on older WebKit-based devices.
- Added a fallback for browsers that do not have `CompressionStream`.
- Kept compressed and uncompressed transfers compatible with the same receiver.

## 0.5.2-luma.2 — 2026-08-17

- Fixed the Send and Receive links inside the Android app.
- Kept navigation inside the bundled `appassets.androidplatform.net` pages.
- Got rid of the dark `ERR_INVALID_RESPONSE` screen that appeared offline.

## 0.5.2-luma.1 — 2026-08-17

- Wrapped Decimen 0.5.2 in the first LumaDrop Android app.
- Bundled the sender, receiver, decoder worker, WebAssembly codec, translations, and PWA files so it could run offline.
- Connected Android's camera and file picker.
- Added native saving for completed transfers.
- Added the first dark neon LumaDrop design and launcher icon.

## 0.1.0-native — 2026-08-17

- Built the first proof of concept in Kotlin and Jetpack Compose.
- Used Android's Storage Access Framework to choose and save files.
- Built a native fountain-style XOR protocol with normal frames and repair frames.
- Checked every frame with CRC-32 and the finished file with SHA-256.
- Used CameraX and the bundled ML Kit scanner on the receiving side.
- Handled missed and repeated frames, with a 100 MB safety limit.

## Where it came from

The optical wrapper builds are based on [Decimen Optical Transfer](https://github.com/bashalarmistalt/decimen-optical-transfer) by Evan Crawley (Bash Alarmist). The original notices and third-party credits are still included, and the project remains under AGPL-3.0-or-later.
