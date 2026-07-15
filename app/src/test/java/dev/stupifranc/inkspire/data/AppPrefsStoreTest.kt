package dev.stupifranc.inkspire.data

import com.google.common.truth.Truth.assertThat
import dev.stupifranc.inkspire.model.CornerStyle
import dev.stupifranc.inkspire.model.AppPrefs
import dev.stupifranc.inkspire.model.ThumbSize
import dev.stupifranc.inkspire.model.AppTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppPrefsStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun store(): AppPrefsStore = AppPrefsStore(tempFolder.root)

    @Test
    fun load_withNoFile_returnsDefaults() {
        assertThat(store().load()).isEqualTo(AppPrefs())
    }

    @Test
    fun save_roundTripsNonDefaultPrefsAcrossInstances() {
        val prefs = AppPrefs(
            thumbSize = ThumbSize.COMPACT,
            borderEnabled = false,
            cornerStyle = CornerStyle.SQUARE,
            theme = AppTheme.LIGHT,
        )
        store().save(prefs)

        assertThat(store().load()).isEqualTo(prefs)
    }

    @Test
    fun corruptFile_recoversToDefaultsInsteadOfCrashing() {
        val file = File(tempFolder.root, "gallery_prefs.json")
        file.writeText("not valid json")

        val store = store()

        assertThat(store.load()).isEqualTo(AppPrefs())
        store.save(AppPrefs(thumbSize = ThumbSize.COMPACT))
        assertThat(store.load()).isEqualTo(AppPrefs(thumbSize = ThumbSize.COMPACT))
    }

    @Test
    fun truncatedFile_recoversToDefaults() {
        val file = File(tempFolder.root, "gallery_prefs.json")
        file.writeText("""{"thumbSize":"MEDIUM","borderEnabled":tr""")

        assertThat(store().load()).isEqualTo(AppPrefs())
    }

    @Test
    fun unknownEnumValueInStoredJson_recoversToDefaults() {
        val file = File(tempFolder.root, "gallery_prefs.json")
        file.writeText(
            """{"thumbSize":"HUGE","borderEnabled":true,"cornerStyle":"ROUNDED","showCaptions":true,"wall":"DARK"}"""
        )

        assertThat(store().load()).isEqualTo(AppPrefs())
    }
}
