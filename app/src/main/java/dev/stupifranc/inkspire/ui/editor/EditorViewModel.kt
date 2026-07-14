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
import dev.stupifranc.inkspire.core.EntryCollection
import dev.stupifranc.inkspire.core.Point
import dev.stupifranc.inkspire.core.ResizeAnchor
import dev.stupifranc.inkspire.core.SymmetryConfig
import dev.stupifranc.inkspire.core.Viewport
import dev.stupifranc.inkspire.data.DrawingRepository
import dev.stupifranc.inkspire.data.RecentColorsStore
import dev.stupifranc.inkspire.ink.CanvasExporter
import dev.stupifranc.inkspire.ink.StrokeStore
import dev.stupifranc.inkspire.ink.translatedBy
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.BrushSpec
import dev.stupifranc.inkspire.model.CanvasSpec
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

private val DEFAULT_SIZES = mapOf(
    BrushFamilyChoice.PRESSURE_PEN to 8f,
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

    var tool by mutableStateOf(Tool.PEN)
        private set

    var brushSpec by mutableStateOf(
        BrushSpec(family = BrushFamilyChoice.PRESSURE_PEN, colorArgb = Color.BLACK, size = DEFAULT_SIZES.getValue(BrushFamilyChoice.PRESSURE_PEN))
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
            ?: return CanvasSpec(width = 0f, height = 0f, backgroundColorArgb = Color.WHITE)
        return CanvasSpec(width = meta.width, height = meta.height, backgroundColorArgb = meta.backgroundColorArgb)
    }

    val symmetryConfig: SymmetryConfig
        get() = if (symmetryEnabled) {
            SymmetryConfig(sectors = symmetrySectors, mirror = symmetryMirror, center = symmetryCenter ?: Point(0f, 0f))
        } else {
            SymmetryConfig.OFF
        }

    fun toggleSymmetryEnabled() {
        symmetryEnabled = !symmetryEnabled
    }

    fun changeSymmetrySectors(sectors: Int) {
        symmetrySectors = sectors.coerceIn(SYMMETRY_SECTOR_RANGE.first, SYMMETRY_SECTOR_RANGE.last)
    }

    fun changeSymmetryMirror(mirror: Boolean) {
        symmetryMirror = mirror
    }

    fun moveSymmetryCenter(center: Point) {
        symmetryCenter = center
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
        viewport = viewport.clampedTo(canvasSpec.width, canvasSpec.height, containerWidth, containerHeight)
    }

    fun panBy(dx: Float, dy: Float) {
        viewport = viewport.pannedBy(dx, dy)
            .clampedTo(canvasSpec.width, canvasSpec.height, containerWidth, containerHeight)
    }

    fun zoomBy(factor: Float, focal: Point) {
        viewport = viewport.zoomedBy(factor, focal)
            .clampedTo(canvasSpec.width, canvasSpec.height, containerWidth, containerHeight)
    }

    fun fitToScreen() {
        viewport = Viewport.fit(canvasSpec.width, canvasSpec.height, containerWidth, containerHeight)
    }

    fun resizeCanvas(newWidth: Float, newHeight: Float, anchor: ResizeAnchor) {
        if (newWidth <= 0f || newHeight <= 0f) return
        val offset = ResizeAnchor.offset(canvasSpec.width, canvasSpec.height, newWidth, newHeight, anchor)
        if (offset.x != 0f || offset.y != 0f) {
            collection.transformAll { entry -> entry.copy(stroke = entry.stroke.translatedBy(offset.x, offset.y)) }
            sync()
        }
        symmetryCenter = symmetryCenter?.let { Point(it.x + offset.x, it.y + offset.y) }
        canvasSpec = canvasSpec.copy(width = newWidth, height = newHeight)
        viewport = viewport.clampedTo(newWidth, newHeight, containerWidth, containerHeight)
        scheduleAutosave()
    }

    fun setCanvasBackground(colorArgb: Int) {
        canvasSpec = canvasSpec.copy(backgroundColorArgb = colorArgb)
        scheduleAutosave()
    }

    fun selectTool(newTool: Tool) {
        tool = newTool
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

    fun onStrokesFinished(finished: List<Stroke>) {
        if (finished.isEmpty()) return
        val groupId = UUID.randomUUID().toString()
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
        repository.updateCanvasSpec(drawingId, spec.width, spec.height, spec.backgroundColorArgb)
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
