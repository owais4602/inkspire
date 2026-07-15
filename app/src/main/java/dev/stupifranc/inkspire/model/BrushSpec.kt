package dev.stupifranc.inkspire.model

enum class BrushFamilyChoice { PEN, MARKER, HIGHLIGHTER }

data class BrushSpec(
    val family: BrushFamilyChoice = BrushFamilyChoice.PEN,
    val colorArgb: Int,
    val size: Float,
)
