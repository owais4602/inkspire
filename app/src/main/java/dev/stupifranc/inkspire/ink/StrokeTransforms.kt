package dev.stupifranc.inkspire.ink

import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import dev.stupifranc.inkspire.core.Affine2
import dev.stupifranc.inkspire.core.Point

/** Rebuilds [this] stroke with every input point remapped through [transform] (same brush, new coordinates). */
fun Stroke.transformedBy(transform: Affine2): Stroke {
    val source = inputs
    val transformed = MutableStrokeInputBatch()
    val scratch = StrokeInput()
    for (i in 0 until source.size) {
        val input = source.populate(i, scratch)
        val mapped = transform.apply(Point(input.x, input.y))
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
    return Stroke(brush, transformed)
}

fun Stroke.translatedBy(dx: Float, dy: Float): Stroke = transformedBy(Affine2.translation(dx, dy))
