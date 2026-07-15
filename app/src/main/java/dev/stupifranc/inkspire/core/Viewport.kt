package dev.stupifranc.inkspire.core

import kotlin.math.abs

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

    /**
     * Keeps the document from drifting fully out of view and scale within bounds. [marginPx] lets
     * pan overshoot each page edge by up to that many screen px (reachability for content that
     * lands outside the page, e.g. off-center symmetry replicas) — `marginPx = 0` reproduces the
     * pre-margin clamp exactly.
     */
    fun clampedTo(documentWidth: Float, documentHeight: Float, viewportWidth: Float, viewportHeight: Float, marginPx: Float = 0f): Viewport {
        if (documentWidth <= 0f || documentHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) return this
        val clampedScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        return copy(
            scale = clampedScale,
            panX = clampAxis(panX, documentWidth * clampedScale, viewportWidth, marginPx),
            panY = clampAxis(panY, documentHeight * clampedScale, viewportHeight, marginPx),
        )
    }

    /**
     * Double-tap = zoom-in/fit toggle: from (near) the fit scale for the rect
     * `[rectX,rectY,rectWidth,rectHeight]`, zooms to 3x centered on the tapped document point;
     * from any other scale, returns to fitting that rect. The caller decides what the rect is
     * (typically the union of the page and the drawing's content bounds, so "fit" always shows
     * everything — see [ContentBounds.union]).
     */
    fun doubleTapTarget(
        rectX: Float,
        rectY: Float,
        rectWidth: Float,
        rectHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        tapScreen: Point,
    ): Viewport {
        val fitViewport = fitRect(rectX, rectY, rectWidth, rectHeight, viewportWidth, viewportHeight)
        val nearFit = fitViewport.scale > 0f && abs(scale - fitViewport.scale) <= fitViewport.scale * FIT_TOLERANCE
        return if (nearFit) {
            zoomedTo(scale = DOUBLE_TAP_ZOOM_SCALE, documentFocus = screenToDocument(tapScreen), viewportWidth = viewportWidth, viewportHeight = viewportHeight)
        } else {
            fitViewport
        }
    }

    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 32f
        private const val FIT_TOLERANCE = 0.1f
        private const val DOUBLE_TAP_ZOOM_SCALE = 3f

        /** A viewport at [scale] with [documentFocus] centered in the viewport. */
        fun zoomedTo(scale: Float, documentFocus: Point, viewportWidth: Float, viewportHeight: Float): Viewport {
            val clampedScale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
            return Viewport(
                scale = clampedScale,
                panX = viewportWidth / 2f - documentFocus.x * clampedScale,
                panY = viewportHeight / 2f - documentFocus.y * clampedScale,
            )
        }

        /** A viewport that centers the whole document within the viewport bounds, scaled to fit. */
        fun fit(documentWidth: Float, documentHeight: Float, viewportWidth: Float, viewportHeight: Float): Viewport =
            fitRect(0f, 0f, documentWidth, documentHeight, viewportWidth, viewportHeight)

        /** A viewport that centers and scales-to-fit the arbitrary document-space rect `[x,y,x+width,y+height]`. */
        fun fitRect(x: Float, y: Float, width: Float, height: Float, viewportWidth: Float, viewportHeight: Float): Viewport {
            if (width <= 0f || height <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) {
                return Viewport()
            }
            val scale = minOf(viewportWidth / width, viewportHeight / height)
            return Viewport(
                scale = scale,
                panX = (viewportWidth - width * scale) / 2f - x * scale,
                panY = (viewportHeight - height * scale) / 2f - y * scale,
            )
        }

        private fun clampAxis(pan: Float, documentScreenSize: Float, viewportSize: Float, marginPx: Float): Float =
            if (documentScreenSize <= viewportSize) {
                val centered = (viewportSize - documentScreenSize) / 2f
                pan.coerceIn(centered - marginPx, centered + marginPx)
            } else {
                pan.coerceIn(viewportSize - documentScreenSize - marginPx, marginPx)
            }
    }
}
