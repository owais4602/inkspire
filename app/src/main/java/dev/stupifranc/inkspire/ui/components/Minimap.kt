package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import dev.stupifranc.inkspire.core.Point
import dev.stupifranc.inkspire.core.Viewport
import dev.stupifranc.inkspire.ink.InkTextures
import dev.stupifranc.inkspire.ink.toWorldToScreenMatrix
import dev.stupifranc.inkspire.model.CanvasSpec
import dev.stupifranc.inkspire.model.StrokeEntry
import dev.stupifranc.inkspire.model.CanvasShape
import kotlin.math.roundToInt

@Composable
fun Minimap(
    canvasSpec: CanvasSpec,
    viewport: Viewport,
    containerWidth: Float,
    containerHeight: Float,
    strokes: List<StrokeEntry>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    if (canvasSpec.width <= 0f || canvasSpec.height <= 0f) return

    val maxDimension = 120.dp
    val aspectRatio = canvasSpec.width / canvasSpec.height
    val (widthDp, heightDp) = if (aspectRatio > 1f) {
        maxDimension to (maxDimension / aspectRatio)
    } else {
        (maxDimension * aspectRatio) to maxDimension
    }

    val context = LocalContext.current
    val store = remember { InkTextures.getStore(context) }
    val renderer = remember(store) { CanvasStrokeRenderer.create(store) }
    val shape = canvasOutlineShape(canvasSpec.shape)

    Surface(
        modifier = modifier.size(widthDp, heightDp),
        shape = shape,
        color = Color(canvasSpec.backgroundColorArgb),
        shadowElevation = 24.dp,
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / canvasSpec.width
                    val scaleY = size.height / canvasSpec.height
                    val mapScale = minOf(scaleX, scaleY)

                    // Draw strokes
                    val mapViewport = Viewport(scale = mapScale, panX = 0f, panY = 0f)
                    val worldToScreen = mapViewport.toWorldToScreenMatrix()
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        nativeCanvas.save()
                        nativeCanvas.concat(worldToScreen)
                        strokes.forEach { entry ->
                            renderer.draw(nativeCanvas, entry.stroke, worldToScreen)
                        }
                        nativeCanvas.restore()
                    }

                    // Draw Viewport Rect
                    val vpLeft = viewport.screenToDocument(Point(0f, 0f))
                    val vpRight = viewport.screenToDocument(Point(containerWidth, containerHeight))
                    
                    val mapVpLeft = vpLeft.x * mapScale
                    val mapVpTop = vpLeft.y * mapScale
                    val mapVpWidth = (vpRight.x - vpLeft.x) * mapScale
                    val mapVpHeight = (vpRight.y - vpLeft.y) * mapScale

                    val isFullyVisible = vpLeft.x <= 1f && vpLeft.y <= 1f && 
                                         vpRight.x >= canvasSpec.width - 1f && vpRight.y >= canvasSpec.height - 1f

                    if (!isFullyVisible) {
                        drawRect(
                            color = Color.Gray.copy(alpha = 0.8f),
                            topLeft = Offset(mapVpLeft, mapVpTop),
                            size = Size(mapVpWidth, mapVpHeight),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
        }
    }
}
