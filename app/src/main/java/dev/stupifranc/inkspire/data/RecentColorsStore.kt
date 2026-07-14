package dev.stupifranc.inkspire.data

import java.io.File
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val RECENT_COLORS_FILE_NAME = "recent_colors.json"
private const val MAX_RECENT_COLORS = 8

/** App-wide (not per-drawing) most-recently-used brush colors, most recent first. */
class RecentColorsStore(private val rootDir: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(rootDir, RECENT_COLORS_FILE_NAME)

    init {
        rootDir.mkdirs()
    }

    fun load(): List<Int> {
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Int>>(file.readText())
        } catch (e: SerializationException) {
            emptyList()
        }
    }

    fun commit(colorArgb: Int): List<Int> {
        val updated = (listOf(colorArgb) + load().filterNot { it == colorArgb }).take(MAX_RECENT_COLORS)
        file.writeText(json.encodeToString(updated))
        return updated
    }
}
