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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import kotlin.math.floor
import kotlin.math.max

class TunerFragmentSimple : Fragment() {

    private val viewModel: TunerViewModel by activityViewModels()
    private val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
        InstrumentsViewModel.Factory(
            AppPreferences.readInstrumentId(requireActivity()),
            requireActivity().application)
    }

    private var pitchPlot: PlotView? = null
    private var volumeMeter: VolumeMeter? = null
    private var stringView: StringView? = null
    private var instrumentIcon: ImageView? = null
    private var instrumentTitle: TextView? = null

    private var isPitchInactive = false
    private var tuningStatus = TargetNote.TuningStatus.Unknown

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
        Log.v("Tuner", "TunerFragmentSimple.onCreateView")
        val view = inflater.inflate(R.layout.diagrams_simple, container, false)

        pitchPlot = view.findViewById(R.id.pitch_plot)
        volumeMeter = view.findViewById(R.id.volume_meter)
        stringView = view.findViewById(R.id.string_view)
        instrumentIcon = view.findViewById(R.id.instrument_icon)
        instrumentTitle = view.findViewById(R.id.instrument_title)

        pitchPlot?.yRange(400f, 500f, PlotView.NO_REDRAW)

        stringView?.stringClickedListener = object : StringView.StringClickedListener {
            override fun onStringClicked(toneIndex: Int) {
                if (toneIndex == stringView?.activeToneIndex && viewModel.isTargetNoteUserDefined.value == true) {
                    viewModel.setTargetNote(TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)
                    stringView?.setAutomaticControl()
                    //stringView?.showAnchor = false
                } else if (toneIndex != StringView.NO_ACTIVE_TONE_INDEX) {
                    viewModel.setTargetNote(toneIndex)
                }
            }

            override fun onAnchorClicked() {
                viewModel.setTargetNote(TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)
                stringView?.setAutomaticControl()
            }

            override fun onBackgroundClicked() {
                stringView?.setAutomaticControl()
            }
        }

//            if (toneIndex != StringView.NO_ACTIVE_TONE_INDEX)
//                stringView?.activeToneIndex = toneIndex

        //val stringNames = arrayOf<CharSequence>("A", "BBBBB", "CC", "D", "E", "F", "Q", "R")
//        stringView?.setStrings(intArrayOf(1,2,3,4,5,6,3,4,3,4)) {
//            stringNames[it]
//        }
//        viewModel.standardDeviation.observe(viewLifecycleOwner) { standardDeviation ->
//            volumeMeter?.volume = log10(max(1e-12f, standardDeviation))
//        }

        instrumentTitle?.setOnClickListener {
            (requireActivity() as MainActivity).loadInstrumentsFragment()
        }

        viewModel.isTargetNoteUserDefined.observe(viewLifecycleOwner) { isTargetNoteUserDefined ->
            stringView?.showAnchor = isTargetNoteUserDefined
        }

        viewModel.tuningFrequencies.observe(viewLifecycleOwner) { tuningFrequencies ->
            val noteFrequencies = FloatArray(100) { tuningFrequencies.getNoteFrequency(it - 50) }
            pitchPlot?.setYTicks(noteFrequencies, false) { _, f -> tuningFrequencies.getNoteName(requireContext(), f) }
            pitchPlot?.setYTouchLimits(noteFrequencies.first(), noteFrequencies.last(), 0L)

            if (instrumentsViewModel.instrument.value?.type == InstrumentType.Piano)
                setStringViewToChromatic()
        }

        instrumentsViewModel.instrument.observe(viewLifecycleOwner) { instrument ->
            Log.v("Tuner", "TunerFragmentSimple.onCreateView: instrumentViewModel.instrument: $instrument")
            viewModel.setInstrument(instrument)
            instrumentIcon?.setImageResource(instrument.iconResource)
            instrumentTitle?.text = instrument.getNameString(requireContext())
            if (instrument.type == InstrumentType.Piano) {
                setStringViewToChromatic()
            } else {
                stringView?.setStrings(instrument.strings) { noteIndex ->
                    viewModel.tuningFrequencies.value?.getNoteName(requireContext(), noteIndex, preferFlat = false)
                }
            }
            stringView?.setAutomaticControl(0L)
        }

        viewModel.pitchHistory.sizeAsLiveData.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
            pitchPlot?.xRange(0f, 1.08f * it.toFloat(), PlotView.NO_REDRAW)
        }

