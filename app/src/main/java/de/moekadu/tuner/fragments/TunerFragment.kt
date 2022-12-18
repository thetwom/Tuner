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

package de.moekadu.tuner.fragments

import android.Manifest
import android.graphics.Paint
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.instrumentResources
import de.moekadu.tuner.misc.WaveFileWriterIntent
import de.moekadu.tuner.models.PitchHistoryModel
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.temperaments.TargetNote
import de.moekadu.tuner.viewmodels.TunerViewModel
import de.moekadu.tuner.views.*
import kotlinx.coroutines.launch

class TunerFragment : Fragment() {
    val viewModel: TunerViewModel by viewModels {
        TunerViewModel.Factory(
            requireActivity().preferenceResources,
            requireActivity().instrumentResources,
            simpleMode = false
        )
    }
//    val viewModel: TunerViewModel by activityViewModels() // ? = null
//    private val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
//        InstrumentsViewModel.Factory(
//            AppPreferences.readInstrumentId(requireActivity()),
//            AppPreferences.readInstrumentSection(requireActivity()),
//            AppPreferences.readCustomInstruments(requireActivity()),
//            AppPreferences.readPredefinedSectionExpanded(requireActivity()),
//            AppPreferences.readCustomSectionExpanded(requireActivity()),
//            requireActivity().application
//        )
//    }
    private val waveFileWriterIntent = WaveFileWriterIntent(this)

    private var spectrumPlot: PlotView? = null
    private var spectrumPlotChangeId = -1
    private var correlationPlot: PlotView? = null
    private var correlationPlotChangeId = -1
    private var pitchPlot: PlotView? = null
    private var pitchPlotChangeId = -1
    private var volumeMeter: VolumeMeter? = null
    private var recordFab: FloatingActionButton? = null

    // private var isPitchInactive = false
    // private var tuningStatus = TargetNote.TuningStatus.Unknown

    private val minCorrelationFrequency = 25f // TODO: this should not stay here

