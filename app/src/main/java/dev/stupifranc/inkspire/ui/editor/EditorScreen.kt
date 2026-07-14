package dev.stupifranc.inkspire.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stupifranc.inkspire.ink.DrawingSurface
import dev.stupifranc.inkspire.ink.toInkBrush
import dev.stupifranc.inkspire.model.Tool
import dev.stupifranc.inkspire.ui.components.BrushPicker
import dev.stupifranc.inkspire.ui.components.ColorPicker
import dev.stupifranc.inkspire.ui.components.SizeSlider

@Composable
fun EditorScreen(viewModel: EditorViewModel = viewModel()) {
    var showColorPicker by remember { mutableStateOf(false) }
    val inkBrush = remember(viewModel.brushSpec) { viewModel.brushSpec.toInkBrush() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            TextButton(onClick = viewModel::undo, enabled = viewModel.canUndo) { Text("Undo") }
            TextButton(onClick = viewModel::redo, enabled = viewModel.canRedo) { Text("Redo") }
            TextButton(onClick = viewModel::clear) { Text("Clear") }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            FilterChip(
                selected = viewModel.tool == Tool.PEN,
                onClick = { viewModel.selectTool(Tool.PEN) },
                label = { Text("Pen") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = viewModel.tool == Tool.ERASER,
                onClick = { viewModel.selectTool(Tool.ERASER) },
                label = { Text("Eraser") },
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        if (viewModel.tool == Tool.PEN) {
            BrushPicker(
                selected = viewModel.brushSpec.family,
                onSelect = viewModel::selectBrushFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            SizeSlider(
                size = viewModel.brushSpec.size,
                range = 2f..48f,
                onSizeChange = viewModel::setSize,
                modifier = Modifier.weight(1f),
            )
            if (viewModel.tool == Tool.PEN) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(viewModel.brushSpec.colorArgb))
                        .clickable { showColorPicker = true },
                )
            }
        }

        DrawingSurface(
            strokes = viewModel.strokes,
            tool = viewModel.tool,
            currentBrush = inkBrush,
            eraserPaddingPx = viewModel.brushSpec.size,
            onStrokesFinished = viewModel::onStrokesFinished,
            onErase = viewModel::eraseHits,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = {
                viewModel.commitCurrentColorToRecents()
                showColorPicker = false
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.commitCurrentColorToRecents()
                    showColorPicker = false
                }) { Text("Done") }
            },
            text = {
                ColorPicker(
                    colorArgb = viewModel.brushSpec.colorArgb,
                    recentColors = viewModel.recentColors,
                    onColorChange = viewModel::setColor,
                )
            },
        )
    }
}
