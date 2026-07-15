package dev.stupifranc.inkspire.model

import dev.stupifranc.inkspire.core.Point
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CanvasShape {
    RECTANGLE, ROUNDED_RECTANGLE,

    // Serial name kept as the old "OVAL" so drawings saved before the oval-to-circle rename
    // (index.json entries with shape="OVAL") still decode to this value instead of crashing.
    @SerialName("OVAL")
    CIRCLE,
}

/** The document's own fixed size (world units) and background — independent of screen/viewport size. */
data class CanvasSpec(
    val width: Float,
    val height: Float,
    val backgroundColorArgb: Int,
    val paperStyle: PaperStyle = PaperStyle.PLAIN,
    val paperSpacing: Float = DEFAULT_PAPER_SPACING,
    val shape: CanvasShape = CanvasShape.RECTANGLE,
)

/** Whether [point] (document space) falls within the page — drawing/erasing outside it should be a no-op. */
fun CanvasSpec.contains(point: Point): Boolean =
    point.x in 0f..width && point.y in 0f..height
