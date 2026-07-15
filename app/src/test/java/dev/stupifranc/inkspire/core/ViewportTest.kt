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

    // --- M8: deep zoom ---

    @Test
    fun zoomedBy_clampsAtDeepMaxScale() {
        val v = Viewport(scale = 16f).zoomedBy(10f, Point(50f, 50f))
        assertThat(v.scale).isEqualTo(32f)
    }

    @Test
    fun documentToScreen_roundTrip_staysAccurateAtScale32() {
        val v = Viewport(scale = 32f, panX = 1234.5f, panY = -987.25f)
        val original = Point(500f, 500f)
        assertPointsClose(v.screenToDocument(v.documentToScreen(original)), original, epsilon = 0.01f)
    }

    @Test
    fun zoomedBy_keepsFocalPointFixedAtDeepScale() {
        val v = Viewport(scale = 20f, panX = 30f, panY = -10f)
        val focal = Point(400f, 300f)
        val before = v.screenToDocument(focal)

        val zoomed = v.zoomedBy(1.5f, focal)

        assertPointsClose(zoomed.screenToDocument(focal), before)
    }

    // --- M8: zoomedTo / doubleTapTarget ---

    @Test
    fun zoomedTo_centersDocumentFocusInViewport() {
        val v = Viewport.zoomedTo(scale = 3f, documentFocus = Point(200f, 100f), viewportWidth = 400f, viewportHeight = 300f)

        assertThat(v.scale).isEqualTo(3f)
        assertPointsClose(v.documentToScreen(Point(200f, 100f)), Point(200f, 150f))
    }

    @Test
    fun zoomedTo_clampsScaleToMax() {
        val v = Viewport.zoomedTo(scale = 999f, documentFocus = Point(0f, 0f), viewportWidth = 100f, viewportHeight = 100f)
        assertThat(v.scale).isEqualTo(Viewport.MAX_SCALE)
    }

    @Test
    fun doubleTapTarget_atFitScale_zoomsInCenteredOnTappedPoint() {
        val fit = Viewport.fit(documentWidth = 1000f, documentHeight = 1000f, viewportWidth = 500f, viewportHeight = 500f)
        val tapScreen = Point(300f, 200f)
        val tappedDocPoint = fit.screenToDocument(tapScreen)

        val target = fit.doubleTapTarget(
            rectX = 0f, rectY = 0f, rectWidth = 1000f, rectHeight = 1000f,
            viewportWidth = 500f, viewportHeight = 500f, tapScreen = tapScreen,
        )

        assertThat(target.scale).isEqualTo(3f)
        // the tapped document point should now be centered in the viewport
        assertPointsClose(target.documentToScreen(tappedDocPoint), Point(250f, 250f))
    }

    @Test
    fun doubleTapTarget_whenZoomedIn_returnsExactFit() {
        val zoomedIn = Viewport(scale = 10f, panX = -500f, panY = -200f)
        val expectedFit = Viewport.fitRect(0f, 0f, 1000f, 1000f, 500f, 500f)

        val target = zoomedIn.doubleTapTarget(
            rectX = 0f, rectY = 0f, rectWidth = 1000f, rectHeight = 1000f,
            viewportWidth = 500f, viewportHeight = 500f, tapScreen = Point(250f, 250f),
        )

        assertThat(target).isEqualTo(expectedFit)
    }

    @Test
    fun doubleTapTarget_justInsideFitTolerance_zoomsIn() {
        val fitScale = Viewport.fit(1000f, 1000f, 500f, 500f).scale
        val nearFit = Viewport(scale = fitScale * 1.09f)

        val target = nearFit.doubleTapTarget(0f, 0f, 1000f, 1000f, 500f, 500f, Point(250f, 250f))

        assertThat(target.scale).isEqualTo(3f)
    }

    @Test
    fun doubleTapTarget_justOutsideFitTolerance_fits() {
        val fitScale = Viewport.fit(1000f, 1000f, 500f, 500f).scale
        val farFromFit = Viewport(scale = fitScale * 1.2f)

        val target = farFromFit.doubleTapTarget(0f, 0f, 1000f, 1000f, 500f, 500f, Point(250f, 250f))

        assertThat(target.scale).isWithin(1e-4f).of(fitScale)
    }

    // --- M8: fitRect ---

    @Test
    fun fitRect_centersAndScalesArbitraryRect() {
        val v = Viewport.fitRect(x = 100f, y = 100f, width = 200f, height = 200f, viewportWidth = 400f, viewportHeight = 400f)

        assertThat(v.scale).isEqualTo(2f)
        // the rect's center (200,200) should land at the viewport's center (200,200)
        assertPointsClose(v.documentToScreen(Point(200f, 200f)), Point(200f, 200f))
    }

    // --- M8: pan reach margin ---

    @Test
    fun clampedTo_marginPxZero_reproducesOldClampExactly() {
        val smallerDoc = Viewport(scale = 1f, panX = 999f, panY = -999f)
            .clampedTo(documentWidth = 100f, documentHeight = 50f, viewportWidth = 300f, viewportHeight = 200f, marginPx = 0f)
        assertThat(smallerDoc.panX).isWithin(1e-3f).of(100f)
        assertThat(smallerDoc.panY).isWithin(1e-3f).of(75f)

        val largerDoc = Viewport(scale = 1f, panX = 500f, panY = 500f)
            .clampedTo(documentWidth = 1000f, documentHeight = 2000f, viewportWidth = 300f, viewportHeight = 400f, marginPx = 0f)
        assertThat(largerDoc.panX).isAtMost(0f)
        assertThat(largerDoc.panY).isAtMost(0f)
    }

    @Test
    fun clampedTo_withMargin_allowsOvershootUpToMarginOnEachSide() {
        // document larger than viewport: pan can overshoot each edge by exactly marginPx
        val overshotTopLeft = Viewport(scale = 1f, panX = 9999f, panY = 9999f)
            .clampedTo(documentWidth = 1000f, documentHeight = 2000f, viewportWidth = 300f, viewportHeight = 400f, marginPx = 50f)
        assertThat(overshotTopLeft.panX).isEqualTo(50f)
        assertThat(overshotTopLeft.panY).isEqualTo(50f)

        val overshotBottomRight = Viewport(scale = 1f, panX = -9999f, panY = -9999f)
            .clampedTo(documentWidth = 1000f, documentHeight = 2000f, viewportWidth = 300f, viewportHeight = 400f, marginPx = 50f)
        assertThat(overshotBottomRight.panX).isEqualTo(300f - 1000f - 50f)
        assertThat(overshotBottomRight.panY).isEqualTo(400f - 2000f - 50f)
    }

    @Test
    fun clampedTo_withMargin_zoomedOutDocument_allowsPanAroundCenteredPositionByMargin() {
        // document smaller than viewport: previously hard-centered (dead zone); margin now allows pan around center
        val panned = Viewport(scale = 1f, panX = 9999f, panY = 9999f)
            .clampedTo(documentWidth = 100f, documentHeight = 50f, viewportWidth = 300f, viewportHeight = 200f, marginPx = 30f)
        // centered pan would be (100, 75); margin allows up to +-30 around that
        assertThat(panned.panX).isEqualTo(130f)
        assertThat(panned.panY).isEqualTo(105f)

        val pannedOtherWay = Viewport(scale = 1f, panX = -9999f, panY = -9999f)
            .clampedTo(documentWidth = 100f, documentHeight = 50f, viewportWidth = 300f, viewportHeight = 200f, marginPx = 30f)
        assertThat(pannedOtherWay.panX).isEqualTo(70f)
        assertThat(pannedOtherWay.panY).isEqualTo(45f)
    }
}
