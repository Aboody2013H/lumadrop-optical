package io.lumadrop.app.transport

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.Random
import java.util.zip.CRC32
import kotlin.math.min

const val DEFAULT_CHUNK_SIZE = 620
private const val TEXT_PREFIX = "LD1:"
private const val MAGIC = 0x4C554D41 // LUMA
private const val VERSION = 1

data class TransferMeta(
    val transferId: Long,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val chunkSize: Int,
    val chunkCount: Int,
    val sha256: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is TransferMeta &&
        transferId == other.transferId && fileName == other.fileName &&
        mimeType == other.mimeType && fileSize == other.fileSize &&
        chunkSize == other.chunkSize && chunkCount == other.chunkCount &&
        sha256.contentEquals(other.sha256)

    override fun hashCode(): Int = 31 * transferId.hashCode() + sha256.contentHashCode()
}

data class Droplet(val meta: TransferMeta, val sequence: Int, val payload: ByteArray)

class FountainEncoder(
    private val bytes: ByteArray,
    fileName: String,
    mimeType: String,
    transferId: Long = Random().nextLong(),
    chunkSize: Int = DEFAULT_CHUNK_SIZE,
) {
    val meta: TransferMeta
    private val chunks: List<ByteArray>

    init {
        require(bytes.isNotEmpty()) { "The selected file is empty" }
        val count = (bytes.size + chunkSize - 1) / chunkSize
        chunks = List(count) { index ->
            ByteArray(chunkSize).also { out ->
                val start = index * chunkSize
                bytes.copyInto(out, endIndex = min(bytes.size, start + chunkSize), startIndex = start)
            }
        }
        meta = TransferMeta(
            transferId = transferId,
            fileName = fileName.take(180),
            mimeType = mimeType.take(100),
            fileSize = bytes.size.toLong(),
            chunkSize = chunkSize,
            chunkCount = count,
            sha256 = MessageDigest.getInstance("SHA-256").digest(bytes),
        )
    }

    fun droplet(sequence: Int): Droplet {
        val indices = selectIndices(meta, sequence)
        val payload = ByteArray(meta.chunkSize)
        indices.forEach { index -> xorInto(payload, chunks[index]) }
        return Droplet(meta, sequence, payload)
    }
}

fun Droplet.toQrText(): String {
    val body = ByteArrayOutputStream().also { buffer ->
        DataOutputStream(buffer).use { out ->
            out.writeInt(MAGIC)
            out.writeByte(VERSION)
            out.writeLong(meta.transferId)
            out.writeInt(sequence)
            out.writeLong(meta.fileSize)
            out.writeShort(meta.chunkSize)
            out.writeInt(meta.chunkCount)
            out.writeUTF(meta.fileName)
            out.writeUTF(meta.mimeType)
            out.write(meta.sha256)
            out.write(payload)
        }
    }.toByteArray()
    val crc = CRC32().apply { update(body) }.value.toInt()
    val packet = body + byteArrayOf(
        (crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte()
    )
    return TEXT_PREFIX + Base64.encodeToString(packet, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}

fun parseDroplet(text: String): Droplet? = runCatching {
    if (!text.startsWith(TEXT_PREFIX)) return null
    val packet = Base64.decode(text.removePrefix(TEXT_PREFIX), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    if (packet.size < 80) return null
    val body = packet.copyOf(packet.size - 4)
    val expected = ((packet[packet.size - 4].toInt() and 0xff) shl 24) or
        ((packet[packet.size - 3].toInt() and 0xff) shl 16) or
        ((packet[packet.size - 2].toInt() and 0xff) shl 8) or
        (packet.last().toInt() and 0xff)
    if (CRC32().apply { update(body) }.value.toInt() != expected) return null
    DataInputStream(ByteArrayInputStream(body)).use { input ->
        if (input.readInt() != MAGIC || input.readUnsignedByte() != VERSION) return null
        val transferId = input.readLong()
        val sequence = input.readInt()
        val fileSize = input.readLong()
        val chunkSize = input.readUnsignedShort()
        val chunkCount = input.readInt()
        val name = input.readUTF()
        val mime = input.readUTF()
        if (fileSize <= 0 || chunkSize !in 128..2048 || chunkCount !in 1..1_000_000) return null
        val sha = ByteArray(32).also(input::readFully)
        val payload = ByteArray(chunkSize).also(input::readFully)
        Droplet(TransferMeta(transferId, name, mime, fileSize, chunkSize, chunkCount, sha), sequence, payload)
    }
}.getOrNull()

internal fun selectIndices(meta: TransferMeta, sequence: Int): IntArray {
    if (sequence in 0 until meta.chunkCount) return intArrayOf(sequence)
    val random = Random(meta.transferId xor sequence.toLong().rotateLeft(21))
    val roll = random.nextInt(100)
    val degree = min(meta.chunkCount, when {
        roll < 35 -> 2
        roll < 60 -> 3
        roll < 78 -> 4
        roll < 90 -> 5
        roll < 97 -> 6
        else -> 7 + random.nextInt(min(6, meta.chunkCount).coerceAtLeast(1))
    })
    val picked = LinkedHashSet<Int>(degree)
    while (picked.size < degree) picked += random.nextInt(meta.chunkCount)
    return picked.toIntArray()
}

internal fun xorInto(target: ByteArray, source: ByteArray) {
    for (i in target.indices) target[i] = (target[i].toInt() xor source[i].toInt()).toByte()
}

