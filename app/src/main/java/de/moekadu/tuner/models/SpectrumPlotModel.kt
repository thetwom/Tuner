package de.moekadu.tuner.models

import android.util.Log
import de.moekadu.tuner.notedetection.FrequencySpectrum
import de.moekadu.tuner.notedetection.Harmonics
import de.moekadu.tuner.temperaments.MusicalNotePrintOptions
import de.moekadu.tuner.temperaments.Notation
import kotlin.math.min

class SpectrumPlotModel {
    var changeId = 0
        private set

    // the following values will be changed togehter with noteDetectionChangedId
    var frequencies = FloatArray(0)
        private set
    var squaredAmplitudes = FloatArray(0)
        private set

    var numHarmonics = 0
        private set
    var harmonicsFrequencies = FloatArray(0)
        private set
    var harmonicNumbers = IntArray(0)
        private set

    var detectedFrequency = -1f
        private set
    var noteDetectionChangeId = changeId
        private set

    // the following values will be changed together with targetChangeId
    var targetFrequency = -1f
        private set
    val frequencyRange = FloatArray(2) { 0f }

    var targetChangeId = changeId
        private set

    // this is independent on change id
    var useExtraPadding = false
        private set

    fun changeSettings(
        frequencySpectrum: FrequencySpectrum? = null,
        harmonics: Harmonics? = null,
        detectedFrequency: Float = -1f,
        targetFrequency: Float = -1f,
        notePrintOptions: MusicalNotePrintOptions? = null
    ) {

        ++changeId
        frequencySpectrum?.let {
            if (frequencies.size != it.size) {
                frequencies = FloatArray(it.size)
                squaredAmplitudes = FloatArray(it.size)
            }
            it.frequencies.copyInto(frequencies)
            it.amplitudeSpectrumSquared.copyInto(squaredAmplitudes)
            noteDetectionChangeId = changeId
        }

        harmonics?.let {
            if (harmonicsFrequencies.size < it.size) {
                harmonicsFrequencies = FloatArray(it.size)
                harmonicNumbers = IntArray(it.size)
            }
            for (i in 0 until it.size) {
                harmonicsFrequencies[i] = harmonics[i].frequency
                harmonicNumbers[i] = harmonics[i].harmonicNumber
            }
            numHarmonics = it.size
            noteDetectionChangeId = changeId
        }

        if (detectedFrequency > 0f) {
            this.detectedFrequency = detectedFrequency
            noteDetectionChangeId = changeId
        }

        if (targetFrequency > 0f) {
            this.targetFrequency = targetFrequency
            Log.v("Tuner", "SpectrumPlotModel.changeSettings: frequencies.size()=${frequencies.size}")
            frequencyRange[1] = min(3.5f * this.targetFrequency, frequencies.lastOrNull() ?: Float.MAX_VALUE)
            targetChangeId = changeId
        }

        notePrintOptions?.let {
            useExtraPadding = (notePrintOptions.notation == Notation.solfege)
        }
    }
}