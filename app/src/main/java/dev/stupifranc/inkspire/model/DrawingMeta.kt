package dev.stupifranc.inkspire.model

import kotlinx.serialization.Serializable

/** Index-file entry for one saved drawing. Stroke data itself lives in a separate per-drawing binary file. */
@Serializable
data class DrawingMeta(
    val id: String,
    val name: String,
    val width: Float,
    val height: Float,
    val backgroundColorArgb: Int,
    val background: CanvasBackground? = null,
    val paperStyle: PaperStyle = PaperStyle.PLAIN,
    val paperSpacing: Float = DEFAULT_PAPER_SPACING,
    val shape: CanvasShape = CanvasShape.RECTANGLE,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val hasThumbnail: Boolean = false,
    val orderIndex: Long = 0L,
    val isPinned: Boolean = false,
)
