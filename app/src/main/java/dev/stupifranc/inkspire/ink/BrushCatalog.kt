package dev.stupifranc.inkspire.ink

import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockBrushes
import dev.stupifranc.inkspire.model.BrushFamilyChoice

@OptIn(ExperimentalInkCustomBrushApi::class)
object BrushCatalog {

    private val families = mutableMapOf<BrushFamilyChoice, BrushFamily>()

    private val calligraphy: BrushFamily by lazy {
        val tip = androidx.ink.brush.BrushTip(
            scaleX = 1f,
            scaleY = 0.28f,
            rotationDegrees = 40f,
            cornerRounding = 0.2f,
            behaviors = listOf(StockBrushes.predictionFadeOutBehavior)
        )
        BrushFamily(tip, androidx.ink.brush.BrushPaint(), "calligraphy")
    }

    private val dashed: BrushFamily by lazy {
        StockBrushes.dashedLine()
    }

    private val watercolor: BrushFamily by lazy {
        val tip = androidx.ink.brush.BrushTip(
            scaleX = 1f,
            scaleY = 1f,
            cornerRounding = 1f,
            behaviors = listOf(
                androidx.ink.brush.BrushBehavior(
                    source = androidx.ink.brush.BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = androidx.ink.brush.BrushBehavior.Target.SIZE_MULTIPLIER,
                    sourceValueRangeStart = 0f,
                    sourceValueRangeEnd = 1f,
                    targetModifierRangeStart = 0.85f,
                    targetModifierRangeEnd = 1.25f
                ),
                androidx.ink.brush.BrushBehavior(
                    source = androidx.ink.brush.BrushBehavior.Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
                    target = androidx.ink.brush.BrushBehavior.Target.OPACITY_MULTIPLIER,
                    sourceValueRangeStart = 0f,
                    sourceValueRangeEnd = 1f,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 0.75f
                ),
                StockBrushes.predictionFadeOutBehavior
            )
        )
        val layer = androidx.ink.brush.BrushPaint.TextureLayer(
            clientTextureId = "inkspire:granulation",
            sizeX = 24f,
            sizeY = 24f,
            sizeUnit = androidx.ink.brush.BrushPaint.TextureSizeUnit.BRUSH_SIZE,
            mapping = androidx.ink.brush.BrushPaint.TextureMapping.TILING,
            blendMode = androidx.ink.brush.BrushPaint.BlendMode.MODULATE,
            opacity = 0.85f
        )
        val paint = androidx.ink.brush.BrushPaint(
            textureLayers = listOf(layer),
            selfOverlap = androidx.ink.brush.SelfOverlap.ACCUMULATE
        )
        BrushFamily(tip, paint, "watercolor")
    }

    private val dryInk: BrushFamily by lazy {
        val tip = androidx.ink.brush.BrushTip(
            scaleX = 1f,
            scaleY = 1f,
            cornerRounding = 1f,
            behaviors = listOf(
                androidx.ink.brush.BrushBehavior(
                    source = androidx.ink.brush.BrushBehavior.Source.NORMALIZED_PRESSURE,
                    target = androidx.ink.brush.BrushBehavior.Target.SIZE_MULTIPLIER,
                    sourceValueRangeStart = 0f,
                    sourceValueRangeEnd = 1f,
                    targetModifierRangeStart = 0.85f,
                    targetModifierRangeEnd = 1.25f
                ),
                androidx.ink.brush.BrushBehavior(
                    source = androidx.ink.brush.BrushBehavior.Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
                    target = androidx.ink.brush.BrushBehavior.Target.OPACITY_MULTIPLIER,
                    sourceValueRangeStart = 0f,
                    sourceValueRangeEnd = 20f,
                    targetModifierRangeStart = 1.0f,
                    targetModifierRangeEnd = 0.35f
                ),
                StockBrushes.predictionFadeOutBehavior
            )
        )
        val layer = androidx.ink.brush.BrushPaint.TextureLayer(
            clientTextureId = "inkspire:dryStreaks",
            sizeX = 24f,
            sizeY = 24f,
            sizeUnit = androidx.ink.brush.BrushPaint.TextureSizeUnit.BRUSH_SIZE,
            mapping = androidx.ink.brush.BrushPaint.TextureMapping.TILING,
            blendMode = androidx.ink.brush.BrushPaint.BlendMode.MODULATE,
            opacity = 0.85f
        )
        val paint = androidx.ink.brush.BrushPaint(
            textureLayers = listOf(layer),
            selfOverlap = androidx.ink.brush.SelfOverlap.ANY
        )
        BrushFamily(tip, paint, "dry_ink")
    }

    fun getFamily(choice: BrushFamilyChoice): BrushFamily {
        return families.getOrPut(choice) {
            when (choice) {
                BrushFamilyChoice.PEN -> StockBrushes.marker()
                BrushFamilyChoice.MARKER -> StockBrushes.pressurePen()
                BrushFamilyChoice.HIGHLIGHTER -> StockBrushes.highlighter()
                BrushFamilyChoice.PENCIL -> StockBrushes.pencilUnstable
                BrushFamilyChoice.CALLIGRAPHY -> calligraphy
                BrushFamilyChoice.DASHED -> dashed
                BrushFamilyChoice.WATERCOLOR -> watercolor
                BrushFamilyChoice.DRY_INK -> dryInk
                else -> StockBrushes.marker()
            }
        }
    }

    fun getChoice(family: BrushFamily): BrushFamilyChoice {
        for (choice in BrushFamilyChoice.values()) {
            if (getFamily(choice) === family || getFamily(choice) == family) {
                return choice
            }
        }
        return BrushFamilyChoice.PEN
    }
}
