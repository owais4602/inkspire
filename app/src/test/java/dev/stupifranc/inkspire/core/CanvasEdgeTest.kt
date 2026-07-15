package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanvasEdgeTest {

    @Test
    fun top_growsHeightOnly_pinnedToBottomCenter() {
        val (width, height, anchor) = CanvasEdge.TOP.grow(width = 500f, height = 800f, amount = 200f)
        assertThat(width).isEqualTo(500f)
        assertThat(height).isEqualTo(1000f)
        assertThat(anchor).isEqualTo(ResizeAnchor.BOTTOM_CENTER)
    }

    @Test
    fun bottom_growsHeightOnly_pinnedToTopCenter() {
        val (width, height, anchor) = CanvasEdge.BOTTOM.grow(width = 500f, height = 800f, amount = 200f)
        assertThat(width).isEqualTo(500f)
        assertThat(height).isEqualTo(1000f)
        assertThat(anchor).isEqualTo(ResizeAnchor.TOP_CENTER)
    }

    @Test
    fun left_growsWidthOnly_pinnedToCenterEnd() {
        val (width, height, anchor) = CanvasEdge.LEFT.grow(width = 500f, height = 800f, amount = 150f)
        assertThat(width).isEqualTo(650f)
        assertThat(height).isEqualTo(800f)
        assertThat(anchor).isEqualTo(ResizeAnchor.CENTER_END)
    }

    @Test
    fun right_growsWidthOnly_pinnedToCenterStart() {
        val (width, height, anchor) = CanvasEdge.RIGHT.grow(width = 500f, height = 800f, amount = 150f)
        assertThat(width).isEqualTo(650f)
        assertThat(height).isEqualTo(800f)
        assertThat(anchor).isEqualTo(ResizeAnchor.CENTER_START)
    }

    @Test
    fun matchesResizeAnchorOffset_contentStaysPinnedToOppositeSide() {
        val (_, newHeight, anchor) = CanvasEdge.TOP.grow(width = 400f, height = 400f, amount = 100f)
        val offset = ResizeAnchor.offset(oldWidth = 400f, oldHeight = 400f, newWidth = 400f, newHeight = newHeight, anchor = anchor)
        assertThat(offset).isEqualTo(Point(0f, 100f))
    }
}
