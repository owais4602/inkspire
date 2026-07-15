package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaperPatternTest {

    // --- lineOffsets: interior pattern lines strictly inside (0, extent) ---

    @Test
    fun lineOffsets_exactMultiples_excludesBothEdges() {
        assertThat(PaperPattern.lineOffsets(extent = 100f, spacing = 25f))
            .containsExactly(25f, 50f, 75f)
            .inOrder()
    }

    @Test
    fun lineOffsets_nonMultipleExtent_stopsBeforeEdge() {
        assertThat(PaperPattern.lineOffsets(extent = 90f, spacing = 25f))
            .containsExactly(25f, 50f, 75f)
            .inOrder()
    }

    @Test
    fun lineOffsets_degenerateInputs_returnEmpty() {
        assertThat(PaperPattern.lineOffsets(extent = 100f, spacing = 0f)).isEmpty()
        assertThat(PaperPattern.lineOffsets(extent = 100f, spacing = -5f)).isEmpty()
        assertThat(PaperPattern.lineOffsets(extent = 0f, spacing = 25f)).isEmpty()
        assertThat(PaperPattern.lineOffsets(extent = 20f, spacing = 25f)).isEmpty()
    }

    // --- interceptOffsets: multiples of spacing covering [min, max] (for slanted-line intercepts) ---

    @Test
    fun interceptOffsets_coversRangeIncludingNegativesAndZero() {
        assertThat(PaperPattern.interceptOffsets(min = -100f, max = 100f, spacing = 40f))
            .containsExactly(-80f, -40f, 0f, 40f, 80f)
            .inOrder()
    }

    @Test
    fun interceptOffsets_includesBoundsWhenExactMultiples() {
        assertThat(PaperPattern.interceptOffsets(min = -40f, max = 40f, spacing = 40f))
            .containsExactly(-40f, 0f, 40f)
            .inOrder()
    }

    @Test
    fun interceptOffsets_degenerateInputs_returnEmpty() {
        assertThat(PaperPattern.interceptOffsets(min = 0f, max = 100f, spacing = 0f)).isEmpty()
        assertThat(PaperPattern.interceptOffsets(min = 100f, max = 0f, spacing = 40f)).isEmpty()
    }

    // --- lineColorArgb: subtle ink that contrasts the paper ---

    @Test
    fun lineColorArgb_onLightBackground_isTranslucentBlack() {
        val color = PaperPattern.lineColorArgb(backgroundArgb = 0xFFFFFFFF.toInt())
        assertThat(color and 0x00FFFFFF).isEqualTo(0x000000)
        val alpha = color ushr 24
        assertThat(alpha).isGreaterThan(0)
        assertThat(alpha).isLessThan(0x80)
    }

    @Test
    fun lineColorArgb_onDarkBackground_isTranslucentWhite() {
        val color = PaperPattern.lineColorArgb(backgroundArgb = 0xFF000000.toInt())
        assertThat(color and 0x00FFFFFF).isEqualTo(0xFFFFFF)
        val alpha = color ushr 24
        assertThat(alpha).isGreaterThan(0)
        assertThat(alpha).isLessThan(0x80)
    }

    @Test
    fun lineColorArgb_midGrayBoundary_isDeterministic() {
        // Exactly mid luminance counts as light paper -> dark ink (documented tie-break).
        val color = PaperPattern.lineColorArgb(backgroundArgb = 0xFF808080.toInt())
        assertThat(color and 0x00FFFFFF).isEqualTo(0x000000)
    }
}
