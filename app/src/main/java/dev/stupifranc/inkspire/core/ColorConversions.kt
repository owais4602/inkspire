package dev.stupifranc.inkspire.core

import kotlin.math.abs
import kotlin.math.roundToInt

data class Hsva(val hue: Float, val saturation: Float, val value: Float, val alpha: Float = 1f)

/** Standalone HSV/hex <-> ARGB math (not android.graphics.Color: that's a framework stub in JVM unit tests). */
fun hsvaToArgb(hsva: Hsva): Int {
    val h = ((hsva.hue % 360f) + 360f) % 360f
    val s = hsva.saturation.coerceIn(0f, 1f)
    val v = hsva.value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1 - abs((h / 60f) % 2 - 1))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = (((r1 + m) * 255f).roundToInt()).coerceIn(0, 255)
    val g = (((g1 + m) * 255f).roundToInt()).coerceIn(0, 255)
    val b = (((b1 + m) * 255f).roundToInt()).coerceIn(0, 255)
    val a = ((hsva.alpha.coerceIn(0f, 1f)) * 255f).roundToInt().coerceIn(0, 255)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

fun argbToHsva(argb: Int): Hsva {
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val rawHue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val hue = if (rawHue < 0f) rawHue + 360f else rawHue
    val saturation = if (max == 0f) 0f else delta / max
    return Hsva(hue, saturation, max, a)
}

fun argbToHex(argb: Int, includeAlpha: Boolean = false): String {
    val a = (argb shr 24) and 0xFF
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return if (includeAlpha) {
        "#%02X%02X%02X%02X".format(a, r, g, b)
    } else {
        "#%02X%02X%02X".format(r, g, b)
    }
}

/** Accepts "#RRGGBB" or "#AARRGGBB" (with or without the leading '#'); returns null if malformed. */
fun hexToArgb(hex: String): Int? {
    val cleaned = hex.removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    return if (cleaned.length == 6) {
        (0xFF shl 24) or value.toInt()
    } else {
        value.toInt()
    }
}
