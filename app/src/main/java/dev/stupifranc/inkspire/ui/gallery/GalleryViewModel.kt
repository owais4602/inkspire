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
import dev.stupifranc.inkspire.data.AppPrefsStore
import dev.stupifranc.inkspire.model.AppPrefs
import dev.stupifranc.inkspire.model.DrawingMeta
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DrawingRepository(File(application.filesDir, "drawings"))
    private val prefsStore = AppPrefsStore(File(application.filesDir, "app_prefs"))

    var drawings by mutableStateOf<List<DrawingMeta>>(emptyList())
        private set

    var prefs by mutableStateOf(AppPrefs())
        private set

    init {
        refresh()
        prefs = prefsStore.load()
    }

    fun updatePrefs(newPrefs: AppPrefs) {
        prefs = newPrefs
        prefsStore.save(newPrefs)
    }

    fun refresh() {
        drawings = repository.listDrawings()
    }

    /** Width/height start at 0 — the editor fills the document to whatever viewport first appears, same as a fresh install. */
    fun createDrawing(): DrawingMeta {
        val isDark = (getApplication<Application>().resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val defaultBg = if (isDark) 0xFF1C1B1F.toInt() else 0xFFFFFFFF.toInt()
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
                initializer { GalleryViewModel(application) }
            }
    }
}
