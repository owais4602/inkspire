package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.stupifranc.inkspire.model.BackgroundKind
import dev.stupifranc.inkspire.model.CanvasBackground

@Composable
fun CanvasBackgroundPicker(
    background: CanvasBackground?,
    defaultColor: Int,
    onBackgroundChange: (CanvasBackground) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentKind = background?.kind ?: BackgroundKind.FLAT
    val colors = background?.colors?.ifEmpty { listOf(defaultColor) } ?: listOf(defaultColor)
    val angleDegrees = background?.angleDegrees ?: 0f

    var activeSlot by remember { mutableIntStateOf(0) }
    // Ensure activeSlot is valid if we switch from gradient (2 colors) back to flat (1 color)
    if (currentKind == BackgroundKind.FLAT && activeSlot > 0) {
        activeSlot = 0
    }

    Column(modifier = modifier) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = currentKind == BackgroundKind.FLAT,
                onClick = {
                    onBackgroundChange(CanvasBackground(BackgroundKind.FLAT, listOf(colors.first()), angleDegrees))
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Text("Flat")
            }
            SegmentedButton(
                selected = currentKind == BackgroundKind.LINEAR,
                onClick = {
                    val newColors = if (colors.size >= 2) colors else listOf(colors.first(), colors.first())
                    onBackgroundChange(CanvasBackground(BackgroundKind.LINEAR, newColors, angleDegrees))
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Text("Linear")
            }
            SegmentedButton(
                selected = currentKind == BackgroundKind.RADIAL,
                onClick = {
                    val newColors = if (colors.size >= 2) colors else listOf(colors.first(), colors.first())
                    onBackgroundChange(CanvasBackground(BackgroundKind.RADIAL, newColors, angleDegrees))
                },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Text("Radial")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentKind != BackgroundKind.FLAT) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ColorSlot(
                    colorArgb = colors.first(),
                    isActive = activeSlot == 0,
                    onClick = { activeSlot = 0 }
                )
                Spacer(modifier = Modifier.width(16.dp))
                ColorSlot(
                    colorArgb = colors.getOrElse(1) { colors.first() },
                    isActive = activeSlot == 1,
                    onClick = { activeSlot = 1 }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (currentKind == BackgroundKind.LINEAR) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Angle: ${angleDegrees.toInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = angleDegrees,
                    onValueChange = { newAngle ->
                        onBackgroundChange(CanvasBackground(currentKind, colors, newAngle))
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        val activeColor = colors.getOrElse(activeSlot) { colors.first() }
        ColorPicker(
            colorArgb = activeColor,
            recentColors = emptyList(),
            onColorChange = { newColor ->
                val newColors = colors.toMutableList()
                if (activeSlot < newColors.size) {
                    newColors[activeSlot] = newColor
                } else {
                    newColors.add(newColor)
                }
                onBackgroundChange(CanvasBackground(currentKind, newColors, angleDegrees))
            },
            showAlphaSlider = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColorSlot(colorArgb: Int, isActive: Boolean, onClick: () -> Unit) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                drawRect(Color(colorArgb))
            }
        }
    }
}
