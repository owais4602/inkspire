package dev.stupifranc.inkspire.ink

import android.graphics.Matrix
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import dev.stupifranc.inkspire.model.StrokeEntry
import dev.stupifranc.inkspire.model.Tool

private class SingleStrokeTouchState {
    var activeStrokeId: InProgressStrokeId? = null
    var activePointerId: Int = -1
}

@Composable
fun DrawingSurface(
    strokes: List<StrokeEntry>,
    tool: Tool,
    currentBrush: Brush,
    eraserPaddingPx: Float,
    onStrokesFinished: (List<Stroke>) -> Unit,
    onErase: (Set<String>) -> Unit,
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

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                strokes.forEach { entry ->
                    renderer.draw(canvas.nativeCanvas, entry.stroke, identity)
                }
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                InProgressStrokesView(context).apply {
                    addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
                        override fun onStrokesFinished(finishedStrokes: Map<InProgressStrokeId, Stroke>) {
                            onFinishedState.value(finishedStrokes.values.toList())
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
