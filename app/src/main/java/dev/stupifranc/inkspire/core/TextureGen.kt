package dev.stupifranc.inkspire.core

/**
 * Deterministic, seeded, tileable texture pixel generators (ARGB ints, row-major).
 * RGB must stay white (0xFFFFFF): brush texture layers use BlendMode.MODULATE, which multiplies
 * texture RGB into the brush color — any non-white base would tint every stroke toward black.
 */
object TextureGen {

    private fun hash(x: Int, y: Int, periodX: Int, periodY: Int, seed: Long): Float {
        val wx = (x % periodX + periodX) % periodX
        val wy = (y % periodY + periodY) % periodY
        var h = seed + wx * 374761393L + wy * 668265263L
        h = (h xor (h shr 13)) * 1274126177L
        return ((h xor (h shr 16)) and 0x7FFFFFFF) / 0x7FFFFFFF.toFloat()
    }

    private fun smoothNoise(x: Float, y: Float, periodX: Int, periodY: Int, seed: Long): Float {
        val ix = x.toInt()
        val iy = y.toInt()
        val fx = x - ix
        val fy = y - iy

        val ux = fx * fx * (3f - 2f * fx)
        val uy = fy * fy * (3f - 2f * fy)

        val v00 = hash(ix, iy, periodX, periodY, seed)
        val v10 = hash(ix + 1, iy, periodX, periodY, seed)
        val v01 = hash(ix, iy + 1, periodX, periodY, seed)
        val v11 = hash(ix + 1, iy + 1, periodX, periodY, seed)

        val nx0 = v00 * (1 - ux) + v10 * ux
        val nx1 = v01 * (1 - ux) + v11 * ux
        return nx0 * (1 - uy) + nx1 * uy
    }

    private fun fbm(px: Float, py: Float, size: Int, seed: Long, basePeriodX: Int, basePeriodY: Int, octaves: Int): Float {
        var value = 0f
        var amp = 0.5f
        var periodX = basePeriodX
        var periodY = basePeriodY
        var maxAmp = 0f
        
        for (i in 0 until octaves) {
            val x = px * periodX / size
            val y = py * periodY / size
            value += smoothNoise(x, y, periodX, periodY, seed + i) * amp
            maxAmp += amp
            amp *= 0.5f
            periodX *= 2
            periodY *= 2
        }
        return value / maxAmp
    }

    fun granulation(size: Int, seed: Long): IntArray {
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val noise = fbm(x.toFloat(), y.toFloat(), size, seed, 8, 8, 3)
                val alpha = (noise * 200).toInt().coerceIn(0, 255)
                pixels[y * size + x] = (alpha shl 24) or 0x00FFFFFF
            }
        }
        return pixels
    }

    fun dryStreaks(size: Int, seed: Long): IntArray {
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val noise = fbm(x.toFloat(), y.toFloat(), size, seed, 2, 16, 4)
                val alpha = (noise * 255).toInt().coerceIn(0, 255)
                pixels[y * size + x] = (alpha shl 24) or 0x00FFFFFF
            }
        }
        return pixels
    }

    fun paperGrain(size: Int, seed: Long): IntArray {
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val noise = fbm(x.toFloat(), y.toFloat(), size, seed, 16, 16, 2)
                val alpha = (noise * 80).toInt().coerceIn(0, 255)
                pixels[y * size + x] = (alpha shl 24) or 0x00FFFFFF
            }
        }
        return pixels
    }
}
