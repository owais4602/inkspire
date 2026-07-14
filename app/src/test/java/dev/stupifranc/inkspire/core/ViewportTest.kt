package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ViewportTest {

    private fun assertPointsClose(actual: Point, expected: Point, epsilon: Float = 1e-3f) {
        assertThat(abs(actual.x - expected.x)).isLessThan(epsilon)
        assertThat(abs(actual.y - expected.y)).isLessThan(epsilon)
    }

    @Test
    fun identityViewport_documentAndScreenCoincide() {
        val v = Viewport()
        assertPointsClose(v.documentToScreen(Point(10f, 20f)), Point(10f, 20f))
    }

    @Test
    fun documentToScreen_thenScreenToDocument_roundTrips() {
        val v = Viewport(scale = 2.5f, panX = 30f, panY = -15f)
        val original = Point(123f, -45f)
        val roundTripped = v.screenToDocument(v.documentToScreen(original))
        assertPointsClose(roundTripped, original)
    }

    @Test
    fun pannedBy_shiftsScreenPositionByDelta() {
        val v = Viewport(scale = 1f, panX = 0f, panY = 0f).pannedBy(40f, -10f)
        assertPointsClose(v.documentToScreen(Point(0f, 0f)), Point(40f, -10f))
    }

    @Test
    fun zoomedBy_keepsFocalPointFixedInDocumentSpace() {
        val v = Viewport(scale = 1f, panX = 50f, panY = 20f)
        val focal = Point(200f, 150f)
        val documentUnderFocalBefore = v.screenToDocument(focal)

        val zoomed = v.zoomedBy(3f, focal)
        val documentUnderFocalAfter = zoomed.screenToDocument(focal)

        assertPointsClose(documentUnderFocalAfter, documentUnderFocalBefore)
    }

    @Test
    fun zoomedBy_stillFixesFocalPointWhenClamped() {
        val v = Viewport(scale = Viewport.MAX_SCALE, panX = 0f, panY = 0f)
        val focal = Point(80f, 60f)
        val documentUnderFocalBefore = v.screenToDocument(focal)

        val zoomed = v.zoomedBy(10f, focal)
        assertThat(zoomed.scale).isEqualTo(Viewport.MAX_SCALE)
        assertPointsClose(zoomed.screenToDocument(focal), documentUnderFocalBefore)
    }

    @Test
    fun clampedTo_centersDocumentSmallerThanViewport() {
        val v = Viewport(scale = 1f, panX = 999f, panY = -999f)
            .clampedTo(documentWidth = 100f, documentHeight = 50f, viewportWidth = 300f, viewportHeight = 200f)

        assertThat(v.panX).isWithin(1e-3f).of(100f)
        assertThat(v.panY).isWithin(1e-3f).of(75f)
    }

    @Test
    fun clampedTo_keepsViewportWithinLargerDocumentBounds() {
        val v = Viewport(scale = 1f, panX = 500f, panY = 500f)
            .clampedTo(documentWidth = 1000f, documentHeight = 2000f, viewportWidth = 300f, viewportHeight = 400f)

        // panning right/down past the document's top-left edge isn't allowed
        assertThat(v.panX).isAtMost(0f)
        assertThat(v.panY).isAtMost(0f)

        val farNegative = Viewport(scale = 1f, panX = -5000f, panY = -5000f)
            .clampedTo(documentWidth = 1000f, documentHeight = 2000f, viewportWidth = 300f, viewportHeight = 400f)
        // nor past the bottom-right edge
        assertThat(farNegative.panX).isAtLeast(300f - 1000f)
        assertThat(farNegative.panY).isAtLeast(400f - 2000f)
    }

    @Test
    fun fit_scalesToSmallerRatioAndCentersDocument() {
        val v = Viewport.fit(documentWidth = 400f, documentHeight = 100f, viewportWidth = 200f, viewportHeight = 200f)

        assertThat(v.scale).isWithin(1e-4f).of(0.5f)
        assertPointsClose(v.documentToScreen(Point(0f, 0f)), Point(0f, 75f))
        assertPointsClose(v.documentToScreen(Point(400f, 100f)), Point(200f, 125f))
    }

    @Test
    fun fit_withZeroSizedInputs_returnsIdentityRatherThanDividingByZero() {
        val v = Viewport.fit(documentWidth = 0f, documentHeight = 0f, viewportWidth = 200f, viewportHeight = 200f)
        assertThat(v).isEqualTo(Viewport())
    }
}
