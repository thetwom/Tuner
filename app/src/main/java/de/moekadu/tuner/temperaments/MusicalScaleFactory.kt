package de.moekadu.tuner.temperaments

class MusicalScaleFactory {
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
        ): MusicalScale {
            val noteNamesResolved = noteNames ?: getSuitableNoteNames(temperament.numberOfNotesPerOctave)!! // TODO: this might be risky?
//            Log.v("Tuner", "MusicalScale2Factory: numberOfNotesPerOctave=${temperament.numberOfNotesPerOctave}, noteNames size=${noteNamesResolved.size}")
            assert(temperament.numberOfNotesPerOctave == noteNamesResolved.size)
            val rootNoteResolved = rootNote ?: noteNamesResolved[0]
            val referenceNoteResolved = referenceNote ?: noteNamesResolved.defaultReferenceNote
//            Log.v("Tuner", "MusicalScale2Factory: $temperament")
            return MusicalScale(
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

        /** Create simple test temperament. */
        fun createTestEdo12(referenceFrequency: Float = 440f): MusicalScale {
            return create(
                createTestTemperamentEdo12(),
                null,
                null,
                null,
                referenceFrequency,
                16f,
                16000f,
                StretchTuning()
            )
        }

        fun createTestWerckmeisterVI(referenceFrequency: Float = 440f): MusicalScale {
            return create(
                createTestTemperamentWerckmeisterVI(),
                null,
                null,
                null,
                referenceFrequency,
                16f,
                16000f,
                StretchTuning()
            )
        }
    }
}