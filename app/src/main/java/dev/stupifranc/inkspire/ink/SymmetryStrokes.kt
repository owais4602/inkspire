package dev.stupifranc.inkspire.ink

import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import dev.stupifranc.inkspire.core.Affine2
import dev.stupifranc.inkspire.core.Point

/**
 * Symmetry fallback (see PLAN.md traps #1/#4): the wet layer only ever draws the primary stroke
 * live. The instant it finishes, its raw [StrokeInput] points are replayed through each remaining
 * transform to build the mirrored/rotated copies as fully-formed dry strokes. [transforms] must
 * start with an identity entry (as [dev.stupifranc.inkspire.core.SymmetryEngine] guarantees), which
 * this reuses [stroke] unchanged for rather than rebuilding it.
 */
fun Stroke.replicateThrough(transforms: List<Affine2>): List<Stroke> =
    listOf(this) + transforms.drop(1).map { transform -> transform.applyTo(this) }

private fun Affine2.applyTo(stroke: Stroke): Stroke {
    val source = stroke.inputs
    val transformed = MutableStrokeInputBatch()
    val scratch = StrokeInput()
    for (i in 0 until source.size) {
        val input = source.populate(i, scratch)
        val mapped = apply(Point(input.x, input.y))
        transformed.add(
            input.toolType,
            mapped.x,
            mapped.y,
            input.elapsedTimeMillis,
            input.strokeUnitLengthCm,
            input.pressure,
            input.tiltRadians,
            input.orientationRadians,
        )
    }
    return Stroke(stroke.brush, transformed)
}
