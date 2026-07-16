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
private const val THUMBNAIL_SUPERSAMPLE_FACTOR = 2f

/** Renders the full document (not just the visible viewport) and saves it as a PNG in the gallery. */
object CanvasExporter {

    fun renderBitmap(context: Context, canvasSpec: CanvasSpec, strokes: List<StrokeEntry>, scale: Int): Bitmap =
        renderBitmap(context, canvasSpec, strokes, scale.toFloat())

    fun renderBitmap(context: Context, canvasSpec: CanvasSpec, strokes: List<StrokeEntry>, scale: Float): Bitmap {
        val width = (canvasSpec.width * scale).toInt().coerceAtLeast(1)
        val height = (canvasSpec.height * scale).toInt().coerceAtLeast(1)
        // A plain Bitmap-backed Canvas is never hardware-accelerated, and CanvasStrokeRenderer
        // silently drops particle-tip strokes (airbrush) on that path — so prefer rendering
        // through a RenderNode/HardwareRenderer, falling back to software below API 29 or if
        // the GPU pass fails (e.g. dimensions beyond the texture limit on a huge export).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hardware = runCatching {
                renderHardware(width, height) { canvas -> drawDocument(context, canvas, canvasSpec, strokes, scale, width, height) }
            }.getOrNull()
            if (hardware != null) return hardware
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawDocument(context, Canvas(bitmap), canvasSpec, strokes, scale, width, height)
        return bitmap
    }

    /** Draws the full document (background, paper style, strokes) onto [canvas] at [scale]. */
    private fun drawDocument(
        context: Context,
        canvas: Canvas,
        canvasSpec: CanvasSpec,
        strokes: List<StrokeEntry>,
        scale: Float,
        width: Int,
        height: Int,
    ) {
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
    }

    /**
     * Renders [drawBlock] on a GPU-backed canvas and reads the pixels back into a software
     * ARGB_8888 bitmap (so callers can still compress/scale it). Returns null if the frame
     * couldn't be produced. API 29+.
     */
    private fun renderHardware(width: Int, height: Int, drawBlock: (Canvas) -> Unit): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val imageReader = android.media.ImageReader.newInstance(
            width, height, android.graphics.PixelFormat.RGBA_8888, 1,
            android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
        )
        val renderer = android.graphics.HardwareRenderer()
        try {
            val node = android.graphics.RenderNode("inkspireExport")
            node.setPosition(0, 0, width, height)
            val recordingCanvas = node.beginRecording(width, height)
            try {
                drawBlock(recordingCanvas)
            } finally {
                node.endRecording()
            }
            renderer.setSurface(imageReader.surface)
            renderer.setContentRoot(node)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            val image = imageReader.acquireLatestImage() ?: return null
            try {
                val hardwareBuffer = image.hardwareBuffer ?: return null
                try {
                    return Bitmap.wrapHardwareBuffer(hardwareBuffer, null)?.copy(Bitmap.Config.ARGB_8888, false)
                } finally {
                    hardwareBuffer.close()
                }
            } finally {
                image.close()
            }
        } finally {
            renderer.destroy()
            imageReader.close()
        }
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

        if (scale >= 1f) {
            // No downscale happening — nothing to supersample against.
            return renderBitmap(context, canvasSpec, strokes, scale)
        }

        // Rendering straight at the tiny target resolution starves faint, high-frequency brush
        // textures (e.g. airbrush's low-opacity granulation texture) of enough samples to survive
        // — they alias away to near-nothing. Render bigger, then downscale with bilinear
        // filtering so the true average density comes through; the brush recipe itself is untouched.
        val targetWidth = (canvasSpec.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (canvasSpec.height * scale).toInt().coerceAtLeast(1)
        val supersampleScale = (scale * THUMBNAIL_SUPERSAMPLE_FACTOR).coerceAtMost(1f)
        val supersampled = renderBitmap(context, canvasSpec, strokes, supersampleScale)
        val downscaled = Bitmap.createScaledBitmap(supersampled, targetWidth, targetHeight, true)
        if (downscaled !== supersampled) supersampled.recycle()
        return downscaled
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
