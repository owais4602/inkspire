package dev.stupifranc.inkspire.core

interface Identifiable {
    val id: String
}

/** Generic add/erase/clear/undo collection, kept pure Kotlin so it's JVM-testable independent of what [T] actually is. */
class EntryCollection<T : Identifiable> {
    private val history = HistoryStack()

    var entries: List<T> = emptyList()
        private set

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo

    /**
     * Replaces every entry and resets undo/redo history (document load, not a user action —
     * there is nothing meaningful to undo "back to" before a drawing was opened).
     */
    fun load(initial: List<T>) {
        entries = initial
        history.clear()
    }

    fun add(newEntries: List<T>) {
        if (newEntries.isEmpty()) return
        entries = entries + newEntries
        history.push(AddCommand(newEntries))
    }

    fun erase(hitIds: Set<String>): List<T> {
        val previous = entries
        val removed = previous.filter { it.id in hitIds }
        if (removed.isEmpty()) return emptyList()
        entries = previous.filterNot { it.id in hitIds }
        history.push(EraseCommand(previous, removed))
        return removed
    }

    fun clear() {
        if (entries.isEmpty()) return
        val previous = entries
        entries = emptyList()
        history.push(ClearCommand(previous))
    }

    /**
     * Remaps every entry in place (e.g. translating stroke coordinates on canvas resize).
     * Deliberately bypasses undo history: it's a structural document change, not an editable action.
     */
    fun transformAll(transform: (T) -> T) {
        entries = entries.map(transform)
    }

    fun undo() = history.undo()
    fun redo() = history.redo()

    private inner class AddCommand(private val added: List<T>) : UndoableCommand {
        private val addedIds = added.map { it.id }.toSet()
        override fun undo() { entries = entries.filterNot { it.id in addedIds } }
        override fun redo() { entries = entries + added }
    }

    private inner class EraseCommand(private val previous: List<T>, private val removed: List<T>) : UndoableCommand {
        override fun undo() { entries = previous }
        override fun redo() {
            val removedIds = removed.map { it.id }.toSet()
            entries = previous.filterNot { it.id in removedIds }
        }
    }

    private inner class ClearCommand(private val previous: List<T>) : UndoableCommand {
        override fun undo() { entries = previous }
        override fun redo() { entries = emptyList() }
    }
}
