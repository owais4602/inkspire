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

private const val THUMBNAIL_MAX_DIMENSION_PX = 320f

/** Renders the full document (not just the visible viewport) and saves it as a PNG in the gallery. */
object CanvasExporter {

    fun renderBitmap(canvasSpec: CanvasSpec, strokes: List<StrokeEntry>, scale: Int): Bitmap =
        renderBitmap(canvasSpec, strokes, scale.toFloat())

    fun renderBitmap(canvasSpec: CanvasSpec, strokes: List<StrokeEntry>, scale: Float): Bitmap {
        val width = (canvasSpec.width * scale).toInt().coerceAtLeast(1)
        val height = (canvasSpec.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(canvasSpec.backgroundColorArgb)
        PaperStyleRenderer.draw(canvas, canvasSpec, 0f, 0f, width.toFloat(), height.toFloat(), scale)

        val renderer = CanvasStrokeRenderer.create()
        canvas.save()
        canvas.scale(scale, scale)
        val identity = Matrix()
        strokes.forEach { entry -> renderer.draw(canvas, entry.stroke, identity) }
        canvas.restore()
        return bitmap
    }

    /** Small preview render for gallery thumbnails, downscaled to fit within [THUMBNAIL_MAX_DIMENSION_PX]. */
    fun renderThumbnail(canvasSpec: CanvasSpec, strokes: List<StrokeEntry>): Bitmap {
        val longestSide = maxOf(canvasSpec.width, canvasSpec.height, 1f)
        val scale = (THUMBNAIL_MAX_DIMENSION_PX / longestSide).coerceAtMost(1f)
        return renderBitmap(canvasSpec, strokes, scale)
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
