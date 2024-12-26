package de.moekadu.tuner.temperaments

import kotlinx.serialization.Serializable

@Serializable
class StretchTuning {

    fun getStretchedFrequency(unstretchedFrequency: Double): Double {
        return unstretchedFrequency
    }
}