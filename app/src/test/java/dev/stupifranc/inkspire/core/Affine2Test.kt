package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class Affine2Test {

    private fun assertPointsClose(actual: Point, expected: Point, epsilon: Float = 1e-3f) {
        assertThat(abs(actual.x - expected.x)).isLessThan(epsilon)
        assertThat(abs(actual.y - expected.y)).isLessThan(epsilon)
    }

    @Test
    fun identity_leavesPointUnchanged() {
        val p = Point(3f, -2f)
        assertPointsClose(Affine2.IDENTITY.apply(p), p)
    }

    @Test
    fun translation_shiftsPoint() {
        val t = Affine2.translation(5f, -3f)
        assertPointsClose(t.apply(Point(1f, 1f)), Point(6f, -2f))
    }

    @Test
    fun rotation90Degrees_mapsUnitXToUnitY() {
        val r = Affine2.rotationDegrees(90f)
        assertPointsClose(r.apply(Point(1f, 0f)), Point(0f, 1f))
    }

    @Test
    fun rotation180Degrees_negatesPoint() {
        val r = Affine2.rotationDegrees(180f)
        assertPointsClose(r.apply(Point(2f, 3f)), Point(-2f, -3f))
    }

    @Test
    fun rotationAboutCenter_fixesCenter() {
        val center = Point(10f, 10f)
        val r = Affine2.rotationDegreesAbout(90f, center)
        assertPointsClose(r.apply(center), center)
    }

    @Test
    fun rotationAboutCenter_rotatesPointAroundIt() {
        val center = Point(10f, 10f)
        val r = Affine2.rotationDegreesAbout(90f, center)
        assertPointsClose(r.apply(Point(15f, 10f)), Point(10f, 15f))
    }

    @Test
    fun composition_appliesRightOperandFirst() {
        val rotateAfterTranslate = Affine2.rotationDegrees(90f) * Affine2.translation(1f, 0f)
        assertPointsClose(rotateAfterTranslate.apply(Point(0f, 0f)), Point(0f, 1f))
    }

    @Test
    fun sixWaySymmetryRotations_sumToFullCircle() {
        val sectors = 6
        val angleStep = 360f / sectors
        var point = Point(1f, 0f)
        repeat(sectors) {
            point = Affine2.rotationDegrees(angleStep).apply(point)
        }
        assertPointsClose(point, Point(1f, 0f))
    }
}
