package dev.stupifranc.inkspire.ink

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.BrushSpec

private const val DEFAULT_EPSILON = 0.1f

fun BrushFamilyChoice.toBrushFamily(): BrushFamily = when (this) {
    BrushFamilyChoice.PRESSURE_PEN -> StockBrushes.pressurePen()
    BrushFamilyChoice.MARKER -> StockBrushes.marker()
    BrushFamilyChoice.HIGHLIGHTER -> StockBrushes.highlighter()
}

/**
 * Reverse of [toBrushFamily]. Structural (not referential) equality on [BrushFamily] makes this a safe
 * round-trip for the app's fixed set of stock families — used by [dev.stupifranc.inkspire.ink.StrokeStore]
 * to persist "which family" as our own enum instead of opting into `ink-storage`'s still-experimental
 * generic `BrushFamily` byte serialization, which exists for custom brush textures we don't use.
 */
fun BrushFamily.toBrushFamilyChoice(): BrushFamilyChoice = when (this) {
    StockBrushes.pressurePen() -> BrushFamilyChoice.PRESSURE_PEN
    StockBrushes.marker() -> BrushFamilyChoice.MARKER
    StockBrushes.highlighter() -> BrushFamilyChoice.HIGHLIGHTER
    else -> BrushFamilyChoice.PRESSURE_PEN
}

fun BrushSpec.toInkBrush(): Brush =
    Brush.createWithColorIntArgb(family.toBrushFamily(), colorArgb, size, DEFAULT_EPSILON)
