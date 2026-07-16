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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
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
import dev.stupifranc.inkspire.core.CanvasEdge
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.PaperStyle
import dev.stupifranc.inkspire.model.Tool
import dev.stupifranc.inkspire.ui.components.icons.ArrowDownIcon
import dev.stupifranc.inkspire.ui.components.icons.ArrowLeftIcon
import dev.stupifranc.inkspire.ui.components.icons.ArrowRightIcon
import dev.stupifranc.inkspire.ui.components.icons.ArrowUpIcon
import dev.stupifranc.inkspire.ui.components.icons.CenterTargetIcon
import dev.stupifranc.inkspire.ui.components.icons.CropIcon
import dev.stupifranc.inkspire.ui.components.icons.EraserIcon
import dev.stupifranc.inkspire.ui.components.icons.ExportIcon
import dev.stupifranc.inkspire.ui.components.icons.StylusIcon
import dev.stupifranc.inkspire.ui.components.icons.HandIcon
import dev.stupifranc.inkspire.ui.components.icons.HighlighterIcon
import dev.stupifranc.inkspire.ui.components.icons.MarkerIcon
import dev.stupifranc.inkspire.ui.components.icons.PenIcon
import dev.stupifranc.inkspire.ui.components.icons.MediaBrushIcon
import dev.stupifranc.inkspire.ui.components.icons.MirrorIcon
import dev.stupifranc.inkspire.ui.components.icons.MoreIcon
import dev.stupifranc.inkspire.ui.components.icons.PaperDotsIcon
import dev.stupifranc.inkspire.ui.components.icons.PaperGridIcon
import dev.stupifranc.inkspire.ui.components.icons.PaperIsoIcon
import dev.stupifranc.inkspire.ui.components.icons.PaperPlainIcon
import dev.stupifranc.inkspire.ui.components.icons.PaperRuledIcon
import dev.stupifranc.inkspire.ui.components.icons.PenIcon
import dev.stupifranc.inkspire.ui.components.icons.ResetIcon
import dev.stupifranc.inkspire.ui.components.icons.ResizeIcon
import dev.stupifranc.inkspire.ui.components.icons.SymmetryIcon
import dev.stupifranc.inkspire.ui.components.icons.TuneIcon
import kotlin.math.roundToInt

private enum class DockExpansion { NONE, SIZE, SYMMETRY, CANVAS, MORE, BRUSHES }

