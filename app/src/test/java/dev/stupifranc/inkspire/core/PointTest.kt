package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PointTest {

    @Test
    fun clampToRect_insideRect_isUnchanged() {
        assertThat(clampToRect(Point(10f, 20f), width = 100f, height = 50f)).isEqualTo(Point(10f, 20f))
    }

    @Test
    fun clampToRect_leftEdge_clampsXToZero() {
        assertThat(clampToRect(Point(-30f, 20f), width = 100f, height = 50f)).isEqualTo(Point(0f, 20f))
    }

    @Test
    fun clampToRect_rightEdge_clampsXToWidth() {
        assertThat(clampToRect(Point(500f, 20f), width = 100f, height = 50f)).isEqualTo(Point(100f, 20f))
    }

    @Test
    fun clampToRect_topEdge_clampsYToZero() {
        assertThat(clampToRect(Point(10f, -30f), width = 100f, height = 50f)).isEqualTo(Point(10f, 0f))
    }

    @Test
    fun clampToRect_bottomEdge_clampsYToHeight() {
        assertThat(clampToRect(Point(10f, 999f), width = 100f, height = 50f)).isEqualTo(Point(10f, 50f))
    }

    @Test
    fun clampToRect_corner_clampsBothAxes() {
        assertThat(clampToRect(Point(-30f, 999f), width = 100f, height = 50f)).isEqualTo(Point(0f, 50f))
    }

    @Test
    fun clampToRect_degenerateZeroSizeRect_collapsesToOrigin() {
        assertThat(clampToRect(Point(10f, 10f), width = 0f, height = 0f)).isEqualTo(Point(0f, 0f))
    }
}
