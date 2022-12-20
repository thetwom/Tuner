package de.moekadu.tuner.notedetection

import kotlin.math.pow

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

enum class TuningState {InTune, TooLow, TooHigh, Unknown}

fun checkTuning(frequency: Float, targetFrequency: Float, toleranceInCents: Float): TuningState {
    if (frequency < 0f || targetFrequency < 0f || toleranceInCents < 0f)
        return TuningState.Unknown

    val ratio = centsToRatio(toleranceInCents)
    val lowerFrequencyBound = targetFrequency / ratio
    val upperFrequencyBound = targetFrequency * ratio

    return if (frequency < lowerFrequencyBound)
        TuningState.TooLow
        else if (frequency > upperFrequencyBound)
        TuningState.TooHigh
        else
        TuningState.InTune
}
