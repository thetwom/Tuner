package de.moekadu.tuner.temperaments

import kotlinx.serialization.Serializable

@Serializable
data class TemperamentWithNoteNames(
    val temperament: Temperament,
    val noteNames: NoteNames?
) {
    val stableId get() = temperament.stableId
    fun clone(newStableId: Long): TemperamentWithNoteNames {
        return TemperamentWithNoteNames(
            temperament = temperament.copy(stableId = newStableId),
            noteNames = noteNames
        )
    }
}