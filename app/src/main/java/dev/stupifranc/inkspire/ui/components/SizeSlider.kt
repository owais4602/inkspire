package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SizeSlider(
    size: Float,
    range: ClosedFloatingPointRange<Float>,
    onSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Size", modifier = Modifier.padding(end = 8.dp))
        Slider(
            value = size,
            onValueChange = onSizeChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
    }
}
