package dev.stupifranc.inkspire.core

/** A point in an arbitrary 2D coordinate space (document, screen, etc). Pure math, no Android deps. */
data class Point(val x: Float, val y: Float)

/** Clamps [point] into the axis-aligned rect `[0,width] x [0,height]` (e.g. keeping the symmetry center on the page). */
fun clampToRect(point: Point, width: Float, height: Float): Point =
    Point(point.x.coerceIn(0f, maxOf(width, 0f)), point.y.coerceIn(0f, maxOf(height, 0f)))
