package de.moekadu.tuner.notedetection

class AutoCorrelation(
    val size: Int,
    val dt: Float,

) {
    val times = FloatArray(size) { it * dt }
    val values = FloatArray(size)

    /** Values, normalized to range 0 to 1. */
    val plotValuesNormalized = FloatArray(size)
    /** The zero position in plotValuesNormalized. */
    var plotValuesNormalizedZero = 0f

    operator fun get(index: Int) = values[index]
//    operator fun set(index: Int, value: Float) {
//        values[index] = value
//    }
}