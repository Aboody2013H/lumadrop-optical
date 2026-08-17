# Changelog

All ten LumaDrop Android development builds are preserved below. The `luma.N` suffix tracks Android wrapper builds based on Decimen 0.5.2.

> **Historical-source note:** this GitHub repository was created after `0.5.2-luma.9`. The older APKs are authentic archived binaries, but exact point-in-time source snapshots were not retained. Their tags therefore reference the consolidated public source history rather than claiming byte-for-byte reproducible historical source. Use `0.5.2-luma.9` unless you specifically need an earlier build.

## 0.5.2-luma.9 — 2026-08-17

- Replaced the original launcher artwork with the neon LumaDrop QR-to-camera aperture icon.
- Updated Android, web, PWA, favicon, and Apple-touch artwork to match.
- Retained all `luma.8` transfer, camera, and Android Share features.

## 0.5.2-luma.8 — 2026-08-17

- Added selection and optical transfer of multiple files through automatic standards-compatible ZIP packaging.
- Added filename sanitization, duplicate-name handling, CRC-32 ZIP records, and exact-byte preservation tests.
- Added Android `ACTION_SEND` and `ACTION_SEND_MULTIPLE` integration so files can be shared into LumaDrop from other apps.
- Added automatic maximum sender brightness while transmitting and safe restoration when transmission stops or the page closes.
- Added native shared-file streaming from Android content URIs into the bundled sender.

## 0.5.2-luma.7 — 2026-08-17

- Increased receiver digital zoom from a 4x ceiling to 10x.
- Kept the zoom path capability-safe for cameras that cannot provide matching hardware zoom.

## 0.5.2-luma.6 — 2026-08-17

- Added a mobile control for switching between rear and front cameras.
- Preserved the current zoom and receiver state while cameras are restarted.
- Added translated camera-control labels across the bundled locales.

## 0.5.2-luma.5 — 2026-08-17

- Added receiver camera zoom with a visible slider.
- Added pinch-to-zoom for touchscreens and mouse-wheel zoom for desktop testing.
- Added decoder-aware center cropping so digital zoom affects the frames actually scanned, not only the preview.
- Added camera zoom behavior tests.

## 0.5.2-luma.4 — 2026-08-17

- Added adaptive frame sizing for small files and text snippets.
- Avoided forcing one-block payloads into unnecessarily dense maximum-capacity QR symbols.
- Improved small text-file reliability and frame readability while retaining the selected density for larger transfers.

## 0.5.2-luma.3 — 2026-08-17

- Improved arbitrary-file compatibility on older WebKit-based devices.
- Added a compression fallback for environments without the browser `CompressionStream` API.
- Kept compressed and uncompressed file containers interoperable with the receiver.

## 0.5.2-luma.2 — 2026-08-17

- Fixed packaged Send and Receive navigation inside Android WebView.
- Kept routes under the secure `appassets.androidplatform.net` origin instead of handing them to an unavailable external page.
- Resolved the dark `ERR_INVALID_RESPONSE` screen seen when opening Send or Receive offline.

## 0.5.2-luma.1 — 2026-08-17

- Created the first Android wrapper around Decimen Optical Transfer 0.5.2.
- Bundled the web application, decoder worker, WebAssembly codec, translations, and PWA assets for offline use.
- Added Android camera and file-picker integration.
- Added native Android saving for reconstructed files.
- Introduced the dark neon LumaDrop interface and the initial launcher icon.

## 0.1.0-native — 2026-08-17

- Created the original native Kotlin/Jetpack Compose proof of concept.
- Added Storage Access Framework file selection and saving.
- Added a native fountain-style XOR protocol with systematic and repair frames.
- Added per-frame CRC-32 validation and final SHA-256 verification.
- Added CameraX reception with the bundled ML Kit QR scanner.
- Added duplicate and dropped-frame tolerance with a 100 MB prototype safety limit.

## Upstream

The wrapper builds are based on [Decimen Optical Transfer](https://github.com/bashalarmistalt/decimen-optical-transfer) by Evan Crawley (Bash Alarmist). Original copyright, contribution, codec, and third-party notices remain included under AGPL-3.0-or-later.
