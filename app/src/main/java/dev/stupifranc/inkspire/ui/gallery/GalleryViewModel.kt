package dev.stupifranc.inkspire.ui.gallery

import android.app.Application
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.stupifranc.inkspire.data.DrawingRepository
import dev.stupifranc.inkspire.data.AppPrefsStore
import dev.stupifranc.inkspire.model.AppPrefs
import dev.stupifranc.inkspire.model.DrawingMeta
import java.io.File

class GalleryViewModel(
    private val repository: DrawingRepository,
    private val prefsStore: AppPrefsStore,
    private val isSystemDark: () -> Boolean,
) : ViewModel() {

    var drawings by mutableStateOf<List<DrawingMeta>>(emptyList())
        private set

    var prefs by mutableStateOf(AppPrefs())
        private set

    init {
        prefs = prefsStore.load()
        refresh()
    }

    fun updatePrefs(newPrefs: AppPrefs) {
        prefs = newPrefs
        prefsStore.save(newPrefs)
    }

    fun refresh() {
        drawings = repository.listDrawings()
    }

    fun togglePin(id: String) {
        repository.togglePin(id)
        refresh()
    }

    fun dropOnto(draggedId: String, targetId: String) {
        val currentDrawings = drawings.toMutableList()
        val draggedIndex = currentDrawings.indexOfFirst { it.id == draggedId }
        val targetIndex = currentDrawings.indexOfFirst { it.id == targetId }
        if (draggedIndex == -1 || targetIndex == -1) return

        val item = currentDrawings.removeAt(draggedIndex)
        currentDrawings.add(targetIndex, item)
        
        repository.updateDrawingOrder(currentDrawings.map { it.id })
        refresh()
    }

    /** Width/height start at 0 — the editor fills the document to whatever viewport first appears, same as a fresh install. */
    fun createDrawing(): DrawingMeta {
        val defaultBg = if (isSystemDark()) 0xFF1C1B1F.toInt() else 0xFFFFFFFF.toInt()
        val meta = repository.createDrawing(name = "Untitled", width = 0f, height = 0f, backgroundColorArgb = defaultBg)
        refresh()
        return meta
    }

    fun rename(id: String, newName: String) {
        repository.renameDrawing(id, newName)
        refresh()
    }

    fun delete(id: String) {
        repository.deleteDrawing(id)
        refresh()
    }

    fun duplicate(id: String) {
        repository.duplicateDrawing(id)
        refresh()
    }

    fun thumbnailFile(id: String): File? = repository.thumbnailFile(id)

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val repo = DrawingRepository(File(application.filesDir, "drawings"))
                    val prefs = AppPrefsStore(File(application.filesDir, "app_prefs"))
                    val isDark = { (application.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES }
                    GalleryViewModel(repo, prefs, isDark)
                }
            }
    }
}
