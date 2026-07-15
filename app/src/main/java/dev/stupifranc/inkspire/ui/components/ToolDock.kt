package dev.stupifranc.inkspire.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.Tool
import dev.stupifranc.inkspire.ui.components.icons.EraserIcon
import dev.stupifranc.inkspire.ui.components.icons.ExportIcon
import dev.stupifranc.inkspire.ui.components.icons.HandIcon
import dev.stupifranc.inkspire.ui.components.icons.HighlighterIcon
import dev.stupifranc.inkspire.ui.components.icons.MarkerIcon
import dev.stupifranc.inkspire.ui.components.icons.MoreIcon
import dev.stupifranc.inkspire.ui.components.icons.PenIcon
import dev.stupifranc.inkspire.ui.components.icons.ResizeIcon
import dev.stupifranc.inkspire.ui.components.icons.SymmetryIcon
import kotlin.math.roundToInt

private enum class DockExpansion { NONE, SIZE, SYMMETRY, MORE }

/** Compact, icon-first floating dock (Apple Notes markup-toolbar style) — replaces the old stacked text-button rows. */
@Composable
fun ToolDock(
    tool: Tool,
    brushFamily: BrushFamilyChoice,
    colorArgb: Int,
    size: Float,
    sizeRange: ClosedFloatingPointRange<Float>,
    symmetryEnabled: Boolean,
    symmetrySectors: Int,
    symmetryMirror: Boolean,
    awaitingCenterPlacement: Boolean,
    canvasColorArgb: Int,
    collapseSignal: Int,
    onSelectBrush: (BrushFamilyChoice) -> Unit,
    onSelectEraser: () -> Unit,
    onSelectHand: () -> Unit,
    onSizeChange: (Float) -> Unit,
    onToggleSymmetry: () -> Unit,
    onSymmetrySectorsChange: (Int) -> Unit,
    onSymmetryMirrorChange: (Boolean) -> Unit,
    onToggleCenterPlacement: () -> Unit,
    onResetCenter: () -> Unit,
    onColorClick: () -> Unit,
    onResize: () -> Unit,
    onExport: () -> Unit,
    onCanvasColorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expansion by remember { mutableStateOf(DockExpansion.NONE) }

    // A touch reaching the canvas collapses whatever panel is open (the panel's own touches never
    // trigger this — the canvas touch handler is a separate composable behind the dock).
    LaunchedEffect(collapseSignal) { expansion = DockExpansion.NONE }

    fun toggle(target: DockExpansion) {
        expansion = if (expansion == target) DockExpansion.NONE else target
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = expansion != DockExpansion.NONE,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    when (expansion) {
                        DockExpansion.SIZE -> SizeSlider(
                            size = size,
                            range = sizeRange,
                            onSizeChange = onSizeChange,
                            modifier = Modifier.width(240.dp),
                        )
                        DockExpansion.SYMMETRY -> SymmetryPanel(
                            sectors = symmetrySectors,
                            mirror = symmetryMirror,
                            awaitingCenterPlacement = awaitingCenterPlacement,
                            onSectorsChange = onSymmetrySectorsChange,
                            onMirrorChange = onSymmetryMirrorChange,
                            onToggleCenterPlacement = onToggleCenterPlacement,
                            onResetCenter = onResetCenter,
                        )
                        DockExpansion.MORE -> MorePanel(
                            canvasColorArgb = canvasColorArgb,
                            onExport = onExport,
                            onCanvasColorClick = onCanvasColorClick,
                        )
                        DockExpansion.NONE -> Unit
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockIconButton(selected = tool == Tool.PEN && brushFamily == BrushFamilyChoice.PRESSURE_PEN, onClick = {
                    if (tool == Tool.PEN && brushFamily == BrushFamilyChoice.PRESSURE_PEN) toggle(DockExpansion.SIZE)
                    else { onSelectBrush(BrushFamilyChoice.PRESSURE_PEN); expansion = DockExpansion.NONE }
                }) { tint -> PenIcon(tint) }

                DockIconButton(selected = tool == Tool.PEN && brushFamily == BrushFamilyChoice.MARKER, onClick = {
                    if (tool == Tool.PEN && brushFamily == BrushFamilyChoice.MARKER) toggle(DockExpansion.SIZE)
                    else { onSelectBrush(BrushFamilyChoice.MARKER); expansion = DockExpansion.NONE }
                }) { tint -> MarkerIcon(tint) }

                DockIconButton(selected = tool == Tool.PEN && brushFamily == BrushFamilyChoice.HIGHLIGHTER, onClick = {
                    if (tool == Tool.PEN && brushFamily == BrushFamilyChoice.HIGHLIGHTER) toggle(DockExpansion.SIZE)
                    else { onSelectBrush(BrushFamilyChoice.HIGHLIGHTER); expansion = DockExpansion.NONE }
                }) { tint -> HighlighterIcon(tint) }

                DockIconButton(selected = tool == Tool.ERASER, onClick = {
                    if (tool == Tool.ERASER) toggle(DockExpansion.SIZE)
                    else { onSelectEraser(); expansion = DockExpansion.NONE }
                }) { tint -> EraserIcon(tint) }

                DockIconButton(selected = tool == Tool.PAN, onClick = {
                    onSelectHand(); expansion = DockExpansion.NONE
                }) { tint -> HandIcon(tint) }

                DockDivider()

                DockIconButton(selected = symmetryEnabled, onClick = {
                    val turningOn = !symmetryEnabled
                    onToggleSymmetry()
                    expansion = if (turningOn) DockExpansion.SYMMETRY else DockExpansion.NONE
                }) { tint -> SymmetryIcon(tint) }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(colorArgb))
                        .clickable(onClick = onColorClick),
                )

                DockIconButton(selected = false, onClick = onResize) { tint -> ResizeIcon(tint) }

                DockDivider()

                DockIconButton(selected = expansion == DockExpansion.MORE, onClick = { toggle(DockExpansion.MORE) }) { tint -> MoreIcon(tint) }
            }
        }
    }
}

@Composable
private fun SymmetryPanel(
    sectors: Int,
    mirror: Boolean,
    awaitingCenterPlacement: Boolean,
    onSectorsChange: (Int) -> Unit,
    onMirrorChange: (Boolean) -> Unit,
    onToggleCenterPlacement: () -> Unit,
    onResetCenter: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sectors: $sectors", modifier = Modifier.padding(end = 8.dp))
            Slider(
                value = sectors.toFloat(),
                onValueChange = { onSectorsChange(it.roundToInt()) },
                valueRange = 1f..12f,
                steps = 10,
                modifier = Modifier.width(160.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mirror", modifier = Modifier.padding(end = 8.dp))
            Switch(checked = mirror, onCheckedChange = onMirrorChange)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggleCenterPlacement) {
                Text(if (awaitingCenterPlacement) "Tap canvas to set center…" else "Set center")
            }
            TextButton(onClick = onResetCenter) { Text("Center") }
        }
    }
}

@Composable
private fun MorePanel(
    canvasColorArgb: Int,
    onExport: () -> Unit,
    onCanvasColorClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DockIconButton(selected = false, onClick = onExport) { tint -> ExportIcon(tint) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 4.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(canvasColorArgb))
                    .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onCanvasColorClick),
            )
            Text("Canvas", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