/** Compact, icon-first floating dock (Apple Notes markup-toolbar style) — replaces the old stacked text-button rows. */
@Composable
fun ToolDock(
    tool: Tool,
    brushFamily: BrushFamilyChoice,
    recentMediaBrush: BrushFamilyChoice,
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
    onGrowEdge: (CanvasEdge) -> Unit,
    onExport: () -> Unit,
    rotationEnabled: Boolean,
    onToggleRotation: () -> Unit,
    stylusOnly: Boolean,
    onStylusOnlyChange: (Boolean) -> Unit,
    onCanvasColorClick: () -> Unit,
    paperStyle: PaperStyle,
    paperSpacing: Float,
    onPaperStyleChange: (PaperStyle) -> Unit,
    onPaperSpacingChange: (Float) -> Unit,
    canvasShape: dev.stupifranc.inkspire.model.CanvasShape,
    onCanvasShapeChange: (dev.stupifranc.inkspire.model.CanvasShape) -> Unit,
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
                        DockExpansion.BRUSHES -> BrushesPanel(
                            selectedBrush = brushFamily,
                            size = size,
                            range = sizeRange,
                            onSelectBrush = { family ->
                                onSelectBrush(family)
                                expansion = DockExpansion.NONE
                            },
                            onSizeChange = onSizeChange,
                        )
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
                        DockExpansion.CANVAS -> CanvasSizePanel(
                            canvasShape = canvasShape,
                            onShapeChange = onCanvasShapeChange,
                            onGrowEdge = onGrowEdge,
                            onResize = {
                                expansion = DockExpansion.NONE
                                onResize()
                            },
                        )
                        DockExpansion.MORE -> MorePanel(
                            canvasColorArgb = canvasColorArgb,
                            paperStyle = paperStyle,
                            paperSpacing = paperSpacing,
                            rotationEnabled = rotationEnabled,
                            onToggleRotation = onToggleRotation,
                            stylusOnly = stylusOnly,
                            onExport = { toggle(DockExpansion.NONE); onExport() },
                            onStylusOnlyChange = onStylusOnlyChange,
                            onCanvasColorClick = { toggle(DockExpansion.NONE); onCanvasColorClick() },
                            onPaperStyleChange = onPaperStyleChange,
                            onPaperSpacingChange = onPaperSpacingChange,
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
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockIconButton(selected = tool == Tool.PEN && brushFamily == BrushFamilyChoice.PEN, onClick = {
                    if (tool == Tool.PEN && brushFamily == BrushFamilyChoice.PEN) toggle(DockExpansion.SIZE)
                    else { onSelectBrush(BrushFamilyChoice.PEN); expansion = DockExpansion.NONE }
                }) { tint -> PenIcon(tint) }

                DockIconButton(selected = tool == Tool.PEN && brushFamily == BrushFamilyChoice.MARKER, onClick = {
                    if (tool == Tool.PEN && brushFamily == BrushFamilyChoice.MARKER) toggle(DockExpansion.SIZE)
                    else { onSelectBrush(BrushFamilyChoice.MARKER); expansion = DockExpansion.NONE }
                }) { tint -> MarkerIcon(tint) }

                DockIconButton(selected = tool == Tool.PEN && brushFamily == BrushFamilyChoice.HIGHLIGHTER, onClick = {
                    if (tool == Tool.PEN && brushFamily == BrushFamilyChoice.HIGHLIGHTER) toggle(DockExpansion.SIZE)
                    else { onSelectBrush(BrushFamilyChoice.HIGHLIGHTER); expansion = DockExpansion.NONE }
                }) { tint -> HighlighterIcon(tint) }

                val isMediaBrush = tool == Tool.PEN && brushFamily !in listOf(BrushFamilyChoice.PEN, BrushFamilyChoice.MARKER, BrushFamilyChoice.HIGHLIGHTER)
                DockIconButton(selected = isMediaBrush, onClick = {
                    if (isMediaBrush) toggle(DockExpansion.BRUSHES)
                    else { onSelectBrush(recentMediaBrush); expansion = DockExpansion.NONE }
                }) { tint -> dev.stupifranc.inkspire.ui.components.icons.MediaBrushIcon(brushFamily = recentMediaBrush, tint = tint) }

                DockIconButton(selected = tool == Tool.ERASER, onClick = {
                    if (tool == Tool.ERASER) toggle(DockExpansion.SIZE)
                    else { onSelectEraser(); expansion = DockExpansion.NONE }
                }) { tint -> EraserIcon(tint) }

                DockIconButton(selected = tool == Tool.PAN, onClick = {
                    onSelectHand(); expansion = DockExpansion.NONE
                }) { tint -> HandIcon(tint) }

                DockDivider()

                DockIconButton(
                    selected = symmetryEnabled,
                    onClick = {
                        if (!symmetryEnabled) {
                            onToggleSymmetry()
                            expansion = DockExpansion.SYMMETRY
                        } else {
                            toggle(DockExpansion.SYMMETRY)
                        }
                    },
                    onDoubleClick = {
                        if (symmetryEnabled) {
                            onToggleSymmetry()
                            expansion = DockExpansion.NONE
                        }
                    }
                ) { tint -> SymmetryIcon(tint) }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (colorArgb == 0) MaterialTheme.colorScheme.surface else Color(colorArgb))
                        .clickable(onClick = onColorClick),
                )

                DockIconButton(selected = expansion == DockExpansion.CANVAS, onClick = { toggle(DockExpansion.CANVAS) }) { tint -> ResizeIcon(tint) }

                DockDivider()

                DockIconButton(selected = expansion == DockExpansion.MORE, onClick = { toggle(DockExpansion.MORE) }) { tint -> MoreIcon(tint) }
            }
        }
    }
}

@Composable
private fun BrushesPanel(
    selectedBrush: BrushFamilyChoice,
    size: Float,
    range: ClosedFloatingPointRange<Float>,
    onSelectBrush: (BrushFamilyChoice) -> Unit,
    onSizeChange: (Float) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val mediaBrushes = dev.stupifranc.inkspire.ui.editor.MEDIA_BRUSHES
        // A grid of media brushes (2 rows)
        val rows = mediaBrushes.chunked(4)
        rows.forEach { rowBrushes ->
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                rowBrushes.forEach { brush ->
                    DockIconButton(selected = brush == selectedBrush, onClick = { onSelectBrush(brush) }) { tint ->
                        MediaBrushIcon(brushFamily = brush, tint = tint)
                    }
                }
            }
        }
        DockDivider()
        SizeSlider(
            size = size,
            range = range,
            onSizeChange = onSizeChange,
            modifier = Modifier.width(240.dp).padding(top = 8.dp),
        )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$sectors", modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.labelMedium)
            Slider(
                value = sectors.toFloat(),
                onValueChange = { onSectorsChange(it.roundToInt()) },
                valueRange = 1f..12f,
                steps = 10,
                modifier = Modifier.width(160.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DockIconButton(selected = mirror, onClick = { onMirrorChange(!mirror) }) { tint -> MirrorIcon(tint) }
            DockDivider()
            DockIconButton(selected = awaitingCenterPlacement, onClick = onToggleCenterPlacement) { tint -> CenterTargetIcon(tint) }
            DockIconButton(selected = false, onClick = onResetCenter) { tint -> ResetIcon(tint) }
        }
    }
}

