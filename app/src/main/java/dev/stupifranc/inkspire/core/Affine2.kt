package dev.stupifranc.inkspire.core

import kotlin.math.cos
import kotlin.math.sin

/** 2D affine transform: x' = a*x + b*y + tx, y' = c*x + d*y + ty. `this * other` applies `other` first. */
data class Affine2(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val tx: Float,
    val ty: Float,
) {
    fun apply(point: Point): Point =
        Point(a * point.x + b * point.y + tx, c * point.x + d * point.y + ty)

    operator fun times(other: Affine2): Affine2 = Affine2(
        a = a * other.a + b * other.c,
        b = a * other.b + b * other.d,
        c = c * other.a + d * other.c,
        d = c * other.b + d * other.d,
        tx = a * other.tx + b * other.ty + tx,
        ty = c * other.tx + d * other.ty + ty,
    )

    companion object {
        val IDENTITY = Affine2(1f, 0f, 0f, 1f, 0f, 0f)

        fun translation(dx: Float, dy: Float): Affine2 = Affine2(1f, 0f, 0f, 1f, dx, dy)

        fun rotationDegrees(degrees: Float): Affine2 {
            val radians = Math.toRadians(degrees.toDouble())
            val cosT = cos(radians).toFloat()
            val sinT = sin(radians).toFloat()
            return Affine2(cosT, -sinT, sinT, cosT, 0f, 0f)
        }

        fun rotationDegreesAbout(degrees: Float, center: Point): Affine2 =
            translation(center.x, center.y) * rotationDegrees(degrees) * translation(-center.x, -center.y)

        fun scale(sx: Float, sy: Float): Affine2 = Affine2(sx, 0f, 0f, sy, 0f, 0f)
    }
}
