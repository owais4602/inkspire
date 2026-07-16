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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
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
fun HandIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        // palm
        drawRoundRect(
            tint,
            topLeft = Offset(w * 0.26f, h * 0.42f),
            size = Size(w * 0.5f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.14f),
            style = stroke,
        )
        // fingers
        val fingerTopYs = listOf(0.16f, 0.12f, 0.14f, 0.22f)
        val fingerXs = listOf(0.34f, 0.46f, 0.58f, 0.7f)
        fingerXs.forEachIndexed { i, fx ->
            drawLine(tint, Offset(w * fx, h * 0.44f), Offset(w * fx, h * fingerTopYs[i]), stroke.width, stroke.cap)
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

@Composable
fun ArrowUpIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w / 2f, h * 0.8f), Offset(w / 2f, h * 0.2f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w / 2f, h * 0.2f), Offset(w * 0.25f, h * 0.45f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w / 2f, h * 0.2f), Offset(w * 0.75f, h * 0.45f), stroke.width, stroke.cap)
    }
}

@Composable
fun ArrowDownIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w / 2f, h * 0.2f), Offset(w / 2f, h * 0.8f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w / 2f, h * 0.8f), Offset(w * 0.25f, h * 0.55f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w / 2f, h * 0.8f), Offset(w * 0.75f, h * 0.55f), stroke.width, stroke.cap)
    }
}

@Composable
fun ArrowLeftIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.8f, h / 2f), Offset(w * 0.2f, h / 2f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.2f, h / 2f), Offset(w * 0.45f, h * 0.25f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.2f, h / 2f), Offset(w * 0.45f, h * 0.75f), stroke.width, stroke.cap)
    }
}

@Composable
fun ArrowRightIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.2f, h / 2f), Offset(w * 0.8f, h / 2f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.8f, h / 2f), Offset(w * 0.55f, h * 0.25f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.8f, h / 2f), Offset(w * 0.55f, h * 0.75f), stroke.width, stroke.cap)
    }
}

@Composable
fun CropIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w * 0.25f, h * 0.1f), Offset(w * 0.25f, h * 0.75f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.25f, h * 0.75f), Offset(w * 0.9f, h * 0.75f), stroke.width, stroke.cap)
        
        drawLine(tint, Offset(w * 0.75f, h * 0.9f), Offset(w * 0.75f, h * 0.25f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.1f, h * 0.25f), stroke.width, stroke.cap)
    }
}

@Composable
fun PaperPlainIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        drawRect(tint, Offset(size.width * 0.2f, size.height * 0.15f), Size(size.width * 0.6f, size.height * 0.7f), style = stroke)
    }
}

@Composable
fun PaperRuledIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val rectStroke = Stroke(width = STROKE_WIDTH.toPx() * 0.8f, cap = StrokeCap.Round)
        drawRect(tint, Offset(size.width * 0.2f, size.height * 0.15f), Size(size.width * 0.6f, size.height * 0.7f), style = rectStroke)
        drawLine(tint, Offset(size.width * 0.2f, size.height * 0.4f), Offset(size.width * 0.8f, size.height * 0.4f), stroke.width * 0.6f)
        drawLine(tint, Offset(size.width * 0.2f, size.height * 0.6f), Offset(size.width * 0.8f, size.height * 0.6f), stroke.width * 0.6f)
    }
}

@Composable
fun PaperDotsIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val rectStroke = Stroke(width = STROKE_WIDTH.toPx() * 0.8f, cap = StrokeCap.Round)
        drawRect(tint, Offset(size.width * 0.2f, size.height * 0.15f), Size(size.width * 0.6f, size.height * 0.7f), style = rectStroke)
        for (i in 1..2) {
            for (j in 1..2) {
                drawCircle(tint, radius = size.width * 0.03f, center = Offset(size.width * (0.2f + i * 0.2f), size.height * (0.15f + j * 0.23f)))
            }
        }
    }
}

@Composable
fun PaperGridIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val rectStroke = Stroke(width = STROKE_WIDTH.toPx() * 0.8f, cap = StrokeCap.Round)
        drawRect(tint, Offset(size.width * 0.2f, size.height * 0.15f), Size(size.width * 0.6f, size.height * 0.7f), style = rectStroke)
        val w = size.width
        val h = size.height
        val lineStroke = STROKE_WIDTH.toPx() * 0.4f
        drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.85f), lineStroke)
        drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), lineStroke)
    }
}

