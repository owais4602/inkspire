package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class SymmetryEngineTest {

    private fun assertPointsClose(actual: Point, expected: Point, epsilon: Float = 1e-3f) {
        assertThat(abs(actual.x - expected.x)).isLessThan(epsilon)
        assertThat(abs(actual.y - expected.y)).isLessThan(epsilon)
    }

    private fun distance(a: Point, b: Point): Float = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    @Test
    fun off_producesSingleIdentityTransform() {
        val config = SymmetryConfig.OFF
        val transforms = SymmetryEngine.transforms(config)
        assertThat(transforms).hasSize(1)
        val p = Point(3f, 4f)
        assertPointsClose(transforms[0].apply(p), p)
    }

    @Test
    fun rotationalOnly_producesSectorsCountTransforms() {
        val config = SymmetryConfig(sectors = 6, mirror = false, center = Point(0f, 0f))
        val transforms = SymmetryEngine.transforms(config)
        assertThat(transforms).hasSize(6)
    }

    @Test
    fun rotationalOnly_firstTransformIsIdentity() {
        val config = SymmetryConfig(sectors = 5, mirror = false, center = Point(2f, -1f))
        val transforms = SymmetryEngine.transforms(config)
        val p = Point(10f, 7f)
        assertPointsClose(transforms[0].apply(p), p)
    }

    @Test
    fun withMirror_producesTwiceSectorsCountTransforms() {
        val config = SymmetryConfig(sectors = 4, mirror = true, center = Point(0f, 0f))
        val transforms = SymmetryEngine.transforms(config)
        assertThat(transforms).hasSize(8)
    }

    @Test
    fun mirrorOnly_producesOriginalAndOneReflectedTwin() {
        val config = SymmetryConfig(sectors = 1, mirror = true, center = Point(0f, 0f))
        val transforms = SymmetryEngine.transforms(config)
        assertThat(transforms).hasSize(2)

        val p = Point(3f, 5f)
        assertPointsClose(transforms[0].apply(p), p)
        assertPointsClose(transforms[1].apply(p), Point(3f, -5f))
    }

    @Test
    fun mirrorTwin_isInvolution_applyingTwiceReturnsOriginal() {
        val config = SymmetryConfig(sectors = 1, mirror = true, center = Point(5f, 5f))
        val mirrorTransform = SymmetryEngine.transforms(config)[1]
        val p = Point(8f, 12f)
        val once = mirrorTransform.apply(p)
        val twice = mirrorTransform.apply(once)
        assertPointsClose(twice, p)
    }

    @Test
    fun nWayRotations_angleStepSumsToFullCircle() {
        val sectors = 8
        val center = Point(0f, 0f)
        val config = SymmetryConfig(sectors = sectors, mirror = false, center = center)
        val transforms = SymmetryEngine.transforms(config)

        var point = Point(1f, 0f)
        // Composing each sector's incremental rotation (transforms[i] relative to transforms[i-1])
        // sector-by-sector should walk all the way around back to the start.
        val angleStep = 360f / sectors
        repeat(sectors) {
            point = Affine2.rotationDegrees(angleStep).apply(point)
        }
        assertPointsClose(point, Point(1f, 0f))

        // And the engine's own transforms should match direct rotation by i * angleStep.
        transforms.forEachIndexed { i, transform ->
            val expected = Affine2.rotationDegrees(angleStep * i).apply(Point(1f, 0f))
            assertPointsClose(transform.apply(Point(1f, 0f)), expected)
        }
    }

    @Test
    fun everyTransform_fixesCenter() {
        val center = Point(4f, -6f)
        val config = SymmetryConfig(sectors = 5, mirror = true, center = center)
        SymmetryEngine.transforms(config).forEach { transform ->
            assertPointsClose(transform.apply(center), center)
        }
    }

    @Test
    fun everyTransform_isAnIsometry_preservesDistanceFromCenter() {
        val center = Point(1f, 1f)
        val config = SymmetryConfig(sectors = 7, mirror = true, center = center)
        val p = Point(11f, 4f)
        val originalDistance = distance(p, center)

        SymmetryEngine.transforms(config).forEach { transform ->
            val mapped = transform.apply(p)
            assertThat(abs(distance(mapped, center) - originalDistance)).isLessThan(1e-2f)
        }
    }

    @Test
    fun roundTrip_rotationFollowedByInverseRotationReturnsOriginalPoint() {
        val center = Point(3f, 3f)
        val config = SymmetryConfig(sectors = 12, mirror = false, center = center)
        val transforms = SymmetryEngine.transforms(config)
        val p = Point(9f, 4f)

        transforms.forEachIndexed { i, transform ->
            val angleStep = 360f / config.sectors
            val inverse = Affine2.rotationDegreesAbout(-angleStep * i, center)
            assertPointsClose(inverse.apply(transform.apply(p)), p)
        }
    }

    @Test
    fun invalidSectors_throws() {
        try {
            SymmetryConfig(sectors = 0, mirror = false, center = Point(0f, 0f))
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
