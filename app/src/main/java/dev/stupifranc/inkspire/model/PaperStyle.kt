package dev.stupifranc.inkspire.model

import kotlinx.serialization.Serializable

/** Background pattern drawn on the page, part of the artwork (baked into export/thumbnails). */
@Serializable
enum class PaperStyle {
    PLAIN,
    RULED,
    DOTS,
    GRID,
    ISOMETRIC,
}

/** Default distance between pattern lines/dots, in world (document) units. */
const val DEFAULT_PAPER_SPACING = 64f
