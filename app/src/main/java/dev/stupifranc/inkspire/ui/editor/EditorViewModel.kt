package dev.stupifranc.inkspire.ui.editor

import android.app.Application
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.strokes.Stroke
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.stupifranc.inkspire.core.BoundingBox
import dev.stupifranc.inkspire.core.CanvasEdge
import dev.stupifranc.inkspire.core.ContentBounds
import dev.stupifranc.inkspire.core.EntryCollection
import dev.stupifranc.inkspire.core.grow
import dev.stupifranc.inkspire.core.Point
import dev.stupifranc.inkspire.core.ResizeAnchor
import dev.stupifranc.inkspire.core.SymmetryConfig
import dev.stupifranc.inkspire.core.Viewport
import dev.stupifranc.inkspire.core.clampToRect
import dev.stupifranc.inkspire.data.DrawingRepository
import dev.stupifranc.inkspire.data.RecentColorsStore
import dev.stupifranc.inkspire.ink.CanvasExporter
import dev.stupifranc.inkspire.ink.StrokeStore
import dev.stupifranc.inkspire.ink.boundingBox
import dev.stupifranc.inkspire.ink.translatedBy
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.BrushSpec
import dev.stupifranc.inkspire.model.CanvasSpec
import dev.stupifranc.inkspire.model.PaperStyle
import dev.stupifranc.inkspire.model.StrokeEntry
import dev.stupifranc.inkspire.model.Tool
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_SYMMETRY_SECTORS = 6
private val SYMMETRY_SECTOR_RANGE = 1..12
private const val AUTOSAVE_DEBOUNCE_MILLIS = 2000L
private const val CONTENT_FIT_PADDING_PX = 80f
private const val PAN_MARGIN_FRACTION = 1f / 3f
private const val EDGE_GROW_FRACTION = 0.5f

private val DEFAULT_SIZES = mapOf(
    BrushFamilyChoice.PEN to 4f,
    BrushFamilyChoice.MARKER to 14f,
    BrushFamilyChoice.HIGHLIGHTER to 24f,
)

class EditorViewModel(application: Application, private val drawingId: String) : AndroidViewModel(application) {
    private val repository = DrawingRepository(File(application.filesDir, "drawings"))
    private val recentColorsStore = RecentColorsStore(File(application.filesDir, "app_prefs"))
    private val collection = EntryCollection<StrokeEntry>()
    private val sizeMemory = DEFAULT_SIZES.toMutableMap()
    private var autosaveJob: Job? = null

    var strokes by mutableStateOf<List<StrokeEntry>>(emptyList())
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    var tool by mutableStateOf(Tool.NONE)
        private set

