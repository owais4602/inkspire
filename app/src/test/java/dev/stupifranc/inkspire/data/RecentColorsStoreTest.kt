package dev.stupifranc.inkspire.data

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentColorsStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun store(): RecentColorsStore = RecentColorsStore(tempFolder.root)

    @Test
    fun load_withNoFile_returnsEmptyList() {
        assertThat(store().load()).isEmpty()
    }

    @Test
    fun commit_addsToFrontAndPersistsAcrossInstances() {
        val store = store()
        store.commit(0xFF0000)
        store.commit(0x00FF00)

        val reopened = store()
        assertThat(reopened.load()).containsExactly(0x00FF00, 0xFF0000).inOrder()
    }

    @Test
    fun commit_movesExistingColorToFrontInsteadOfDuplicating() {
        val store = store()
        store.commit(0xFF0000)
        store.commit(0x00FF00)
        store.commit(0xFF0000)

        assertThat(store.load()).containsExactly(0xFF0000, 0x00FF00).inOrder()
    }

    @Test
    fun commit_capsListAtEightEntries() {
        val store = store()
        (0 until 10).forEach { store.commit(it) }

        val result = store.load()
        assertThat(result).hasSize(8)
        assertThat(result.first()).isEqualTo(9)
    }

    @Test
    fun corruptFile_recoversToEmptyListInsteadOfCrashing() {
        val file = File(tempFolder.root, "recent_colors.json")
        file.writeText("not valid json")

        val store = store()

        assertThat(store.load()).isEmpty()
        store.commit(123)
        assertThat(store.load()).containsExactly(123)
    }
}
