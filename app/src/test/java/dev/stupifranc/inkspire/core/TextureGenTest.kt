package dev.stupifranc.inkspire.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class TextureGenTest {

    private fun assertTileable(pixels: IntArray, size: Int) {
        var edgeDeltaSum = 0L
        for (i in 0 until size) {
            val topAlpha = (pixels[0 * size + i] ushr 24) and 0xFF
            val bottomAlpha = (pixels[(size - 1) * size + i] ushr 24) and 0xFF
            edgeDeltaSum += abs(topAlpha - bottomAlpha)

            val leftAlpha = (pixels[i * size + 0] ushr 24) and 0xFF
            val rightAlpha = (pixels[i * size + (size - 1)] ushr 24) and 0xFF
            edgeDeltaSum += abs(leftAlpha - rightAlpha)
        }
        val meanEdgeDelta = edgeDeltaSum.toDouble() / (size * 2)

        var interiorDeltaSum = 0L
        var interiorCount = 0
        val rand = Random(42)
        for (k in 0 until (size * 2)) {
            val x = rand.nextInt(1, size - 1)
            val y = rand.nextInt(1, size - 1)
            val cAlpha = (pixels[y * size + x] ushr 24) and 0xFF
            val nAlpha = (pixels[y * size + x + 1] ushr 24) and 0xFF
            val sAlpha = (pixels[(y + 1) * size + x] ushr 24) and 0xFF
            interiorDeltaSum += abs(cAlpha - nAlpha)
            interiorDeltaSum += abs(cAlpha - sAlpha)
            interiorCount += 2
        }
        val meanInteriorDelta = interiorDeltaSum.toDouble() / interiorCount

        assertTrue("Tileability failed: meanEdgeDelta=$meanEdgeDelta, meanInteriorDelta=$meanInteriorDelta", meanEdgeDelta <= meanInteriorDelta * 1.5 + 5.0)
    }

    private fun assertGenerator(generator: (Int, Long) -> IntArray) {
        val size = 64
        val seed1 = 12345L
        val seed2 = 54321L

        val p1 = generator(size, seed1)
        val p2 = generator(size, seed1)
        val p3 = generator(size, seed2)

        assertEquals(size * size, p1.size)
        assertTrue(p1.contentEquals(p2))
        assertTrue(!p1.contentEquals(p3))

        for (p in p1) {
            val alpha = (p ushr 24) and 0xFF
            assertTrue(alpha in 0..255)
            // White base is required by BlendMode.MODULATE — see TextureGen's doc.
            assertEquals(0xFFFFFF, p and 0xFFFFFF)
        }

        assertTileable(p1, size)
    }

    @Test
    fun testGranulation() {
        assertGenerator(TextureGen::granulation)
    }

    @Test
    fun testDryStreaks() {
        assertGenerator(TextureGen::dryStreaks)
    }

    @Test
    fun testPaperGrain() {
        assertGenerator(TextureGen::paperGrain)
    }
}
