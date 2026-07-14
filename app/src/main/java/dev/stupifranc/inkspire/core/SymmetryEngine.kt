package dev.stupifranc.inkspire.core

/**
 * Symmetry setup for kaleidoscope drawing: [sectors] rotational copies about [center],
 * each with a reflected twin when [mirror] is on (2 * sectors copies total).
 * `sectors = 1, mirror = false` is the "off" (freehand) case.
 */
data class SymmetryConfig(
    val sectors: Int,
    val mirror: Boolean,
    val center: Point,
) {
    init {
        require(sectors in 1..12) { "sectors must be in 1..12, was $sectors" }
    }

    companion object {
        val OFF = SymmetryConfig(sectors = 1, mirror = false, center = Point(0f, 0f))
    }
}

/** Pure math: turns a [SymmetryConfig] into the list of document-space transforms one gesture should be replicated through. */
object SymmetryEngine {

    fun transforms(config: SymmetryConfig): List<Affine2> {
        val angleStep = 360f / config.sectors
        val rotations = (0 until config.sectors).map { i ->
            Affine2.rotationDegreesAbout(angleStep * i, config.center)
        }
        if (!config.mirror) return rotations

        val mirrorAboutCenter = Affine2.translation(config.center.x, config.center.y) *
            Affine2.scale(1f, -1f) *
            Affine2.translation(-config.center.x, -config.center.y)

        val mirroredRotations = rotations.map { rotation -> rotation * mirrorAboutCenter }
        return rotations + mirroredRotations
    }
}
