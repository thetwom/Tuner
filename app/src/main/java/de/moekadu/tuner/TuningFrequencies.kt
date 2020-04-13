package de.moekadu.tuner

import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

class TuningFrequencies {

    private val concertPitch = 440f

    private val noteName0ToneIndex = -12
    private val noteNames = charArrayOf('A', '-', 'B', 'C', '-', 'D', '-', 'E', 'F', '-', 'G', '-')

    val halfToneRatio = 2.0f.pow(1.0f/noteNames.size)

    fun getClosestToneIndex(frequency : Float)  : Int {
        return log(frequency/concertPitch, halfToneRatio).roundToInt()
    }

    fun getNoteFrequency(noteIndex : Int) : Float {
       return concertPitch * halfToneRatio.pow(noteIndex)
    }

    fun getNoteName(frequency : Float) : String {
        val noteIndex = getClosestToneIndex(frequency)
        return getNoteName(noteIndex, false)
    }

    fun getNoteName(toneIndex : Int, preferFlat : Boolean) : String {
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