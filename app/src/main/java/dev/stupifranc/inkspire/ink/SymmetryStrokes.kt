package dev.stupifranc.inkspire.ink

import androidx.ink.strokes.Stroke
import dev.stupifranc.inkspire.core.Affine2

/**
 * Symmetry fallback (see PLAN.md traps #1/#4): the wet layer only ever draws the primary stroke
 * live. The instant it finishes, its raw [androidx.ink.strokes.StrokeInput] points are replayed
 * through each remaining transform to build the mirrored/rotated copies as fully-formed dry
 * strokes. [transforms] must start with an identity entry (as
 * [dev.stupifranc.inkspire.core.SymmetryEngine] guarantees), which this reuses [this] unchanged for
 * rather than rebuilding it.
 */
fun Stroke.replicateThrough(transforms: List<Affine2>): List<Stroke> =
    listOf(this) + transforms.drop(1).map { transform -> transformedBy(transform) }
