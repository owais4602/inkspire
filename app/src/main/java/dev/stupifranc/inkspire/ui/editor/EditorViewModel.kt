package dev.stupifranc.inkspire.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.strokes.Stroke
import androidx.lifecycle.ViewModel
import dev.stupifranc.inkspire.core.HistoryStack
import dev.stupifranc.inkspire.core.UndoableCommand
import dev.stupifranc.inkspire.model.StrokeEntry
import java.util.UUID

class EditorViewModel : ViewModel() {
    private val history = HistoryStack()

    var strokes by mutableStateOf<List<StrokeEntry>>(emptyList())
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun onStrokesFinished(finished: List<Stroke>) {
        if (finished.isEmpty()) return
        val groupId = UUID.randomUUID().toString()
        val entries = finished.map { StrokeEntry(groupId = groupId, stroke = it) }
        strokes = strokes + entries
        history.push(AddStrokesCommand(entries))
        syncFlags()
    }

    fun clear() {
        if (strokes.isEmpty()) return
        val previous = strokes
        strokes = emptyList()
        history.push(ClearCommand(previous))
        syncFlags()
    }

    fun undo() {
        history.undo()
        syncFlags()
    }

    fun redo() {
        history.redo()
        syncFlags()
    }

    private fun syncFlags() {
        canUndo = history.canUndo
        canRedo = history.canRedo
    }

    private inner class AddStrokesCommand(private val entries: List<StrokeEntry>) : UndoableCommand {
        override fun undo() {
            val entryIds = entries.map { it.id }.toSet()
            strokes = strokes.filterNot { it.id in entryIds }
        }

        override fun redo() {
            strokes = strokes + entries
        }
    }

    private inner class ClearCommand(private val previous: List<StrokeEntry>) : UndoableCommand {
        override fun undo() {
            strokes = previous
        }

        override fun redo() {
            strokes = emptyList()
        }
    }
}
