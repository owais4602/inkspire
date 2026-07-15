package dev.stupifranc.inkspire.data

import dev.stupifranc.inkspire.model.AppPrefs
import java.io.File
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists app-wide preferences.
 * Modeled on RecentColorsStore: load-or-defaults, atomic-ish write, swallow-and-default on corrupt JSON.
 */
class AppPrefsStore(private val rootDir: File) {
    private val file = File(rootDir, "app_prefs.json")
    private val tempFile = File(rootDir, "app_prefs.json.tmp")
    private val json = Json { ignoreUnknownKeys = true }

    init {
        rootDir.mkdirs()
    }

    fun load(): AppPrefs {
        if (!file.exists()) return AppPrefs()
        return try {
            json.decodeFromString<AppPrefs>(file.readText())
        } catch (e: SerializationException) {
            AppPrefs()
        }
    }

    fun save(prefs: AppPrefs) {
        file.writeText(json.encodeToString(prefs))
    }
}
