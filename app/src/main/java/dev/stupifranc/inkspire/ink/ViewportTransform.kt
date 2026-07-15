package dev.stupifranc.inkspire.ink

import android.graphics.Matrix
import dev.stupifranc.inkspire.core.Viewport

/**
 * `strokeToScreenTransform` for [androidx.ink.rendering.android.canvas.CanvasStrokeRenderer.draw]
 * (verified against the real 1.0.0 API: it maps world/document coordinates to screen coordinates).
 */
fun Viewport.toWorldToScreenMatrix(): Matrix =
    Matrix().apply {
        postScale(scale, scale)
        postRotate(Math.toDegrees(rotation.toDouble()).toFloat())
        postTranslate(panX, panY)
    }

/**
 * The transform argument for [androidx.ink.authoring.InProgressStrokesView.startStroke]
 * (verified: it maps input/screen coordinates to world/document coordinates — the inverse of the
 * screen transform above).
 */
fun Viewport.toScreenToWorldMatrix(): Matrix {
    val worldToScreen = toWorldToScreenMatrix()
    val screenToWorld = Matrix()
    worldToScreen.invert(screenToWorld)
    return screenToWorld
}
