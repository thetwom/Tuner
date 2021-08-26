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
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

class TunerFragment : Fragment() {

    private val viewModel: TunerViewModel by viewModels() // ? = null

    private var spectrumPlot: PlotView? = null
    private var correlationPlot: PlotView? = null
    private var pitchPlot: PlotView? = null
    private var volumeMeter: VolumeMeter? = null

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
                "num_moving_average" -> {
                    viewModel.pitchHistory.numMovingAverage = sharedPreferences.getInt(key, 5)
                }
                "max_noise" -> {
                    viewModel.pitchHistory.maxNoise = sharedPreferences.getInt(key, 10) / 100f
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
//        Log.v("Tuner", "TunerFragment.onCreateView")
        val view = inflater.inflate(R.layout.diagrams, container, false)

        pitchPlot = view.findViewById(R.id.pitch_plot)
        spectrumPlot = view.findViewById(R.id.spectrum_plot)
        correlationPlot = view.findViewById(R.id.correlation_plot)
        volumeMeter = view.findViewById(R.id.volume_meter)

        spectrumPlot?.xRange(0f, 1760f, PlotView.NO_REDRAW)
        spectrumPlot?.setXTicks(
            floatArrayOf(0f, 200f, 400f, 600f, 800f, 1000f, 1200f, 1400f, 1600f, 1800f, 2000f, 2200f, 2400f,
                2600f, 2800f, 3000f, 3200f, 3400f, 3600f, 3800f, 4000f, 4500f, 5000f, 5500f, 6000f,
                6500f, 7000f, 7500f, 8000f, 8500f, 9000f, 9500f, 10000f, 11000f, 12000f, 13000f, 14000f,
                15000f, 16000f, 17000f, 18000f, 19000f, 20000f, 25000f, 30000f, 35000f, 40000f),
            false
        ) { _, i ->
            getString(R.string.hertz, i)
        }
        spectrumPlot?.setYTouchLimits(0f, Float.POSITIVE_INFINITY, PlotView.NO_REDRAW)

        correlationPlot?.xRange(0f, 1f / minCorrelationFrequency, PlotView.NO_REDRAW)
        correlationPlot?.setXTicks(
            floatArrayOf(
                1 / 1600f,
                1 / 200f,
                1 / 80f,
                1 / 50f,
                1 / 38f,
                1 / 30f,
                1 / 25f,
                1 / 20f,
                1 / 17f,
                1 / 15f,
                1 / 13f,
                1 / 11f,
                1 / 10f,
                1 / 9f,
                1 / 8f,
                1 / 7f,
                1 / 6f,
                1 / 5f,
                1 / 4f,
                1 / 3f,
                1 / 2f,
                1 / 1f,
            ), false
        ) { _, i ->
            getString(R.string.hertz, 1 / i)
        }
        correlationPlot?.setYTicks(floatArrayOf(0f), false) { _, _ -> "" }

        pitchPlot?.yRange(400f, 500f, PlotView.NO_REDRAW)
        // spectrumPlot?.setXTickTextFormat { i -> getString(R.string.hertz, i) }
//        frequencyText = findViewById(R.id.frequency_text)

        setPreferencesInViewModel()

//        viewModel.standardDeviation.observe(viewLifecycleOwner) { standardDeviation ->
//            volumeMeter?.volume = log10(max(1e-12f, standardDeviation))
//        }
        viewModel.tuningFrequencies.observe(viewLifecycleOwner) { tuningFrequencies ->
            val noteFrequencies = FloatArray(100) { tuningFrequencies.getNoteFrequency(it - 50) }
            pitchPlot?.setYTicks(noteFrequencies, false) { _, f -> tuningFrequencies.getNoteName(f) }
            pitchPlot?.setYTouchLimits(noteFrequencies.first(), noteFrequencies.last(), 0L)
        }

        viewModel.tunerResults.observe(viewLifecycleOwner) { results ->
            if (results.pitchFrequency == null) {
                correlationPlot?.removePlotMarks(null, false)
                spectrumPlot?.removePlotMarks(null, false)
            }
            else {
                results.pitchFrequency?.let { pitchFrequency ->
                    val label = getString(R.string.hertz, pitchFrequency)
                    correlationPlot?.setXMark(1.0f / pitchFrequency, label, MARK_ID_FREQUENCY, MarkAnchor.SouthWest)
                    spectrumPlot?.setXMark(pitchFrequency, label, MARK_ID_FREQUENCY, MarkAnchor.SouthWest)
                }
            }

            correlationPlot?.setXTouchLimits(0f, results.correlationTimes.last(), PlotView.NO_REDRAW)
            correlationPlot?.plot(results.correlationTimes, results.correlation)

            spectrumPlot?.setXTouchLimits(0f, results.ampSpecSqrFrequencies.last(), PlotView.NO_REDRAW)
            spectrumPlot?.plot(results.ampSpecSqrFrequencies, results.ampSqrSpec)

            volumeMeter?.volume = results.noise
        }

        viewModel.pitchHistory.sizeAsLiveData.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
            pitchPlot?.xRange(0f, 1.15f * it.toFloat(), PlotView.NO_REDRAW)
        }

