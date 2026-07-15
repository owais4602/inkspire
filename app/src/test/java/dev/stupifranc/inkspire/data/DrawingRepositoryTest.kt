package dev.stupifranc.inkspire.data

import com.google.common.truth.Truth.assertThat
import dev.stupifranc.inkspire.model.DEFAULT_PAPER_SPACING
import dev.stupifranc.inkspire.model.PaperStyle
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DrawingRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun repository(): DrawingRepository = DrawingRepository(tempFolder.root)

    @Test
    fun createDrawing_appearsInListDrawings() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Untitled", width = 1000f, height = 800f, backgroundColorArgb = -1)

        val listed = repo.listDrawings()
        assertThat(listed).hasSize(1)
        assertThat(listed.single().id).isEqualTo(meta.id)
        assertThat(listed.single().name).isEqualTo("Untitled")
    }

    @Test
    fun indexSurvivesRepositoryRestart() {
        val meta = repository().createDrawing(name = "Sketch", width = 500f, height = 500f, backgroundColorArgb = 0)

        val reopened = repository()
        assertThat(reopened.listDrawings().map { it.id }).containsExactly(meta.id)
    }

    @Test
    fun saveAndLoadStrokes_roundTrips() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Doodle", width = 100f, height = 100f, backgroundColorArgb = 0)
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        repo.saveStrokes(meta.id, bytes)

        assertThat(repo.loadStrokes(meta.id)).isEqualTo(bytes)
    }

    @Test
    fun loadStrokes_returnsNullWhenNeverSaved() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Empty", width = 100f, height = 100f, backgroundColorArgb = 0)

        assertThat(repo.loadStrokes(meta.id)).isNull()
    }

    @Test
    fun renameDrawing_updatesNameAndBumpsUpdatedAt() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Old name", width = 100f, height = 100f, backgroundColorArgb = 0)

        repo.renameDrawing(meta.id, "New name")

        val renamed = repo.listDrawings().single()
        assertThat(renamed.name).isEqualTo("New name")
        assertThat(renamed.updatedAtEpochMillis).isAtLeast(meta.updatedAtEpochMillis)
    }

    @Test
    fun deleteDrawing_removesFromIndexAndDeletesFiles() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Gone soon", width = 100f, height = 100f, backgroundColorArgb = 0)
        repo.saveStrokes(meta.id, byteArrayOf(9))

        repo.deleteDrawing(meta.id)

        assertThat(repo.listDrawings()).isEmpty()
        assertThat(File(tempFolder.root, meta.id).exists()).isFalse()
    }

    @Test
    fun duplicateDrawing_copiesStrokesAsIndependentDrawing() {
        val repo = repository()
        val original = repo.createDrawing(name = "Original", width = 100f, height = 100f, backgroundColorArgb = 0)
        val bytes = byteArrayOf(7, 7, 7)
        repo.saveStrokes(original.id, bytes)

        val copy = repo.duplicateDrawing(original.id)

        assertThat(copy).isNotNull()
        assertThat(copy!!.id).isNotEqualTo(original.id)
        assertThat(repo.loadStrokes(copy.id)).isEqualTo(bytes)
        assertThat(repo.listDrawings()).hasSize(2)
    }

    @Test
    fun duplicateDrawing_missingSource_returnsNull() {
        val repo = repository()
        assertThat(repo.duplicateDrawing("does-not-exist")).isNull()
    }

    @Test
    fun corruptIndexFile_recoversToEmptyListInsteadOfCrashing() {
        val indexFile = File(tempFolder.root, "index.json")
        indexFile.writeText("{ not valid json ][")

        val repo = repository()

        assertThat(repo.listDrawings()).isEmpty()
        // Repository should still be usable after recovering from corruption, not permanently wedged.
        val meta = repo.createDrawing(name = "Fresh start", width = 100f, height = 100f, backgroundColorArgb = 0)
        assertThat(repo.listDrawings().map { it.id }).containsExactly(meta.id)
    }

    @Test
    fun updateCanvasSpec_persistsSizeAndBackground() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Resizable", width = 100f, height = 100f, backgroundColorArgb = 0)

        repo.updateCanvasSpec(meta.id, width = 300f, height = 400f, backgroundColorArgb = -16711936)

        val updated = repo.listDrawings().single()
        assertThat(updated.width).isEqualTo(300f)
        assertThat(updated.height).isEqualTo(400f)
        assertThat(updated.backgroundColorArgb).isEqualTo(-16711936)
    }

    @Test
    fun updateCanvasSpec_persistsPaperStyleAndSpacing() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Dotted", width = 100f, height = 100f, backgroundColorArgb = -1)

        repo.updateCanvasSpec(
            meta.id, width = 100f, height = 100f, backgroundColorArgb = -1,
            paperStyle = PaperStyle.DOTS, paperSpacing = 48f,
        )

        val updated = repository().listDrawings().single()
        assertThat(updated.paperStyle).isEqualTo(PaperStyle.DOTS)
        assertThat(updated.paperSpacing).isEqualTo(48f)
    }

    @Test
    fun index_withoutPaperFields_loadsWithPlainDefaults() {
        // An index written before the paper-style fields existed must still parse (forward compat).
        val repo = repository()
        File(tempFolder.root, "index.json").writeText(
            """[{"id":"legacy","name":"Old","width":100.0,"height":100.0,"backgroundColorArgb":-1,""" +
                """"createdAtEpochMillis":1,"updatedAtEpochMillis":1}]"""
        )

        val listed = repo.listDrawings().single()
        assertThat(listed.paperStyle).isEqualTo(PaperStyle.PLAIN)
        assertThat(listed.paperSpacing).isEqualTo(DEFAULT_PAPER_SPACING)
    }

    @Test
    fun saveThumbnail_marksHasThumbnailAndFileIsReadable() {
        val repo = repository()
        val meta = repo.createDrawing(name = "Thumb", width = 100f, height = 100f, backgroundColorArgb = 0)

        repo.saveThumbnail(meta.id, byteArrayOf(1, 2, 3))

        assertThat(repo.listDrawings().single().hasThumbnail).isTrue()
        assertThat(repo.thumbnailFile(meta.id)?.readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
    }
}
