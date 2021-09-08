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
import kotlin.math.floor
import kotlin.math.max

class TunerFragmentSimple : Fragment() {

    private val viewModel: TunerViewModel by viewModels() // ? = null

    private var pitchPlot: PlotView? = null
    private var volumeMeter: VolumeMeter? = null

    private val currentFrequency
        get() = viewModel.pitchHistory.historyAveraged.value?.lastOrNull()
    private val currentPointStyle
        get() = when (viewModel.targetNote.value?.getTuningStatus(currentFrequency)) {
            TargetNote.TuningStatus.InTune -> 0
            else -> 2
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Log.v("Tuner", "TunerFragment.onCreateView")
        val view = inflater.inflate(R.layout.diagrams_simple, container, false)

        pitchPlot = view.findViewById(R.id.pitch_plot)
        volumeMeter = view.findViewById(R.id.volume_meter)

        pitchPlot?.yRange(400f, 500f, PlotView.NO_REDRAW)

//        viewModel.standardDeviation.observe(viewLifecycleOwner) { standardDeviation ->
//            volumeMeter?.volume = log10(max(1e-12f, standardDeviation))
//        }
        viewModel.tuningFrequencies.observe(viewLifecycleOwner) { tuningFrequencies ->
            val noteFrequencies = FloatArray(100) { tuningFrequencies.getNoteFrequency(it - 50) }
            pitchPlot?.setYTicks(noteFrequencies, false) { _, f -> tuningFrequencies.getNoteName(f) }
            pitchPlot?.setYTouchLimits(noteFrequencies.first(), noteFrequencies.last(), 0L)
        }

        viewModel.pitchHistory.sizeAsLiveData.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
            pitchPlot?.xRange(0f, 1.08f * it.toFloat(), PlotView.NO_REDRAW)
        }

        viewModel.pitchHistory.frequencyPlotRangeAveraged.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.plotRange: ${it[0]} -- ${it[1]}")
            pitchPlot?.yRange(it[0], it[1], 600)
        }

        viewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
            if (it.size > 0) {
                setTuningArrow(redraw = false)
                pitchPlot?.setPointStyle(currentPointStyle, suppressInvalidate = false)
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), it.last()), redraw = false)
                pitchPlot?.plot(it)
            }
        }

        viewModel.targetNote.observe(viewLifecycleOwner) { targetNote ->
            val nameMinusBound = getString(R.string.cent, -targetNote.toleranceInCents)
            val namePlusBound = getString(R.string.cent, targetNote.toleranceInCents)
            setTuningArrow(redraw = false)

            pitchPlot?.setMarks(
                null,
                floatArrayOf(targetNote.frequencyLowerTolerance, targetNote.frequencyUpperTolerance),
                MARK_ID_TOLERANCE,
                1,
                arrayOf(MarkAnchor.NorthWest, MarkAnchor.SouthWest),
                MarkLabelBackgroundSize.FitLargest,
                placeLabelsOutsideBoundsIfPossible = false,
                redraw = false) { i, _, _ ->
                when (i) {
                    0 -> nameMinusBound
                    1 -> namePlusBound
                    else -> ""
                }
            }

            pitchPlot?.setYMark(targetNote.frequency, targetNote.name, MARK_ID_FREQUENCY, MarkAnchor.East,
                0, placeLabelsOutsideBoundsIfPossible = true,
                redraw = true)
        }

        viewModel.pitchHistory.numValuesSinceLastLineUpdate.observe(viewLifecycleOwner) { numValuesSinceLastUpdate ->
            val maxTimeBeforeInactive = 0.3f // seconds
            val maxNumValuesBeforeInactive = max(1f, floor(maxTimeBeforeInactive / viewModel.pitchHistoryUpdateInterval))
//            if (numValuesSinceLastUpdate > maxNumValuesBeforeInactive)
//                pitchPlot?.setPointVisible(false, 1L, suppressInvalidate = true)
            pitchPlot?.setLineStyle(if (numValuesSinceLastUpdate > maxNumValuesBeforeInactive) 1 else 0, suppressInvalidate = true)
            pitchPlot?.setPointStyle(if (numValuesSinceLastUpdate > maxNumValuesBeforeInactive) 1 else currentPointStyle, suppressInvalidate = false)
        }

//        viewModel.pitchHistory.history.value?.let {
//            if (it.size > 0) {
//                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), it.last()), redraw = false)
//                pitchPlot?.setPointStyle(currentPointStyle, suppressInvalidate = true)
//                pitchPlot?.plot(it)
//            }
//        }

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

    private fun setTuningArrow(redraw: Boolean) {
        val pointSize = pitchPlot?.pointSizes?.get(0) ?: return
        val frequency = viewModel.pitchHistory.historyAveraged.value?.lastOrNull() ?: return
        val historyIndex = viewModel.pitchHistory.historyAveraged.value?.size?.toFloat() ?: return
        val tuningStatus = viewModel.targetNote.value?.getTuningStatus(frequency)
            ?: TargetNote.TuningStatus.Unknown
        // Log.v("Tuner", "TunerFragmentSimple.setTuningStatus: tuningStatus=$tuningStatus, freq=$frequency, pointSize=$pointSize")

        when (tuningStatus) {
            TargetNote.TuningStatus.TooHigh -> {
                pitchPlot?.setPointVisible(true, 1L, true)
                pitchPlot?.setPointStyle(3, 1L, true)
                pitchPlot?.setPointOffset(0f, 1.5f * pointSize, 1L, true)
                pitchPlot?.setPoints(floatArrayOf(historyIndex - 1, frequency), 1L, redraw = redraw)
            }
            TargetNote.TuningStatus.TooLow -> {
                pitchPlot?.setPointVisible(true, 1L, true)
                pitchPlot?.setPointStyle(4, 1L, true)
                pitchPlot?.setPointOffset(0f, -1.5f * pointSize, 1L, true)
                pitchPlot?.setPoints(floatArrayOf(historyIndex - 1, frequency), 1L, redraw = redraw)
            }
            else -> {
                pitchPlot?.setPointVisible(false, 1L, !redraw)
            }
        }
    }

    companion object{
        private const val MARK_ID_TOLERANCE = 10L
        private const val MARK_ID_FREQUENCY = 11L
    }
}
