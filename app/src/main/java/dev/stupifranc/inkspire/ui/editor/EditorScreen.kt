package dev.stupifranc.inkspire.ui.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stupifranc.inkspire.ink.CanvasExporter
import dev.stupifranc.inkspire.ink.DrawingSurface
import dev.stupifranc.inkspire.ink.toInkBrush
import dev.stupifranc.inkspire.model.Tool
import dev.stupifranc.inkspire.ui.components.CanvasResizeOverlay
import dev.stupifranc.inkspire.ui.components.ColorPicker
import dev.stupifranc.inkspire.ui.components.HistoryPill
import dev.stupifranc.inkspire.ui.components.ToolDock
import kotlin.math.roundToInt

@Composable
fun EditorScreen(viewModel: EditorViewModel = viewModel()) {
    var showColorPicker by remember { mutableStateOf(false) }
    var isResizeMode by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportScale by remember { mutableStateOf<Int?>(null) }
    val inkBrush = remember(viewModel.brushSpec) { viewModel.brushSpec.toInkBrush() }
    val context = LocalContext.current

    fun exportAndShare(scale: Int) {
        val bitmap = CanvasExporter.renderBitmap(viewModel.canvasSpec, viewModel.strokes, scale)
        val uri = CanvasExporter.saveToGallery(context, bitmap) ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share drawing"))
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val scale = pendingExportScale
        pendingExportScale = null
        if (granted && scale != null) exportAndShare(scale)
    }

    fun requestExport(scale: Int) {
        showExportDialog = false
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingExportScale = scale
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            exportAndShare(scale)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DrawingSurface(
            strokes = viewModel.strokes,
            tool = viewModel.tool,
            currentBrush = inkBrush,
            eraserPaddingPx = viewModel.brushSpec.size,
            symmetryConfig = viewModel.symmetryConfig,
            canvasSpec = viewModel.canvasSpec,
            viewport = viewModel.viewport,
            onStrokesFinished = viewModel::onStrokesFinished,
            onErase = viewModel::eraseHits,
            onContainerSizeChanged = viewModel::onContainerSizeChanged,
            onSymmetryCenterChanged = viewModel::moveSymmetryCenter,
            onPan = viewModel::panBy,
            onZoom = viewModel::zoomBy,
            onDoubleTapFit = viewModel::fitToScreen,
            modifier = Modifier.fillMaxSize(),
        )

        if (isResizeMode) {
            CanvasResizeOverlay(
                canvasWidth = viewModel.canvasSpec.width,
                canvasHeight = viewModel.canvasSpec.height,
                viewport = viewModel.viewport,
                onConfirm = { width, height, anchor ->
                    viewModel.resizeCanvas(width, height, anchor)
                    isResizeMode = false
                },
                onCancel = { isResizeMode = false },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            HistoryPill(
                canUndo = viewModel.canUndo,
                canRedo = viewModel.canRedo,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onClear = viewModel::clear,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Text(
                    "${(viewModel.viewport.scale * 100).roundToInt()}%",
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            ToolDock(
                tool = viewModel.tool,
                brushFamily = viewModel.brushSpec.family,
                colorArgb = viewModel.brushSpec.colorArgb,
                size = viewModel.brushSpec.size,
                sizeRange = 2f..48f,
                symmetryEnabled = viewModel.symmetryEnabled,
                symmetrySectors = viewModel.symmetrySectors,
                symmetryMirror = viewModel.symmetryMirror,
                onSelectBrush = { family ->
                    viewModel.selectBrushFamily(family)
                    viewModel.selectTool(Tool.PEN)
                },
                onSelectEraser = { viewModel.selectTool(Tool.ERASER) },
                onSizeChange = viewModel::setSize,
                onToggleSymmetry = viewModel::toggleSymmetryEnabled,
                onSymmetrySectorsChange = viewModel::changeSymmetrySectors,
                onSymmetryMirrorChange = viewModel::changeSymmetryMirror,
                onColorClick = { showColorPicker = true },
                onResize = { isResizeMode = true },
                onExport = { showExportDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export as image") },
            text = { Text("Renders the entire canvas, not just what's visible on screen.") },
            confirmButton = {
                Row {
                    TextButton(onClick = { requestExport(1) }) { Text("1x") }
                    TextButton(onClick = { requestExport(2) }) { Text("2x") }
                    TextButton(onClick = { requestExport(4) }) { Text("4x") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            },
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
