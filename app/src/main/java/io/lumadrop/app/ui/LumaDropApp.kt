package io.lumadrop.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.lumadrop.app.LumaViewModel
import io.lumadrop.app.transport.FountainEncoder
import io.lumadrop.app.transport.toQrText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class Page { HOME, SEND, RECEIVE }

@Composable
fun LumaDropApp(model: LumaViewModel) {
    var page by remember { mutableStateOf(Page.HOME) }
    BackHandler(page != Page.HOME) { page = Page.HOME }
    Box(Modifier.fillMaxSize().background(Ink)) {
        TechBackdrop()
        AnimatedContent(page, label = "page") { current ->
            when (current) {
                Page.HOME -> HomeScreen(onSend = { page = Page.SEND }, onReceive = { page = Page.RECEIVE })
                Page.SEND -> SenderScreen(model) { page = Page.HOME }
                Page.RECEIVE -> ReceiverScreen(model) { page = Page.HOME }
            }
        }
    }
}

@Composable
private fun TechBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 42.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(Mint.copy(alpha = .035f), Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Mint.copy(alpha = .035f), Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        drawCircle(Mint.copy(alpha = .05f), radius = size.width * .65f, center = Offset(size.width, 0f))
    }
}

@Composable
private fun HomeScreen(onSend: () -> Unit, onReceive: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Mint), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.NearMe, null, tint = Ink)
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text("LUMADROP", fontWeight = FontWeight.Black, letterSpacing = 3.sp, fontSize = 22.sp)
                Text("OPTICAL FILE TRANSPORT", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(42.dp))
        Text("MOVE DATA\nAT LIGHT SPEED.", fontSize = 40.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Text("No cloud. No pairing. Just point two phones at each other.", color = Muted, lineHeight = 21.sp)
        Spacer(Modifier.height(34.dp))
        ActionCard(
            title = "SEND A FILE", caption = "Turn any file into a live QR stream",
            icon = Icons.Rounded.QrCode2, accent = Mint, onClick = onSend,
        )
        Spacer(Modifier.height(14.dp))
        ActionCard(
            title = "RECEIVE", caption = "Capture the stream with your camera",
            icon = Icons.Rounded.CameraAlt, accent = Cyan, onClick = onReceive,
        )
        Spacer(Modifier.height(34.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MicroStat("LINK", "AIR-GAPPED")
            MicroStat("RECOVERY", "FOUNTAIN")
            MicroStat("TARGET", "60 FPS")
        }
    }
}

@Composable
private fun ActionCard(title: String, caption: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel)
            .border(1.dp, accent.copy(alpha = .26f), RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(21.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(3.dp))
            Text(caption, color = Muted, fontSize = 13.sp)
        }
        Text("→", color = accent, fontSize = 24.sp)
    }
}

