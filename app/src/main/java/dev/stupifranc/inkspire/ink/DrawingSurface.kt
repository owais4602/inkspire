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
import androidx.compose.ui.geometry.Size
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
import dev.stupifranc.inkspire.core.Viewport
import dev.stupifranc.inkspire.model.CanvasSpec
import dev.stupifranc.inkspire.model.StrokeEntry
import dev.stupifranc.inkspire.model.contains
import dev.stupifranc.inkspire.model.Tool
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private class SingleStrokeTouchState {
    var activeStrokeId: InProgressStrokeId? = null
    var activePointerId: Int = -1
}

/** Tracks a two-finger pinch/pan gesture by the two pointer IDs first seen when it started. */
private class PanZoomState {
    var active = false
    var pointerIdA = -1
    var pointerIdB = -1
    var lastAX = 0f
    var lastAY = 0f
    var lastBX = 0f
    var lastBY = 0f
}

/** Detects a single-finger double-tap (two quick, near-stationary taps) to trigger zoom-to-fit. */
private class TapTracker {
    var downTimeMs = 0L
    var downX = 0f
    var downY = 0f
    var lastTapUpTimeMs = 0L
    var lastTapUpX = 0f
    var lastTapUpY = 0f
}

private const val MIN_PINCH_DISTANCE_PX = 20f
private const val TAP_MAX_DURATION_MS = 250L
private const val TAP_MAX_MOVEMENT_PX = 24f
private const val DOUBLE_TAP_WINDOW_MS = 300L
private const val DOUBLE_TAP_MAX_DISTANCE_PX = 40f
private val CENTER_HANDLE_SIZE = 28.dp

@Composable
fun DrawingSurface(
    strokes: List<StrokeEntry>,
    tool: Tool,
    currentBrush: Brush,
    eraserPaddingPx: Float,
    symmetryConfig: SymmetryConfig,
    canvasSpec: CanvasSpec,
    viewport: Viewport,
    onStrokesFinished: (List<Stroke>) -> Unit,
    onErase: (Set<String>) -> Unit,
    onContainerSizeChanged: (Float, Float) -> Unit,
    onSymmetryCenterChanged: (Point) -> Unit,
    onPan: (dx: Float, dy: Float) -> Unit,
    onZoom: (factor: Float, focal: Point) -> Unit,
    onDoubleTapFit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderer = remember { CanvasStrokeRenderer.create() }
    val brushState = rememberUpdatedState(currentBrush)
    val toolState = rememberUpdatedState(tool)
    val strokesState = rememberUpdatedState(strokes)
    val eraserPaddingState = rememberUpdatedState(eraserPaddingPx)
    val onFinishedState = rememberUpdatedState(onStrokesFinished)
    val onEraseState = rememberUpdatedState(onErase)
    val viewportState = rememberUpdatedState(viewport)
    val canvasSpecState = rememberUpdatedState(canvasSpec)
    val onPanState = rememberUpdatedState(onPan)
    val onZoomState = rememberUpdatedState(onZoom)
    val onDoubleTapFitState = rememberUpdatedState(onDoubleTapFit)
    val touchState = remember { SingleStrokeTouchState() }
    val panZoomState = remember { PanZoomState() }
    val tapTracker = remember { TapTracker() }
    val eraserTester = remember { EraserHitTester() }

    val symmetryTransforms = remember(symmetryConfig) { SymmetryEngine.transforms(symmetryConfig) }
    val transformsState = rememberUpdatedState(symmetryTransforms)
    val isSymmetryActive = symmetryTransforms.size > 1

    val symmetryCenterState = rememberUpdatedState(symmetryConfig.center)
    val onSymmetryCenterChangedState = rememberUpdatedState(onSymmetryCenterChanged)

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            onContainerSizeChanged(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
        },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCanvasBackground(canvasSpec, viewport)
            val worldToScreen = viewport.toWorldToScreenMatrix()
            drawIntoCanvas { canvas ->
                strokes.forEach { entry ->
                    renderer.draw(canvas.nativeCanvas, entry.stroke, worldToScreen)
                }
            }
            if (isSymmetryActive) {
                drawSymmetryGuides(symmetryConfig, viewport)
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
                        handleTouch(
                            view = this,
                            event = event,
                            tool = toolState.value,
                            brush = brushState.value,
                            screenToWorld = viewportState.value.toScreenToWorldMatrix(),
                            strokeTouchState = touchState,
                            panZoomState = panZoomState,
                            eraserTester = eraserTester,
                            entries = strokesState.value,
                            eraserPaddingPx = eraserPaddingState.value,
                            viewport = viewportState.value,
                            canvasSpec = canvasSpecState.value,
                            tapTracker = tapTracker,
                            onErase = onEraseState.value,
                            onPan = onPanState.value,
                            onZoom = onZoomState.value,
                            onDoubleTapFit = onDoubleTapFitState.value,
                        )
                    }
                }
            },
        )

        if (isSymmetryActive) {
            val centerScreen = viewport.documentToScreen(symmetryConfig.center)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (centerScreen.x - CENTER_HANDLE_SIZE.toPx() / 2f).roundToInt(),
                            (centerScreen.y - CENTER_HANDLE_SIZE.toPx() / 2f).roundToInt(),
                        )
                    }
                    .size(CENTER_HANDLE_SIZE)
                    .clip(CircleShape)
                    .background(SYMMETRY_GUIDE_COLOR.copy(alpha = 0.35f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val scale = viewportState.value.scale
                            val current = symmetryCenterState.value
                            onSymmetryCenterChangedState.value(
                                Point(current.x + dragAmount.x / scale, current.y + dragAmount.y / scale),
                            )
                        }
                    },
            )
        }
    }
}

