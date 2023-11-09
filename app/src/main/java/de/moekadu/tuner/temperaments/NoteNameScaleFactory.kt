package de.moekadu.tuner.temperaments

class NoteNameScaleFactory {
    companion object {
        /** Return default reference note for a given temperament (or rather musical scale)
         * @param temperamentType Type of temperament.
         * @return Default reference note (for 12-note-scales, this is A4)
         */
        fun getDefaultReferenceNote(temperamentType: TemperamentType): MusicalNote {
            return MusicalNote(BaseNote.A, NoteModifier.None, 4)
//            return when (temperamentType) {
//                TemperamentType.EDO12, TemperamentType.Pythagorean, TemperamentType.Pure, TemperamentType.QuarterCommaMeanTone, TemperamentType.ThirdCommaMeanTone, TemperamentType.WerckmeisterIII, TemperamentType.WerckmeisterIV, TemperamentType.WerckmeisterV, TemperamentType.WerckmeisterVI, TemperamentType.Kirnberger1, TemperamentType.Kirnberger2, TemperamentType.Kirnberger3, TemperamentType.Neidhardt1, TemperamentType.Neidhardt2, TemperamentType.Neidhardt3, TemperamentType.Valotti, TemperamentType.Young2, TemperamentType.EDO19, TemperamentType.EDO41, TemperamentType.Test -> {
//                    MusicalNote(BaseNote.A, NoteModifier.None, 4)
//                }
//            }
        }
        /** Create a new note name scale.
         * @param temperamentType Type of temperament.
         * @return Note name scale.
         */
        fun create(temperamentType: TemperamentType): NoteNameScale {
            val referenceNote = getDefaultReferenceNote(temperamentType)
            return when (temperamentType) {
                TemperamentType.EDO12, TemperamentType.Pythagorean, TemperamentType.Pure, TemperamentType.QuarterCommaMeanTone, TemperamentType.ThirdCommaMeanTone, TemperamentType.WerckmeisterIII, TemperamentType.WerckmeisterIV, TemperamentType.WerckmeisterV, TemperamentType.WerckmeisterVI, TemperamentType.Kirnberger1, TemperamentType.Kirnberger2, TemperamentType.Kirnberger3, TemperamentType.Neidhardt1, TemperamentType.Neidhardt2, TemperamentType.Neidhardt3, TemperamentType.Valotti, TemperamentType.Young2, TemperamentType.Test -> {
                    createNoteNameScale12Tone(referenceNote)
                }
                TemperamentType.ExtendedQuarterCommaMeanTone -> {
                    createNoteNameScale15Tone(referenceNote)
                }
                TemperamentType.EDO17 -> {
                    createNoteNameScale17Tone(referenceNote)
                }
                TemperamentType.EDO19 -> {
                    createNoteNameScale19Tone(referenceNote)
                }
                TemperamentType.EDO22 -> {
                    createNoteNameScale22Tone(referenceNote)
                }
                TemperamentType.EDO24 -> {
                    createNoteNameScale24Tone(referenceNote)
                }
                TemperamentType.EDO27 -> {
                    createNoteNameScale27Tone(referenceNote)
                }
                TemperamentType.EDO29 -> {
                    createNoteNameScale29Tone(referenceNote)
                }
                TemperamentType.EDO31 -> {
                    createNoteNameScale31Tone(referenceNote)
                }
                TemperamentType.EDO41 -> {
                    createNoteNameScale41Tone(referenceNote)
                }
                TemperamentType.EDO53 -> {
                    createNoteNameScale53Tone(referenceNote)
                }
            }
        }
    }
}