package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ColorConversionsTest {

    @Test
    fun hsvaToArgb_pureRed() {
        assertThat(hsvaToArgb(Hsva(0f, 1f, 1f))).isEqualTo(0xFFFF0000.toInt())
    }

    @Test
    fun hsvaToArgb_pureGreen() {
        assertThat(hsvaToArgb(Hsva(120f, 1f, 1f))).isEqualTo(0xFF00FF00.toInt())
    }

    @Test
    fun hsvaToArgb_pureBlue() {
        assertThat(hsvaToArgb(Hsva(240f, 1f, 1f))).isEqualTo(0xFF0000FF.toInt())
    }

    @Test
    fun hsvaToArgb_white() {
        assertThat(hsvaToArgb(Hsva(0f, 0f, 1f))).isEqualTo(0xFFFFFFFF.toInt())
    }

    @Test
    fun hsvaToArgb_black() {
        assertThat(hsvaToArgb(Hsva(0f, 0f, 0f))).isEqualTo(0xFF000000.toInt())
    }

    @Test
    fun hsvaToArgb_respectsAlpha() {
        assertThat(hsvaToArgb(Hsva(0f, 1f, 1f, alpha = 0.5f))).isEqualTo(0x80FF0000.toInt())
    }

    @Test
    fun argbToHsva_roundTripsThroughHsvaToArgb() {
        val samples = listOf(
            Hsva(0f, 1f, 1f),
            Hsva(120f, 1f, 1f),
            Hsva(240f, 1f, 1f),
            Hsva(45f, 0.5f, 0.75f),
            Hsva(300f, 0.2f, 0.9f),
            Hsva(0f, 0f, 0.5f),
        )
        for (original in samples) {
            val argb = hsvaToArgb(original)
            val roundTripped = argbToHsva(argb)
            val backToArgb = hsvaToArgb(roundTripped)
            assertThat(backToArgb).isEqualTo(argb)
        }
    }

    @Test
    fun argbToHsva_grayHasZeroSaturation() {
        val gray = argbToHsva(0xFF808080.toInt())
        assertThat(gray.saturation).isWithin(1e-3f).of(0f)
    }

    @Test
    fun argbToHex_withoutAlpha() {
        assertThat(argbToHex(0xFF1A2B3C.toInt())).isEqualTo("#1A2B3C")
    }

    @Test
    fun argbToHex_withAlpha() {
        assertThat(argbToHex(0x801A2B3C.toInt(), includeAlpha = true)).isEqualTo("#801A2B3C")
    }

    @Test
    fun hexToArgb_sixDigitAssumesOpaque() {
        assertThat(hexToArgb("#1A2B3C")).isEqualTo(0xFF1A2B3C.toInt())
    }

    @Test
    fun hexToArgb_sixDigitWithoutHash() {
        assertThat(hexToArgb("1A2B3C")).isEqualTo(0xFF1A2B3C.toInt())
    }

    @Test
    fun hexToArgb_eightDigitIncludesAlpha() {
        assertThat(hexToArgb("#801A2B3C")).isEqualTo(0x801A2B3C.toInt())
    }

    @Test
    fun hexToArgb_invalidLength_returnsNull() {
        assertThat(hexToArgb("#ABC")).isNull()
    }

    @Test
    fun hexToArgb_invalidCharacters_returnsNull() {
        assertThat(hexToArgb("#ZZZZZZ")).isNull()
    }

    @Test
    fun argbToHex_hexToArgb_roundTrips() {
        val original = 0xFF3C7BF0.toInt()
        assertThat(hexToArgb(argbToHex(original))).isEqualTo(original)
    }
}
