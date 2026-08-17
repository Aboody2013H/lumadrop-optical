package io.lumadrop.decimen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebViewAssetLoader
import java.io.OutputStream
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private data class SharedFile(
        val uri: Uri,
        val name: String,
        val mediaType: String,
        val size: Long,
    )

    private lateinit var webView: WebView
    private var fileChooser: ValueCallback<Array<Uri>>? = null
    private var cameraRequest: PermissionRequest? = null
    private var pendingSaveName = "received-file"
    private var pendingSaveType = "application/octet-stream"
    private var saveStream: OutputStream? = null
    private var saveFailed = false
    private val sharedFiles = mutableListOf<SharedFile>()
    private var originalBrightness: Float? = null

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(3, 9, 8)
        window.navigationBarColor = Color.rgb(3, 9, 8)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val openedFromShare = handleIncomingShare(intent)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/native-shared/", WebViewAssetLoader.PathHandler { path ->
                openSharedFile(path)
            })
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(3, 9, 8))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.setSupportZoom(false)
            addJavascriptInterface(NativeBridge(), "LumaDropNative")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    // WebViewAssetLoader serves files but has no web-server
                    // directory-index behavior. Decimen deliberately links to
                    // clean URLs such as ./send/ and ./receive/, so translate
                    // those local directory requests to their bundled
                    // index.html while leaving the visible URL unchanged.
                    val uri = request.url
                    val resolved = if (
                        uri.scheme == "https" &&
                        uri.host == APP_ASSET_HOST &&
                        uri.path?.endsWith('/') == true
                    ) {
                        uri.buildUpon().appendPath("index.html").build()
                    } else {
                        uri
                    }
                    return assetLoader.shouldInterceptRequest(resolved)
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "https" && uri.host == APP_ASSET_HOST) return false
                    return runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    }.getOrDefault(true)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView,
                    callback: ValueCallback<Array<Uri>>,
                    params: FileChooserParams,
                ): Boolean {
                    fileChooser?.onReceiveValue(null)
                    fileChooser = callback
                    val intent = params.createIntent().apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    return runCatching {
                        startActivityForResult(intent, REQUEST_OPEN_FILE)
                        true
                    }.getOrElse {
                        fileChooser = null
                        false
                    }
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread {
                        val trusted = request.origin.scheme == "https" && request.origin.host == APP_ASSET_HOST
                        val wantsCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                        if (!trusted || !wantsCamera) {
                            request.deny()
                            return@runOnUiThread
                        }
                        cameraRequest?.deny()
                        cameraRequest = request
                        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            grantPendingCamera()
                        } else {
                            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
                        }
                    }
                }

                override fun onPermissionRequestCanceled(request: PermissionRequest) {
                    if (cameraRequest == request) cameraRequest = null
                }
            }
        }

        WebView.setWebContentsDebuggingEnabled(
            applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0,
        )
        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        webView.loadUrl(if (openedFromShare) SEND_URL else HOME_URL)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (handleIncomingShare(intent)) webView.loadUrl(SEND_URL)
    }

    @Suppress("DEPRECATION")
    private fun handleIncomingShare(intent: Intent?): Boolean {
        val action = intent?.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return false
        val uris = linkedSetOf<Uri>()
        if (action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::add)
        } else {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::addAll)
        }
        intent.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let(uris::add)
        }
        synchronized(sharedFiles) {
            sharedFiles.clear()
            uris.forEachIndexed { index, uri ->
                sharedFiles += describeSharedFile(uri, index)
            }
        }
        return uris.isNotEmpty()
    }

    private fun describeSharedFile(uri: Uri, index: Int): SharedFile {
        var name = "shared-file-${index + 1}"
        var size = -1L
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameColumn >= 0) name = cursor.getString(nameColumn) ?: name
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
                }
            }
        }
        name = name.substringAfterLast('/').substringAfterLast('\\')
            .filter { it.code >= 32 && it.code != 127 }
            .ifBlank { "shared-file-${index + 1}" }
        return SharedFile(
            uri = uri,
            name = name,
            mediaType = contentResolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" },
            size = size,
        )
    }

    private fun openSharedFile(path: String): WebResourceResponse? {
        val index = path.substringBefore('/').toIntOrNull() ?: return null
        val file = synchronized(sharedFiles) { sharedFiles.getOrNull(index) } ?: return null
        val input = runCatching { contentResolver.openInputStream(file.uri) }.getOrNull() ?: return null
        return WebResourceResponse(file.mediaType, null, input)
    }

    private fun grantPendingCamera() {
        cameraRequest?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        cameraRequest = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode != REQUEST_CAMERA) return
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            grantPendingCamera()
        } else {
            cameraRequest?.deny()
            cameraRequest = null
            Toast.makeText(this, "Camera permission is required to receive files", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Android activity result compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OPEN_FILE -> {
                val result = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                fileChooser?.onReceiveValue(result)
                fileChooser = null
            }
            REQUEST_SAVE_FILE -> {
                val uri = if (resultCode == RESULT_OK) data?.data else null
                if (uri == null) {
                    webView.evaluateJavascript("window.__lumaCancelNativeWrite?.()", null)
                    return
                }
                saveStream = runCatching { contentResolver.openOutputStream(uri, "w") }.getOrNull()
                saveFailed = saveStream == null
                if (saveFailed) {
                    Toast.makeText(this, "Could not open the selected destination", Toast.LENGTH_LONG).show()
                    webView.evaluateJavascript("window.__lumaCancelNativeWrite?.()", null)
                } else {
                    webView.evaluateJavascript("window.__lumaBeginNativeWrite?.()", null)
                }
            }
        }
    }

    override fun onDestroy() {
        setTransferBrightness(false)
        cameraRequest?.deny()
        fileChooser?.onReceiveValue(null)
        runCatching { saveStream?.close() }
        webView.removeJavascriptInterface("LumaDropNative")
        webView.destroy()
        super.onDestroy()
    }

    inner class NativeBridge {
        @JavascriptInterface
        fun sharedFilesJson(): String {
            val snapshot = synchronized(sharedFiles) { sharedFiles.toList() }
            val array = JSONArray()
            snapshot.forEachIndexed { index, file ->
                array.put(
                    JSONObject()
                        .put("url", "$APP_ORIGIN/native-shared/$index")
                        .put("name", file.name)
                        .put("type", file.mediaType)
                        .put("size", file.size),
                )
            }
            return array.toString()
        }

        @JavascriptInterface
        fun clearSharedFiles() {
            synchronized(sharedFiles) { sharedFiles.clear() }
        }

        @JavascriptInterface
        fun setTransferActive(active: Boolean) {
            runOnUiThread { setTransferBrightness(active) }
        }

        @JavascriptInterface
        fun requestSave(name: String, mediaType: String) {
            runOnUiThread {
                pendingSaveName = name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "received-file" }
                pendingSaveType = mediaType.ifBlank { "application/octet-stream" }
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = pendingSaveType
                    putExtra(Intent.EXTRA_TITLE, pendingSaveName)
                }
                startActivityForResult(intent, REQUEST_SAVE_FILE)
            }
        }

        @JavascriptInterface
        @Synchronized
        fun appendBase64(chunk: String): Boolean {
            if (saveFailed) return false
            return runCatching {
                saveStream?.write(Base64.decode(chunk, Base64.DEFAULT)) ?: error("Save stream closed")
                true
            }.getOrElse {
                saveFailed = true
                false
            }
        }

        @JavascriptInterface
        @Synchronized
        fun finishSave() {
            runCatching {
                saveStream?.flush()
                saveStream?.close()
            }.onFailure { saveFailed = true }
            saveStream = null
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    if (saveFailed) "Save failed" else "Saved $pendingSaveName",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun setTransferBrightness(active: Boolean) {
        val attributes = window.attributes
        if (active) {
            if (originalBrightness == null) originalBrightness = attributes.screenBrightness
            attributes.screenBrightness = 1f
        } else {
            val restore = originalBrightness ?: return
            attributes.screenBrightness = restore
            originalBrightness = null
        }
        window.attributes = attributes
    }

    companion object {
        private const val APP_ASSET_HOST = "appassets.androidplatform.net"
        private const val APP_ORIGIN = "https://$APP_ASSET_HOST"
        private const val HOME_URL = "https://$APP_ASSET_HOST/assets/web/index.html"
        private const val SEND_URL = "https://$APP_ASSET_HOST/assets/web/send/index.html"
        private const val REQUEST_CAMERA = 41
        private const val REQUEST_OPEN_FILE = 42
        private const val REQUEST_SAVE_FILE = 43
    }
}
