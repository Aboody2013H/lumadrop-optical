# Changelog

All notable LumaDrop changes are documented here. The LumaDrop version suffix tracks Android wrapper builds based on Decimen 0.5.2.

## 0.5.2-luma.9 — 2026-08-17

This is the first public LumaDrop release and consolidates the work completed through Android build 9.

### Android application

- Added a native Android wrapper that serves the complete web application from secure bundled assets and works offline after installation.
- Fixed Send and Receive navigation so routes resolve inside the packaged application instead of attempting unavailable external `appassets` pages.
- Added native Android save integration for received files.
- Added Android `ACTION_SEND` and `ACTION_SEND_MULTIPLE` support, allowing files to be shared directly into LumaDrop from other applications.
- Added automatic maximum screen brightness while a QR transmission is active, with restoration when transmission stops or the page closes.
- Added a new LumaDrop launcher icon and matching web/PWA artwork.

### Sending

- Added arbitrary file transfer up to a 64 MB safety limit.
- Added multi-file selection. Multiple selections are packaged locally into a standards-compatible, uncompressed ZIP while preserving the original bytes.
- Added filename sanitization and deterministic duplicate-name handling inside generated ZIP archives.
- Added a text-snippet transfer mode.
- Added a WebKit-compatible compression fallback so sending does not depend on `CompressionStream` availability.
- Added adaptive QR payload density for small files and text, avoiding unnecessarily dense frames.
- Retained fountain-coded streaming so dropped frames cost additional time rather than creating holes in the received file.

### Receiving

- Added rear/front camera switching on mobile devices.
- Added decoder-aware digital zoom from 1x to 10x.
- Added pinch-to-zoom on touchscreens, mouse-wheel zoom on desktop, and a visible zoom control.
- Added native Android file saving after successful reconstruction and integrity verification.

### Interface

- Restyled the application with the LumaDrop dark neon technical theme.
- Improved mobile layout, transfer status, controls, and bundled/offline behavior.

### Validation

- Added ZIP writer tests covering CRC-32, ZIP records, exact payload preservation, safe names, and duplicate names.
- Verified 126 automated web/protocol tests.
- Verified TypeScript compilation, Vite production builds, Android lint, and Android debug builds.
- Tested installation and transfer behavior on Samsung SM-X200 and SM-S721B devices.

## Upstream

The underlying Decimen project and earlier Decimen history are maintained at <https://github.com/bashalarmistalt/decimen-optical-transfer>. Original notices and license history remain included in this repository.
