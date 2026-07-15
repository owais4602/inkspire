package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.stupifranc.inkspire.core.Point
import dev.stupifranc.inkspire.core.ResizeAnchor
import dev.stupifranc.inkspire.core.Viewport
import kotlin.math.roundToInt

private const val MIN_CANVAS_SIZE = 100f
private val HANDLE_SIZE = 24.dp
private val OVERLAY_COLOR = Color(0xFF6650a4)

/** Which edge/corner is being dragged, how it affects pending width/height, and which corner stays pinned. */
private enum class Handle(val fxSign: Int, val fySign: Int, val anchor: ResizeAnchor) {
    N(0, -1, ResizeAnchor.BOTTOM_CENTER),
    S(0, 1, ResizeAnchor.TOP_CENTER),
    E(1, 0, ResizeAnchor.CENTER_START),
    W(-1, 0, ResizeAnchor.CENTER_END),
    NE(1, -1, ResizeAnchor.BOTTOM_START),
    NW(-1, -1, ResizeAnchor.BOTTOM_END),
    SE(1, 1, ResizeAnchor.TOP_START),
    SW(-1, 1, ResizeAnchor.TOP_END),
}

/**
 * Direct-manipulation canvas resize: drag any of the 8 edge/corner handles to grow or shrink the
 * canvas from that side, with a translucent live preview. The opposite edge/corner stays pinned,
 * matching how image editors resize a canvas (not the document being scaled, just its bounds).
 */
@Composable
fun CanvasResizeOverlay(
    canvasWidth: Float,
    canvasHeight: Float,
    viewport: Viewport,
    onConfirm: (newWidth: Float, newHeight: Float, anchor: ResizeAnchor) -> Unit,
    onCancel: () -> Unit,
    onFitToContent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingWidth by remember { mutableStateOf(canvasWidth) }
    var pendingHeight by remember { mutableStateOf(canvasHeight) }
    var anchor by remember { mutableStateOf(ResizeAnchor.CENTER) }

    // Preview rect in the CURRENT (pre-resize) document frame: grows/shrinks away from the
    // fixed edge implied by `anchor`, so it visually pins against the un-moved content.
    val previewLeft = if (anchor.fx == 1f) canvasWidth - pendingWidth else 0f
    val previewTop = if (anchor.fy == 1f) canvasHeight - pendingHeight else 0f

    val screenTopLeft = viewport.documentToScreen(Point(previewLeft, previewTop))
    val screenBottomRight = viewport.documentToScreen(Point(previewLeft + pendingWidth, previewTop + pendingHeight))

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = OVERLAY_COLOR.copy(alpha = 0.12f),
                topLeft = Offset(screenTopLeft.x, screenTopLeft.y),
                size = Size(screenBottomRight.x - screenTopLeft.x, screenBottomRight.y - screenTopLeft.y),
            )
            drawRect(
                color = OVERLAY_COLOR,
                topLeft = Offset(screenTopLeft.x, screenTopLeft.y),
                size = Size(screenBottomRight.x - screenTopLeft.x, screenBottomRight.y - screenTopLeft.y),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Handle.entries.forEach { handle ->
            val handleScreenPos = handlePosition(handle, screenTopLeft, screenBottomRight)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (handleScreenPos.x - HANDLE_SIZE.toPx() / 2f).roundToInt(),
                            (handleScreenPos.y - HANDLE_SIZE.toPx() / 2f).roundToInt(),
                        )
                    }
                    .size(HANDLE_SIZE)
                    .clip(CircleShape)
                    .background(OVERLAY_COLOR)
                    .pointerInput(handle) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val scale = viewport.scale
                            anchor = handle.anchor
                            pendingWidth = (pendingWidth + dragAmount.x / scale * handle.fxSign)
                                .coerceAtLeast(MIN_CANVAS_SIZE)
                            pendingHeight = (pendingHeight + dragAmount.y / scale * handle.fySign)
                                .coerceAtLeast(MIN_CANVAS_SIZE)
                        }
                    },
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                TextButton(onClick = onFitToContent) { Text("Fit to content") }
                TextButton(onClick = { onConfirm(pendingWidth, pendingHeight, anchor) }) { Text("Done") }
            }
        }
    }
}

private fun handlePosition(handle: Handle, topLeft: Point, bottomRight: Point): Point {
    val midX = (topLeft.x + bottomRight.x) / 2f
    val midY = (topLeft.y + bottomRight.y) / 2f
    return when (handle) {
        Handle.N -> Point(midX, topLeft.y)
        Handle.S -> Point(midX, bottomRight.y)
        Handle.E -> Point(bottomRight.x, midY)
        Handle.W -> Point(topLeft.x, midY)
        Handle.NE -> Point(bottomRight.x, topLeft.y)
        Handle.NW -> Point(topLeft.x, topLeft.y)
        Handle.SE -> Point(bottomRight.x, bottomRight.y)
        Handle.SW -> Point(topLeft.x, bottomRight.y)
    }
}
