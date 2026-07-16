package dev.stupifranc.inkspire.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import dev.stupifranc.inkspire.model.BackgroundKind
import dev.stupifranc.inkspire.model.CanvasBackground
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Maps a persisted [CanvasBackground] to a Compose [Brush] filling [bounds].
 * Returns null when there is nothing to draw (no colors) so callers can fall
 * back to the legacy flat `backgroundColorArgb` fill.
 */
fun CanvasBackground.toComposeBrush(bounds: Rect): Brush? {
    if (colors.isEmpty()) return null
    if (colors.size == 1) return SolidColor(Color(colors.first()))

    val composeColors = colors.map { Color(it) }
    return when (kind) {
        BackgroundKind.FLAT -> SolidColor(composeColors.first())
        BackgroundKind.LINEAR -> {
            val cx = bounds.center.x
            val cy = bounds.center.y
            val angleRad = Math.toRadians(angleDegrees.toDouble())
            val halfDiagonal = hypot((bounds.width / 2).toDouble(), (bounds.height / 2).toDouble()).toFloat()
            val dx = cos(angleRad).toFloat() * halfDiagonal
            val dy = sin(angleRad).toFloat() * halfDiagonal
            Brush.linearGradient(
                colors = composeColors,
                start = Offset(cx - dx, cy - dy),
                end = Offset(cx + dx, cy + dy)
            )
        }
        BackgroundKind.RADIAL -> {
            val radius = maxOf(bounds.width, bounds.height) / 2f
            Brush.radialGradient(
                colors = composeColors,
                center = bounds.center,
                radius = radius.coerceAtLeast(1f)
            )
        }
    }
}
