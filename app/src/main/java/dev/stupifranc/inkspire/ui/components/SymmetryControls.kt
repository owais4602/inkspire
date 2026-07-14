package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SymmetryControls(
    enabled: Boolean,
    sectors: Int,
    mirror: Boolean,
    onToggleEnabled: () -> Unit,
    onSectorsChange: (Int) -> Unit,
    onMirrorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FilterChip(
            selected = enabled,
            onClick = onToggleEnabled,
            label = { Text("Symmetry") },
        )
        if (enabled) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sectors: $sectors", modifier = Modifier.padding(end = 8.dp))
                Slider(
                    value = sectors.toFloat(),
                    onValueChange = { onSectorsChange(it.roundToInt()) },
                    valueRange = 1f..12f,
                    steps = 10,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mirror", modifier = Modifier.padding(end = 8.dp))
                Switch(checked = mirror, onCheckedChange = onMirrorChange)
            }
        }
    }
}
