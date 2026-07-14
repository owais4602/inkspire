package dev.stupifranc.inkspire.model

enum class BrushFamilyChoice { PRESSURE_PEN, MARKER, HIGHLIGHTER }

data class BrushSpec(
    val family: BrushFamilyChoice = BrushFamilyChoice.PRESSURE_PEN,
    val colorArgb: Int,
    val size: Float,
)
