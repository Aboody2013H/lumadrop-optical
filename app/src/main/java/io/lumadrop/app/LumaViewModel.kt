package io.lumadrop.app

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lumadrop.app.transport.DecodeProgress
import io.lumadrop.app.transport.FountainDecoder
import io.lumadrop.app.transport.FountainEncoder
import io.lumadrop.app.transport.TransferMeta
import io.lumadrop.app.transport.parseDroplet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SenderUiState(
    val loading: Boolean = false,
    val encoder: FountainEncoder? = null,
    val error: String? = null,
)

data class ReceiverUiState(
    val meta: TransferMeta? = null,
    val progress: DecodeProgress = DecodeProgress(0, 0, 0),
    val completedBytes: ByteArray? = null,
    val error: String? = null,
)

class LumaViewModel : ViewModel() {
    private val _sender = MutableStateFlow(SenderUiState())
    val sender = _sender.asStateFlow()
    private val _receiver = MutableStateFlow(ReceiverUiState())
    val receiver = _receiver.asStateFlow()
    private var decoder = FountainDecoder()

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _sender.value = SenderUiState(loading = true)
            _sender.value = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Could not open that file")
                }
                require(bytes.size <= 100 * 1024 * 1024) { "Prototype limit is 100 MB" }
                val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                } ?: "received-file"
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                SenderUiState(encoder = FountainEncoder(bytes, name, mime))
            }.getOrElse { SenderUiState(error = it.message ?: "Could not prepare file") }
        }
    }

    @Synchronized
    fun ingest(qrText: String) {
        val droplet = parseDroplet(qrText) ?: return
        if (_receiver.value.completedBytes != null) return
        runCatching {
            val progress = decoder.add(droplet)
            val completed = if (decoder.isComplete()) decoder.reconstruct() else null
            _receiver.value = ReceiverUiState(droplet.meta, progress, completed)
        }.onFailure {
            if (it is IllegalArgumentException && it.message?.contains("different transfers") == true) return
            _receiver.value = _receiver.value.copy(error = it.message ?: "Frame rejected")
        }
    }

    fun resetSender() { _sender.value = SenderUiState() }

    fun resetReceiver() {
        decoder = FountainDecoder()
        _receiver.value = ReceiverUiState()
    }
}

