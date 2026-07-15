package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReorderTargetTest {

    @Test
    fun reorderTarget_pointInsideAnotherBox_returnsThatKey() {
        val boxes = listOf(
            ItemBox("A", 0f, 0f, 100f, 100f),
            ItemBox("B", 100f, 0f, 100f, 100f)
        )
        assertThat(reorderTarget(150f, 50f, boxes, "A")).isEqualTo("B")
    }

    @Test
    fun reorderTarget_pointInsideOwnBox_returnsNull() {
        val boxes = listOf(
            ItemBox("A", 0f, 0f, 100f, 100f),
            ItemBox("B", 100f, 0f, 100f, 100f)
        )
        assertThat(reorderTarget(50f, 50f, boxes, "A")).isNull()
    }

    @Test
    fun reorderTarget_pointInGapOrOutside_returnsNull() {
        val boxes = listOf(
            ItemBox("A", 0f, 0f, 100f, 100f),
            ItemBox("B", 200f, 0f, 100f, 100f) // gap between 100 and 200
        )
        assertThat(reorderTarget(150f, 50f, boxes, "A")).isNull() // gap
        assertThat(reorderTarget(350f, 50f, boxes, "A")).isNull() // outside right
        assertThat(reorderTarget(50f, -50f, boxes, "A")).isNull() // outside top
    }

    @Test
    fun reorderTarget_boundaryPoints_followContainsRule() {
        val boxes = listOf(
            ItemBox("A", 0f, 0f, 100f, 100f)
        )
        // left <= x < left+width && top <= y < top+height
        assertThat(reorderTarget(0f, 0f, boxes, "B")).isEqualTo("A") // top-left is inside
        assertThat(reorderTarget(99.9f, 99.9f, boxes, "B")).isEqualTo("A") // just inside
        assertThat(reorderTarget(100f, 0f, boxes, "B")).isNull() // exactly right edge (outside)
        assertThat(reorderTarget(0f, 100f, boxes, "B")).isNull() // exactly bottom edge (outside)
    }
}