    /** Instance for requesting audio recording permission.
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

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.toolbar, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    // User chose the "Settings" item, show the app settings UI...
                    (activity as MainActivity?)?.loadSettingsFragment()
                    return true
                }
            }
            return false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Log.v("Tuner", "TunerFragment.onCreateView")
        val view = inflater.inflate(R.layout.diagrams, container, false)

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        pitchPlot = view.findViewById(R.id.pitch_plot)
        spectrumPlot = view.findViewById(R.id.spectrum_plot)
        correlationPlot = view.findViewById(R.id.correlation_plot)
        volumeMeter = view.findViewById(R.id.volume_meter)
        recordFab = view.findViewById(R.id.record)

        spectrumPlot?.xRange(0f, 1600f, PlotView.NO_REDRAW)
        spectrumPlot?.setXTicks(
            floatArrayOf(0f, 250f, 500f, 750f, 1000f, 1250f, 1500f, 1750f, 2000f, 2250f, 2500f,
                2750f, 3000f, 3250f, 3500f, 3750f, 4000f, 4500f, 5000f, 5500f, 6000f,
                6500f, 7000f, 7500f, 8000f, 8500f, 9000f, 9500f, 10000f, 11000f, 12000f, 13000f, 14000f,
                15000f, 16000f, 17000f, 18000f, 19000f, 20000f, 25000f, 30000f, 35000f, 40000f),
            redraw = false
        ) { _, i ->
            getString(R.string.hertz, i)
        }
        spectrumPlot?.setYTouchLimits(0f, Float.POSITIVE_INFINITY, PlotView.NO_REDRAW)

        correlationPlot?.xRange(0f, 1f / minCorrelationFrequency, PlotView.NO_REDRAW)
        correlationPlot?.setXTicks(
            floatArrayOf(
                1 / 1600f,
                1 / 150f,
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
            ), redraw = false
        ) { _, i ->
            getString(R.string.hertz, 1 / i)
        }
        correlationPlot?.setYTicks(floatArrayOf(0f), redraw = false) { _, _ -> "" }

        viewModel.spectrumPlotModel.observe(viewLifecycleOwner) { model ->
//            Log.v("Tuner", "TunerFragment: spectrumModel.observe: changeId=${model.changeId}, noteDetectionId=${model.noteDetectionChangeId}, targetId=${model.targetChangeId}")
            if (model.changeId < spectrumPlotChangeId)
                spectrumPlotChangeId = -1
            if (model.noteDetectionChangeId > spectrumPlotChangeId) {
                spectrumPlot?.plot(model.frequencies, model.squaredAmplitudes, redraw = false)
                spectrumPlot?.setMarks(model.harmonicsFrequencies, null, HARMONIC_ID,
                    indexEnd = model.numHarmonics, redraw = false)
                val label = getString(R.string.hertz, model.detectedFrequency)
                if (model.detectedFrequency > 0f) {
                    spectrumPlot?.setXMark(
                        model.detectedFrequency, label, MARK_ID_FREQUENCY, LabelAnchor.SouthWest,
                        placeLabelsOutsideBoundsIfPossible = false,
                        redraw = false
                    )
                } else {
                    spectrumPlot?.removePlotMarks(MARK_ID_FREQUENCY, true)
                }
                if (model.frequencies.isNotEmpty())
                    spectrumPlot?.setXTouchLimits(0f, model.frequencies.last(), PlotView.NO_REDRAW)
            }

            if (model.targetChangeId > spectrumPlotChangeId) {
                spectrumPlot?.xRange(model.frequencyRange[0], model.frequencyRange[1], 300L)
            }
            spectrumPlot?.enableExtraPadding = model.useExtraPadding
            spectrumPlot?.invalidate()

            spectrumPlotChangeId = model.changeId
        }

        viewModel.correlationPlotModel.observe(viewLifecycleOwner) { model ->
//            Log.v("Tuner", "TunerFragment: spectrumModel.observe: changeId=${model.changeId}, noteDetectionId=${model.noteDetectionChangeId}, targetId=${model.targetChangeId}")
            if (model.changeId < correlationPlotChangeId)
                correlationPlotChangeId = -1

            if (model.noteDetectionChangeId > correlationPlotChangeId) {
                correlationPlot?.plot(model.timeShifts, model.correlationValues, redraw = false)
                val label = getString(R.string.hertz, model.detectedFrequency)
                if (model.detectedFrequency > 0f) {
                    correlationPlot?.setXMark(
                        1.0f / model.detectedFrequency,
                        label,
                        MARK_ID_FREQUENCY,
                        LabelAnchor.SouthWest,
                        placeLabelsOutsideBoundsIfPossible = false,
                        redraw = false
                    )
                } else {
                    correlationPlot?.removePlotMarks(MARK_ID_FREQUENCY, true)
                }
                if (model.timeShifts.isNotEmpty())
                    correlationPlot?.setXTouchLimits(0f, model.timeShifts.last(), PlotView.NO_REDRAW)
            }

            if (model.targetChangeId > correlationPlotChangeId) {
                correlationPlot?.xRange(model.timeShiftRange[0], model.timeShiftRange[1], 300L)
            }

            correlationPlot?.enableExtraPadding = model.useExtraPadding
            correlationPlot?.invalidate()
            correlationPlotChangeId = model.changeId
        }

        viewModel.pitchHistoryModel.observe(viewLifecycleOwner) { model ->
            if (model.changeId < pitchPlotChangeId)
                pitchPlotChangeId = -1

            if (model.musicalScaleChangeId > pitchPlotChangeId || model.notePrintOptionsChangeId > pitchPlotChangeId) {
                pitchPlot?.setYTicks(model.musicalScaleFrequencies, redraw = false,
                    noteNameScale = model.musicalScale.noteNameScale,
                    noteIndexBegin = model.musicalScale.noteIndexBegin,
                    notePrintOptions = model.notePrintOptions
                )

                pitchPlot?.setYTouchLimits(model.musicalScaleFrequencies[0], model.musicalScaleFrequencies.last(), 0L)
                pitchPlot?.enableExtraPadding = model.useExtraPadding
            }
            if (model.historyValuesChangeId > pitchPlotChangeId) {
                if (model.numHistoryValues == 0 || model.currentFrequency <= 0f) {
                    pitchPlot?.removePlotPoints(PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG, suppressInvalidate = true)
                    pitchPlot?.removePlotPoints(PitchHistoryModel.TUNING_DIRECTION_POINT_TAG, suppressInvalidate = true)
                } else {
                    val point = floatArrayOf(model.numHistoryValues - 1f, model.currentFrequency)
                    pitchPlot?.setPoints(point, tag = PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG, redraw = false)
                    pitchPlot?.setPoints(point, tag = PitchHistoryModel.TUNING_DIRECTION_POINT_TAG, redraw = false)
                }
                pitchPlot?.plot(
                    model.historyValues, PitchHistoryModel.HISTORY_LINE_TAG,
                    indexBegin = 0, indexEnd = model.numHistoryValues,
                    redraw = false
                )
                pitchPlot?.xRange(0f, 1.08f * model.historyValues.size, PlotView.NO_REDRAW)
            }

            if (model.yRangeChangeId > pitchPlotChangeId) {
                pitchPlot?.yRange(model.yRangeAuto[0], model.yRangeAuto[1], 600L)
            }

            if (model.targetNoteChangeId > pitchPlotChangeId || model.notePrintOptionsChangeId > pitchPlotChangeId) {
                val targetNote = model.targetNote
                if (model.targetNoteFrequency > 0f && targetNote != null) {
                    pitchPlot?.setYMark(
                        model.targetNoteFrequency,
                        targetNote,
                        model.notePrintOptions,
                        PitchHistoryModel.TARGET_NOTE_MARK_TAG,
                        LabelAnchor.East,
                        model.targetNoteMarkStyle,
                        placeLabelsOutsideBoundsIfPossible = true,
                        redraw = false
                    )
                } else {
                    pitchPlot?.removePlotMarks(PitchHistoryModel.TARGET_NOTE_MARK_TAG, suppressInvalidate = true)
                }
            }

            if (model.toleranceChangeId > pitchPlotChangeId) {
                if (model.lowerToleranceFrequency > 0f && model.upperToleranceFrequency > 0f) {
//                    Log.v("Tuner","TunerFragment: setting tolerance in pitchhistory: ${model.lowerToleranceFrequency} -- ${model.upperToleranceFrequency}, plotrange=${model.yRangeAuto[0]} -- ${model.yRangeAuto[1]}, currentFreq=${model.currentFrequency}")
                    pitchPlot?.setMarks(
                        null,
                        floatArrayOf(
                            model.lowerToleranceFrequency,
                            model.upperToleranceFrequency
                        ),
                        PitchHistoryModel.TOLERANCE_MARK_TAG,
                        styleIndex = PitchHistoryModel.TOLERANCE_STYLE,
                        anchors = arrayOf(LabelAnchor.NorthWest, LabelAnchor.SouthWest),
                        backgroundSizeType = MarkLabelBackgroundSize.FitLargest,
                        placeLabelsOutsideBoundsIfPossible = false,
                        redraw = false,
                        maxLabelBounds = null
                    ) { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float ->
                        val s = when (index) {
                            0 -> getString(R.string.cent, -model.toleranceInCents)
                            1 -> getString(R.string.cent, model.toleranceInCents)
                            else -> ""
                        }
                        StringLabel(
                            s,
                            textPaint,
                            backgroundPaint,
                            cornerRadius,
                            gravity,
                            paddingHorizontal,
                            paddingHorizontal,
                            paddingVertical,
                            paddingVertical
                        )
                    }
                } else {
                    pitchPlot?.removePlotMarks(PitchHistoryModel.TOLERANCE_MARK_TAG, true)
                }
            }

            pitchPlot?.setLineStyle(model.historyLineStyle, PitchHistoryModel.HISTORY_LINE_TAG, suppressInvalidate = true)
            pitchPlot?.setPointStyle(model.currentFrequencyPointStyle, PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG, suppressInvalidate = true)
            pitchPlot?.setPointStyle(model.tuningDirectionPointStyle, PitchHistoryModel.TUNING_DIRECTION_POINT_TAG, suppressInvalidate = true)
            val pointSize = pitchPlot?.pointSizes?.get(model.currentFrequencyPointStyle) ?: 1f
            pitchPlot?.setPointOffset(
                0f, pointSize * model.tuningDirectionPointRelativeOffset,
                PitchHistoryModel.TUNING_DIRECTION_POINT_TAG,
                suppressInvalidate = false
            )
//            Log.v("Tuner", "TunerFragment: tuningDirectionPointVisible = ${model.tuningDirectionPointVisible}, offset=${pointSize * model.tuningDirectionPointRelativeOffset}")
            pitchPlot?.setPointVisible(model.tuningDirectionPointVisible, PitchHistoryModel.TUNING_DIRECTION_POINT_TAG, suppressInvalidate = true)
            pitchPlot?.setMarkStyle(model.targetNoteMarkStyle, PitchHistoryModel.TARGET_NOTE_MARK_TAG, suppressInvalidate = true)

            pitchPlot?.invalidate()
            pitchPlotChangeId = model.changeId
        }
//        pitchPlot?.yRange(400f, 500f, PlotView.NO_REDRAW)
//
//        viewModel.musicalScale.observe(viewLifecycleOwner) { tuningFrequencies ->
//            updatePitchPlotNoteNames()
//            // TODO: should we extend the limits slightly, that the whole mark is visible?
//            val firstFrequencyIndex = tuningFrequencies.noteIndexBegin
//            val lastFrequencyIndex = tuningFrequencies.noteIndexEnd - 1
//            val firstFrequency = tuningFrequencies.getNoteFrequency(firstFrequencyIndex)
//            val lastFrequency = tuningFrequencies.getNoteFrequency(lastFrequencyIndex)
//            pitchPlot?.setYTouchLimits(firstFrequency, lastFrequency, 0L)
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecyscle(Lifecycle.State.STARTED) {
//                requireContext().preferenceResources.notePrintOptions.collect {
//                    updatePitchPlotMarks(redraw = false)
//                    updatePitchPlotNoteNames()
//
//                    pitchPlot?.enableExtraPadding = it.isSolfege
//                    spectrumPlot?.enableExtraPadding = it.isSolfege
//                    correlationPlot?.enableExtraPadding = it.isSolfege
//                }
//            }
//        }
//
//        viewModel.noteDetectionResults.observe(viewLifecycleOwner) { resultsMemory ->
//            resultsMemory.incRef()
//            val results = resultsMemory.memory
//            if (results.frequency == 0f) {
//                correlationPlot?.removePlotMarks(null, false)
//                spectrumPlot?.removePlotMarks(null, false)
//            }
//            else {
//                val label = getString(R.string.hertz, results.frequency)
//                correlationPlot?.setXMark(
//                    1.0f / results.frequency, label,
//                    MARK_ID_FREQUENCY, LabelAnchor.SouthWest,
//                    placeLabelsOutsideBoundsIfPossible = false
//                )
//                spectrumPlot?.setXMark(
//                    results.frequency, label, MARK_ID_FREQUENCY,
//                    LabelAnchor.SouthWest,
//                    placeLabelsOutsideBoundsIfPossible = false
//                )
//            }
//
//            correlationPlot?.setXTouchLimits(
//                0f,
//                results.autoCorrelation.times.last(),
//                PlotView.NO_REDRAW
//            )
//            correlationPlot?.plot(
//                results.autoCorrelation.times,
//                results.autoCorrelation.values
//            )
//
//            spectrumPlot?.setXTouchLimits(
//                0f,
//                results.frequencySpectrum.frequencies.last(),
//                PlotView.NO_REDRAW
//            )
//            spectrumPlot?.plot(
//                results.frequencySpectrum.frequencies,
//                results.frequencySpectrum.amplitudeSpectrumSquared)
//            resultsMemory.decRef()
//        }
//
//        viewModel.pitchHistory.sizeAsLiveData.observe(viewLifecycleOwner) {
////            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
//            pitchPlot?.xRange(0f, 1.08f * it.toFloat(), PlotView.NO_REDRAW)
//        }
//
//        viewModel.frequencyPlotRange.observe(viewLifecycleOwner) {
////            Log.v("TestRecordFlow", "TunerFragment.plotRange: ${it[0]} -- ${it[1]}")
//            pitchPlot?.yRange(it[0], it[1], 600)
//        }
//
//        viewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
//            if (it.size > 0) {
//                val frequency = it.last()
//                tuningStatus = viewModel.targetNote.value?.getTuningStatus(frequency) ?: TargetNote.TuningStatus.Unknown
//
//                setStyles(isPitchInactive, tuningStatus, redraw = false)
//                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), frequency), redraw = false)
//                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), frequency), tag = 1L, redraw = false)
//                pitchPlot?.plot(it)
//            }
//        }
//
//        viewModel.targetNote.observe(viewLifecycleOwner) { targetNote ->
//            viewModel.pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
//                tuningStatus = targetNote.getTuningStatus(frequency)
//            }
//            setStyles(isPitchInactive, tuningStatus, redraw = false)
//            updatePitchPlotMarks(redraw = true)
//        }
//
//        viewModel.pitchHistory.numValuesSinceLastLineUpdate.observe(viewLifecycleOwner) { numValuesSinceLastUpdate ->
//            val maxTimeBeforeInactive = 0.3f // seconds
//            val maxNumValuesBeforeInactive = max(1f, floor(maxTimeBeforeInactive / (viewModel.pitchHistoryUpdateInterval.value ?: 1f)))
//            isPitchInactive = (numValuesSinceLastUpdate > maxNumValuesBeforeInactive)
//            setStyles(isPitchInactive, tuningStatus, redraw = true)
//        }
//
//        // plot the values if available, since the plots currently cant store the plot lines.
//        viewModel.noteDetectionResults.value?.let { resultsMemory ->
//            resultsMemory.incRef()
//            val results = resultsMemory.memory
//            correlationPlot?.plot(
//                results.autoCorrelation.times,
//                results.autoCorrelation.values
//            )
//            spectrumPlot?.plot(
//                results.frequencySpectrum.frequencies,
//                results.frequencySpectrum.amplitudeSpectrumSquared
//            )
//            resultsMemory.decRef()
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                requireContext().preferenceResources.waveWriterDurationInSeconds.collect { duration ->
//                    recordFab?.visibility = if (duration == 0) View.GONE else View.VISIBLE
//                }
//            }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showWaveWriterFab.collect {
                    recordFab?.visibility = if (it) View.VISIBLE else View.GONE
                }
            }
        }

        recordFab?.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.waveWriter.storeSnapshot()
                waveFileWriterIntent.launch()
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
        //viewModel.setInstrument(instrumentDatabase[0]) // TODO: we need completely own view model here instead
        //viewModel.setTargetNote(-1, null) // TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)
    }

    override fun onResume() {
        super.onResume()
        activity?.let {
            it.setTitle(R.string.app_name)
            if (it is MainActivity) {
                it.setStatusAndNavigationBarColors()
                it.setPreferenceBarVisibilty(View.VISIBLE)
            }
        }
    }

    override fun onStop() {
        viewModel.stopSampling()
        super.onStop()
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
        if (redraw)
            pitchPlot?.invalidate()
    }
//    INFO: the following temporarily switched off
//    private fun updatePitchPlotMarks(redraw: Boolean = true) {
//        val targetNote = viewModel.targetNote.value ?: return
//        //val musicalScale = viewModel.musicalScale.value ?: return
//        //val noteNames = viewModel.noteNames.value ?: return
//        //val preferFlat = viewModel.preferFlat.value ?: false
//
//        val nameMinusBound = getString(R.string.cent, -targetNote.toleranceInCents)
//        val namePlusBound = getString(R.string.cent, targetNote.toleranceInCents)
//
//        if (targetNote.isTargetNoteAvailable) {
//            pitchPlot?.setMarks(
//                null,
//                floatArrayOf(
//                    targetNote.frequencyLowerTolerance,
//                    targetNote.frequencyUpperTolerance
//                ),
//                MARK_ID_TOLERANCE,
//                1,
//                arrayOf(LabelAnchor.NorthWest, LabelAnchor.SouthWest),
//                MarkLabelBackgroundSize.FitLargest,
//                placeLabelsOutsideBoundsIfPossible = false,
//                redraw = false,
//                maxLabelBounds = null
//            ) { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float ->
//                val s = when (index) {
//                    0 -> nameMinusBound
//                    1 -> namePlusBound
//                    else -> ""
//                }
//                StringLabel(
//                    s,
//                    textPaint,
//                    backgroundPaint,
//                    cornerRadius,
//                    gravity,
//                    paddingHorizontal,
//                    paddingHorizontal,
//                    paddingVertical,
//                    paddingVertical
//                )
//            }
//            pitchPlot?.setYMark(
//                targetNote.frequency,
//                targetNote.note,
//                requireContext().preferenceResources.notePrintOptions.value,
//                MARK_ID_FREQUENCY,
//                LabelAnchor.East,
//                if (tuningStatus == TargetNote.TuningStatus.InTune) 0 else 2,
//                placeLabelsOutsideBoundsIfPossible = true,
//                redraw = redraw
//            )
//        } else {
//            pitchPlot?.removePlotMarks(MARK_ID_TOLERANCE, suppressInvalidate = true)
//            pitchPlot?.removePlotMarks(MARK_ID_FREQUENCY, suppressInvalidate = !redraw)
//        }
//    }
//
//    private fun updatePitchPlotNoteNames(redraw: Boolean = true) {
//        //val noteNames = viewModel.noteNames.value ?: return
//        //val preferFlat = viewModel.preferFlat.value ?: return
//        val musicalScale = viewModel.musicalScale.value ?: return
//
//        val numNotes = musicalScale.noteIndexEnd - musicalScale.noteIndexBegin
//        val noteFrequencies = FloatArray(numNotes) {
//            musicalScale.getNoteFrequency(musicalScale.noteIndexBegin + it)
//        }
//
//        // Update ticks in pitch history plot
//        pitchPlot?.setYTicks(noteFrequencies, redraw = false,
//            noteNameScale = musicalScale.noteNameScale, noteIndexBegin = musicalScale.noteIndexBegin,
//            requireContext().preferenceResources.notePrintOptions.value
//        )
////        { _, f ->
////            val toneIndex = musicalScale.getClosestNoteIndex(f)
////            noteNames.getNoteName(requireContext(), toneIndex, preferFlat = preferFlat)
////        }
//
//        // Update active y-mark in pitch history plot
//        viewModel.targetNote.value?.let { targetNote ->
//            if (targetNote.isTargetNoteAvailable) {
//                pitchPlot?.setYMark(
//                    targetNote.frequency,
//                    targetNote.note,
//                    requireContext().preferenceResources.notePrintOptions.value,
//                    //noteNames.getNoteName(requireContext(), targetNote.noteIndex, preferFlat),
//                    MARK_ID_FREQUENCY,
//                    LabelAnchor.East,
//                    style = if (tuningStatus == TargetNote.TuningStatus.InTune) 0 else 2,
//                    placeLabelsOutsideBoundsIfPossible = true,
//                    redraw = redraw
//                )
//            } else {
//                pitchPlot?.removePlotMarks(MARK_ID_FREQUENCY, suppressInvalidate = !redraw)
//            }
//        }
//    }

    companion object{
        private const val MARK_ID_TOLERANCE = 10L
        private const val MARK_ID_FREQUENCY = 11L
        private const val HARMONIC_ID = 12L
    }
}
