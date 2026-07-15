package dev.stupifranc.inkspire.model

import dev.stupifranc.inkspire.core.Point

/** The document's own fixed size (world units) and background — independent of screen/viewport size. */
data class CanvasSpec(
    val width: Float,
    val height: Float,
    val backgroundColorArgb: Int,
    val paperStyle: PaperStyle = PaperStyle.PLAIN,
    val paperSpacing: Float = DEFAULT_PAPER_SPACING,
)

/** Whether [point] (document space) falls within the page — drawing/erasing outside it should be a no-op. */
fun CanvasSpec.contains(point: Point): Boolean =
    point.x in 0f..width && point.y in 0f..height
