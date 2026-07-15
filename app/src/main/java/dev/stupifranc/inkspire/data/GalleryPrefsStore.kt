package dev.stupifranc.inkspire.data

import dev.stupifranc.inkspire.model.GalleryPrefs
import java.io.File
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val GALLERY_PREFS_FILE_NAME = "gallery_prefs.json"

/** App-wide (not per-drawing) gallery display preferences: thumbnail size, card styling, wall tone. */
class GalleryPrefsStore(private val rootDir: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(rootDir, GALLERY_PREFS_FILE_NAME)

    init {
        rootDir.mkdirs()
    }

    fun load(): GalleryPrefs {
        if (!file.exists()) return GalleryPrefs()
        return try {
            json.decodeFromString<GalleryPrefs>(file.readText())
        } catch (e: SerializationException) {
            GalleryPrefs()
        }
    }

    fun save(prefs: GalleryPrefs) {
        file.writeText(json.encodeToString(prefs))
    }
}
