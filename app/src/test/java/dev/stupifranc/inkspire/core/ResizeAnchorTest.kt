package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ResizeAnchorTest {

    private fun assertPointsClose(actual: Point, expected: Point, epsilon: Float = 1e-3f) {
        assertThat(abs(actual.x - expected.x)).isLessThan(epsilon)
        assertThat(abs(actual.y - expected.y)).isLessThan(epsilon)
    }

    @Test
    fun topStart_pinsOriginRegardlessOfSizeChange() {
        val offset = ResizeAnchor.offset(oldWidth = 100f, oldHeight = 200f, newWidth = 500f, newHeight = 50f, anchor = ResizeAnchor.TOP_START)
        assertPointsClose(offset, Point(0f, 0f))
    }

    @Test
    fun bottomEnd_shiftsContentByFullSizeDelta() {
        val offset = ResizeAnchor.offset(oldWidth = 100f, oldHeight = 200f, newWidth = 300f, newHeight = 250f, anchor = ResizeAnchor.BOTTOM_END)
        assertPointsClose(offset, Point(200f, 50f))
    }

    @Test
    fun center_shiftsContentByHalfSizeDeltaOnBothAxes() {
        val offset = ResizeAnchor.offset(oldWidth = 100f, oldHeight = 100f, newWidth = 300f, newHeight = 400f, anchor = ResizeAnchor.CENTER)
        assertPointsClose(offset, Point(100f, 150f))
    }

    @Test
    fun shrinkingCanvas_producesNegativeOffsetTowardAnchor() {
        val offset = ResizeAnchor.offset(oldWidth = 400f, oldHeight = 400f, newWidth = 100f, newHeight = 100f, anchor = ResizeAnchor.BOTTOM_END)
        assertPointsClose(offset, Point(-300f, -300f))
    }

    @Test
    fun noSizeChange_producesZeroOffsetForAnyAnchor() {
        ResizeAnchor.entries.forEach { anchor ->
            val offset = ResizeAnchor.offset(oldWidth = 250f, oldHeight = 180f, newWidth = 250f, newHeight = 180f, anchor = anchor)
            assertPointsClose(offset, Point(0f, 0f))
        }
    }

    @Test
    fun centerStart_onlyShiftsVerticalAxis() {
        val offset = ResizeAnchor.offset(oldWidth = 100f, oldHeight = 100f, newWidth = 300f, newHeight = 300f, anchor = ResizeAnchor.CENTER_START)
        assertPointsClose(offset, Point(0f, 100f))
    }
}
