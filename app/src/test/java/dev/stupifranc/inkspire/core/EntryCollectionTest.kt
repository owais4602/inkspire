package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private data class FakeEntry(override val id: String, val tag: String) : Identifiable

class EntryCollectionTest {

    @Test
    fun add_appendsEntriesAndEnablesUndo() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a"), FakeEntry("2", "b")))

        assertThat(collection.entries.map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(collection.canUndo).isTrue()
    }

    @Test
    fun undo_afterAdd_removesWholeGroup() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a")))
        collection.add(listOf(FakeEntry("2", "b"), FakeEntry("3", "b")))

        collection.undo()

        assertThat(collection.entries.map { it.id }).containsExactly("1")
    }

    @Test
    fun redo_afterUndoAdd_restoresGroup() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a")))
        collection.undo()

        collection.redo()

        assertThat(collection.entries.map { it.id }).containsExactly("1")
    }

    @Test
    fun erase_removesOnlyMatchingIds() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a"), FakeEntry("2", "b"), FakeEntry("3", "c")))

        val removed = collection.erase(setOf("2"))

        assertThat(removed.map { it.id }).containsExactly("2")
        assertThat(collection.entries.map { it.id }).containsExactly("1", "3").inOrder()
    }

    @Test
    fun erase_withNoMatches_isNoOpAndDoesNotPollutedHistory() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a")))

        val removed = collection.erase(setOf("does-not-exist"))

        assertThat(removed).isEmpty()
        assertThat(collection.entries.map { it.id }).containsExactly("1")
        // erase was a no-op, so a single undo should undo the original add, not a phantom erase
        collection.undo()
        assertThat(collection.entries).isEmpty()
    }

    @Test
    fun undo_afterErase_restoresErasedEntries() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a"), FakeEntry("2", "b")))
        collection.erase(setOf("1"))

        collection.undo()

        assertThat(collection.entries.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun redo_afterUndoErase_reappliesErase() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a"), FakeEntry("2", "b")))
        collection.erase(setOf("1"))
        collection.undo()

        collection.redo()

        assertThat(collection.entries.map { it.id }).containsExactly("2")
    }

    @Test
    fun clear_removesEverythingAndIsUndoable() {
        val collection = EntryCollection<FakeEntry>()
        collection.add(listOf(FakeEntry("1", "a"), FakeEntry("2", "b")))

        collection.clear()
        assertThat(collection.entries).isEmpty()

        collection.undo()
        assertThat(collection.entries.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun clear_onEmptyCollection_isNoOp() {
        val collection = EntryCollection<FakeEntry>()

        collection.clear()

        assertThat(collection.canUndo).isFalse()
    }
}