private fun DrawScope.drawCanvasBackground(canvasSpec: CanvasSpec, viewport: Viewport) {
    val topLeft = viewport.documentToScreen(Point(0f, 0f))
    val bottomRight = viewport.documentToScreen(Point(canvasSpec.width, canvasSpec.height))
    drawRect(
        color = Color(canvasSpec.backgroundColorArgb),
        topLeft = Offset(topLeft.x, topLeft.y),
        size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y),
    )
}

private val SYMMETRY_GUIDE_COLOR = Color(0xFF6650a4)

private fun DrawScope.drawSymmetryGuides(config: SymmetryConfig, viewport: Viewport) {
    val center = viewport.documentToScreen(config.center)
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
            drawLine(color = guideColor, start = Offset(center.x, center.y), end = end, strokeWidth = 1.5f)
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

/** Dispatches raw touch to pinch/pan, eraser, or pen handling, arbitrating between them by pointer count. */
private fun handleTouch(
    view: InProgressStrokesView,
    event: MotionEvent,
    tool: Tool,
    brush: Brush,
    screenToWorld: Matrix,
    strokeTouchState: SingleStrokeTouchState,
    panZoomState: PanZoomState,
    eraserTester: EraserHitTester,
    entries: List<StrokeEntry>,
    eraserPaddingPx: Float,
    viewport: Viewport,
    canvasSpec: CanvasSpec,
    tapTracker: TapTracker,
    onErase: (Set<String>) -> Unit,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float, Point) -> Unit,
    onDoubleTapFit: () -> Unit,
): Boolean {
    if (event.pointerCount >= 2 || panZoomState.active) {
        strokeTouchState.activeStrokeId?.let { view.cancelStroke(it, event) }
        strokeTouchState.activeStrokeId = null
        strokeTouchState.activePointerId = -1
        eraserTester.endSweep()
        tapTracker.lastTapUpTimeMs = 0L
        return handlePanZoomTouch(event, panZoomState, onPan, onZoom)
    }
    return if (tool == Tool.ERASER) {
        handleEraserTouch(event, entries, eraserTester, eraserPaddingPx, viewport, onErase)
    } else {
        handlePenTouch(
            view,
            event,
            brush,
            touchState = strokeTouchState,
            screenToWorld = screenToWorld,
            viewport = viewport,
            canvasSpec = canvasSpec,
            tapTracker = tapTracker,
            onDoubleTapFit = onDoubleTapFit,
        )
    }
}

private fun handlePanZoomTouch(
    event: MotionEvent,
    state: PanZoomState,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float, Point) -> Unit,
): Boolean {
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
            if (event.pointerCount >= 2) {
                state.active = true
                state.pointerIdA = event.getPointerId(0)
                state.pointerIdB = event.getPointerId(1)
                state.lastAX = event.getX(0)
                state.lastAY = event.getY(0)
                state.lastBX = event.getX(1)
                state.lastBY = event.getY(1)
            }
            return true
        }
        MotionEvent.ACTION_MOVE -> {
            if (!state.active) return true
            val indexA = event.findPointerIndex(state.pointerIdA)
            val indexB = event.findPointerIndex(state.pointerIdB)
            if (indexA == -1 || indexB == -1) return true

            val ax = event.getX(indexA)
            val ay = event.getY(indexA)
            val bx = event.getX(indexB)
            val by = event.getY(indexB)

            val prevCenterX = (state.lastAX + state.lastBX) / 2f
            val prevCenterY = (state.lastAY + state.lastBY) / 2f
            val currCenterX = (ax + bx) / 2f
            val currCenterY = (ay + by) / 2f

            val prevDistance = hypot((state.lastBX - state.lastAX).toDouble(), (state.lastBY - state.lastAY).toDouble()).toFloat()
            val currDistance = hypot((bx - ax).toDouble(), (by - ay).toDouble()).toFloat()

            onPan(currCenterX - prevCenterX, currCenterY - prevCenterY)
            if (prevDistance > MIN_PINCH_DISTANCE_PX && currDistance > MIN_PINCH_DISTANCE_PX) {
                onZoom(currDistance / prevDistance, Point(currCenterX, currCenterY))
            }

            state.lastAX = ax
            state.lastAY = ay
            state.lastBX = bx
            state.lastBY = by
            return true
        }
        MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            if (event.pointerCount <= 2) {
                state.active = false
                state.pointerIdA = -1
                state.pointerIdB = -1
            }
            return true
        }
        else -> return true
    }
}

