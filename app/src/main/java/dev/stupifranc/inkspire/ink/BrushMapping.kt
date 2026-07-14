package dev.stupifranc.inkspire.ink

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.BrushSpec

private const val DEFAULT_EPSILON = 0.1f

private fun BrushFamilyChoice.toBrushFamily(): BrushFamily = when (this) {
    BrushFamilyChoice.PRESSURE_PEN -> StockBrushes.pressurePen()
    BrushFamilyChoice.MARKER -> StockBrushes.marker()
    BrushFamilyChoice.HIGHLIGHTER -> StockBrushes.highlighter()
}

fun BrushSpec.toInkBrush(): Brush =
    Brush.createWithColorIntArgb(family.toBrushFamily(), colorArgb, size, DEFAULT_EPSILON)
