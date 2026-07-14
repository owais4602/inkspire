package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.stupifranc.inkspire.model.BrushFamilyChoice

@Composable
fun BrushPicker(
    selected: BrushFamilyChoice,
    onSelect: (BrushFamilyChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        BrushFamilyChoice.entries.forEach { family ->
            FilterChip(
                selected = family == selected,
                onClick = { onSelect(family) },
                label = { Text(family.displayName()) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

private fun BrushFamilyChoice.displayName(): String = when (this) {
    BrushFamilyChoice.PRESSURE_PEN -> "Pen"
    BrushFamilyChoice.MARKER -> "Marker"
    BrushFamilyChoice.HIGHLIGHTER -> "Highlighter"
}
