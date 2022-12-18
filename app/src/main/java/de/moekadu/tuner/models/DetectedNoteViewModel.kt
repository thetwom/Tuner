package de.moekadu.tuner.models

import de.moekadu.tuner.temperaments.*

class DetectedNoteViewModel {
    var changeId = 0
        private set

    var musicalScale = MusicalScaleFactory.create(TemperamentType.EDO12)
        private set
    var notePrintOptions = MusicalNotePrintOptions.None
        private set
    var scaleChangeId = 0
        private set

    var note: MusicalNote? = null
        private set
    var noteChangedId = 0
        private set

    var noteUpdateInterval = 1f
        private set
    var noteUpdateIntervalChangedId = 0
        private set

    fun changeSettings(
        note: MusicalNote? = null,
        musicalScale: MusicalScale? = null,
        notePrintOptions: MusicalNotePrintOptions = this.notePrintOptions,
        noteUpdateInterval: Float = this.noteUpdateInterval
    ) {
        changeId++

        if (note != null) {
            this.note = note
            noteChangedId = changeId
        }

        if (notePrintOptions != this.notePrintOptions) {
            this.notePrintOptions = notePrintOptions
            scaleChangeId = changeId
        }

        if (musicalScale != null) {
            this.musicalScale = musicalScale
            scaleChangeId = changeId
        }

        if (noteUpdateInterval != this.noteUpdateInterval) {
            this.noteUpdateInterval = noteUpdateInterval
            noteUpdateIntervalChangedId = changeId
        }
    }

}