package dev.stupifranc.inkspire.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinchStepsTest {

    @Test
    fun slowCreepBelowThreshold_emitsNothing() {
        val steps = PinchSteps()

        val results = listOf(1.05f, 1.05f, 1.05f).map { steps.onScaleFactor(it) }

        assertThat(results).containsExactly(null, null, null)
    }

    @Test
    fun crossingUpThreshold_emitsExactlyOneStepThenRequiresRecrossing() {
        val steps = PinchSteps()

        val first = steps.onScaleFactor(1.3f)
        val second = steps.onScaleFactor(1.01f)

        assertThat(first).isEqualTo(PinchStep.StepUp)
        assertThat(second).isNull()
    }

    @Test
    fun alternatingJitterAroundOne_emitsNothing() {
        val steps = PinchSteps()

        val results = listOf(1.1f, 0.91f, 1.1f, 0.91f).map { steps.onScaleFactor(it) }

        assertThat(results).containsExactly(null, null, null, null)
    }

    @Test
    fun twoFullCrossings_emitTwoSteps() {
        val steps = PinchSteps()

        val first = steps.onScaleFactor(1.3f)
        val second = steps.onScaleFactor(1.3f)

        assertThat(first).isEqualTo(PinchStep.StepUp)
        assertThat(second).isEqualTo(PinchStep.StepUp)
    }

    @Test
    fun crossingDownThreshold_emitsStepDown() {
        val steps = PinchSteps()

        val result = steps.onScaleFactor(0.7f)

        assertThat(result).isEqualTo(PinchStep.StepDown)
    }

    @Test
    fun gestureReset_clearsAccumulation() {
        val steps = PinchSteps()
        steps.onScaleFactor(1.1f)

        steps.reset()

        assertThat(steps.onScaleFactor(1.2f)).isNull()
    }
}
