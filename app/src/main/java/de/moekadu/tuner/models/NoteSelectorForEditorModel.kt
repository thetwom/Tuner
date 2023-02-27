package de.moekadu.tuner.models

import de.moekadu.tuner.temperaments.*

class NoteSelectorForEditorModel {
    var changeId = 0
        private set

    var musicalScale = MusicalScaleFactory.create(TemperamentType.EDO12)
        private set
    var noteNamePrinter: NoteNamePrinter? = null
        private set
    var scaleChangeId = 0
        private set

    var selectedNote = MusicalNote(BaseNote.A, NoteModifier.None)
        private set
    var selectedNoteId = 0
        private set

    fun changeSettings(
        selectedNote: MusicalNote? = null,
        musicalScale: MusicalScale? = null,
        noteNamePrinter: NoteNamePrinter? = null
    ) {
        changeId++

        if (selectedNote != null) {
            this.selectedNote = selectedNote
            selectedNoteId = changeId
        }

        if (noteNamePrinter != null) {
            this.noteNamePrinter = noteNamePrinter
            scaleChangeId = changeId
        }

        if (musicalScale != null) {
            this.musicalScale = musicalScale
            scaleChangeId = changeId
        }
    }
}