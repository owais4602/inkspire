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
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val hasThumbnail: Boolean = false,
)