@Composable
fun PaperIsoIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val rectStroke = Stroke(width = STROKE_WIDTH.toPx() * 0.8f, cap = StrokeCap.Round)
        drawRect(tint, Offset(size.width * 0.2f, size.height * 0.15f), Size(size.width * 0.6f, size.height * 0.7f), style = rectStroke)
        val w = size.width
        val h = size.height
        val lineStroke = STROKE_WIDTH.toPx() * 0.4f
        drawLine(tint, Offset(w * 0.2f, h * 0.3f), Offset(w * 0.8f, h * 0.65f), lineStroke)
        drawLine(tint, Offset(w * 0.2f, h * 0.65f), Offset(w * 0.8f, h * 0.3f), lineStroke)
    }
}

@Composable
fun CenterTargetIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawCircle(tint, radius = w * 0.15f, center = Offset(w / 2f, h / 2f), style = stroke)
        drawLine(tint, Offset(w / 2f, h * 0.1f), Offset(w / 2f, h * 0.3f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w / 2f, h * 0.7f), Offset(w / 2f, h * 0.9f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.1f, h / 2f), Offset(w * 0.3f, h / 2f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.7f, h / 2f), Offset(w * 0.9f, h / 2f), stroke.width, stroke.cap)
    }
}

@Composable
fun MirrorIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(tint, Offset(w / 2f, h * 0.1f), Offset(w / 2f, h * 0.9f), stroke.width, stroke.cap)
        
        // Left triangle
        drawLine(tint, Offset(w * 0.4f, h * 0.3f), Offset(w * 0.1f, h * 0.5f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.1f, h * 0.5f), Offset(w * 0.4f, h * 0.7f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.4f, h * 0.7f), Offset(w * 0.4f, h * 0.3f), stroke.width, stroke.cap)
        
        // Right triangle (mirrored)
        drawLine(tint, Offset(w * 0.6f, h * 0.3f), Offset(w * 0.9f, h * 0.5f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.9f, h * 0.5f), Offset(w * 0.6f, h * 0.7f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.6f, h * 0.7f), Offset(w * 0.6f, h * 0.3f), stroke.width, stroke.cap)
    }
}

@Composable
fun ResetIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.2f, h * 0.2f),
            size = Size(w * 0.6f, h * 0.6f),
            style = stroke
        )
        // Arrow head
        drawLine(tint, Offset(w * 0.8f, h * 0.5f), Offset(w * 0.6f, h * 0.4f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.8f, h * 0.5f), Offset(w * 1.0f, h * 0.4f), stroke.width, stroke.cap)
    }
}

@Composable
fun RectangleIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx())
        val w = size.width
        val h = size.height
        drawRect(tint, Offset(w * 0.15f, h * 0.25f), Size(w * 0.7f, h * 0.5f), style = stroke)
    }
}

@Composable
fun RoundedRectangleIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx())
        val w = size.width
        val h = size.height
        drawRoundRect(
            tint,
            Offset(w * 0.15f, h * 0.25f),
            Size(w * 0.7f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.15f),
            style = stroke
        )
    }
}

@Composable
fun CircleIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx())
        val d = minOf(size.width, size.height) * 0.7f
        val left = (size.width - d) / 2f
        val top = (size.height - d) / 2f
        drawOval(tint, Offset(left, top), Size(d, d), style = stroke)
    }
}

@Composable
fun StylusIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        
        // A simple diagonal stylus shape
        val path = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.75f)
            lineTo(size.width * 0.35f, size.height * 0.65f)
            lineTo(size.width * 0.85f, size.height * 0.15f)
            lineTo(size.width * 0.95f, size.height * 0.25f)
            lineTo(size.width * 0.45f, size.height * 0.75f)
            lineTo(size.width * 0.25f, size.height * 0.85f)
            close()
        }
        drawPath(path, tint, style = stroke)
        // Draw the tip
        drawCircle(tint, radius = size.width * 0.05f, center = Offset(size.width * 0.25f, size.height * 0.85f))
    }
}

