package dev.stupifranc.inkspire.core

/** An axis-aligned bounding box in document space. */
data class BoundingBox(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
}

/** A candidate page covering some content: size plus the offset needed to bring content back to `(0,0)`. */
data class ContentBoundsResult(val width: Float, val height: Float, val offsetX: Float, val offsetY: Float)

private const val MIN_PAGE_SIDE_PX = 256f

object ContentBounds {

    /**
     * The minimal page (with [paddingPx] breathing room on every side) covering every box in
     * [boxes], never smaller than [MIN_PAGE_SIDE_PX] per side. Returns null for an empty list —
     * the caller should keep the current page rather than resize to nothing.
     */
    fun compute(boxes: List<BoundingBox>, paddingPx: Float = 0f): ContentBoundsResult? {
        if (boxes.isEmpty()) return null
        val minX = boxes.minOf { it.minX } - paddingPx
        val minY = boxes.minOf { it.minY } - paddingPx
        val maxX = boxes.maxOf { it.maxX } + paddingPx
        val maxY = boxes.maxOf { it.maxY } + paddingPx
        return ContentBoundsResult(
            width = (maxX - minX).coerceAtLeast(MIN_PAGE_SIDE_PX),
            height = (maxY - minY).coerceAtLeast(MIN_PAGE_SIDE_PX),
            offsetX = minX,
            offsetY = minY,
        )
    }

    /** The union of every box in [boxes] with the page rect `[0,0,pageWidth,pageHeight]` — used for "fit shows everything". */
    fun union(boxes: List<BoundingBox>, pageWidth: Float, pageHeight: Float): BoundingBox {
        var minX = 0f
        var minY = 0f
        var maxX = pageWidth
        var maxY = pageHeight
        for (box in boxes) {
            minX = minOf(minX, box.minX)
            minY = minOf(minY, box.minY)
            maxX = maxOf(maxX, box.maxX)
            maxY = maxOf(maxY, box.maxY)
        }
        return BoundingBox(minX, minY, maxX, maxY)
    }
}
