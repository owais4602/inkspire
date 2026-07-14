package dev.stupifranc.inkspire.ui.editor

import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stupifranc.inkspire.ink.DrawingSurface

@Composable
fun EditorScreen(viewModel: EditorViewModel = viewModel()) {
    val defaultBrush = remember {
        Brush.createWithColorIntArgb(StockBrushes.pressurePen(), Color.BLACK, 8f, 0.1f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            TextButton(onClick = viewModel::undo, enabled = viewModel.canUndo) {
                Text("Undo")
            }
            TextButton(onClick = viewModel::redo, enabled = viewModel.canRedo) {
                Text("Redo")
            }
            TextButton(onClick = viewModel::clear) {
                Text("Clear")
            }
        }
        DrawingSurface(
            strokes = viewModel.strokes,
            currentBrush = defaultBrush,
            onStrokesFinished = viewModel::onStrokesFinished,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}
