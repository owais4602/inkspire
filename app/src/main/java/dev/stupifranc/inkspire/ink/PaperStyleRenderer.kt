package dev.stupifranc.inkspire.ink

import android.graphics.Canvas
import android.graphics.Paint
import dev.stupifranc.inkspire.core.PaperPattern
import dev.stupifranc.inkspire.model.CanvasSpec
import dev.stupifranc.inkspire.model.PaperStyle
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Android-canvas glue for [PaperPattern]: draws the page's background pattern (ruled/dots/
 * grid/isometric) clipped to the page rect. Shared by the live dry layer (`DrawingSurface`) and
 * [CanvasExporter], so exported PNGs/thumbnails always match what's on screen.
 */
object PaperStyleRenderer {

    /**
     * Draws [CanvasSpec.paperStyle] into the page rect `[pageLeft, pageTop, pageLeft + pageWidth,
     * pageTop + pageHeight]` (target/canvas units, e.g. screen px or bitmap px), where [scale] maps
     * one world (document) unit to one target unit — so `spec.paperSpacing * scale` is the on-canvas
     * spacing. No-op for [PaperStyle.PLAIN] or non-positive spacing.
     */
    fun draw(canvas: Canvas, spec: CanvasSpec, pageLeft: Float, pageTop: Float, pageWidth: Float, pageHeight: Float, scale: Float) {
        if (spec.paperStyle == PaperStyle.PLAIN) return
        val spacingPx = spec.paperSpacing * scale
        if (spacingPx <= 0f) return

        val paint = Paint().apply {
            isAntiAlias = true
            color = PaperPattern.lineColorArgb(spec.backgroundColorArgb)
            strokeWidth = max(1f, scale)
        }
        val right = pageLeft + pageWidth
        val bottom = pageTop + pageHeight

        canvas.save()
        canvas.clipRect(pageLeft, pageTop, right, bottom)
        paint.style = Paint.Style.STROKE
        when (spec.paperStyle) {
            PaperStyle.RULED -> drawHorizontalLines(canvas, paint, pageLeft, pageTop, pageWidth, pageHeight, right, spacingPx)
            PaperStyle.GRID -> {
                drawHorizontalLines(canvas, paint, pageLeft, pageTop, pageWidth, pageHeight, right, spacingPx)
                drawVerticalLines(canvas, paint, pageLeft, pageTop, pageWidth, pageHeight, bottom, spacingPx)
            }
            PaperStyle.DOTS -> {
                paint.style = Paint.Style.FILL
                drawDots(canvas, paint, pageLeft, pageTop, pageWidth, pageHeight, spacingPx)
            }
            PaperStyle.ISOMETRIC -> drawIsometric(canvas, paint, pageLeft, pageTop, pageWidth, pageHeight, spacingPx)
            PaperStyle.PLAIN -> Unit
        }
        canvas.restore()
    }

    private fun drawHorizontalLines(canvas: Canvas, paint: Paint, pageLeft: Float, pageTop: Float, pageWidth: Float, pageHeight: Float, right: Float, spacingPx: Float) {
        PaperPattern.lineOffsets(pageHeight, spacingPx).forEach { y ->
            canvas.drawLine(pageLeft, pageTop + y, right, pageTop + y, paint)
        }
    }

    private fun drawVerticalLines(canvas: Canvas, paint: Paint, pageLeft: Float, pageTop: Float, pageWidth: Float, pageHeight: Float, bottom: Float, spacingPx: Float) {
        PaperPattern.lineOffsets(pageWidth, spacingPx).forEach { x ->
            canvas.drawLine(pageLeft + x, pageTop, pageLeft + x, bottom, paint)
        }
    }

    private fun drawDots(canvas: Canvas, paint: Paint, pageLeft: Float, pageTop: Float, pageWidth: Float, pageHeight: Float, spacingPx: Float) {
        val xs = PaperPattern.lineOffsets(pageWidth, spacingPx)
        val ys = PaperPattern.lineOffsets(pageHeight, spacingPx)
        val radius = max(1f, spacingPx * 0.05f)
        for (x in xs) {
            for (y in ys) {
                canvas.drawCircle(pageLeft + x, pageTop + y, radius, paint)
            }
        }
    }

    /**
     * Triangular guide grid: a vertical family plus two families at ±30° from horizontal, all
     * centered on the page. Drawn as long lines relying on the canvas's own clip (set by [draw])
     * rather than manual line/rect intersection math.
     */
    private fun drawIsometric(canvas: Canvas, paint: Paint, pageLeft: Float, pageTop: Float, pageWidth: Float, pageHeight: Float, spacingPx: Float) {
        val centerX = pageLeft + pageWidth / 2f
        val centerY = pageTop + pageHeight / 2f
        val diagonal = hypot(pageWidth.toDouble(), pageHeight.toDouble()).toFloat()
        val halfLength = diagonal
        val maxOffset = diagonal / 2f + spacingPx

        drawLineFamily(canvas, paint, centerX, centerY, angleDegrees = 90.0, spacingPx = spacingPx, maxOffset = maxOffset, halfLength = halfLength)
        drawLineFamily(canvas, paint, centerX, centerY, angleDegrees = 30.0, spacingPx = spacingPx, maxOffset = maxOffset, halfLength = halfLength)
        drawLineFamily(canvas, paint, centerX, centerY, angleDegrees = -30.0, spacingPx = spacingPx, maxOffset = maxOffset, halfLength = halfLength)
    }

    private fun drawLineFamily(canvas: Canvas, paint: Paint, centerX: Float, centerY: Float, angleDegrees: Double, spacingPx: Float, maxOffset: Float, halfLength: Float) {
        val angle = Math.toRadians(angleDegrees)
        val dirX = cos(angle).toFloat()
        val dirY = sin(angle).toFloat()
        val normalAngle = Math.toRadians(angleDegrees + 90.0)
        val normalX = cos(normalAngle).toFloat()
        val normalY = sin(normalAngle).toFloat()

        PaperPattern.interceptOffsets(-maxOffset, maxOffset, spacingPx).forEach { k ->
            val px = centerX + normalX * k
            val py = centerY + normalY * k
            canvas.drawLine(px - dirX * halfLength, py - dirY * halfLength, px + dirX * halfLength, py + dirY * halfLength, paint)
        }
    }
}
