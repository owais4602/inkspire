package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

private class RecordingCommand(private val log: MutableList<String>, private val id: String) : UndoableCommand {
    override fun undo() { log.add("undo-$id") }
    override fun redo() { log.add("redo-$id") }
}

class HistoryStackTest {

    @Test
    fun undo_callsCommandUndoAndMovesToRedoStack() {
        val log = mutableListOf<String>()
        val stack = HistoryStack()
        stack.push(RecordingCommand(log, "a"))

        stack.undo()

        assertThat(log).containsExactly("undo-a")
        assertThat(stack.canUndo).isFalse()
        assertThat(stack.canRedo).isTrue()
    }

    @Test
    fun redo_callsCommandRedoAndMovesBackToUndoStack() {
        val log = mutableListOf<String>()
        val stack = HistoryStack()
        stack.push(RecordingCommand(log, "a"))
        stack.undo()

        stack.redo()

        assertThat(log).containsExactly("undo-a", "redo-a").inOrder()
        assertThat(stack.canUndo).isTrue()
        assertThat(stack.canRedo).isFalse()
    }

    @Test
    fun undo_whenEmpty_doesNothing() {
        val stack = HistoryStack()
        stack.undo()
        assertThat(stack.canUndo).isFalse()
    }

    @Test
    fun redo_whenEmpty_doesNothing() {
        val stack = HistoryStack()
        stack.redo()
        assertThat(stack.canRedo).isFalse()
    }

    @Test
    fun pushingAfterUndo_clearsRedoStack() {
        val log = mutableListOf<String>()
        val stack = HistoryStack()
        stack.push(RecordingCommand(log, "a"))
        stack.undo()
        assertThat(stack.canRedo).isTrue()

        stack.push(RecordingCommand(log, "b"))

        assertThat(stack.canRedo).isFalse()
    }

    @Test
    fun undoRedo_multipleCommands_happensInLifoOrder() {
        val log = mutableListOf<String>()
        val stack = HistoryStack()
        stack.push(RecordingCommand(log, "a"))
        stack.push(RecordingCommand(log, "b"))
        stack.push(RecordingCommand(log, "c"))

        stack.undo()
        stack.undo()

        assertThat(log).containsExactly("undo-c", "undo-b").inOrder()
    }

    @Test
    fun boundedSize_dropsOldestCommandWhenExceeded() {
        val log = mutableListOf<String>()
        val stack = HistoryStack(maxSize = 3)
        stack.push(RecordingCommand(log, "1"))
        stack.push(RecordingCommand(log, "2"))
        stack.push(RecordingCommand(log, "3"))
        stack.push(RecordingCommand(log, "4")) // "1" should be dropped

        repeat(4) { stack.undo() } // only 3 are available; the 4th call is a no-op

        assertThat(log).containsExactly("undo-4", "undo-3", "undo-2").inOrder()
        assertThat(stack.canUndo).isFalse()
    }

    @Test
    fun clear_removesAllHistory() {
        val log = mutableListOf<String>()
        val stack = HistoryStack()
        stack.push(RecordingCommand(log, "a"))
        stack.undo()
        stack.push(RecordingCommand(log, "b"))

        stack.clear()

        assertThat(stack.canUndo).isFalse()
        assertThat(stack.canRedo).isFalse()
    }
}
