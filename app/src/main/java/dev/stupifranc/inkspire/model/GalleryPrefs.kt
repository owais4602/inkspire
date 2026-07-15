package dev.stupifranc.inkspire.model

import kotlinx.serialization.Serializable

/** User-adjustable gallery thumbnail size, carrying the adaptive grid's minimum cell width. */
@Serializable
enum class ThumbSize(val minCellDp: Int) {
    COMPACT(104),
    MEDIUM(156),
    LARGE(224),
}

/** User-adjustable gallery card corner treatment, carrying the corner radius. */
@Serializable
enum class CornerStyle(val radiusDp: Int) {
    SQUARE(2),
    ROUNDED(14),
}

/** User-adjustable gallery wall (background) tone. */
@Serializable
enum class WallTone {
    DARK,
    LIGHT,
}

/** App-wide (not per-drawing) gallery display preferences, persisted via [dev.stupifranc.inkspire.data.GalleryPrefsStore]. */
@Serializable
data class GalleryPrefs(
    val thumbSize: ThumbSize = ThumbSize.MEDIUM,
    val borderEnabled: Boolean = true,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val showCaptions: Boolean = true,
    val wall: WallTone = WallTone.DARK,
)
