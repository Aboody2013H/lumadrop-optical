package io.lumadrop.app.transport

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class FountainCodecTest {
    private val source = ByteArray(83_117).also { Random(42).nextBytes(it) }

    @Test
    fun systematicFramesReconstructExactFile() {
        val encoder = FountainEncoder(source, "sample.bin", "application/octet-stream", transferId = 7L)
        val decoder = FountainDecoder()
        val order = (0 until encoder.meta.chunkCount).shuffled(Random(9))

        order.forEach { decoder.add(encoder.droplet(it)) }

        assertTrue(decoder.isComplete())
        assertArrayEquals(source, decoder.reconstruct())
    }

    @Test
    fun repairDropletsRecoverDroppedFrames() {
        val encoder = FountainEncoder(source, "photo.jpg", "image/jpeg", transferId = 91L)
        val decoder = FountainDecoder()

        // Lose every third original frame, then feed unlimited fountain repair droplets.
        repeat(encoder.meta.chunkCount) { if (it % 3 != 0) decoder.add(encoder.droplet(it)) }
        var sequence = encoder.meta.chunkCount
        while (!decoder.isComplete() && sequence < encoder.meta.chunkCount * 20) {
            decoder.add(encoder.droplet(sequence++))
        }

        assertTrue("Repair stream should solve all missing chunks", decoder.isComplete())
        assertArrayEquals(source, decoder.reconstruct())
    }

    @Test
    fun duplicateFramesAreIgnored() {
        val encoder = FountainEncoder(source, "data.bin", "application/octet-stream", transferId = 11L)
        val decoder = FountainDecoder()
        val frame = encoder.droplet(0)

        val first = decoder.add(frame)
        val duplicate = decoder.add(frame)

        assertEquals(first, duplicate)
        assertEquals(1, duplicate.uniqueFrames)
        assertFalse(decoder.isComplete())
    }
}

