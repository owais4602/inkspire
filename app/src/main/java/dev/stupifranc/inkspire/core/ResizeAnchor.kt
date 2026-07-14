package dev.stupifranc.inkspire.core

/** Where existing content should stay pinned when the canvas is resized, as a fractional (fx, fy) position. */
enum class ResizeAnchor(val fx: Float, val fy: Float) {
    TOP_START(0f, 0f), TOP_CENTER(0.5f, 0f), TOP_END(1f, 0f),
    CENTER_START(0f, 0.5f), CENTER(0.5f, 0.5f), CENTER_END(1f, 0.5f),
    BOTTOM_START(0f, 1f), BOTTOM_CENTER(0.5f, 1f), BOTTOM_END(1f, 1f);

    companion object {
        /** The translation to apply to existing content so it stays pinned to [anchor] after resizing. */
        fun offset(oldWidth: Float, oldHeight: Float, newWidth: Float, newHeight: Float, anchor: ResizeAnchor): Point =
            Point(
                (newWidth - oldWidth) * anchor.fx,
                (newHeight - oldHeight) * anchor.fy,
            )
    }
}
