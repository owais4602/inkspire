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

    private val rainbow: BrushFamily by lazy {
        val markerTip = StockBrushes.marker().coats[0].tip
        val tip = androidx.ink.brush.BrushTip(
            scaleX = markerTip.scaleX,
            scaleY = markerTip.scaleY,
            cornerRounding = markerTip.cornerRounding,
            slantDegrees = markerTip.slantDegrees,
            pinch = markerTip.pinch,
            rotationDegrees = markerTip.rotationDegrees,
            particleGapDistanceScale = markerTip.particleGapDistanceScale,
            particleGapDurationMillis = markerTip.particleGapDurationMillis,
            behaviors = markerTip.behaviors + listOf(
                androidx.ink.brush.BrushBehavior(
                    source = androidx.ink.brush.BrushBehavior.Source.DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
                    target = androidx.ink.brush.BrushBehavior.Target.HUE_OFFSET_IN_RADIANS,
                    sourceValueRangeStart = 0f,
                    sourceValueRangeEnd = 120f,
                    targetModifierRangeStart = 0f,
                    targetModifierRangeEnd = (2.0 * Math.PI).toFloat(),
                    sourceOutOfRangeBehavior = androidx.ink.brush.BrushBehavior.OutOfRange.REPEAT
                )
            )
        )
        val paint = StockBrushes.marker().coats[0].paintPreferences.firstOrNull() ?: androidx.ink.brush.BrushPaint()
        BrushFamily(tip, paint, "rainbow")
    }

    private val neon: BrushFamily by lazy {
        val markerTip = StockBrushes.marker().coats[0].tip
        
        val underTip = androidx.ink.brush.BrushTip(
            scaleX = markerTip.scaleX * 2.2f,
            scaleY = markerTip.scaleY * 2.2f,
            cornerRounding = markerTip.cornerRounding,
            slantDegrees = markerTip.slantDegrees,
            pinch = markerTip.pinch,
            rotationDegrees = markerTip.rotationDegrees,
            particleGapDistanceScale = markerTip.particleGapDistanceScale,
            particleGapDurationMillis = markerTip.particleGapDurationMillis,
            behaviors = markerTip.behaviors
        )
        val underPaint = androidx.ink.brush.BrushPaint(
            textureLayers = emptyList(),
            colorFunctions = listOf(androidx.ink.brush.ColorFunction.OpacityMultiplier(0.35f))
        )
        val underCoat = androidx.ink.brush.BrushCoat(underTip, underPaint)
        
        val overTip = androidx.ink.brush.BrushTip(
            scaleX = markerTip.scaleX * 0.8f,
            scaleY = markerTip.scaleY * 0.8f,
            cornerRounding = markerTip.cornerRounding,
            slantDegrees = markerTip.slantDegrees,
            pinch = markerTip.pinch,
            rotationDegrees = markerTip.rotationDegrees,
            particleGapDistanceScale = markerTip.particleGapDistanceScale,
            particleGapDurationMillis = markerTip.particleGapDurationMillis,
            behaviors = markerTip.behaviors
        )
        val overPaint = androidx.ink.brush.BrushPaint()
        val overCoat = androidx.ink.brush.BrushCoat(overTip, overPaint)
        
        BrushFamily(coats = listOf(underCoat, overCoat), clientBrushFamilyId = "neon")
    }

    private val airbrush: BrushFamily by lazy {
        val tip = androidx.ink.brush.BrushTip(
            scaleX = 1f,
            scaleY = 1f,
            cornerRounding = 1f,
            particleGapDistanceScale = 0.3f,
            behaviors = listOf(
                androidx.ink.brush.BrushBehavior(listOf(
                    androidx.ink.brush.BrushBehavior.TargetNode(
                        target = androidx.ink.brush.BrushBehavior.Target.POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE,
                        targetModifierRangeStart = -1.5f,
                        targetModifierRangeEnd = 1.5f,
                        input = androidx.ink.brush.BrushBehavior.NoiseNode(
                            seed = 1337,
                            varyOver = androidx.ink.brush.BrushBehavior.DampingSource.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE,
                            basePeriod = 0.1f
                        )
                    )
                )),
                androidx.ink.brush.BrushBehavior(listOf(
                    androidx.ink.brush.BrushBehavior.TargetNode(
                        target = androidx.ink.brush.BrushBehavior.Target.POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE,
                        targetModifierRangeStart = -1.5f,
                        targetModifierRangeEnd = 1.5f,
                        input = androidx.ink.brush.BrushBehavior.NoiseNode(
                            seed = 7331,
                            varyOver = androidx.ink.brush.BrushBehavior.DampingSource.DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE,
                            basePeriod = 0.1f
                        )
                    )
                )),
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
            opacity = 0.15f
        )
        val paint = androidx.ink.brush.BrushPaint(
            textureLayers = listOf(layer),
            selfOverlap = androidx.ink.brush.SelfOverlap.ACCUMULATE
        )
        BrushFamily(tip, paint, "airbrush")
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
                BrushFamilyChoice.RAINBOW -> rainbow
                BrushFamilyChoice.NEON -> neon
                BrushFamilyChoice.AIRBRUSH -> airbrush
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
