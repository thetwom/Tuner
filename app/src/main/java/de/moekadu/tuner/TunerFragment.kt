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
import android.content.SharedPreferences
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

    private val onPreferenceChangedListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null)
                return
//            Log.v("Tuner", "TunerFragment.setupPreferenceListener: key=$key")
            when (key) {
                "a4_frequency" -> {
//                    Log.v("Tuner", "TunerFragment.setupPreferenceListener: a4_frequency changed")
                    viewModel.a4Frequency = sharedPreferences.getString("a4_frequency", "440")?.toFloat() ?: 440f
                }
                "windowing" -> {
                    val value = sharedPreferences.getString(key, null)
                    viewModel.windowingFunction =
                        when (value) {
                            "no_window" -> WindowingFunction.Tophat
                            "window_hamming" -> WindowingFunction.Hamming
                            "window_hann" -> WindowingFunction.Hann
                            else -> throw RuntimeException("Unknown window")
                        }
                }
                "window_size" -> {
                    viewModel.windowSize = indexToWindowSize(sharedPreferences.getInt(key, 5))
                }
                "overlap" -> {
                    viewModel.overlap = sharedPreferences.getInt(key, 25) / 100f
                }
                "pitch_history_duration" -> {
                    viewModel.pitchHistoryDuration = percentToPitchHistoryDuration(sharedPreferences.getInt(key, 50))
                }
                "pitch_history_num_faulty_values" -> {
                    viewModel.pitchHistory.maxNumFaultyValues = sharedPreferences.getInt(key, 3)
                }
                "use_hint" -> {
                    viewModel.useHint = sharedPreferences.getBoolean(key, true)
                }
            }
        }
    }

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
            Toast.makeText(activity, getString(R.string.no_audio_recording_permission), Toast.LENGTH_LONG)
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

            correlationPlot?.xRange(
                0f,
                min(results.correlationTimes.last(), 1f / minCorrelationFrequency),
                300L
            )
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

        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        pref.registerOnSharedPreferenceChangeListener(onPreferenceChangedListener)
    }

    override fun onDestroy() {
        val pref = PreferenceManager.getDefaultSharedPreferences(activity)
        pref.unregisterOnSharedPreferenceChangeListener(onPreferenceChangedListener)
        super.onDestroy()
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

        setPreferencesInViewModel()

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

    private fun setPreferencesInViewModel() {

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
        viewModel.pitchHistoryDuration = percentToPitchHistoryDuration(sharedPreferences.getInt("pitch_history_duration", 50))
        viewModel.pitchHistory.maxNumFaultyValues = sharedPreferences.getInt("pitch_history_num_faulty_values", 3)
        viewModel.useHint = sharedPreferences.getBoolean("use_hint", true)
    }
}
