package dev.stupifranc.inkspire.ink

import dev.stupifranc.inkspire.core.BoundingBox
import dev.stupifranc.inkspire.model.StrokeEntry

/**
 * The stroke's bounding box in document space, or null for a degenerate (empty-mesh) stroke.
 * Verified against the real 1.0.0 `ink-geometry` jar (`javap` on `ink-geometry-android`):
 * `PartitionedMesh.computeBoundingBox()` returns a nullable `Box` with `xMin`/`yMin`/`xMax`/`yMax`
 * — there is no `computeBoundingBox` on `Stroke` itself, it lives on `Stroke.shape` (a `PartitionedMesh`).
 */
fun StrokeEntry.boundingBox(): BoundingBox? {
    val box = stroke.shape.computeBoundingBox() ?: return null
    return BoundingBox(box.xMin, box.yMin, box.xMax, box.yMax)
}
