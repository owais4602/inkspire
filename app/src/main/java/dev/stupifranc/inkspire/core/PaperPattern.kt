package dev.stupifranc.inkspire.core

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Pure geometry/color math for paper background patterns (ruled lines, dot grid, square grid,
 * isometric). The Android rendering glue lives in `ink/PaperStyleRenderer.kt`; this object stays
 * JVM-testable.
 */
object PaperPattern {

    /** Offsets of pattern lines strictly inside `(0, extent)`, spaced [spacing] apart. */
    fun lineOffsets(extent: Float, spacing: Float): List<Float> {
        if (spacing <= 0f || extent <= spacing) return emptyList()
        val result = mutableListOf<Float>()
        var offset = spacing
        while (offset < extent) {
            result.add(offset)
            offset += spacing
        }
        return result
    }

    /** All multiples of [spacing] within `[min, max]` (inclusive) — intercepts for slanted-line families. */
    fun interceptOffsets(min: Float, max: Float, spacing: Float): List<Float> {
        if (spacing <= 0f || max < min) return emptyList()
        val first = ceil((min / spacing).toDouble()).toInt()
        val last = floor((max / spacing).toDouble()).toInt()
        return (first..last).map { it * spacing }
    }

    /**
     * Subtle pattern-line color contrasting the paper: translucent black on light backgrounds,
     * translucent white on dark ones. Mid luminance counts as light paper (dark ink).
     */
    fun lineColorArgb(backgroundArgb: Int): Int {
        val r = (backgroundArgb shr 16) and 0xFF
        val g = (backgroundArgb shr 8) and 0xFF
        val b = backgroundArgb and 0xFF
        val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        return if (luminance >= 0.5f) LINE_ALPHA or 0x000000 else LINE_ALPHA or 0xFFFFFF
    }

    private const val LINE_ALPHA = 0x3C shl 24
}