@Composable private fun MicroStat(label: String, value: String) {
    Column {
        Text(label, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        Text(value, color = MintSoft, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PageHeader(title: String, status: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 18.sp)
            Text(status, color = Mint, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        Box(Modifier.size(8.dp).background(Mint, CircleShape))
    }
}

@Composable
private fun SenderScreen(model: LumaViewModel, onBack: () -> Unit) {
    val state by model.sender.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { model.loadFile(context.contentResolver, it) }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        PageHeader("TRANSMIT", if (state.encoder == null) "STANDBY" else "OPTICAL LINK ACTIVE", onBack)
        when {
            state.loading -> LoadingPanel("ENCODING FILE")
            state.encoder == null -> FilePickerPanel(state.error) { picker.launch(arrayOf("*/*")) }
            else -> ActiveSender(state.encoder!!, onStop = model::resetSender)
        }
    }
}

@Composable
private fun LoadingPanel(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Mint)
            Spacer(Modifier.height(18.dp))
            Text(label, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun FilePickerPanel(error: String?, onPick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(110.dp).clip(CircleShape).background(Mint.copy(alpha = .08f)).border(1.dp, Mint.copy(alpha = .3f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.FileOpen, null, tint = Mint, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(26.dp))
            Text("CHOOSE YOUR PAYLOAD", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Any file up to 100 MB", color = Muted, fontSize = 13.sp)
            error?.let { Text(it, color = Danger, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(28.dp))
            Button(onClick = onPick, colors = ButtonDefaults.buttonColors(containerColor = Mint), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Rounded.Bolt, null)
                Spacer(Modifier.size(8.dp))
                Text("SELECT FILE", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ActiveSender(encoder: FountainEncoder, onStop: () -> Unit) {
    var sequence by remember(encoder) { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fps by remember { mutableFloatStateOf(0f) }
    var countWindow by remember { mutableIntStateOf(0) }
    var windowStart by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(encoder) {
        while (true) {
            val started = SystemClock.elapsedRealtime()
            val next = sequence + 1
            bitmap = withContext(Dispatchers.Default) { qrBitmap(encoder.droplet(next).toQrText(), 900) }
            sequence = next
            countWindow++
            val now = SystemClock.elapsedRealtime()
            if (now - windowStart >= 1000) {
                fps = countWindow * 1000f / (now - windowStart)
                countWindow = 0
                windowStart = now
            }
            delay((16L - (SystemClock.elapsedRealtime() - started)).coerceAtLeast(0L))
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(encoder.meta.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(formatBytes(encoder.meta.fileSize), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            Text("%.0f FPS".format(Locale.US, fps), color = Mint, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            bitmap?.let { Image(it.asImageBitmap(), "Animated transfer QR", Modifier.fillMaxSize()) }
                ?: CircularProgressIndicator(color = Ink)
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulseDot()
            Spacer(Modifier.size(9.dp))
            Text("KEEP BOTH PHONES STEADY", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Frame ${sequence.toString().padStart(6, '0')}  •  fountain stream", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("STOP TRANSMISSION") }
    }
}

@Composable private fun PulseDot() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(.25f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseAlpha")
    Box(Modifier.size(9.dp).background(Mint.copy(alpha = alpha), CircleShape))
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.CHARACTER_SET to "ISO-8859-1")
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) AndroidColor.BLACK else AndroidColor.WHITE }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.2f MB".format(Locale.US, bytes / 1_048_576f)
    bytes >= 1024 -> "%.1f KB".format(Locale.US, bytes / 1024f)
    else -> "$bytes B"
}

@Composable
private fun ReceiverScreen(model: LumaViewModel, onBack: () -> Unit) {
    val state by model.receiver.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted = it }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(state.meta?.mimeType ?: "application/octet-stream")) { uri ->
        if (uri != null) saveReceived(context, uri, state.completedBytes)
    }
    LaunchedEffect(Unit) { if (!permissionGranted) permission.launch(Manifest.permission.CAMERA) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        PageHeader("RECEIVE", if (state.completedBytes != null) "TRANSFER VERIFIED" else "SCANNER ARMED", onBack)
        Spacer(Modifier.height(14.dp))
        if (!permissionGranted) {
            PermissionPanel { permission.launch(Manifest.permission.CAMERA) }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp)).background(Panel)) {
                if (state.completedBytes == null) QrCameraScanner(onQr = model::ingest)
                ScannerOverlay(state.progress.fraction)
                if (state.completedBytes != null) CompleteOverlay(state.meta?.fileName.orEmpty())
            }
            Spacer(Modifier.height(15.dp))
            ReceiveStats(state.meta?.fileName, state.progress.solved, state.progress.total, state.progress.uniqueFrames)
            AnimatedVisibility(state.completedBytes != null) {
                Button(
                    onClick = { saveLauncher.launch(state.meta?.fileName ?: "lumadrop-file") },
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp),
                ) { Text("SAVE FILE", fontWeight = FontWeight.Black) }
            }
            OutlinedButton(onClick = model::resetReceiver, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(48.dp)) {
                Icon(Icons.Rounded.Refresh, null)
                Spacer(Modifier.size(8.dp))
                Text("RESET SCANNER")
            }
        }
    }
}

@Composable private fun PermissionPanel(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.CameraAlt, null, tint = Mint, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(18.dp))
            Text("CAMERA ACCESS REQUIRED", fontWeight = FontWeight.Bold)
            Text("Frames are analyzed only on this phone.", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(22.dp))
            Button(onClick = onRequest) { Text("ENABLE CAMERA") }
        }
    }
}

@Composable private fun ScannerOverlay(progress: Float) {
    Canvas(Modifier.fillMaxSize().padding(34.dp)) {
        val corner = 42.dp.toPx()
        val stroke = 3.dp.toPx()
        val c = if (progress > 0f) Mint else Color.White.copy(alpha = .8f)
        drawArc(c, 180f, 90f, false, Offset.Zero, Size(corner * 2, corner * 2), style = Stroke(stroke))
        drawArc(c, 270f, 90f, false, Offset(size.width - corner * 2, 0f), Size(corner * 2, corner * 2), style = Stroke(stroke))
        drawArc(c, 0f, 90f, false, Offset(size.width - corner * 2, size.height - corner * 2), Size(corner * 2, corner * 2), style = Stroke(stroke))
        drawArc(c, 90f, 90f, false, Offset(0f, size.height - corner * 2), Size(corner * 2, corner * 2), style = Stroke(stroke))
    }
}

@Composable private fun CompleteOverlay(fileName: String) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = .92f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Mint, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("PAYLOAD RECOVERED", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text(fileName, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 28.dp))
        }
    }
}

@Composable private fun ReceiveStats(name: String?, solved: Int, total: Int, frames: Int) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name ?: "Waiting for optical stream…", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text("$solved / $total", color = Mint, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else solved.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Mint, trackColor = PanelRaised,
        )
        Spacer(Modifier.height(8.dp))
        Text("$frames unique frames captured", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}

private fun saveReceived(context: Context, uri: Uri, bytes: ByteArray?) {
    if (bytes == null) return
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
}

