package dev.stupifranc.inkspire.core

interface UndoableCommand {
    fun undo()
    fun redo()
}

class HistoryStack(private val maxSize: Int = 100) {
    private val undoStack = ArrayDeque<UndoableCommand>()
    private val redoStack = ArrayDeque<UndoableCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun push(command: UndoableCommand) {
        undoStack.addLast(command)
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        val command = undoStack.removeLastOrNull() ?: return
        command.undo()
        redoStack.addLast(command)
    }

    fun redo() {
        val command = redoStack.removeLastOrNull() ?: return
        command.redo()
        undoStack.addLast(command)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    /** Visits every pending command (both stacks) — used to remap commands' captured entry snapshots. */
    fun forEachCommand(action: (UndoableCommand) -> Unit) {
        undoStack.forEach(action)
        redoStack.forEach(action)
    }
}
