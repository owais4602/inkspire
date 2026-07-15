package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContentBoundsTest {

    @Test
    fun compute_emptyList_returnsNull() {
        assertThat(ContentBounds.compute(emptyList())).isNull()
    }

    @Test
    fun compute_singleBox_addsPaddingOnEverySide() {
        val result = ContentBounds.compute(listOf(BoundingBox(1000f, 1000f, 1400f, 1500f)), paddingPx = 20f)!!

        assertThat(result.offsetX).isEqualTo(980f)
        assertThat(result.offsetY).isEqualTo(980f)
        assertThat(result.width).isEqualTo(440f) // 400 + 2*20
        assertThat(result.height).isEqualTo(540f) // 500 + 2*20
    }

    @Test
    fun compute_disjointBoxes_returnsTheirUnion() {
        val boxes = listOf(
            BoundingBox(0f, 0f, 100f, 100f),
            BoundingBox(500f, -200f, 600f, -100f),
        )
        val result = ContentBounds.compute(boxes)!!

        assertThat(result.offsetX).isEqualTo(0f)
        assertThat(result.offsetY).isEqualTo(-200f)
        assertThat(result.width).isEqualTo(600f)
        assertThat(result.height).isEqualTo(300f)
    }

    @Test
    fun compute_tinyContent_enforcesFloor() {
        val result = ContentBounds.compute(listOf(BoundingBox(0f, 0f, 10f, 10f)))!!

        assertThat(result.width).isEqualTo(256f)
        assertThat(result.height).isEqualTo(256f)
    }

    @Test
    fun union_contentFullyInsidePage_returnsPageUnchanged() {
        val result = ContentBounds.union(listOf(BoundingBox(10f, 10f, 90f, 90f)), pageWidth = 100f, pageHeight = 200f)

        assertThat(result).isEqualTo(BoundingBox(0f, 0f, 100f, 200f))
    }

    @Test
    fun union_contentHangingOffOneSide_extendsThatSideOnly() {
        val result = ContentBounds.union(listOf(BoundingBox(-50f, 10f, 90f, 90f)), pageWidth = 100f, pageHeight = 200f)

        assertThat(result).isEqualTo(BoundingBox(-50f, 0f, 100f, 200f))
    }

    @Test
    fun union_noBoxes_returnsPageExactly() {
        val result = ContentBounds.union(emptyList(), pageWidth = 100f, pageHeight = 200f)

        assertThat(result).isEqualTo(BoundingBox(0f, 0f, 100f, 200f))
    }
}
