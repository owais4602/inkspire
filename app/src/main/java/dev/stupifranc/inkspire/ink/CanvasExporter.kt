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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders the full document (not just the visible viewport) and saves it as a PNG in the gallery. */
object CanvasExporter {

    fun renderBitmap(canvasSpec: CanvasSpec, strokes: List<StrokeEntry>, scale: Int): Bitmap {
        val width = (canvasSpec.width * scale).toInt().coerceAtLeast(1)
        val height = (canvasSpec.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(canvasSpec.backgroundColorArgb)

        val renderer = CanvasStrokeRenderer.create()
        val worldToBitmap = Matrix().apply { setScale(scale.toFloat(), scale.toFloat()) }
        strokes.forEach { entry -> renderer.draw(canvas, entry.stroke, worldToBitmap) }
        return bitmap
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
