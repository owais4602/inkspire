package dev.stupifranc.inkspire.ink

import android.graphics.Matrix
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import dev.stupifranc.inkspire.core.Point
import dev.stupifranc.inkspire.core.SymmetryConfig
import dev.stupifranc.inkspire.core.SymmetryEngine
import dev.stupifranc.inkspire.model.StrokeEntry
import dev.stupifranc.inkspire.model.Tool
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.roundToInt

private class SingleStrokeTouchState {
    var activeStrokeId: InProgressStrokeId? = null
    var activePointerId: Int = -1
}

private val CENTER_HANDLE_SIZE = 28.dp

@Composable
fun DrawingSurface(
    strokes: List<StrokeEntry>,
    tool: Tool,
    currentBrush: Brush,
    eraserPaddingPx: Float,
    symmetryConfig: SymmetryConfig,
    onStrokesFinished: (List<Stroke>) -> Unit,
    onErase: (Set<String>) -> Unit,
    onCanvasSizeChanged: (Float, Float) -> Unit,
    onSymmetryCenterChanged: (Point) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderer = remember { CanvasStrokeRenderer.create() }
    val identity = remember { Matrix() }
    val brushState = rememberUpdatedState(currentBrush)
    val toolState = rememberUpdatedState(tool)
    val strokesState = rememberUpdatedState(strokes)
    val eraserPaddingState = rememberUpdatedState(eraserPaddingPx)
    val onFinishedState = rememberUpdatedState(onStrokesFinished)
    val onEraseState = rememberUpdatedState(onErase)
    val touchState = remember { SingleStrokeTouchState() }
    val eraserTester = remember { EraserHitTester() }

    val symmetryTransforms = remember(symmetryConfig) { SymmetryEngine.transforms(symmetryConfig) }
    val transformsState = rememberUpdatedState(symmetryTransforms)
    val isSymmetryActive = symmetryTransforms.size > 1

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            onCanvasSizeChanged(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
        },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                strokes.forEach { entry ->
                    renderer.draw(canvas.nativeCanvas, entry.stroke, identity)
                }
            }
            if (isSymmetryActive) {
                drawSymmetryGuides(symmetryConfig)
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                InProgressStrokesView(context).apply {
                    addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
                        override fun onStrokesFinished(finishedStrokes: Map<InProgressStrokeId, Stroke>) {
                            val copies = finishedStrokes.values.flatMap { stroke ->
                                stroke.replicateThrough(transformsState.value)
                            }
                            onFinishedState.value(copies)
                            removeFinishedStrokes(finishedStrokes.keys)
                        }
                    })
                    setOnTouchListener { _, event ->
                        if (toolState.value == Tool.ERASER) {
                            handleEraserTouch(
                                event = event,
                                entries = strokesState.value,
                                tester = eraserTester,
                                paddingPx = eraserPaddingState.value,
                                onErase = onEraseState.value,
                            )
                        } else {
                            handlePenTouch(
                                view = this,
                                event = event,
                                brush = brushState.value,
                                state = touchState,
                            )
                        }
                    }
                }
            },
        )

        if (isSymmetryActive) {
            val center = symmetryConfig.center
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (center.x - CENTER_HANDLE_SIZE.toPx() / 2f).roundToInt(),
                            (center.y - CENTER_HANDLE_SIZE.toPx() / 2f).roundToInt(),
                        )
                    }
                    .size(CENTER_HANDLE_SIZE)
                    .clip(CircleShape)
                    .background(SYMMETRY_GUIDE_COLOR.copy(alpha = 0.35f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onSymmetryCenterChanged(Point(center.x + dragAmount.x, center.y + dragAmount.y))
                        }
                    },
            )
        }
    }
}

private val SYMMETRY_GUIDE_COLOR = Color(0xFF6650a4)

private fun DrawScope.drawSymmetryGuides(config: SymmetryConfig) {
    val center = Offset(config.center.x, config.center.y)
    val radius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
    val guideColor = SYMMETRY_GUIDE_COLOR.copy(alpha = 0.18f)

    if (config.sectors > 1) {
        val angleStep = 360f / config.sectors
        for (i in 0 until config.sectors) {
            val radians = Math.toRadians((angleStep * i).toDouble())
            val end = Offset(
                center.x + radius * cos(radians).toFloat(),
                center.y + radius * sin(radians).toFloat(),
            )
            drawLine(color = guideColor, start = center, end = end, strokeWidth = 1.5f)
        }
    }
    if (config.mirror) {
        drawLine(
            color = guideColor,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.5f,
        )
    }
}

private fun handlePenTouch(
    view: InProgressStrokesView,
    event: MotionEvent,
    brush: Brush,
    state: SingleStrokeTouchState,
): Boolean {
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            if (state.activeStrokeId != null) return false
            val pointerId = event.getPointerId(event.actionIndex)
            state.activePointerId = pointerId
            state.activeStrokeId = view.startStroke(event, pointerId, brush)
            return true
        }
        MotionEvent.ACTION_MOVE -> {
            val strokeId = state.activeStrokeId ?: return false
            if (event.findPointerIndex(state.activePointerId) == -1) return false
            view.addToStroke(event, state.activePointerId, strokeId)
            return true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
            val strokeId = state.activeStrokeId ?: return false
            val liftedPointerId = event.getPointerId(event.actionIndex)
            if (liftedPointerId != state.activePointerId) return false
            view.finishStroke(event, state.activePointerId, strokeId)
            state.activeStrokeId = null
            state.activePointerId = -1
            return true
        }
        MotionEvent.ACTION_CANCEL -> {
            state.activeStrokeId?.let { view.cancelStroke(it, event) }
            state.activeStrokeId = null
            state.activePointerId = -1
            return true
        }
        else -> return false
    }
}

private fun handleEraserTouch(
    event: MotionEvent,
    entries: List<StrokeEntry>,
    tester: EraserHitTester,
    paddingPx: Float,
    onErase: (Set<String>) -> Unit,
): Boolean {
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            tester.startSweep(event.x, event.y)
            val hits = tester.sweepTo(event.x, event.y, entries, paddingPx)
            if (hits.isNotEmpty()) onErase(hits)
            return true
        }
        MotionEvent.ACTION_MOVE -> {
            val hits = tester.sweepTo(event.x, event.y, entries, paddingPx)
            if (hits.isNotEmpty()) onErase(hits)
            return true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            tester.endSweep()
            return true
        }
        else -> return false
    }
}
