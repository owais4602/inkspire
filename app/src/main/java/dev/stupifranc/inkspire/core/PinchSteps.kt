package dev.stupifranc.inkspire.core

/** A discrete thumbnail-size step crossed by an accumulated pinch gesture. */
enum class PinchStep { StepUp, StepDown }

private const val STEP_UP_THRESHOLD = 1.25f
private const val STEP_DOWN_THRESHOLD = 0.8f

/**
 * Turns a stream of per-frame pinch scale factors into discrete [PinchStep] events with hysteresis,
 * so a single pinch gesture steps the gallery's thumbnail size at most once per [STEP_UP_THRESHOLD]/
 * [STEP_DOWN_THRESHOLD] crossing instead of resizing continuously.
 */
class PinchSteps {
    private var accumulated = 1f

    fun onScaleFactor(factor: Float): PinchStep? {
        accumulated *= factor
        return when {
            accumulated >= STEP_UP_THRESHOLD -> {
                accumulated = 1f
                PinchStep.StepUp
            }
            accumulated <= STEP_DOWN_THRESHOLD -> {
                accumulated = 1f
                PinchStep.StepDown
            }
            else -> null
        }
    }

    fun reset() {
        accumulated = 1f
    }
}
