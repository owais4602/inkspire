package dev.stupifranc.inkspire.core

/**
 * Document<->screen mapping for canvas pan/zoom: `screen = document * scale + pan`.
 * No rotation — canvas pan/zoom is axis-aligned, unlike [SymmetryEngine]'s per-stroke transforms.
 */
data class Viewport(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
) {
    fun documentToScreen(point: Point): Point = Point(point.x * scale + panX, point.y * scale + panY)

    fun screenToDocument(point: Point): Point = Point((point.x - panX) / scale, (point.y - panY) / scale)

    fun pannedBy(dx: Float, dy: Float): Viewport = copy(panX = panX + dx, panY = panY + dy)

    /** Zooms by [factor] (clamped to [MIN_SCALE]..[MAX_SCALE]) while keeping [focal] fixed on screen. */
    fun zoomedBy(factor: Float, focal: Point): Viewport {
        val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        val applied = newScale / scale
        return copy(
            scale = newScale,
            panX = focal.x - (focal.x - panX) * applied,
            panY = focal.y - (focal.y - panY) * applied,
        )
    }

    /** Keeps the document from drifting fully out of view and scale within bounds. */
    fun clampedTo(documentWidth: Float, documentHeight: Float, viewportWidth: Float, viewportHeight: Float): Viewport {
        if (documentWidth <= 0f || documentHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) return this
        val clampedScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        return copy(
            scale = clampedScale,
            panX = clampAxis(panX, documentWidth * clampedScale, viewportWidth),
            panY = clampAxis(panY, documentHeight * clampedScale, viewportHeight),
        )
    }

    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 8f

        /** A viewport that centers the whole document within the viewport bounds, scaled to fit. */
        fun fit(documentWidth: Float, documentHeight: Float, viewportWidth: Float, viewportHeight: Float): Viewport {
            if (documentWidth <= 0f || documentHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) {
                return Viewport()
            }
            val scale = minOf(viewportWidth / documentWidth, viewportHeight / documentHeight)
            return Viewport(
                scale = scale,
                panX = (viewportWidth - documentWidth * scale) / 2f,
                panY = (viewportHeight - documentHeight * scale) / 2f,
            )
        }

        private fun clampAxis(pan: Float, documentScreenSize: Float, viewportSize: Float): Float =
            if (documentScreenSize <= viewportSize) {
                (viewportSize - documentScreenSize) / 2f
            } else {
                pan.coerceIn(viewportSize - documentScreenSize, 0f)
            }
    }
}
