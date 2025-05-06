package de.moekadu.tuner.instruments

import de.moekadu.tuner.notenames.MusicalNote
import kotlinx.serialization.Serializable

@Serializable
data class InstrumentOld(
    private val name: String?,
    private val nameResource: Int?,
    val strings: Array<MusicalNote>,
    val icon: InstrumentIcon,
    val stableId: Long,
    val isChromatic: Boolean = false
) {
    fun toNew(): Instrument {
       return if (name == null) {
            instrumentDatabase.firstOrNull {
                (icon.name == it.icon.name) &&
                        strings.size == it.strings.size
                        && isChromatic == it.isChromatic
                        && strings.contentEquals(it.strings)
            } ?: instrumentDatabase[0]
        } else {
            Instrument(
                name = name,
                nameResource = nameResource,
                strings = strings,
                icon = icon,
                stableId = stableId,
                isChromatic = isChromatic
            )
        }
    }
}
