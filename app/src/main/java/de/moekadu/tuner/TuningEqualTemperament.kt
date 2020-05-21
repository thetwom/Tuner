package de.moekadu.tuner

import kotlin.math.log
import kotlin.math.pow
import kotlin.math.roundToInt

class TuningEqualTemperament(val a4Frequency : Float) : TuningFrequencies {

    private val noteName0ToneIndex = -48
    private val noteNames = charArrayOf('A', '-', 'B', 'C', '-', 'D', '-', 'E', 'F', '-', 'G', '-')

    private val halfToneRatio = 2.0f.pow(1.0f/noteNames.size)

    override fun getClosestToneIndex(frequency : Float)  : Int {
        return log(frequency / a4Frequency, halfToneRatio).roundToInt()
    }

    override fun getNoteFrequency(noteIndex : Int) : Float {
       return a4Frequency * halfToneRatio.pow(noteIndex)
    }

   override fun getNoteFrequency(noteIndex : Float) : Float {
       return a4Frequency * halfToneRatio.pow(noteIndex)
    }

    override fun getNoteName(frequency : Float) : String {
        val noteIndex = getClosestToneIndex(frequency)
        return getNoteName(noteIndex, false)
    }

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