//        viewModel.pitchHistory.frequencyPlotRangeAveraged.observe(viewLifecycleOwner) {
////            Log.v("TestRecordFlow", "TunerFragment.plotRange: ${it[0]} -- ${it[1]}")
//            pitchPlot?.yRange(it[0], it[1], 600)
//        }

        viewModel.frequencyPlotRange.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.plotRange: ${it[0]} -- ${it[1]}")
            pitchPlot?.yRange(it[0], it[1], 600)
        }

        viewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
            if (it.size > 0) {
                val frequency = it.last()
                tuningStatus = viewModel.targetNote.value?.getTuningStatus(frequency) ?: TargetNote.TuningStatus.Unknown

                setStyles(isPitchInactive, tuningStatus, false)
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), frequency), redraw = false)
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), frequency), tag = 1L, redraw = false)
                pitchPlot?.plot(it)
            }
        }

        viewModel.targetNote.observe(viewLifecycleOwner) { targetNote ->
            val nameMinusBound = getString(R.string.cent, -targetNote.toleranceInCents)
            val namePlusBound = getString(R.string.cent, targetNote.toleranceInCents)
            viewModel.pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                tuningStatus = targetNote.getTuningStatus(frequency)
            }
            setStyles(isPitchInactive, tuningStatus, false)

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

            pitchPlot?.setYMark(targetNote.frequency, targetNote.getNoteName(requireContext(), false), MARK_ID_FREQUENCY, MarkAnchor.East,
                0, placeLabelsOutsideBoundsIfPossible = true,
                redraw = true)

            stringView?.activeToneIndex = targetNote.toneIndex
            //stringView?.scrollToString(targetNote.toneIndex, 300L)
        }

        viewModel.pitchHistory.numValuesSinceLastLineUpdate.observe(viewLifecycleOwner) { numValuesSinceLastUpdate ->
            val maxTimeBeforeInactive = 0.3f // seconds
            val maxNumValuesBeforeInactive = max(1f, floor(maxTimeBeforeInactive / viewModel.pitchHistoryUpdateInterval))
            isPitchInactive = (numValuesSinceLastUpdate > maxNumValuesBeforeInactive)
            setStyles(isPitchInactive, tuningStatus, redraw = true)
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
        viewModel.setInstrument(instrumentsViewModel.instrument.value ?: instrumentDatabase[0])
    }

    override fun onStop() {
        viewModel.stopSampling()
        super.onStop()
    }

    private fun setStringViewToChromatic() {
        val tuningFrequencies = viewModel.tuningFrequencies.value ?: return
        stringView?.setStrings(IntArray(100) {it - 50}.reversedArray()) { noteIndex ->
            tuningFrequencies.getNoteName(requireContext(), noteIndex, preferFlat = false)
        }
    }

    private fun setStyles(isPitchInactive: Boolean, tuningStatus: TargetNote.TuningStatus, redraw: Boolean) {

        if (isPitchInactive) {
            pitchPlot?.setLineStyle(1, suppressInvalidate = true)
            pitchPlot?.setPointStyle(1, suppressInvalidate = true)
            val pointSize = pitchPlot?.pointSizes?.get(1) ?: 1f

            when (tuningStatus) {
                TargetNote.TuningStatus.TooLow -> {
                    pitchPlot?.setPointStyle(6, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointVisible(true, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointOffset(0f, -1.5f * pointSize, 1L, suppressInvalidate = true)
                }
                TargetNote.TuningStatus.TooHigh -> {
                    pitchPlot?.setPointStyle(5, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointVisible(true, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointOffset(0f, 1.5f * pointSize, 1L, suppressInvalidate = true)
                }
                else -> pitchPlot?.setPointVisible(false, tag = 1L, suppressInvalidate = true)
            }
        } else {
            pitchPlot?.setLineStyle(0, suppressInvalidate = true)
            val pointSize = pitchPlot?.pointSizes?.get(0) ?: 1f

            when (tuningStatus) {
                TargetNote.TuningStatus.TooLow -> {
                    pitchPlot?.setPointStyle(2, suppressInvalidate = true)
                    pitchPlot?.setPointVisible(true, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointStyle(4, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointOffset(0f, -1.5f * pointSize, 1L, suppressInvalidate = true)
                    pitchPlot?.setMarkStyle(2, MARK_ID_FREQUENCY, suppressInvalidate = true)
                }
                TargetNote.TuningStatus.TooHigh -> {
                    pitchPlot?.setPointStyle(2, suppressInvalidate = true)
                    pitchPlot?.setPointVisible(true, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointStyle(3, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setPointOffset(0f, 1.5f * pointSize, 1L, suppressInvalidate = true)
                    pitchPlot?.setMarkStyle(2, MARK_ID_FREQUENCY, suppressInvalidate = true)
                }
                else -> {
                    pitchPlot?.setPointStyle(0, suppressInvalidate = true)
                    pitchPlot?.setPointVisible(false, tag = 1L, suppressInvalidate = true)
                    pitchPlot?.setMarkStyle(0, MARK_ID_FREQUENCY, suppressInvalidate = true)
                }
            }
        }

        when (tuningStatus) {
            TargetNote.TuningStatus.InTune -> stringView?.activeToneStyle = 1
            else -> stringView?.activeToneStyle = 2
        }
        if (redraw)
            pitchPlot?.invalidate()
    }

    companion object{
        private const val MARK_ID_TOLERANCE = 10L
        private const val MARK_ID_FREQUENCY = 11L
    }
}