/** Explicit, deliberate canvas growth in a chosen direction ("extend it like a sheet of paper"), plus the precise drag-handle resize. */
@Composable
private fun CanvasSizePanel(
    canvasShape: dev.stupifranc.inkspire.model.CanvasShape,
    onShapeChange: (dev.stupifranc.inkspire.model.CanvasShape) -> Unit,
    onGrowEdge: (CanvasEdge) -> Unit,
    onResize: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            dev.stupifranc.inkspire.model.CanvasShape.entries.forEach { shape ->
                DockIconButton(selected = shape == canvasShape, onClick = { onShapeChange(shape) }) { tint ->
                    when (shape) {
                        dev.stupifranc.inkspire.model.CanvasShape.RECTANGLE -> dev.stupifranc.inkspire.ui.components.icons.RectangleIcon(tint)
                        dev.stupifranc.inkspire.model.CanvasShape.ROUNDED_RECTANGLE -> dev.stupifranc.inkspire.ui.components.icons.RoundedRectangleIcon(tint)
                        dev.stupifranc.inkspire.model.CanvasShape.CIRCLE -> dev.stupifranc.inkspire.ui.components.icons.CircleIcon(tint)
                    }
                }
            }
        }
        DockDivider(horizontal = true)
        DockIconButton(selected = false, onClick = { onGrowEdge(CanvasEdge.TOP) }) { tint -> ArrowUpIcon(tint) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DockIconButton(selected = false, onClick = { onGrowEdge(CanvasEdge.LEFT) }) { tint -> ArrowLeftIcon(tint) }
            DockDivider()
            DockIconButton(selected = false, onClick = onResize) { tint -> CropIcon(tint) }
            DockDivider()
            DockIconButton(selected = false, onClick = { onGrowEdge(CanvasEdge.RIGHT) }) { tint -> ArrowRightIcon(tint) }
        }
        DockIconButton(selected = false, onClick = { onGrowEdge(CanvasEdge.BOTTOM) }) { tint -> ArrowDownIcon(tint) }
    }
}

@Composable
fun DockDivider(horizontal: Boolean = false) {
    Box(
        modifier = Modifier
            .padding(horizontal = if (horizontal) 0.dp else 4.dp, vertical = if (horizontal) 4.dp else 0.dp)
            .size(
                width = if (horizontal) 32.dp else 1.dp,
                height = if (horizontal) 1.dp else 32.dp
            )
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    )
}

@Composable
private fun MorePanel(
    canvasColorArgb: Int,
    paperStyle: PaperStyle,
    paperSpacing: Float,
    rotationEnabled: Boolean,
    onToggleRotation: () -> Unit,
    stylusOnly: Boolean,
    onExport: () -> Unit,
    onStylusOnlyChange: (Boolean) -> Unit,
    onCanvasColorClick: () -> Unit,
    onPaperStyleChange: (PaperStyle) -> Unit,
    onPaperSpacingChange: (Float) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DockIconButton(selected = false, onClick = onExport) { tint -> ExportIcon(tint) }
            DockDivider()
            DockIconButton(selected = rotationEnabled, onClick = onToggleRotation) { tint -> androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Refresh, contentDescription = "Rotation", tint = tint, modifier = Modifier.size(24.dp)) }
            DockDivider()
            DockIconButton(selected = stylusOnly, onClick = { onStylusOnlyChange(!stylusOnly) }) { tint -> StylusIcon(tint) }
            DockDivider()
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (canvasColorArgb == 0) androidx.compose.material3.MaterialTheme.colorScheme.surface else Color(canvasColorArgb))
                    .border(1.dp, Color.Black.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onCanvasColorClick),
            )
        }
        DockDivider()
        Row {
            PaperStyle.entries.forEach { style ->
                DockIconButton(
                    selected = style == paperStyle,
                    onClick = { onPaperStyleChange(style) }
                ) { tint ->
                    when (style) {
                        PaperStyle.PLAIN -> PaperPlainIcon(tint)
                        PaperStyle.RULED -> PaperRuledIcon(tint)
                        PaperStyle.DOTS -> PaperDotsIcon(tint)
                        PaperStyle.GRID -> PaperGridIcon(tint)
                        PaperStyle.ISOMETRIC -> PaperIsoIcon(tint)
                    }
                }
            }
        }
        AnimatedVisibility(visible = paperStyle != PaperStyle.PLAIN) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                TuneIcon(tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Slider(
                    value = paperSpacing,
                    onValueChange = onPaperSpacingChange,
                    valueRange = 24f..128f,
                    modifier = Modifier.width(140.dp).padding(start = 8.dp),
                )
            }
        }
    }
}