@Composable
fun MediaBrushIcon(brushFamily: dev.stupifranc.inkspire.model.BrushFamilyChoice, tint: Color, modifier: Modifier = Modifier) {
    when (brushFamily) {
        dev.stupifranc.inkspire.model.BrushFamilyChoice.PENCIL -> PencilIcon(tint, modifier)
        dev.stupifranc.inkspire.model.BrushFamilyChoice.WATERCOLOR -> WatercolorIcon(tint, modifier)
        dev.stupifranc.inkspire.model.BrushFamilyChoice.DRY_INK -> DryInkIcon(tint, modifier)
        dev.stupifranc.inkspire.model.BrushFamilyChoice.CALLIGRAPHY -> CalligraphyIcon(tint, modifier)
        dev.stupifranc.inkspire.model.BrushFamilyChoice.DASHED -> DashedIcon(tint, modifier)
        dev.stupifranc.inkspire.model.BrushFamilyChoice.RAINBOW -> RainbowIcon(tint, modifier)
        dev.stupifranc.inkspire.model.BrushFamilyChoice.NEON -> NeonIcon(tint, modifier)
        dev.stupifranc.inkspire.model.BrushFamilyChoice.AIRBRUSH -> AirbrushIcon(tint, modifier)
        else -> MarkerIcon(tint, modifier)
    }
}

@Composable
fun PencilIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        // straight body
        drawLine(tint, Offset(w * 0.25f, h * 0.8f), Offset(w * 0.75f, h * 0.2f), stroke.width, stroke.cap)
        // point
        drawLine(tint, Offset(w * 0.15f, h * 0.95f), Offset(w * 0.25f, h * 0.8f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.15f, h * 0.95f), Offset(w * 0.35f, h * 0.75f), stroke.width, stroke.cap)
    }
}

@Composable
fun WatercolorIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        // tear drop
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            quadraticBezierTo(w * 0.9f, h * 0.5f, w * 0.5f, h * 0.9f)
            quadraticBezierTo(w * 0.1f, h * 0.5f, w * 0.5f, h * 0.1f)
            close()
        }
        drawPath(path, tint, style = stroke)
    }
}

@Composable
fun DryInkIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        // jagged streaks
        drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.4f, h * 0.5f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.3f, h * 0.7f), Offset(w * 0.6f, h * 0.7f), stroke.width, stroke.cap)
    }
}

@Composable
fun CalligraphyIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Square)
        val w = size.width
        val h = size.height
        // angled thick stroke
        drawLine(tint, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.2f), stroke.width * 2f, stroke.cap)
    }
}

@Composable
fun DashedIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        // dashed diagonal
        drawLine(tint, Offset(w * 0.15f, h * 0.85f), Offset(w * 0.35f, h * 0.65f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.45f, h * 0.55f), Offset(w * 0.65f, h * 0.35f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.95f, h * 0.05f), stroke.width, stroke.cap)
    }
}

@Composable
fun RainbowIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawArc(tint, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.1f, h * 0.3f), size = Size(w * 0.8f, h * 0.8f), style = stroke)
        drawArc(tint, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.25f, h * 0.45f), size = Size(w * 0.5f, h * 0.5f), style = stroke)
    }
}

@Composable
fun NeonIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val stroke = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        // glowing tube
        drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), stroke.width, stroke.cap)
        drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), stroke.width * 2.5f, stroke.cap, alpha = 0.4f)
    }
}

@Composable
fun AirbrushIcon(tint: Color, modifier: Modifier = Modifier) {
    IconCanvas(modifier) {
        val w = size.width
        val h = size.height
        // scatter dots
        drawCircle(tint, STROKE_WIDTH.toPx() * 0.5f, Offset(w * 0.5f, h * 0.5f))
        drawCircle(tint, STROKE_WIDTH.toPx() * 0.3f, Offset(w * 0.3f, h * 0.4f))
        drawCircle(tint, STROKE_WIDTH.toPx() * 0.4f, Offset(w * 0.7f, h * 0.6f))
        drawCircle(tint, STROKE_WIDTH.toPx() * 0.2f, Offset(w * 0.6f, h * 0.3f))
        drawCircle(tint, STROKE_WIDTH.toPx() * 0.4f, Offset(w * 0.4f, h * 0.7f))
    }
}
