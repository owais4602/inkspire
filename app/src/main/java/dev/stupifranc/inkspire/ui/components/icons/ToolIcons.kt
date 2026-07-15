package dev.stupifranc.inkspire.ui.components.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// Hand-drawn glyphs (not material-icons-extended: too large, and lacks app-specific concepts anyway).
private val ICON_SIZE = 24.dp
private val STROKE_WIDTH = 1.8.dp

@Composable
private fun IconCanvas(modifier: Modifier = Modifier, onDraw: DrawScope.() -> Unit) {
    Canvas(modifier = modifier.size(ICON_SIZE), onDraw = onDraw)
}

@Composable
fun PenIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.22f, h * 0.85f), Offset(w * 0.78f, h * 0.18f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.18f, h * 0.9f), Offset(w * 0.3f, h * 0.78f), stroke.width, stroke.cap)
    }
}

@Composable
fun MarkerIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.25f, h * 0.85f), Offset(w * 0.75f, h * 0.2f), STROKE_WIDTH.toPx() * 2.6f, StrokeCap.Round)
    }
}

@Composable
fun HighlighterIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.78f, h * 0.3f), STROKE_WIDTH.toPx() * 4f, StrokeCap.Butt)
    }
}

@Composable
fun EraserIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        rotate(-32f) {
            val rectSize = Size(size.width * 0.62f, size.height * 0.4f)
            val topLeft = Offset((size.width - rectSize.width) / 2f, (size.height - rectSize.height) / 2f)
            drawRoundRect(tint, topLeft, rectSize, cornerRadius = CornerRadius(rectSize.height * 0.25f), style = stroke)
            drawLine(
                tint,
                Offset(topLeft.x, topLeft.y + rectSize.height * 0.55f),
                Offset(topLeft.x + rectSize.width, topLeft.y + rectSize.height * 0.55f),
                stroke.width,
            )
        }
    }
}

@Composable
fun SymmetryIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.42f
        val strokeWidth = STROKE_WIDTH.toPx()
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            val end = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat(),
            )
            drawLine(tint, center, end, strokeWidth, StrokeCap.Round)
        }
        drawCircle(tint, radius = strokeWidth * 1.3f, center = center)
    }
}

@Composable
fun UndoIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val rect = Rect(size.width * 0.2f, size.height * 0.25f, size.width * 0.85f, size.height * 0.85f)
        drawArc(tint, startAngle = 160f, sweepAngle = 190f, useCenter = false, topLeft = rect.topLeft, size = rect.size, style = stroke)
        val tip = Offset(size.width * 0.2f, size.height * 0.42f)
        drawLine(tint, tip, Offset(tip.x + size.width * 0.16f, tip.y - size.height * 0.14f), stroke.width, stroke.cap)
        drawLine(tint, tip, Offset(tip.x + size.width * 0.05f, tip.y + size.height * 0.2f), stroke.width, stroke.cap)
    }
}

@Composable
fun RedoIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val rect = Rect(size.width * 0.15f, size.height * 0.25f, size.width * 0.8f, size.height * 0.85f)
        drawArc(tint, startAngle = -20f, sweepAngle = -190f, useCenter = false, topLeft = rect.topLeft, size = rect.size, style = stroke)
        val tip = Offset(size.width * 0.8f, size.height * 0.42f)
        drawLine(tint, tip, Offset(tip.x - size.width * 0.16f, tip.y - size.height * 0.14f), stroke.width, stroke.cap)
        drawLine(tint, tip, Offset(tip.x - size.width * 0.05f, tip.y + size.height * 0.2f), stroke.width, stroke.cap)
    }
}

@Composable
fun ClearIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.25f, h * 0.32f), Offset(w * 0.75f, h * 0.32f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.4f, h * 0.32f), Offset(w * 0.42f, h * 0.22f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.6f, h * 0.32f), Offset(w * 0.58f, h * 0.22f), stroke.width, stroke.cap)
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.3f, h * 0.32f),
            size = Size(w * 0.4f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.05f),
            style = stroke,
        )
    }
}

@Composable
fun ResizeIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.4f, h * 0.6f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.15f, h * 0.6f), Offset(w * 0.15f, h * 0.85f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.4f, h * 0.85f), stroke.width, stroke.cap)

        drawLine(tint, Offset(w * 0.85f, h * 0.15f), Offset(w * 0.6f, h * 0.4f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.85f, h * 0.4f), Offset(w * 0.85f, h * 0.15f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.85f, h * 0.15f), Offset(w * 0.6f, h * 0.15f), stroke.width, stroke.cap)
    }
}

@Composable
fun ExportIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w / 2f, h * 0.75f), Offset(w / 2f, h * 0.2f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w / 2f, h * 0.2f), Offset(w * 0.32f, h * 0.4f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w / 2f, h * 0.2f), Offset(w * 0.68f, h * 0.4f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.2f, h * 0.85f), Offset(w * 0.8f, h * 0.85f), stroke.width, stroke.cap)
    }
}

@Composable
fun PlusIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx() * 1.3f, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w / 2f, h * 0.2f), Offset(w / 2f, h * 0.8f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.2f, h / 2f), Offset(w * 0.8f, h / 2f), stroke.width, stroke.cap)
    }
}

@Composable
fun MoreIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val dotRadius = size.minDimension * 0.06f
        val y = size.height / 2f
        listOf(0.25f, 0.5f, 0.75f).forEach { fx ->
            drawCircle(tint, radius = dotRadius, center = Offset(size.width * fx, y))
        }
    }
}

/** Three horizontal sliders with knobs at staggered positions — the classic "tune"/settings glyph. */
@Composable
fun TuneIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val knobRadius = size.minDimension * 0.07f
        val rows = listOf(0.28f to 0.62f, 0.5f to 0.38f, 0.72f to 0.5f)
        rows.forEach { (fy, knobFx) ->
            val y = size.height * fy
            drawLine(tint, Offset(w * 0.18f, y), Offset(w * 0.82f, y), stroke.width, stroke.cap)
            drawCircle(tint, radius = knobRadius, center = Offset(w * knobFx, y))
        }
    }
}
