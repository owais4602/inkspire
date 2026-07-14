package dev.stupifranc.inkspire.ink

import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import dev.stupifranc.inkspire.model.StrokeEntry

/** Whole-stroke eraser: sweeps a padded segment between consecutive touch points and hit-tests it against each stroke's mesh. */
class EraserHitTester {
    private var previousPoint: MutableVec? = null

    fun startSweep(x: Float, y: Float) {
        previousPoint = MutableVec(x, y)
    }

    fun sweepTo(x: Float, y: Float, entries: List<StrokeEntry>, paddingPx: Float): Set<String> {
        val prev = previousPoint ?: MutableVec(x, y)
        val current = MutableVec(x, y)
        previousPoint = current

        val segment = MutableSegment(prev, current)
        val parallelogram = MutableParallelogram().populateFromSegmentAndPadding(segment, paddingPx)

        return entries
            .filter { it.stroke.shape.computeCoverageIsGreaterThan(parallelogram, 0f, AffineTransform.IDENTITY) }
            .map { it.id }
            .toSet()
    }

    fun endSweep() {
        previousPoint = null
    }
}
