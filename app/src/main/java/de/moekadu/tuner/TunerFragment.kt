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

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import kotlin.math.min

class TunerFragment : Fragment() {

    private val viewModel: TunerViewModel by viewModels() // ? = null

    private var spectrumPlot: PlotView? = null
    private var correlationPlot: PlotView? = null

    private var pitchPlot: PlotView? = null

    private val minCorrelationFrequency = 25f
    /// Instance for requesting audio recording permission.
    /**
     * This will create the sourceJob as soon as the permissions are granted.
     */
    private val askForPermissionAndNotifyViewModel = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            viewModel.startSampling()
        } else {
            // TODO: use string resource
            Toast.makeText(activity, "No audio recording permission is granted", Toast.LENGTH_LONG)
                .show()
            Log.v(
                "TestRecordFlow",
                "TunerFragment.askForPermissionAnNotifyViewModel: No audio recording permission is granted."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.tuningFrequencies.observe(this) { tuningFrequencies ->
            val noteFrequencies = FloatArray(100) { tuningFrequencies.getNoteFrequency(it - 50) }
            pitchPlot?.setYTicks(noteFrequencies, false) { tuningFrequencies.getNoteName(it) }
        }

        viewModel.tunerResults.observe(this) { results ->
            val freqMax = floatArrayOf(results.pitchFrequency)
            val shiftMax = floatArrayOf(1.0f / results.pitchFrequency)
            correlationPlot?.setXMarks(shiftMax) { i -> getString(R.string.hertz, 1.0 / i) }
            spectrumPlot?.setXMarks(freqMax) { i -> getString(R.string.hertz, i) }

            correlationPlot?.xRange(0f,
                min(results.correlationTimes.last(), 1f / minCorrelationFrequency),
                300L)
            correlationPlot?.plot(results.correlationTimes, results.correlation)
            spectrumPlot?.plot(results.ampSpecSqrFrequencies, results.ampSqrSpec)
        }

        viewModel.pitchHistory.sizeAsLiveData.observe(this) {
//            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
            pitchPlot?.xRange(0f, 1.1f * it.toFloat(), PlotView.NO_REDRAW)
        }

        viewModel.pitchHistory.frequencyPlotRange.observe(this) {
//            Log.v("TestRecordFlow", "TunerFragment.plotRange: ${it[0]} -- ${it[1]}")
            pitchPlot?.yRange(it[0], it[1], 600)
        }

        viewModel.pitchHistory.history.observe(this) {
            if (it.size > 0) {
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), it.last()), false)
                pitchPlot?.plot(it)
            }
        }

        viewModel.pitchHistory.currentEstimatedToneIndex.observe(this) {
            viewModel.tuningFrequencies.value?.let { tuningFrequencies ->
                pitchPlot?.setYMarks(floatArrayOf(tuningFrequencies.getNoteFrequency(it))) { i ->
                    tuningFrequencies.getNoteName(i)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.diagrams, container, false)

        pitchPlot = view.findViewById(R.id.pitch_plot)
        spectrumPlot = view.findViewById(R.id.spectrum_plot)
        correlationPlot = view.findViewById(R.id.correlation_plot)

        spectrumPlot?.xRange(0f, 1760f, PlotView.NO_REDRAW)
        spectrumPlot?.setXTicks(
            floatArrayOf(0f, 200f, 400f, 600f, 800f, 1000f, 1200f, 1400f, 1600f),
            true
        ) { i ->
            getString(R.string.hertz, i)
        }

        correlationPlot?.xRange(0f, 1f / minCorrelationFrequency, PlotView.NO_REDRAW)
        correlationPlot?.setXTicks(
            floatArrayOf(
                1 / 1600f,
                1 / 200f,
                1 / 80f,
                1 / 50f,
                1 / 38f,
                1 / 30f
            ), false
        ) { i ->
            getString(R.string.hertz, 1 / i)
        }
        correlationPlot?.setYTicks(floatArrayOf(0f), true) { "" }

        pitchPlot?.yRange(400f, 500f)
        // spectrumPlot?.setXTickTextFormat { i -> getString(R.string.hertz, i) }
//        frequencyText = findViewById(R.id.frequency_text)

        setupPreferencesInViewModel()
        setupPreferenceListeners()

        // plot the values if available, since the plots currently cant store the plot lines.
        viewModel.tunerResults.value?.let { results ->
//            Log.v("Tuner", "TunerFragment: results: 0, ${results.correlationTimes.last()}")
            correlationPlot?.xRange(0f, results.correlationTimes.last(), PlotView.NO_REDRAW)
            correlationPlot?.plot(results.correlationTimes, results.correlation)
            spectrumPlot?.plot(results.ampSpecSqrFrequencies, results.ampSqrSpec)
        }
        viewModel.pitchHistory.history.value?.let {
            if (it.size > 0) {
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), it.last()), false)
                pitchPlot?.plot(it)
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onStop() {
        viewModel.stopSampling()
        super.onStop()
    }

    private fun setupPreferencesInViewModel() {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        viewModel.a4Frequency = sharedPreferences.getString("a4_frequency", "440")?.toFloat() ?: 440f
        viewModel.windowingFunction = when (sharedPreferences.getString("windowing", "no_window")) {
            "no_window" -> WindowingFunction.Tophat
            "window_hamming" -> WindowingFunction.Hamming
            "window_hann" -> WindowingFunction.Hann
            else -> throw RuntimeException("Unknown window")
        }
        viewModel.windowSize = indexToWindowSize(sharedPreferences.getInt("window_size", 5))
        viewModel.overlap = sharedPreferences.getInt("overlap", 25) / 100f
    }

    private fun setupPreferenceListeners() {
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        pref.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
//            Log.v("TestRecordFlow", "TunerFragment.setupPreferenceListener: key=$key")
            when (key) {
                "a4_frequency" -> {
                    val f = sharedPreferences.getFloat(key, 440f)
//                    Log.v("TestRecordFlow", "TunerFragment.setupPreferenceListener: f=$f")
                    viewModel.a4Frequency = f
                }
                "windowing" -> {
                    viewModel.windowingFunction =
                        when (sharedPreferences.getString(key, "no_window")) {
                            "no_window" -> WindowingFunction.Tophat
                            "window_hamming" -> WindowingFunction.Hamming
                            "window_hann" -> WindowingFunction.Hann
                            else -> throw RuntimeException("Unknown window")
                        }
                }
                "window_size" -> {
                    viewModel.windowSize = indexToWindowSize(sharedPreferences.getInt("window_size", 5))
                }
                "overlap" -> {
                    viewModel.overlap = sharedPreferences.getInt(key, 25) / 100f
                }
            }
        }
    }
}

