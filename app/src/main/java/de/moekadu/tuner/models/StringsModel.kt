package de.moekadu.tuner.models

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.notedetection.SortedAndDistinctInstrumentStrings
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.NoteNamePrinter

class StringsModel {
    var changeId = 0
        private set

    var useExtraPadding = false
        private set
    var musicalScale = MusicalScaleFactory.create(DefaultValues.TEMPERAMENT)
        private set
    var instrument = instrumentDatabase[0]
        private set
    var noteNamePrinter: NoteNamePrinter? = null
        private set
    var isIncompatibleInstrument = false
        private set

    var settingsChangeId = 0
        private set

    var showStringAnchor = false
        private set
    var highlightedStringIndex = -1
        private set
    var highlightedNote: MusicalNote? = null
        private set
    var highlightedStyleIndex = tuningStateToStyleIndex(TuningState.Unknown)
        private set
    var highlightChangeId = 0
        private set

    var isVisible = true
        private set

    fun changeSettings(
        instrument: Instrument? = null,
        musicalScale: MusicalScale? = null,
        noteNamePrinter: NoteNamePrinter? = null,
        highlightedStringIndex: Int = this.highlightedStringIndex,
        highlightedNote: MusicalNote? = this.highlightedNote,
        tuningState: TuningState? = null
    ) {
        changeId++

        var doCheckInstrumentCompatibility = false
        instrument?.let {
            if (this.instrument != it) {
                settingsChangeId = changeId
                doCheckInstrumentCompatibility = true
                isVisible = !(!instrument.isChromatic && instrument.strings.isEmpty())
                this.instrument = it
            }
        }
        musicalScale?.let {
            if (this.musicalScale != it) {
                settingsChangeId = changeId
                doCheckInstrumentCompatibility = true
                this.musicalScale = it
            }
        }
        noteNamePrinter?.let {
            settingsChangeId = changeId
            this.noteNamePrinter = it
            useExtraPadding = (it.noteNameWidth == NoteNamePrinter.MaxNoteNameWidth.MultipleLetters)
        }
        if (this.highlightedStringIndex != highlightedStringIndex) {
            this.highlightedStringIndex = highlightedStringIndex
            showStringAnchor = highlightedStringIndex >= 0
            highlightChangeId = changeId
        }
        if (this.highlightedNote != highlightedNote) {
            this.highlightedNote = highlightedNote
            highlightChangeId = changeId
        }

        tuningState?.let {
            val newTuningStateStyle = tuningStateToStyleIndex(it)
            if (newTuningStateStyle != highlightedStyleIndex) {
                highlightedStyleIndex = newTuningStateStyle
                highlightChangeId = changeId
            }
        }

        if (doCheckInstrumentCompatibility)
            isIncompatibleInstrument = !checkInstrumentCompatibility(this.musicalScale, this.instrument)
    }

    private fun tuningStateToStyleIndex(tuningState: TuningState): Int {
        return when (tuningState) {
            TuningState.InTune -> 2
            else -> 3
        }
    }

    private fun checkInstrumentCompatibility(
        musicalScale: MusicalScale = this.musicalScale,
        instrument: Instrument = this.instrument): Boolean {
        if (instrument.isChromatic)
            return true
        val sortedStrings = SortedAndDistinctInstrumentStrings(instrument, musicalScale)
        return when {
            sortedStrings.sortedAndDistinctNoteIndices.isEmpty() -> true
            sortedStrings.sortedAndDistinctNoteIndices.last() == Int.MAX_VALUE -> false
            else -> true
        }
    }
}