private fun handlePenTouch(
    view: InProgressStrokesView,
    event: MotionEvent,
    brush: Brush,
    touchState: SingleStrokeTouchState,
    screenToWorld: Matrix,
    viewport: Viewport,
    canvasSpec: CanvasSpec,
    tapTracker: TapTracker,
    onDoubleTapFit: () -> Unit,
): Boolean {
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            if (touchState.activeStrokeId != null) return false
            tapTracker.downTimeMs = event.eventTime
            tapTracker.downX = event.x
            tapTracker.downY = event.y
            val pointerId = event.getPointerId(event.actionIndex)
            touchState.activePointerId = pointerId
            // Only start real ink outside the page boundary is a no-op, but the touch is still
            // consumed so the eventual ACTION_UP reaches us for double-tap-to-fit detection.
            if (canvasSpec.contains(viewport.screenToDocument(Point(event.x, event.y)))) {
                touchState.activeStrokeId = view.startStroke(event, pointerId, brush, screenToWorld)
            }
            return true
        }
        MotionEvent.ACTION_MOVE -> {
            val strokeId = touchState.activeStrokeId ?: return false
            if (event.findPointerIndex(touchState.activePointerId) == -1) return false
            view.addToStroke(event, touchState.activePointerId, strokeId)
            return true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
            val liftedPointerId = event.getPointerId(event.actionIndex)
            if (liftedPointerId != touchState.activePointerId) return false
            touchState.activeStrokeId?.let { strokeId -> view.finishStroke(event, touchState.activePointerId, strokeId) }
            touchState.activeStrokeId = null
            touchState.activePointerId = -1
            registerTapAndMaybeFit(event, tapTracker, onDoubleTapFit)
            return true
        }
        MotionEvent.ACTION_CANCEL -> {
            touchState.activeStrokeId?.let { view.cancelStroke(it, event) }
            touchState.activeStrokeId = null
            touchState.activePointerId = -1
            return true
        }
        else -> return false
    }
}

/** Recognizes a tap-like up (little movement, short duration) and pairs it with the previous one to detect a double-tap. */
private fun registerTapAndMaybeFit(event: MotionEvent, tracker: TapTracker, onDoubleTapFit: () -> Unit) {
    val duration = event.eventTime - tracker.downTimeMs
    val movement = hypot((event.x - tracker.downX).toDouble(), (event.y - tracker.downY).toDouble()).toFloat()
    if (duration > TAP_MAX_DURATION_MS || movement > TAP_MAX_MOVEMENT_PX) {
        tracker.lastTapUpTimeMs = 0L
        return
    }

    val sinceLastTap = event.eventTime - tracker.lastTapUpTimeMs
    val distanceFromLastTap = hypot((event.x - tracker.lastTapUpX).toDouble(), (event.y - tracker.lastTapUpY).toDouble()).toFloat()
    if (sinceLastTap in 1 until DOUBLE_TAP_WINDOW_MS && distanceFromLastTap <= DOUBLE_TAP_MAX_DISTANCE_PX) {
        onDoubleTapFit()
        tracker.lastTapUpTimeMs = 0L
    } else {
        tracker.lastTapUpTimeMs = event.eventTime
        tracker.lastTapUpX = event.x
        tracker.lastTapUpY = event.y
    }
}

/**
 * Unlike drawing, erasing is never bounds-gated: it's a cleanup action, not content creation, so it
 * must still reach stray strokes stranded outside the page (e.g. after a canvas shrink).
 */
private fun handleEraserTouch(
    event: MotionEvent,
    entries: List<StrokeEntry>,
    tester: EraserHitTester,
    paddingPx: Float,
    viewport: Viewport,
    onErase: (Set<String>) -> Unit,
): Boolean {
    val documentPoint = viewport.screenToDocument(Point(event.x, event.y))
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            tester.startSweep(documentPoint.x, documentPoint.y)
            val hits = tester.sweepTo(documentPoint.x, documentPoint.y, entries, paddingPx)
            if (hits.isNotEmpty()) onErase(hits)
            return true
        }
        MotionEvent.ACTION_MOVE -> {
            val hits = tester.sweepTo(documentPoint.x, documentPoint.y, entries, paddingPx)
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
