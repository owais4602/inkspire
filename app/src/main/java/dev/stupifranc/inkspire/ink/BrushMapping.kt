package dev.stupifranc.inkspire.ink

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import dev.stupifranc.inkspire.model.BrushFamilyChoice
import dev.stupifranc.inkspire.model.BrushSpec

private const val DEFAULT_EPSILON = 0.0078125f // 0.25 world units ÷ Viewport.MAX_SCALE (32): keeps quantization ≤ 1 px at max zoom, per the Ink epsilon guidance

fun BrushFamilyChoice.toBrushFamily(): BrushFamily = BrushCatalog.getFamily(this)

/**
 * Reverse of [toBrushFamily]. Structural (not referential) equality on [BrushFamily] makes this a safe
 * round-trip for the app's fixed set of stock families — used by [dev.stupifranc.inkspire.ink.StrokeStore]
 * to persist "which family" as our own enum instead of opting into `ink-storage`'s still-experimental
 * generic `BrushFamily` byte serialization, which exists for custom brush textures we don't use.
 */
fun BrushFamily.toBrushFamilyChoice(): BrushFamilyChoice = BrushCatalog.getChoice(this)

fun BrushSpec.toInkBrush(): Brush =
    Brush.createWithColorIntArgb(family.toBrushFamily(), colorArgb, size, DEFAULT_EPSILON)