    var brushSpec by mutableStateOf(
        BrushSpec(
            family = BrushFamilyChoice.PEN,
            colorArgb = if ((application.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.BLACK
            },
            size = DEFAULT_SIZES.getValue(BrushFamilyChoice.PEN)
        )
    )
        private set

    var recentColors by mutableStateOf(recentColorsStore.load())
        private set

    var symmetryEnabled by mutableStateOf(false)
        private set
    var symmetrySectors by mutableStateOf(DEFAULT_SYMMETRY_SECTORS)
        private set
    var symmetryMirror by mutableStateOf(false)
        private set
    private var symmetryCenter by mutableStateOf<Point?>(null)
    var awaitingCenterPlacement by mutableStateOf(false)
        private set

    var canvasSpec by mutableStateOf(loadInitialCanvasSpec())
        private set
    var viewport by mutableStateOf(Viewport())
        private set
    var containerWidth by mutableStateOf(0f)
        private set
    var containerHeight by mutableStateOf(0f)
        private set

    init {
        repository.loadStrokes(drawingId)?.let { bytes ->
            collection.load(StrokeStore.decode(bytes))
            strokes = collection.entries
            canUndo = collection.canUndo
            canRedo = collection.canRedo
        }
    }

    private fun loadInitialCanvasSpec(): CanvasSpec {
        val meta = repository.listDrawings().find { it.id == drawingId }
            ?: return CanvasSpec(width = 0f, height = 0f, backgroundColorArgb = 0)
        return CanvasSpec(width = meta.width, height = meta.height, backgroundColorArgb = meta.backgroundColorArgb, paperStyle = meta.paperStyle, paperSpacing = meta.paperSpacing, shape = meta.shape)
    }

    val symmetryConfig: SymmetryConfig
        get() = if (symmetryEnabled) {
            SymmetryConfig(sectors = symmetrySectors, mirror = symmetryMirror, center = symmetryCenter ?: Point(0f, 0f))
        } else {
            SymmetryConfig.OFF
        }

    fun toggleSymmetryEnabled() {
        symmetryEnabled = !symmetryEnabled
        if (!symmetryEnabled) awaitingCenterPlacement = false
    }

    fun changeSymmetrySectors(sectors: Int) {
        symmetrySectors = sectors.coerceIn(SYMMETRY_SECTOR_RANGE.first, SYMMETRY_SECTOR_RANGE.last)
    }

    fun changeSymmetryMirror(mirror: Boolean) {
        symmetryMirror = mirror
    }

    /** Applied on both handle drag and tap-place — the center can never leave the page. */
    fun moveSymmetryCenter(center: Point) {
        symmetryCenter = clampToRect(center, canvasSpec.width, canvasSpec.height)
    }

    fun resetSymmetryCenter() {
        symmetryCenter = Point(canvasSpec.width / 2f, canvasSpec.height / 2f)
    }

    /** Arms/disarms the one-shot "tap the canvas to place the symmetry center" mode. */
    fun toggleCenterPlacementArmed() {
        awaitingCenterPlacement = !awaitingCenterPlacement
    }

    /** Consumes an armed tap-to-place: moves the center there and disarms. No-op if not armed. */
    fun placeSymmetryCenterAt(point: Point) {
        if (!awaitingCenterPlacement) return
        moveSymmetryCenter(point)
        awaitingCenterPlacement = false
    }

    fun onContainerSizeChanged(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        containerWidth = width
        containerHeight = height

        if (canvasSpec.width <= 0f) {
            // First layout: default the document to fill the viewport, matching pre-M4 behavior
            // until the user explicitly resizes it larger via the resize dialog.
            canvasSpec = canvasSpec.copy(width = width, height = height)
            viewport = Viewport()
        }
        if (symmetryCenter == null) {
            symmetryCenter = Point(canvasSpec.width / 2f, canvasSpec.height / 2f)
        }
        viewport = viewport.clampedTo(canvasSpec.width, canvasSpec.height, containerWidth, containerHeight, panMargin())
    }

    /** Pan may overshoot each page edge by this much (screen px) — off-page ink (e.g. symmetry replicas) stays reachable. */
    private fun panMargin(): Float = minOf(containerWidth, containerHeight) * PAN_MARGIN_FRACTION

    private fun contentUnion(): BoundingBox =
        ContentBounds.union(collection.entries.mapNotNull { it.boundingBox() }, canvasSpec.width, canvasSpec.height)

    fun panBy(dx: Float, dy: Float) {
        viewport = viewport.pannedBy(dx, dy)
            .clampedTo(canvasSpec.width, canvasSpec.height, containerWidth, containerHeight, panMargin())
    }

    fun zoomBy(factor: Float, focal: Point) {
        viewport = viewport.zoomedBy(factor, focal)
            .clampedTo(canvasSpec.width, canvasSpec.height, containerWidth, containerHeight, panMargin())
    }

    /** Always fits *everything* — the page union'd with the drawing's content bounds, not just the page. */
    fun fitToScreen() {
        val union = contentUnion()
        viewport = Viewport.fitRect(union.minX, union.minY, union.width, union.height, containerWidth, containerHeight)
    }

    /** Double-tap: from (near) fit, zooms in 3x centered on the tap; otherwise fits everything again. */
    fun onDoubleTap(tapScreen: Point) {
        val union = contentUnion()
        viewport = viewport.doubleTapTarget(union.minX, union.minY, union.width, union.height, containerWidth, containerHeight, tapScreen)
    }

    fun resizeCanvas(newWidth: Float, newHeight: Float, anchor: ResizeAnchor) {
        if (newWidth <= 0f || newHeight <= 0f) return
        val offset = ResizeAnchor.offset(canvasSpec.width, canvasSpec.height, newWidth, newHeight, anchor)
        applyContentTranslation(offset.x, offset.y)
        canvasSpec = canvasSpec.copy(width = newWidth, height = newHeight)
        viewport = viewport.clampedTo(newWidth, newHeight, containerWidth, containerHeight, panMargin())
        scheduleAutosave()
    }

    /**
     * Explicit "extend canvas" action: grows one edge by a fixed chunk (half a screen of world
     * units), pinning the opposite edge. Counter-pans so existing content stays visually fixed —
     * the new space appears beyond the grown edge, reachable by panning toward it.
     */
    fun growEdge(edge: CanvasEdge) {
        val chunk = when (edge) {
            CanvasEdge.LEFT, CanvasEdge.RIGHT -> containerWidth * EDGE_GROW_FRACTION
            CanvasEdge.TOP, CanvasEdge.BOTTOM -> containerHeight * EDGE_GROW_FRACTION
        }
        if (chunk <= 0f) return
        val (newWidth, newHeight, anchor) = edge.grow(canvasSpec.width, canvasSpec.height, chunk)
        val offset = ResizeAnchor.offset(canvasSpec.width, canvasSpec.height, newWidth, newHeight, anchor)
        applyContentTranslation(offset.x, offset.y)
        canvasSpec = canvasSpec.copy(width = newWidth, height = newHeight)
        viewport = viewport.pannedBy(-offset.x * viewport.scale, -offset.y * viewport.scale)
            .clampedTo(newWidth, newHeight, containerWidth, containerHeight, panMargin())
        scheduleAutosave()
    }

    /** Resizes the page to the minimal bounds covering all content (never smaller than the existing floor), translating strokes to match. No-op if there's no content. */
    fun fitCanvasToContent() {
        val result = ContentBounds.compute(collection.entries.mapNotNull { it.boundingBox() }, CONTENT_FIT_PADDING_PX) ?: return
        applyContentTranslation(-result.offsetX, -result.offsetY)
        canvasSpec = canvasSpec.copy(width = result.width, height = result.height)
        viewport = viewport.clampedTo(result.width, result.height, containerWidth, containerHeight, panMargin())
        scheduleAutosave()
    }

    private fun applyContentTranslation(dx: Float, dy: Float) {
        if (dx != 0f || dy != 0f) {
            collection.transformAll { entry -> entry.copy(stroke = entry.stroke.translatedBy(dx, dy)) }
            sync()
        }
        symmetryCenter = symmetryCenter?.let { Point(it.x + dx, it.y + dy) }
    }

    /** The page fill is always opaque — a translucent page over the workspace gray would read as a rendering bug. */
    fun setCanvasBackground(colorArgb: Int) {
        canvasSpec = canvasSpec.copy(backgroundColorArgb = colorArgb or (0xFF shl 24))
        scheduleAutosave()
    }

    fun setPaperStyle(style: PaperStyle) {
        canvasSpec = canvasSpec.copy(paperStyle = style)
        scheduleAutosave()
    }

    fun setPaperSpacing(spacing: Float) {
        canvasSpec = canvasSpec.copy(paperSpacing = spacing.coerceAtLeast(16f))
        scheduleAutosave()
    }

    fun setCanvasShape(shape: dev.stupifranc.inkspire.model.CanvasShape) {
        canvasSpec = canvasSpec.copy(shape = shape)
        scheduleAutosave()
    }

    fun selectTool(newTool: Tool) {
        tool = newTool
        awaitingCenterPlacement = false
    }

    fun selectBrushFamily(family: BrushFamilyChoice) {
        brushSpec = brushSpec.copy(family = family, size = sizeMemory.getValue(family))
    }

    fun setColor(colorArgb: Int) {
        brushSpec = brushSpec.copy(colorArgb = colorArgb)
    }

    fun commitCurrentColorToRecents() {
        recentColors = recentColorsStore.commit(brushSpec.colorArgb)
    }

    fun setSize(size: Float) {
        sizeMemory[brushSpec.family] = size
        brushSpec = brushSpec.copy(size = size)
    }

    fun onStrokesFinished(groupId: String, finished: List<Stroke>) {
        if (finished.isEmpty()) return
        val entries = finished.map { StrokeEntry(groupId = groupId, stroke = it) }
        collection.add(entries)
        sync()
    }

    fun eraseHits(hitIds: Set<String>) {
        if (hitIds.isEmpty()) return
        collection.erase(hitIds)
        sync()
    }

    fun clear() {
        collection.clear()
        sync()
    }

    fun undo() {
        collection.undo()
        sync()
    }

    fun redo() {
        collection.redo()
        sync()
    }

    /** Cancels any pending debounced autosave and saves immediately (e.g. on ON_STOP). */
    fun saveNow() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch(Dispatchers.IO) { performSave(collection.entries, canvasSpec) }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        val entries = collection.entries
        val spec = canvasSpec
        autosaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(AUTOSAVE_DEBOUNCE_MILLIS)
            performSave(entries, spec)
        }
    }

    private fun performSave(entries: List<StrokeEntry>, spec: CanvasSpec) {
        repository.saveStrokes(drawingId, StrokeStore.encode(entries))
        repository.updateCanvasSpec(drawingId, spec.width, spec.height, spec.backgroundColorArgb, spec.paperStyle, spec.paperSpacing, spec.shape)
        if (spec.width > 0f && spec.height > 0f) {
            val thumbnail = CanvasExporter.renderThumbnail(spec, entries)
            repository.saveThumbnail(drawingId, CanvasExporter.toPngBytes(thumbnail))
        }
    }

    private fun sync() {
        strokes = collection.entries
        canUndo = collection.canUndo
        canRedo = collection.canRedo
        scheduleAutosave()
    }

    companion object {
        fun factory(application: Application, drawingId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { EditorViewModel(application, drawingId) }
            }
    }
}
