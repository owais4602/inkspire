package dev.stupifranc.inkspire.model

import androidx.ink.strokes.Stroke
import java.util.UUID

/** A committed (dry) stroke. Strokes finished in the same gesture share a [groupId] so undo removes them together. */
data class StrokeEntry(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val stroke: Stroke,
)
