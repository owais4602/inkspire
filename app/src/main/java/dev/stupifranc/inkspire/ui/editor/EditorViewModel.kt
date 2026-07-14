package dev.stupifranc.inkspire.ui.editor

import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.strokes.Stroke
import androidx.lifecycle.ViewModel
import dev.stupifranc.inkspire.core.EntryCollection
import dev.stupifranc.inkspire.core.Point
import dev.stupifranc.inkspire.core.SymmetryConfig
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.BrushSpec
import dev.stupifranc.inkspire.model.StrokeEntry
import dev.stupifranc.inkspire.model.Tool
import java.util.UUID

private const val DEFAULT_SYMMETRY_SECTORS = 6
private val SYMMETRY_SECTOR_RANGE = 1..12

private val DEFAULT_SIZES = mapOf(
    BrushFamilyChoice.PRESSURE_PEN to 8f,
    BrushFamilyChoice.MARKER to 14f,
    BrushFamilyChoice.HIGHLIGHTER to 24f,
)

class EditorViewModel : ViewModel() {
    private val collection = EntryCollection<StrokeEntry>()
    private val sizeMemory = DEFAULT_SIZES.toMutableMap()

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

    var recentColors by mutableStateOf<List<Int>>(emptyList())
        private set

    var symmetryEnabled by mutableStateOf(false)
        private set
    var symmetrySectors by mutableStateOf(DEFAULT_SYMMETRY_SECTORS)
        private set
    var symmetryMirror by mutableStateOf(false)
        private set
    private var symmetryCenter by mutableStateOf<Point?>(null)

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

    fun onCanvasSizeChanged(width: Float, height: Float) {
        if (symmetryCenter == null && width > 0f && height > 0f) {
            symmetryCenter = Point(width / 2f, height / 2f)
        }
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
        val color = brushSpec.colorArgb
        recentColors = (listOf(color) + recentColors.filterNot { it == color }).take(8)
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

    private fun sync() {
        strokes = collection.entries
        canUndo = collection.canUndo
        canRedo = collection.canRedo
    }
}
