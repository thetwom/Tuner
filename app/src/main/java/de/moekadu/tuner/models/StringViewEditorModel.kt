package de.moekadu.tuner.models

import de.moekadu.tuner.temperaments.*

class StringViewEditorModel {
    var changeId = 0
        private set
    var musicalScale = MusicalScaleFactory.create(TemperamentType.EDO12)
        private set
    var strings = Array(0) { MusicalNote(BaseNote.A, NoteModifier.None) }
        private set
    var notePrintOptions = MusicalNotePrintOptions.None
        private set
    var stringChangedId = 0
        private set

    var selectedStringIndex = -1
        private set
    var settingsChangedId = 0
        private set

    fun changeSettings(
        strings: Array<MusicalNote>? = null,
        musicalScale: MusicalScale? = null,
        selectedStringIndex: Int = this.selectedStringIndex,
        notePrintOptions: MusicalNotePrintOptions = this.notePrintOptions
    ) {
        changeId++

        if (selectedStringIndex != this.selectedStringIndex) {
            this.selectedStringIndex = selectedStringIndex
            settingsChangedId = changeId
        }

        if (notePrintOptions != this.notePrintOptions) {
            this.notePrintOptions = notePrintOptions
            stringChangedId = changeId
        }

        if (musicalScale != null) {
            this.musicalScale = musicalScale
            stringChangedId = changeId
        }
        if (strings != null) {
            this.strings = strings
            stringChangedId = changeId
        }
    }
}