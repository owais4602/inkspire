package dev.stupifranc.inkspire.core

interface Identifiable {
    val id: String
}

/** Commands whose captured entry snapshots can be remapped in place (e.g. on canvas resize/translate). */
interface TransformableCommand<T> {
    fun transformEntries(mapper: (T) -> T)
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
     * Deliberately bypasses undo history as a *user-visible* action, but still remaps the entry
     * snapshots captured inside any pending undo/redo commands — otherwise undoing/redoing a
     * command pushed before a transform would resurrect stale, untranslated coordinates.
     */
    fun transformAll(transform: (T) -> T) {
        entries = entries.map(transform)
        history.forEachCommand { command ->
            @Suppress("UNCHECKED_CAST")
            (command as? TransformableCommand<T>)?.transformEntries(transform)
        }
    }

    fun undo() = history.undo()
    fun redo() = history.redo()

    private inner class AddCommand(private var added: List<T>) : UndoableCommand, TransformableCommand<T> {
        private val addedIds = added.map { it.id }.toSet()
        override fun undo() { entries = entries.filterNot { it.id in addedIds } }
        override fun redo() { entries = entries + added }
        override fun transformEntries(mapper: (T) -> T) { added = added.map(mapper) }
    }

    private inner class EraseCommand(private var previous: List<T>, private var removed: List<T>) : UndoableCommand, TransformableCommand<T> {
        override fun undo() { entries = previous }
        override fun redo() {
            val removedIds = removed.map { it.id }.toSet()
            entries = previous.filterNot { it.id in removedIds }
        }
        override fun transformEntries(mapper: (T) -> T) {
            previous = previous.map(mapper)
            removed = removed.map(mapper)
        }
    }

    private inner class ClearCommand(private var previous: List<T>) : UndoableCommand, TransformableCommand<T> {
        override fun undo() { entries = previous }
        override fun redo() { entries = emptyList() }
        override fun transformEntries(mapper: (T) -> T) { previous = previous.map(mapper) }
    }
}
