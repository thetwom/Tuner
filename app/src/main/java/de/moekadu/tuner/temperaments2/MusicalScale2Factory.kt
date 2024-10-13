package de.moekadu.tuner.temperaments2

import de.moekadu.tuner.temperaments.MusicalNote

class MusicalScale2Factory {
    companion object {
        fun create(
            temperament: Temperament,
            noteNames: NoteNames?,
            referenceNote: MusicalNote?,
            rootNote: MusicalNote?,
            referenceFrequency: Float,
            frequencyMin: Float,
            frequencyMax: Float,
            stretchTuning: StretchTuning
        ): MusicalScale2 {
            val noteNamesResolved = noteNames ?: getSuitableNoteNames(temperament.numberOfNotesPerOctave)
            assert(temperament.numberOfNotesPerOctave == noteNamesResolved.size)
            val rootNoteResolved = rootNote ?: noteNamesResolved[0]
            val referenceNoteResolved = referenceNote ?: noteNamesResolved.defaultReferenceNote

            return MusicalScale2(
                temperament,
                noteNamesResolved,
                rootNoteResolved,
                referenceNoteResolved,
                referenceFrequency,
                frequencyMin,
                frequencyMax,
                stretchTuning
            )
        }
    }
}