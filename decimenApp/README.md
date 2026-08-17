# LumaDrop Optical Android wrapper

This module packages the modified Decimen Optical Transfer web application as
a self-contained Android APK. The production HTML, JavaScript, workers, WASM
decoder, fonts, icons, service worker, and locale bundles live under
`src/main/assets/web/` and are served to WebView from Android's secure
`appassets.androidplatform.net` origin.

Native integration in `MainActivity.kt` provides:

- runtime camera permission for `getUserMedia`
- Android's system file picker for sender input
- Android's system create-document picker for verified received files
- bounded base64 chunks between JavaScript and the Android output stream
- external-browser handling for off-app links
- predictive-back-compatible WebView navigation
- screen wake lock while transmitting or receiving

## Rebuild the bundled web application

From `decimen-web/`:

```powershell
npm ci
npm test
npm run build
```

Copy everything inside `decimen-web/dist/` to
`decimenApp/src/main/assets/web/`, then run:

```powershell
.\gradlew.bat :decimenApp:assembleDebug
```

## License and source

The bundled Decimen code is licensed AGPL-3.0-or-later. Its complete modified
source is retained in `decimen-web/`; the original `LICENSE` and `NOTICE` are
also packaged into the APK as `DECIMEN-LICENSE.txt` and `DECIMEN-NOTICE.txt`.
The visual theme and Android wrapper are modifications made for LumaDrop.
