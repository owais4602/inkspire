package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.stupifranc.inkspire.ui.components.icons.ClearIcon
import dev.stupifranc.inkspire.ui.components.icons.RedoIcon
import dev.stupifranc.inkspire.ui.components.icons.UndoIcon

/**
 * Floating top-left pill for history actions — undo is the most-used action in a drawing app,
 * so it lives one tap away instead of behind the dock's overflow.
 */
@Composable
fun HistoryPill(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockIconButton(selected = false, enabled = canUndo, onClick = onUndo) { tint -> UndoIcon(tint) }
            DockIconButton(selected = false, enabled = canRedo, onClick = onRedo) { tint -> RedoIcon(tint) }
            DockDivider()
            DockIconButton(selected = false, onClick = onClear) { tint -> ClearIcon(tint) }
        }
    }
}