        viewModel.pitchHistory.frequencyPlotRangeAveraged.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.plotRange: ${it[0]} -- ${it[1]}")
            pitchPlot?.yRange(it[0], it[1], 600)
        }

        viewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
            if (it.size > 0) {
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), it.last()), redraw = false)
                pitchPlot?.plot(it)
            }
        }

        viewModel.pitchHistory.currentEstimatedToneIndex.observe(viewLifecycleOwner) { toneIndex ->
            viewModel.tuningFrequencies.value?.let { tuningFrequencies ->
                val boundCents = 5
                val frequency = tuningFrequencies.getNoteFrequency(toneIndex)

                val namePlusBound = getString(R.string.cent, boundCents)
                val frequencyPlusBound = tuningFrequencies.getNoteFrequency(toneIndex + boundCents / 100f)

                val nameMinusBound = getString(R.string.cent, -boundCents)
                val frequencyMinusBound = tuningFrequencies.getNoteFrequency(toneIndex - boundCents / 100f)

                pitchPlot?.setMarks(
                    null,
                    floatArrayOf(frequencyMinusBound, frequencyPlusBound),
                    MARK_ID_TOLERANCE_UPPER,
                    1,
                    arrayOf(MarkAnchor.NorthWest, MarkAnchor.SouthWest),
                    MarkLabelBackgroundSize.FitLargest,
                    false) { i, _, _ ->
                        when (i) {
                            0 -> nameMinusBound
                            1 -> namePlusBound
                            else -> ""
                        }
                    }

                val noteName = tuningFrequencies.getNoteName(frequency)
                pitchPlot?.setYMark(frequency, noteName, MARK_ID_FREQUENCY, MarkAnchor.East, 0, true)
//                val marks = ArrayList<PlotView.Mark>(2)
//                marks.add(PlotView.Mark(PlotView.DRAW_LINE, frequencyPlusBound, namePlusBound, MarkAnchor.SouthWest, 1))
//                marks.add(PlotView.Mark(PlotView.DRAW_LINE, frequencyMinusBound, nameMinusBound, MarkAnchor.NorthWest, 1))
//                pitchPlot?.setMarks(marks, MARK_ID_TOLERANCE, PlotView.MarkLabelBackgroundSize.FitLargest, false)
//                val noteName = tuningFrequencies.getNoteName(frequency)
//                pitchPlot?.setYMark(frequency, noteName, MARK_ID_FREQUENCY, PlotView.MarkAnchor.East, 0, true)
            }
        }

        viewModel.pitchHistory.numValuesSinceLastLineUpdate.observe(viewLifecycleOwner) { numValuesSinceLastUpdate ->
            val maxTimeBeforeInactive = 0.3f // seconds
            val maxNumValuesBeforeInactive = max(1f, floor(maxTimeBeforeInactive / viewModel.pitchHistoryUpdateInterval))
            pitchPlot?.setInactive(numValuesSinceLastUpdate > maxNumValuesBeforeInactive, false)
        }

        // plot the values if available, since the plots currently cant store the plot lines.
        viewModel.tunerResults.value?.let { results ->
//            Log.v("Tuner", "TunerFragment: results: 0, ${results.correlationTimes.last()}")
//            correlationPlot?.xRange(0f, results.correlationTimes.last(), PlotView.NO_REDRAW)
            correlationPlot?.plot(results.correlationTimes, results.correlation)
            spectrumPlot?.plot(results.ampSpecSqrFrequencies, results.ampSqrSpec)
        }
        viewModel.pitchHistory.history.value?.let {
            if (it.size > 0) {
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), it.last()), redraw = false)
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
        viewModel.pitchHistory.numMovingAverage = sharedPreferences.getInt("num_moving_average", 5)
        viewModel.useHint = sharedPreferences.getBoolean("use_hint", true)
        viewModel.pitchHistory.maxNoise = sharedPreferences.getInt("max_noise", 10) / 100f
    }

    companion object{
        private const val MARK_ID_TOLERANCE_LOWER = 9L
        private const val MARK_ID_TOLERANCE_UPPER = 10L
        private const val MARK_ID_FREQUENCY = 11L
    }
}
