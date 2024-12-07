package de.moekadu.tuner.ui.notes

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class NotePrintOptionsOld(
    val sharpFlatPreference: SharpFlatPreference = SharpFlatPreference.None,
    val helmholtzNotation: Boolean = false,
    val notationType: NotationType = NotationType.Standard
) {
    enum class SharpFlatPreference {
        Sharp, Flat, None
    }
}
