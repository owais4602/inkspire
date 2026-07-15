package dev.stupifranc.inkspire.ui.gallery

import android.app.Application
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.stupifranc.inkspire.data.DrawingRepository
import dev.stupifranc.inkspire.data.GalleryPrefsStore
import dev.stupifranc.inkspire.model.DrawingMeta
import dev.stupifranc.inkspire.model.GalleryPrefs
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DrawingRepository(File(application.filesDir, "drawings"))
    private val prefsStore = GalleryPrefsStore(File(application.filesDir, "app_prefs"))

    var drawings by mutableStateOf<List<DrawingMeta>>(emptyList())
        private set

    var prefs by mutableStateOf(GalleryPrefs())
        private set

    init {
        refresh()
        prefs = prefsStore.load()
    }

    fun updatePrefs(newPrefs: GalleryPrefs) {
        prefs = newPrefs
        prefsStore.save(newPrefs)
    }

    fun refresh() {
        drawings = repository.listDrawings()
    }

    /** Width/height start at 0 — the editor fills the document to whatever viewport first appears, same as a fresh install. */
    fun createDrawing(): DrawingMeta {
        val meta = repository.createDrawing(name = "Untitled", width = 0f, height = 0f, backgroundColorArgb = Color.WHITE)
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
                initializer { GalleryViewModel(application) }
            }
    }
}
