package de.moekadu.tuner.temperaments

class NoteNameScaleFactory {
    companion object {
        /** Return default reference note for a given temperament (or rather musical scale)
         * @param temperamentType Type of temperament.
         * @return Default reference note (for 12-note-scales, this is A4)
         */
        fun getDefaultReferenceNote(temperamentType: TemperamentType): MusicalNote {
            return when (temperamentType) {
                TemperamentType.EDO12, TemperamentType.Pythagorean, TemperamentType.Pure, TemperamentType.QuarterCommaMeanTone, TemperamentType.ThirdCommaMeanTone, TemperamentType.WerckmeisterIII, TemperamentType.WerckmeisterIV, TemperamentType.WerckmeisterV, TemperamentType.WerckmeisterVI, TemperamentType.Kirnberger1, TemperamentType.Kirnberger2, TemperamentType.Kirnberger3, TemperamentType.Neidhardt1, TemperamentType.Neidhardt2, TemperamentType.Neidhardt3, TemperamentType.Valotti, TemperamentType.Young2, TemperamentType.Test -> {
                    MusicalNote(BaseNote.A, NoteModifier.None, 4)
                }
            }
        }
        /** Create a new note name scale.
         * @param temperamentType Type of temperament.
         * @param preferFlat If true enharmonics with flat are used.
         * @return Note name scale.
         */
        fun create(temperamentType: TemperamentType,
                   preferFlat: Boolean): NoteNameScale {
            val referenceNote = getDefaultReferenceNote(temperamentType)
            return when (temperamentType) {
                TemperamentType.EDO12, TemperamentType.Pythagorean, TemperamentType.Pure, TemperamentType.QuarterCommaMeanTone, TemperamentType.ThirdCommaMeanTone, TemperamentType.WerckmeisterIII, TemperamentType.WerckmeisterIV, TemperamentType.WerckmeisterV, TemperamentType.WerckmeisterVI, TemperamentType.Kirnberger1, TemperamentType.Kirnberger2, TemperamentType.Kirnberger3, TemperamentType.Neidhardt1, TemperamentType.Neidhardt2, TemperamentType.Neidhardt3, TemperamentType.Valotti, TemperamentType.Young2, TemperamentType.Test -> {
                    createNoteNameScale12Tone(referenceNote, preferFlat = preferFlat)
                }
            }
        }
    }
}