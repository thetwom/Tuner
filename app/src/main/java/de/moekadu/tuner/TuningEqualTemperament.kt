/*
 * Copyright 2020 Michael Moessner
 *
 * This file is part of Tuner.
 *
 * Tuner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner

import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

/// Class containing notes for equal temperament
/**
 * @param a4Frequency Frequency for a4
 */
class TuningEqualTemperament(private val a4Frequency : Float = 440f) : TuningFrequencies {

    /// Tone index (as e.g. used in getNoteFrequency) for name of noteNames[0]
    private val noteName0ToneIndex = -48

    /// Note names in half tones. A '-' refers to sharp/flat note in between the neighboring tones.
    private val noteNames = charArrayOf('A', '-', 'B', 'C', '-', 'D', '-', 'E', 'F', '-', 'G', '-')

    /// Ratio between two neighboring half tones
    private val halfToneRatio = 2.0f.pow(1.0f / noteNames.size)

    /// Get tone index which for the given frequency.
    /**
     * @note We return a float here since a frequency can lay between two tones
     * @param frequency Frequency
     * @return Note index.
     */
    override fun getToneIndex(frequency : Float)  : Float {
        return log(frequency / a4Frequency, halfToneRatio)
    }

    /// Get tone index which is closest to the given frequency.
    /**
     * @param frequency Frequency
     * @return Note index which is needed by several other class methods.
     */
    override fun getClosestToneIndex(frequency : Float)  : Int {
        return getToneIndex(frequency).roundToInt()
    }

    /// Get frequency of note with the given index.
    /**
     * @param noteIndex Note index as e.g. returned by getClosestToneIndex. Two succeeding
     *   indices give a distance of one half tone.
     * @return Frequency of note index.
     */
    override fun getNoteFrequency(noteIndex : Int) : Float {
       return a4Frequency * halfToneRatio.pow(noteIndex)
    }

    /// Get frequency of not with the given index, where the index can also be in between two notes.
    /**
     * @param noteIndex Note index as e.g. returned by getClosestToneIndex. Two succeeding
     *   indices give a distance of one half tone.
     * @return Frequency for note index.
    */
   override fun getNoteFrequency(noteIndex : Float) : Float {
       return a4Frequency * halfToneRatio.pow(noteIndex)
    }

    /// Get note name best fitting to a specific frequency.
    /**
     * @param frequency Frequency
     * @return Note name which fits best to the frequency.
     */
    override fun getNoteName(frequency : Float) : String {
        val noteIndex = getClosestToneIndex(frequency)
        return getNoteName(noteIndex, false)
    }


    /// Get note name for a given note index
    /**
     * @param toneIndex Note index as e.g. returned by getClosestToneIndex. Two succeeding
     *   indices give a distance of one half tone.
     * @param preferFlat If the best fitting note is flat or sharp and this parameter is true,
     *   the "flat" version is preferred. Else the sharp version is returned.
     * @return Note name.
     */
    override fun getNoteName(toneIndex : Int, preferFlat : Boolean) : String {
        val relativeTone0 = toneIndex - noteName0ToneIndex
        var octaveIndex = relativeTone0 / noteNames.size
        var noteIndexWithinOctave = relativeTone0 % noteNames.size
        if (noteIndexWithinOctave < 0) {
            octaveIndex -= 1
            noteIndexWithinOctave += noteNames.size
        }

        var noteName = noteNames[noteIndexWithinOctave]
        var noteModifier = ""

        if (noteName == '-' && preferFlat) {
            noteName = noteNames[(noteIndexWithinOctave + 1) % noteNames.size]
            if (noteIndexWithinOctave + 1 == noteNames.size)
                octaveIndex += 1
            noteModifier = "\u266D"
        } else if (noteName == '-') {
            noteName = noteNames[(noteIndexWithinOctave - 1) % noteNames.size]
            if (noteIndexWithinOctave == 0)
                octaveIndex -= 1
            noteModifier = "\u266F"
        }
        var octaveText = ""
        if (octaveIndex > 0)
            octaveText = "'".repeat(octaveIndex)
        else if (octaveIndex < 0)
            octaveText = ",".repeat(-octaveIndex)
        return noteName + noteModifier + octaveText
    }
}
