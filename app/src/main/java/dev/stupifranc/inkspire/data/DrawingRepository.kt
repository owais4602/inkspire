package dev.stupifranc.inkspire.data

import dev.stupifranc.inkspire.model.DEFAULT_PAPER_SPACING
import dev.stupifranc.inkspire.model.DrawingMeta
import dev.stupifranc.inkspire.model.PaperStyle
import java.io.File
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val INDEX_FILE_NAME = "index.json"
private const val STROKES_FILE_NAME = "strokes.ink"
private const val THUMBNAIL_FILE_NAME = "thumbnail.png"

/**
 * Pure file/JSON persistence for drawing metadata and opaque stroke/thumbnail blobs.
 * Deliberately knows nothing about `androidx.ink` types (trap #1: native-backed, not JVM-testable) —
 * stroke encoding/decoding is [dev.stupifranc.inkspire.ink.StrokeStore]'s job.
 */
class DrawingRepository(private val rootDir: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val indexFile = File(rootDir, INDEX_FILE_NAME)

    init {
        rootDir.mkdirs()
    }

    fun listDrawings(): List<DrawingMeta> {
        val index = readIndex()
        val needsNormalization = index.isNotEmpty() && index.map { it.orderIndex }.distinct().size != index.size
        
        if (needsNormalization) {
            val sortedForNormalization = index.sortedWith(
                compareByDescending<DrawingMeta> { it.isPinned }
                    .thenByDescending { it.updatedAtEpochMillis }
            )
            val normalized = sortedForNormalization.mapIndexed { i, meta -> meta.copy(orderIndex = i.toLong()) }
            writeIndex(normalized)
            return normalized
        }
        
        return index.sortedWith(
            compareByDescending<DrawingMeta> { it.isPinned }
                .thenBy { it.orderIndex }
        )
    }

    fun createDrawing(name: String, width: Float, height: Float, backgroundColorArgb: Int, shape: dev.stupifranc.inkspire.model.CanvasShape = dev.stupifranc.inkspire.model.CanvasShape.RECTANGLE): DrawingMeta {
        val now = System.currentTimeMillis()
        val currentIndex = readIndex()
        val minOrder = currentIndex.minOfOrNull { it.orderIndex } ?: 0L
        val meta = DrawingMeta(
            id = UUID.randomUUID().toString(),
            name = name,
            width = width,
            height = height,
            backgroundColorArgb = backgroundColorArgb,
            shape = shape,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            orderIndex = minOrder - 1,
        )
        drawingDir(meta.id).mkdirs()
        writeIndex(currentIndex + meta)
        return meta
    }

    fun renameDrawing(id: String, newName: String) {
        updateMeta(id) { it.copy(name = newName, updatedAtEpochMillis = System.currentTimeMillis()) }
    }

    fun togglePin(id: String) {
        updateMeta(id) { it.copy(isPinned = !it.isPinned) }
    }

    fun updateDrawingOrder(orderedIds: List<String>) {
        val currentDrawings = readIndex().associateBy { it.id }
        val newDrawings = orderedIds.mapIndexedNotNull { index, id ->
            currentDrawings[id]?.copy(orderIndex = index.toLong())
        }
        val remainingDrawings = currentDrawings.values.filter { it.id !in orderedIds }
        writeIndex(newDrawings + remainingDrawings)
    }

    fun updateCanvasSpec(
        id: String,
        width: Float,
        height: Float,
        backgroundColorArgb: Int,
        paperStyle: PaperStyle = PaperStyle.PLAIN,
        paperSpacing: Float = DEFAULT_PAPER_SPACING,
        shape: dev.stupifranc.inkspire.model.CanvasShape = dev.stupifranc.inkspire.model.CanvasShape.RECTANGLE,
    ) {
        updateMeta(id) {
            it.copy(
                width = width,
                height = height,
                backgroundColorArgb = backgroundColorArgb,
                paperStyle = paperStyle,
                paperSpacing = paperSpacing,
                shape = shape,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    fun deleteDrawing(id: String) {
        writeIndex(readIndex().filterNot { it.id == id })
        drawingDir(id).deleteRecursively()
    }

    fun duplicateDrawing(id: String): DrawingMeta? {
        val currentIndex = readIndex()
        val source = currentIndex.find { it.id == id } ?: return null
        val now = System.currentTimeMillis()
        val minOrder = currentIndex.minOfOrNull { it.orderIndex } ?: 0L
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = "${source.name} copy",
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            orderIndex = minOrder - 1,
        )
        drawingDir(source.id).copyRecursively(drawingDir(copy.id), overwrite = true)
        writeIndex(currentIndex + copy)
        return copy
    }

    fun saveStrokes(id: String, bytes: ByteArray) {
        File(drawingDir(id), STROKES_FILE_NAME).writeBytes(bytes)
        updateMeta(id) { it.copy(updatedAtEpochMillis = System.currentTimeMillis()) }
    }

    fun loadStrokes(id: String): ByteArray? {
        val file = File(drawingDir(id), STROKES_FILE_NAME)
        return if (file.exists()) file.readBytes() else null
    }

    fun saveThumbnail(id: String, pngBytes: ByteArray) {
        File(drawingDir(id), THUMBNAIL_FILE_NAME).writeBytes(pngBytes)
        updateMeta(id) { it.copy(hasThumbnail = true) }
    }

    fun thumbnailFile(id: String): File? {
        val file = File(drawingDir(id), THUMBNAIL_FILE_NAME)
        return if (file.exists()) file else null
    }

    private fun drawingDir(id: String) = File(rootDir, id)

    private fun updateMeta(id: String, transform: (DrawingMeta) -> DrawingMeta) {
        writeIndex(readIndex().map { if (it.id == id) transform(it) else it })
    }

    private fun readIndex(): List<DrawingMeta> {
        if (!indexFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<DrawingMeta>>(indexFile.readText())
        } catch (e: SerializationException) {
            // Corrupt index: recover by starting fresh rather than crashing the app.
            // Per-drawing directories on disk are left untouched (not surfaced without an index entry,
            // but not destroyed either) in case manual recovery is ever needed.
            emptyList()
        }
    }

    private fun writeIndex(drawings: List<DrawingMeta>) {
        indexFile.writeText(json.encodeToString(drawings))
    }
}
