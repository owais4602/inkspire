package dev.stupifranc.inkspire.model

enum class BrushFamilyChoice {
    PEN, MARKER, HIGHLIGHTER,
    PENCIL, WATERCOLOR, DRY_INK, CALLIGRAPHY, DASHED, // M10a
    RAINBOW, NEON, AIRBRUSH // M10b
}

fun parseBrushFamilyChoice(name: String): BrushFamilyChoice {
    if (name == "PRESSURE_PEN") return BrushFamilyChoice.PEN
    return try {
        BrushFamilyChoice.valueOf(name)
    } catch (e: IllegalArgumentException) {
        BrushFamilyChoice.PEN
    }
}

data class BrushSpec(
    val family: BrushFamilyChoice = BrushFamilyChoice.PEN,
    val colorArgb: Int,
    val size: Float,
)
