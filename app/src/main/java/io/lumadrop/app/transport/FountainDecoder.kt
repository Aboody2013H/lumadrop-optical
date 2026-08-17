package io.lumadrop.app.transport

import java.security.MessageDigest

data class DecodeProgress(val solved: Int, val total: Int, val uniqueFrames: Int) {
    val fraction: Float get() = if (total == 0) 0f else solved.toFloat() / total
}

class FountainDecoder {
    private data class Equation(val indices: MutableSet<Int>, val payload: ByteArray)

    var meta: TransferMeta? = null
        private set
    private val solved = mutableMapOf<Int, ByteArray>()
    private val equations = mutableListOf<Equation>()
    private val seenSequences = mutableSetOf<Int>()

    fun add(droplet: Droplet): DecodeProgress {
        if (meta == null) meta = droplet.meta
        require(meta == droplet.meta) { "Frames belong to different transfers" }
        if (!seenSequences.add(droplet.sequence)) return progress()

        val indices = selectIndices(droplet.meta, droplet.sequence).toMutableSet()
        val payload = droplet.payload.copyOf()
        solved.forEach { (index, chunk) -> if (indices.remove(index)) xorInto(payload, chunk) }
        when (indices.size) {
            0 -> Unit
            1 -> solve(indices.first(), payload)
            else -> equations += Equation(indices, payload)
        }
        peel()
        return progress()
    }

    fun progress() = DecodeProgress(solved.size, meta?.chunkCount ?: 0, seenSequences.size)
    fun isComplete(): Boolean = meta?.chunkCount == solved.size

    fun reconstruct(): ByteArray {
        val currentMeta = requireNotNull(meta)
        check(isComplete()) { "Transfer is not complete" }
        val padded = ByteArray(currentMeta.chunkCount * currentMeta.chunkSize)
        repeat(currentMeta.chunkCount) { index ->
            requireNotNull(solved[index]).copyInto(padded, index * currentMeta.chunkSize)
        }
        val file = padded.copyOf(currentMeta.fileSize.toInt())
        check(MessageDigest.getInstance("SHA-256").digest(file).contentEquals(currentMeta.sha256)) {
            "File checksum did not match"
        }
        return file
    }

    private fun solve(index: Int, chunk: ByteArray) {
        val existing = solved[index]
        if (existing == null) solved[index] = chunk
        else check(existing.contentEquals(chunk)) { "Conflicting optical frame" }
    }

    private fun peel() {
        var changed: Boolean
        do {
            changed = false
            val iterator = equations.iterator()
            val newlySolved = mutableListOf<Pair<Int, ByteArray>>()
            while (iterator.hasNext()) {
                val equation = iterator.next()
                solved.forEach { (index, chunk) ->
                    if (equation.indices.remove(index)) xorInto(equation.payload, chunk)
                }
                if (equation.indices.isEmpty()) {
                    iterator.remove()
                } else if (equation.indices.size == 1) {
                    newlySolved += equation.indices.first() to equation.payload
                    iterator.remove()
                }
            }
            newlySolved.forEach { (index, bytes) ->
                if (!solved.containsKey(index)) {
                    solve(index, bytes)
                    changed = true
                }
            }
        } while (changed)
    }
}
