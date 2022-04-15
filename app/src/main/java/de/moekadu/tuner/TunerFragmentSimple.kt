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
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import kotlin.math.floor
import kotlin.math.max

class TunerFragmentSimple : Fragment() {
    // TODO: can we make lower strings in string view a bit thicker?
    private val viewModel: TunerViewModel by activityViewModels()
    private val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
        InstrumentsViewModel.Factory(
            AppPreferences.readInstrumentId(requireActivity()),
            AppPreferences.readInstrumentSection(requireActivity()),
            AppPreferences.readCustomInstruments(requireActivity()),
            AppPreferences.readPredefinedSectionExpanded(requireActivity()),
            AppPreferences.readCustomSectionExpanded(requireActivity()),
            requireActivity().application)
    }

    private var pitchPlot: PlotView? = null
    private var volumeMeter: VolumeMeter? = null
    private var stringView: StringView? = null
    private var instrumentTitle: MaterialButton? = null

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
                "Tuner",
                "TunerFragment.askForPermissionAnNotifyViewModel: No audio recording permission is granted."
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Log.v("Tuner", "TunerFragmentSimple.onCreateView")
        val view = inflater.inflate(R.layout.diagrams_simple, container, false)

        pitchPlot = view.findViewById(R.id.pitch_plot)
        volumeMeter = view.findViewById(R.id.volume_meter)
        stringView = view.findViewById(R.id.string_view)
        instrumentTitle = view.findViewById(R.id.instrument_title)

        pitchPlot?.yRange(400f, 500f, PlotView.NO_REDRAW)

        stringView?.stringClickedListener = object : StringView.StringClickedListener {
            override fun onStringClicked(stringIndex: Int, toneIndex: Int) {
                if (stringIndex == stringView?.highlightedStringIndex && viewModel.isTargetNoteUserDefined.value == true) {
                    viewModel.setTargetNote(-1, TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)
                    stringView?.setAutomaticControl()
                    //stringView?.showAnchor = false
                } else if (stringIndex != -1) {
                    viewModel.setTargetNote(stringIndex, toneIndex)
                }
            }

            override fun onAnchorClicked() {
                viewModel.setTargetNote(-1, TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)
                stringView?.setAutomaticControl()
            }

            override fun onBackgroundClicked() {
                // stringView?.setAutomaticControl()
            }
        }

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
            updatePitchPlotNoteNames()
            // TODO: should we extend the limits slightly, that the whole mark is visible?
            val firstFrequencyIndex = tuningFrequencies.getToneIndexBegin()
            val lastFrequencyIndex = tuningFrequencies.getToneIndexEnd() - 1
            val firstFrequency = tuningFrequencies.getNoteFrequency(firstFrequencyIndex)
            val lastFrequency = tuningFrequencies.getNoteFrequency(lastFrequencyIndex)
            pitchPlot?.setYTouchLimits(firstFrequency, lastFrequency, 0L)
            // we do not have to call updatePitchPlotMarks, since this is fully handled by
            // observing the target note

            if (instrumentsViewModel.instrument.value?.instrument?.isChromatic == true)
                updateStringViewNoteNames()
        }

        viewModel.noteNames.observe(viewLifecycleOwner) { // noteNames ->
            updatePitchPlotNoteNames()
            updateStringViewNoteNames()
        }

        viewModel.preferFlat.observe(viewLifecycleOwner) {
            updatePitchPlotNoteNames()
            updateStringViewNoteNames()
        }

        instrumentsViewModel.instrument.observe(viewLifecycleOwner) { instrumentAndSection ->
            val instrument = instrumentAndSection.instrument
            //Log.v("Tuner", "TunerFragmentSimple.onCreateView: instrumentViewModel.instrument: $instrument")
            viewModel.setInstrument(instrument)
            //instrumentIcon?.setImageResource(instrument.iconResource)
            instrumentTitle?.setIconResource(instrument.iconResource)
            instrumentTitle?.text = instrument.getNameString(requireContext())
            updateStringViewNoteNames()
            //stringView?.setAutomaticControl(0L)
        }

        viewModel.pitchHistory.sizeAsLiveData.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
            pitchPlot?.xRange(0f, 1.08f * it.toFloat(), PlotView.NO_REDRAW)
        }

        viewModel.frequencyPlotRange.observe(viewLifecycleOwner) {
//            Log.v("Tuner", "TunerFragmentSimple observe frequencyPlotRange: ${it[0]} -- ${it[1]}")
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
            viewModel.pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                tuningStatus = targetNote.getTuningStatus(frequency)
            }
//            Log.v("Tuner", "TunerFragmentSimple: observing targetNote: tuningStatus=$tuningStatus")
            setStyles(isPitchInactive, tuningStatus, redraw = false)

            updatePitchPlotMarks(redraw = true)

