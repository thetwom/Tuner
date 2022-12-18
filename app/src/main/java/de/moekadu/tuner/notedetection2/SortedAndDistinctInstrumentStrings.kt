package de.moekadu.tuner.notedetection2

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale

class SortedAndDistinctInstrumentStrings(
    private val instrument: Instrument,
    private val musicalScale: MusicalScale
) {
    /** Note indices of different strings.
     * This list is sorted according to the note indices and made unique such that each note
     * index exists only once. Notes of the instrument, which are not part of the musical scale
     * are excluded. However, the last entry is Int.MAX_VALUE if there are strings which are are
     * not part of the musical scale.
     */
    val sortedAndDistinctNoteIndices = sortStringsAccordingToNoteIndex(instrument, musicalScale)

    /** Number of different notes.
     * Note that the last entry of the sorted note indices can be Int.MAX_VALUE if not all notes are
     * part of the musical scale.
     */
    val numDifferentNotes get() = when {
        sortedAndDistinctNoteIndices.isEmpty() -> 0
        sortedAndDistinctNoteIndices.last() == Int.MAX_VALUE -> sortedAndDistinctNoteIndices.size - 1
        else -> sortedAndDistinctNoteIndices.size
    }

    /** Tells if a note is part of the instrument.
     * @param note Musical note to be checked.
     * @return True, if note is part of instrument, else false.
     */
    fun isNotePartOfInstrument(note: MusicalNote?): Boolean {
        return when {
            note == null -> false
            instrument.isChromatic -> musicalScale.getNoteIndex(note) != Int.MAX_VALUE
            numDifferentNotes == 0 -> false
            else -> {
                val noteIndex = musicalScale.getNoteIndex(note)
                if (noteIndex != Int.MAX_VALUE) {
                    false
                } else {
                    val sortedStringListIndex = sortedAndDistinctNoteIndices.binarySearch(noteIndex)
                    sortedStringListIndex >= 0
                }
            }
        }
    }


    private fun sortStringsAccordingToNoteIndex(instrument: Instrument, musicalScale: MusicalScale): List<Int> {
        if (instrument.isChromatic)
            return ArrayList()
        val strings = instrument.strings

        return strings.map { musicalScale.getNoteIndex(it) }.distinct().sorted()
    }
}