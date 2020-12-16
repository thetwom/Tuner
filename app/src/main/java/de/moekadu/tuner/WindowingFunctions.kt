package de.moekadu.tuner

import kotlin.math.PI
import kotlin.math.cos

enum class WindowingFunction {
    Tophat,
    Hamming,
    Hann
}

fun getWindow(window: WindowingFunction, size: Int) = FloatArray(size) { i ->
    when (window) {
        WindowingFunction.Tophat ->
            1.0f
        WindowingFunction.Hamming ->
            0.54f - 0.46f * cos(2.0f * PI.toFloat() * i.toFloat() / size.toFloat())
        WindowingFunction.Hann ->
            0.5f * (1.0f - cos(2.0f * PI.toFloat() * i.toFloat() / size.toFloat()))

    }
}
