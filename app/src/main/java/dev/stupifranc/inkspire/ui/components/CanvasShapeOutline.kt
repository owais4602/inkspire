package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.stupifranc.inkspire.model.CanvasShape

/** A Compose [Shape] matching a drawing's canvas shape, for clipping thumbnails/previews (minimap, gallery cards). */
fun canvasOutlineShape(canvasShape: CanvasShape): Shape = when (canvasShape) {
    CanvasShape.RECTANGLE -> RoundedCornerShape(0.dp)
    CanvasShape.ROUNDED_RECTANGLE -> RoundedCornerShape(16.dp)
    CanvasShape.CIRCLE -> object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val d = minOf(size.width, size.height)
            val left = (size.width - d) / 2f
            val top = (size.height - d) / 2f
            return Outline.Rounded(RoundRect(Rect(left, top, left + d, top + d), d / 2f, d / 2f))
        }
    }
}
