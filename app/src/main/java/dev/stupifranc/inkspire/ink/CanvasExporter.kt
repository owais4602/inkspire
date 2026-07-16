package dev.stupifranc.inkspire.ink

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import dev.stupifranc.inkspire.model.CanvasSpec
import dev.stupifranc.inkspire.model.StrokeEntry
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val THUMBNAIL_TARGET_SHORT_SIDE_PX = 480f
private const val THUMBNAIL_MAX_AREA_PX = 1024f * 1024f // 1 Megapixel
private const val THUMBNAIL_MAX_DIMENSION_PX = 4096f // Hardware texture limit safety

/** Renders the full document (not just the visible viewport) and saves it as a PNG in the gallery. */
object CanvasExporter {

    fun renderBitmap(context: Context, canvasSpec: CanvasSpec, strokes: List<StrokeEntry>, scale: Int): Bitmap =
        renderBitmap(context, canvasSpec, strokes, scale.toFloat())

    fun renderBitmap(context: Context, canvasSpec: CanvasSpec, strokes: List<StrokeEntry>, scale: Float): Bitmap {
        val width = (canvasSpec.width * scale).toInt().coerceAtLeast(1)
        val height = (canvasSpec.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (canvasSpec.background != null && canvasSpec.background.colors.isNotEmpty()) {
            val bg = canvasSpec.background
            if (bg.colors.size == 1) {
                canvas.drawColor(bg.colors.first())
            } else {
                val paint = android.graphics.Paint()
                val colorsArray = bg.colors.toIntArray()
                when (bg.kind) {
                    dev.stupifranc.inkspire.model.BackgroundKind.FLAT -> {
                        canvas.drawColor(colorsArray.first())
                    }
                    dev.stupifranc.inkspire.model.BackgroundKind.LINEAR -> {
                        val cx = width / 2f
                        val cy = height / 2f
                        val angleRad = Math.toRadians(bg.angleDegrees.toDouble())
                        val halfDiagonal = kotlin.math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                        val dx = kotlin.math.cos(angleRad).toFloat() * halfDiagonal
                        val dy = kotlin.math.sin(angleRad).toFloat() * halfDiagonal
                        paint.shader = android.graphics.LinearGradient(
                            cx - dx, cy - dy, cx + dx, cy + dy,
                            colorsArray, null, android.graphics.Shader.TileMode.CLAMP
                        )
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                    }
                    dev.stupifranc.inkspire.model.BackgroundKind.RADIAL -> {
                        val radius = maxOf(width.toFloat(), height.toFloat()) / 2f
                        paint.shader = android.graphics.RadialGradient(
                            width / 2f, height / 2f, radius.coerceAtLeast(1f),
                            colorsArray, null, android.graphics.Shader.TileMode.CLAMP
                        )
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                    }
                }
            }
        } else {
            canvas.drawColor(canvasSpec.backgroundColorArgb)
        }
        PaperStyleRenderer.draw(canvas, canvasSpec, 0f, 0f, width.toFloat(), height.toFloat(), scale)

        val renderer = CanvasStrokeRenderer.create(InkTextures.getStore(context))
        canvas.save()
        val scaleMatrix = Matrix().apply { setScale(scale, scale) }
        canvas.concat(scaleMatrix)
        strokes.forEach { entry -> renderer.draw(canvas, entry.stroke, scaleMatrix) }
        canvas.restore()
        return bitmap
    }

    /** Small preview render for gallery thumbnails, downscaled to ensure crisp rendering while preventing OOM. */
    fun renderThumbnail(context: Context, canvasSpec: CanvasSpec, strokes: List<StrokeEntry>): Bitmap {
        val shortestSide = minOf(canvasSpec.width, canvasSpec.height).coerceAtLeast(1f)
        var scale = THUMBNAIL_TARGET_SHORT_SIDE_PX / shortestSide
        
        // Ensure the area doesn't exceed 1 Megapixel
        val area = (canvasSpec.width * scale) * (canvasSpec.height * scale)
        if (area > THUMBNAIL_MAX_AREA_PX) {
            scale = kotlin.math.sqrt(THUMBNAIL_MAX_AREA_PX / (canvasSpec.width * canvasSpec.height))
        }
        
        // Ensure the longest side doesn't exceed hardware texture limits
        val longestSide = maxOf(canvasSpec.width, canvasSpec.height).coerceAtLeast(1f)
        if (longestSide * scale > THUMBNAIL_MAX_DIMENSION_PX) {
            scale = THUMBNAIL_MAX_DIMENSION_PX / longestSide
        }
        
        // Never upscale beyond 1x (if the canvas is tiny, leave it tiny)
        scale = scale.coerceAtMost(1f)

        return renderBitmap(context, canvasSpec, strokes, scale)
    }

    fun toPngBytes(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    /** Saves [bitmap] as a new PNG in the Pictures/Inkspire gallery folder; returns its content URI. */
    fun saveToGallery(context: Context, bitmap: Bitmap): Uri? {
        val filename = "inkspire_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Inkspire")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
        return uri
    }
}
