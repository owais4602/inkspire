package dev.stupifranc.inkspire.core

/** Which side of the canvas an explicit "extend" action grows. */
enum class CanvasEdge { TOP, BOTTOM, LEFT, RIGHT }

/**
 * The new canvas size and the [ResizeAnchor] that keeps the *opposite* edge pinned when growing
 * [this] edge by [amount] — existing strokes stay exactly where they are; only the far edge moves.
 */
fun CanvasEdge.grow(width: Float, height: Float, amount: Float): Triple<Float, Float, ResizeAnchor> = when (this) {
    CanvasEdge.TOP -> Triple(width, height + amount, ResizeAnchor.BOTTOM_CENTER)
    CanvasEdge.BOTTOM -> Triple(width, height + amount, ResizeAnchor.TOP_CENTER)
    CanvasEdge.LEFT -> Triple(width + amount, height, ResizeAnchor.CENTER_END)
    CanvasEdge.RIGHT -> Triple(width + amount, height, ResizeAnchor.CENTER_START)
}
