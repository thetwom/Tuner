package de.moekadu.tuner.notedetection2

class AutoCorrelation(
    val size: Int,
    val dt: Float,

) {
    val times = FloatArray(size) { it * dt }
    val values = FloatArray(size)

    operator fun get(index: Int) = values[index]
//    operator fun set(index: Int, value: Float) {
//        values[index] = value
//    }
}