package dev.stupifranc.inkspire.model

import androidx.ink.strokes.Stroke
import dev.stupifranc.inkspire.core.Identifiable
import java.util.UUID

/** A committed (dry) stroke. Strokes finished in the same gesture share a [groupId] so undo removes them together. */
data class StrokeEntry(
    override val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val stroke: Stroke,
) : Identifiable