//    private fun initPitchPlot() {
//        pitchPlot?.xRange(0f, 1.1f * 100f, PlotView.NO_REDRAW)
//    pitchPlot?.yRange(200f, 900f, PlotView.NO_REDRAW)
//    //pitchPlot?.setYTickTextFormat { i -> getString(R.string.hertz, i) }
//    //pitchPlot?.setYMarkTextFormat { i -> getString(R.string.hertz, i) }
//    val noteFrequencies = FloatArray(100) { i -> tuningFrequencies.getNoteFrequency(i - 50) }
//    pitchPlot?.setYTicks(noteFrequencies, false) { frequency -> tuningFrequencies.getNoteName(frequency) }
//    //pitchPlot?.setYTicks(floatArrayOf(0f,200f, 400f, 600f, 800f, 1000f,1200f, 1400f, 1600f), false) { i -> getString(R.string.hertz, i) }
//    //pitchPlot?.setYMarks(floatArrayOf(440f))
//    pitchPlot?.setYMarks(floatArrayOf(tuningFrequencies.getNoteFrequency(currentTargetPitchIndex))) { i ->
//      tuningFrequencies.getNoteName(
//        i
//      )
//    }
//    pitchPlot?.plot(pitchHistory.getHistory())
//  }

//
//  private fun doVisualize(preprocessingDataAndPostprocessingResults: PostprocessorThread.PreprocessingDataAndPostprocessingResults) {
//    Log.v("Tuner", "TunerFragment.doVisualize")
//    if(context == null)
//      return
//
//        for (i in 0 until result.numLocalMaxima)
//          spectrumPlotXMarks[i] = RealFFT.getFrequency(result.localMaxima[i], result.spectrum.size-2, dt)
//        spectrumPlot?.setXMarks(spectrumPlotXMarks, result.numLocalMaxima, false) { i ->
//          getString(R.string.hertz, i)
//        }
//        //spectrumPlotXMarks[0] = postResults?.frequency ?: 0f
//        //spectrumPlot?.setXMarks(spectrumPlotXMarks, 1, false)
//        spectrumPlot?.plot(result.frequencies, result.ampSpec)
//      }
//    }
//    else if(pitchDetectorTypePrep == PreprocessorThread.PREPROCESS_AUTOCORRELATION_BASED) {
//      prepArray.last()?.autocorrelationBasedResults?.let{ result ->
//        val dt = 1.0f / sampleRate
//        correlationPlotXMarks[0] = result.idxMaxPitch * dt
//        correlationPlot?.setXMarks(correlationPlotXMarks, 1, false) {i ->
//          getString(R.string.hertz, 1.0/i)
//        }
//        spectrumPlotXMarks[0] = 1.0f / correlationPlotXMarks[0]
//        spectrumPlot?.setXMarks(spectrumPlotXMarks, 1, false) {i ->
//          getString(R.string.hertz, i)
//        }
//        correlationPlot?.plot(result.times, result.correlation)
//        spectrumPlot?.plot(result.frequencies, result.ampSpec)
//      }
//    }
//
//
//        // highlight new target pitch
//        pitchPlot?.setYMarks(
//          floatArrayOf(
//            tuningFrequencies.getNoteFrequency(
//              currentTargetPitchIndex
//            )
//          )
//        ) { i -> tuningFrequencies.getNoteName(i) }
//
//        currentPitchPlotRange[0] = tuningFrequencies.getNoteFrequency(currentTargetPitchIndex - 1.5f)
//        currentPitchPlotRange[1] = tuningFrequencies.getNoteFrequency(currentTargetPitchIndex + 1.5f)
//        pitchPlot?.yRange(currentPitchPlotRange[0], currentPitchPlotRange[1], 600)
//      }
//
//      currentPitchPoint[0] = (pitchHistory.size - 1).toFloat()
//      currentPitchPoint[1] = pitchHistory.getCurrentValue()
//      pitchPlot?.setPoints(currentPitchPoint)
//      pitchPlot?.plot(pitchHistory.getHistory())
//    }
//  }
//
//  private fun setupPitchPlot() {
//    pitchPlot?.xRange(0f, 1.1f * pitchHistory.size.toFloat(), PlotView.NO_REDRAW)
//    //pitchPlot?.yRange(200f, 900f, PlotView.NO_REDRAW)
//    pitchPlot?.yRange(currentPitchPlotRange[0], currentPitchPlotRange[1], PlotView.NO_REDRAW)
//    //pitchPlot?.setYTickTextFormat { i -> getString(R.string.hertz, i) }
//    //pitchPlot?.setYMarkTextFormat { i -> getString(R.string.hertz, i) }
//    val noteFrequencies = FloatArray(100) { i -> tuningFrequencies.getNoteFrequency(i - 50) }
//    pitchPlot?.setYTicks(noteFrequencies, false) { frequency -> tuningFrequencies.getNoteName(frequency) }
//    //pitchPlot?.setYTicks(floatArrayOf(0f,200f, 400f, 600f, 800f, 1000f,1200f, 1400f, 1600f), false) { i -> getString(R.string.hertz, i) }
//    //pitchPlot?.setYMarks(floatArrayOf(440f))
//    pitchPlot?.setYMarks(floatArrayOf(tuningFrequencies.getNoteFrequency(currentTargetPitchIndex))) { i ->
//      tuningFrequencies.getNoteName(
//        i
//      )
//    }
//    pitchPlot?.plot(pitchHistory.getHistory())
//  }