//            Log.v("Tuner", "TunerFragmentSimple: target note changed: stringIndex = ${targetNote.stringIndex}, toneIndex=${targetNote.toneIndex}")
            if (targetNote.stringIndex != -1)
                stringView?.highlightSingleString(targetNote.stringIndex, 300L)
            else
                stringView?.highlightByToneIndex(targetNote.toneIndex, 300L)
            //stringView?.scrollToString(targetNote.toneIndex, 300L)
        }

        viewModel.pitchHistory.numValuesSinceLastLineUpdate.observe(viewLifecycleOwner) { numValuesSinceLastUpdate ->
            val maxTimeBeforeInactive = 0.3f // seconds
            val maxNumValuesBeforeInactive = max(1f, floor(maxTimeBeforeInactive / (viewModel.pitchHistoryUpdateInterval.value ?: 1f)))
            isPitchInactive = (numValuesSinceLastUpdate > maxNumValuesBeforeInactive)
            setStyles(isPitchInactive, tuningStatus, redraw = true)
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
        viewModel.setInstrument(instrumentsViewModel.instrument.value?.instrument ?: instrumentDatabase[0])
    }

    override fun onStop() {
        viewModel.stopSampling()
        super.onStop()
    }

    private fun setStyles(isPitchInactive: Boolean, tuningStatus: TargetNote.TuningStatus, redraw: Boolean) {
        // Log.v("Tuner", "TunerFragmentSimple.setStyles: tuningStatus=$tuningStatus")
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

    private fun updatePitchPlotMarks(redraw: Boolean = true) {
        val targetNote = viewModel.targetNote.value ?: return
        val noteNames = viewModel.noteNames.value ?: return
        val preferFlat = viewModel.preferFlat.value ?: false

        val nameMinusBound = getString(R.string.cent, -targetNote.toleranceInCents)
        val namePlusBound = getString(R.string.cent, targetNote.toleranceInCents)

        pitchPlot?.setMarks(
            null,
            floatArrayOf(targetNote.frequencyLowerTolerance, targetNote.frequencyUpperTolerance),
            MARK_ID_TOLERANCE,
            1,
            arrayOf(MarkAnchor.NorthWest, MarkAnchor.SouthWest),
            MarkLabelBackgroundSize.FitLargest,
            placeLabelsOutsideBoundsIfPossible = false,
            redraw = false
        ) { i, _, _ ->
            when (i) {
                0 -> nameMinusBound
                1 -> namePlusBound
                else -> ""
            }
        }

        pitchPlot?.setYMark(
            targetNote.frequency,
            noteNames.getNoteName(requireContext(), targetNote.toneIndex, preferFlat = preferFlat),
            MARK_ID_FREQUENCY,
            MarkAnchor.East,
            if (tuningStatus == TargetNote.TuningStatus.InTune) 0 else 2,
            placeLabelsOutsideBoundsIfPossible = true,
            redraw = redraw
        )
    }

    private fun updatePitchPlotNoteNames(redraw: Boolean = true) {
        val noteNames = viewModel.noteNames.value ?: return
        val preferFlat = viewModel.preferFlat.value ?: return
        val tuningFrequencies = viewModel.tuningFrequencies.value ?: return

        val numNotes = tuningFrequencies.getToneIndexEnd() - tuningFrequencies.getToneIndexBegin()
        val noteFrequencies = FloatArray(numNotes) {
            tuningFrequencies.getNoteFrequency(tuningFrequencies.getToneIndexBegin() + it)
        }

        // Update ticks in pitch history plot
        pitchPlot?.setYTicks(noteFrequencies, false) { _, f ->
            val toneIndex = tuningFrequencies.getClosestToneIndex(f)
            noteNames.getNoteName(requireContext(), toneIndex, preferFlat = preferFlat)
        }

        // Update active ymark in pitch history plot
        viewModel.targetNote.value?.let { targetNote ->
            pitchPlot?.setYMark(
                targetNote.frequency,
                noteNames.getNoteName(requireContext(), targetNote.toneIndex, preferFlat),
                MARK_ID_FREQUENCY,
                MarkAnchor.East,
                if (tuningStatus == TargetNote.TuningStatus.InTune) 0 else 2,
                placeLabelsOutsideBoundsIfPossible = true,
                redraw = redraw
            )
        }
    }

    private fun updateStringViewNoteNames() {
        val instrument = instrumentsViewModel.instrument.value?.instrument ?: return
        val noteNames = viewModel.noteNames.value ?: return
        val preferFlat = viewModel.preferFlat.value ?: false
        val ctx = context ?: return

        if (instrument.isChromatic) {
            viewModel.tuningFrequencies.value?.let { tuningFrequencies ->
                val numNotes = tuningFrequencies.getToneIndexEnd() - tuningFrequencies.getToneIndexBegin()
                stringView?.setStrings(IntArray(numNotes) { tuningFrequencies.getToneIndexBegin() + it }
                    .reversedArray()) { noteIndex ->
                    noteNames.getNoteName(ctx, noteIndex, preferFlat = preferFlat)
                }
            }
        } else {
            stringView?.setStrings(instrument.strings) { noteIndex ->
                noteNames.getNoteName(ctx, noteIndex, preferFlat = preferFlat)
            }
        }
    }

    companion object{
        private const val MARK_ID_TOLERANCE = 10L
        private const val MARK_ID_FREQUENCY = 11L
    }
}
