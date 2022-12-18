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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.instrumentResources
import de.moekadu.tuner.models.PitchHistoryModel
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.Notation
import de.moekadu.tuner.temperaments.TargetNote
import de.moekadu.tuner.viewmodels.TunerViewModel
import de.moekadu.tuner.views.*
import kotlinx.coroutines.launch

class TunerFragmentSimple : Fragment() {
    // TODO: can we make lower strings in string view a bit thicker?
    private val viewModel: TunerViewModel by viewModels {
        TunerViewModel.Factory(
            requireActivity().preferenceResources,
            requireActivity().instrumentResources,
            simpleMode = true
        )
    }
//    private val viewModel: TunerViewModel by activityViewModels()
//    private val instrumentsViewModel: InstrumentsViewModel by activityViewModels {
//        InstrumentsViewModel.Factory(
//            AppPreferences.readInstrumentId(requireActivity()),
//            AppPreferences.readInstrumentSection(requireActivity()),
//            AppPreferences.readCustomInstruments(requireActivity()),
//            AppPreferences.readPredefinedSectionExpanded(requireActivity()),
//            AppPreferences.readCustomSectionExpanded(requireActivity()),
//            requireActivity().application)
//    }

    private var pitchPlot: PlotView? = null
    private var volumeMeter: VolumeMeter? = null
    private var stringView: StringView? = null
    private var instrumentTitle: MaterialButton? = null
    private var invalidInstrumentWarning: TextView? = null

    private var pitchPlotChangeId = -1
    private var stringViewChangeId = -1

//    private var isPitchInactive = false
//    private var tuningStatus = TargetNote.TuningStatus.Unknown

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
        // Log.v("Tuner", "TunerFragmentSimple.onCreateView")
        val view = inflater.inflate(R.layout.diagrams_simple, container, false)

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        pitchPlot = view.findViewById(R.id.pitch_plot)
        volumeMeter = view.findViewById(R.id.volume_meter)
        stringView = view.findViewById(R.id.string_view)
        instrumentTitle = view.findViewById(R.id.instrument_title)
        invalidInstrumentWarning = view.findViewById(R.id.incorrect_instrument_text)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.instrument.collect {
                    instrumentTitle?.setIconResource(it.instrument.iconResource)
                    instrumentTitle?.text = it.instrument.getNameString(requireContext())
                    // TODO: call content of updateInvalidStringsAppearance
                }
            }
        }

        instrumentTitle?.setOnClickListener {
            (requireActivity() as MainActivity).loadInstrumentsFragment()
        }

        viewModel.stringsModel.observe(viewLifecycleOwner) { model ->
            if (model.changeId < stringViewChangeId)
                stringViewChangeId = -1

            if (model.settingsChangeId > stringViewChangeId) {
                stringView?.enableExtraPadding = model.useExtraPadding
                if (model.instrument.isChromatic) {
                    stringView?.setStrings(
                        null,
                        true,
                        model.musicalScale.noteNameScale,
                        model.musicalScale.noteIndexBegin,
                        model.musicalScale.noteIndexEnd,
                        model.notePrintOptions
                    )
                } else {
                    stringView?.setStrings(
                        model.instrument.strings,
                        false,
                        model.musicalScale.noteNameScale,
                        model.musicalScale.noteIndexBegin,
                        model.musicalScale.noteIndexEnd,
                        model.notePrintOptions
                    )
                }

                if (model.isIncompatibleInstrument) {
                    invalidInstrumentWarning?.visibility = View.VISIBLE
                    instrumentTitle?.setStrokeColorResource(R.color.instrument_button_color_error)
                    instrumentTitle?.setTextColor(context?.getColorStateList(R.color.instrument_button_color_error))
                    instrumentTitle?.setIconTintResource(R.color.instrument_button_color_error)
                    instrumentTitle?.backgroundTintList = context?.getColorStateList(R.color.instrument_button_background_color_error)
                } else {
                    invalidInstrumentWarning?.visibility = View.GONE
                    instrumentTitle?.setStrokeColorResource(R.color.instrument_button_color_normal)
                    instrumentTitle?.setTextColor(context?.getColorStateList(R.color.instrument_button_color_normal))
                    instrumentTitle?.setIconTintResource(R.color.instrument_button_color_normal)
                    instrumentTitle?.backgroundTintList = context?.getColorStateList(R.color.instrument_button_background_color_normal)
                }
            }

            if (model.highlightChangeId > stringViewChangeId) {
                stringView?.showAnchor = model.showStringAnchor
                stringView?.activeStyleIndex = model.highlightedStyleIndex
//                Log.v(
//                    "Tuner",
//                    "TunerFragmentSimple: observeStringsModel: highlighedStringIndex = ${model.highlightedStringIndex}"
//                )
                if (model.highlightedStringIndex >= 0)
                    stringView?.highlightSingleString(model.highlightedStringIndex)
                else
                    stringView?.highlightByNote(model.highlightedNote)
            }
            stringViewChangeId = model.changeId
        }

        stringView?.stringClickedListener = object : StringView.StringClickedListener {
            override fun onStringClicked(stringIndex: Int, note: MusicalNote) {
                viewModel.clickString(stringIndex, note)
            }

            override fun onAnchorClicked() {
                viewModel.setTargetNote(-1, null)
                stringView?.setAutomaticControl()
            }

            override fun onBackgroundClicked() {
                // stringView?.setAutomaticControl()
            }
        }


//        pitchPlot?.yRange(400f, 500f, PlotView.NO_REDRAW)
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
//        INFO: the following lines have been temporarily switched off
//        stringView?.stringClickedListener = object : StringView.StringClickedListener {
//            override fun onStringClicked(stringIndex: Int, note: MusicalNote) {
//                //if (stringIndex == stringView?.highlightedStringIndex && viewModel.isTargetNoteUserDefined.value == true) {
//                if (stringIndex == viewModel.selectedStringIndex && viewModel.isTargetNoteUserDefined.value == true) {
//                    stringView?.setAutomaticControl()
//                    viewModel.setTargetNote(-1, null)
//                    //stringView?.showAnchor = false
//                } else if (stringIndex != -1) {
//                    viewModel.setTargetNote(stringIndex, note)
//                }
//            }
//
//            override fun onAnchorClicked() {
//                viewModel.setTargetNote(-1, null)
//                stringView?.setAutomaticControl()
//            }
//
//            override fun onBackgroundClicked() {
//                // stringView?.setAutomaticControl()
//            }
//        }
//
//        instrumentTitle?.setOnClickListener {
//            (requireActivity() as MainActivity).loadInstrumentsFragment()
//        }
//
//        viewModel.isTargetNoteUserDefined.observe(viewLifecycleOwner) { isTargetNoteUserDefined ->
//            stringView?.showAnchor = isTargetNoteUserDefined
//        }
//
//        viewModel.musicalScale.observe(viewLifecycleOwner) { tuningFrequencies ->
//            updatePitchPlotNoteNames()
//            // TODO: should we extend the limits slightly, that the whole mark is visible?
//            val firstFrequencyIndex = tuningFrequencies.noteIndexBegin
//            val lastFrequencyIndex = tuningFrequencies.noteIndexEnd - 1
//            val firstFrequency = tuningFrequencies.getNoteFrequency(firstFrequencyIndex)
//            val lastFrequency = tuningFrequencies.getNoteFrequency(lastFrequencyIndex)
//            pitchPlot?.setYTouchLimits(firstFrequency, lastFrequency, 0L)
//            // we do not have to call updatePitchPlotMarks, since this is fully handled by
//            // observing the target note
//
//            if (instrumentsViewModel.instrument.value?.instrument?.isChromatic == true)
//                updateStringViewNoteNames()
//            updateInvalidStringsAppearance()
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                requireContext().preferenceResources.notePrintOptions.collect {
//                    updatePitchPlotMarks(redraw = false)
//                    updatePitchPlotNoteNames()
//                    updateStringViewNoteNames()
//
//                    pitchPlot?.enableExtraPadding = it.isSolfege
//                    stringView?.enableExtraPadding = it.isSolfege
//                }
//            }
//        }
//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                instrumentsViewModel.instrument.collect { instrumentAndSection ->
//                    val instrument = instrumentAndSection.instrument
//                    //Log.v("Tuner", "TunerFragmentSimple.onCreateView: instrumentViewModel.instrument: $instrument")
////                    viewModel.setInstrument(instrument)
//                    //instrumentIcon?.setImageResource(instrument.iconResource)
//                    instrumentTitle?.setIconResource(instrument.iconResource)
//                    instrumentTitle?.text = instrument.getNameString(requireContext())
//                    updateStringViewNoteNames()
//                    //stringView?.setAutomaticControl(0L)
//                    updateInvalidStringsAppearance()
//                }
//            }
//        }
//
//        viewModel.pitchHistory.sizeAsLiveData.observe(viewLifecycleOwner) {
////            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
//            pitchPlot?.xRange(0f, 1.08f * it.toFloat(), PlotView.NO_REDRAW)
//        }
//
//        viewModel.frequencyPlotRange.observe(viewLifecycleOwner) {
////            Log.v("Tuner", "TunerFragmentSimple observe frequencyPlotRange: ${it[0]} -- ${it[1]}")
//            pitchPlot?.yRange(it[0], it[1], 600)
//        }
//
//        viewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
//            if (it.size > 0) {
//                val frequency = it.last()
//                tuningStatus = viewModel.targetNote.value?.getTuningStatus(frequency) ?: TargetNote.TuningStatus.Unknown
//
//                setStyles(isPitchInactive, tuningStatus, false)
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
////            Log.v("Tuner", "TunerFragmentSimple: observing targetNote: tuningStatus=$tuningStatus")
//            setStyles(isPitchInactive, tuningStatus, redraw = false)
//
//            updatePitchPlotMarks(redraw = true)
//
//            //Log.v("Tuner", "TunerFragmentSimple: target note changed: stringIndex = ${targetNote.stringIndex}, toneIndex=${targetNote.toneIndex}")
//            if (viewModel.selectedStringIndex != -1)
//                stringView?.highlightSingleString(viewModel.selectedStringIndex, 300L)
//            else
//                stringView?.highlightByNote(targetNote.note, 300L)
//            //stringView?.scrollToString(targetNote.toneIndex, 300L)
//        }
//
//        viewModel.pitchHistory.numValuesSinceLastLineUpdate.observe(viewLifecycleOwner) { numValuesSinceLastUpdate ->
//            val maxTimeBeforeInactive = 0.3f // seconds
//            val maxNumValuesBeforeInactive = max(1f, floor(maxTimeBeforeInactive / (viewModel.pitchHistoryUpdateInterval.value ?: 1f)))
//            isPitchInactive = (numValuesSinceLastUpdate > maxNumValuesBeforeInactive)
//            setStyles(isPitchInactive, tuningStatus, redraw = true)
//        }

        return view
    }

    override fun onStart() {
        super.onStart()
        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
//        viewModel.setInstrument(instrumentsViewModel.instrument.value?.instrument ?: instrumentDatabase[0])
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
            TargetNote.TuningStatus.InTune -> stringView?.activeStyleIndex = 2
            else -> stringView?.activeStyleIndex = 3
        }
        if (redraw)
            pitchPlot?.invalidate()
    }

//    INFO: the following lines have been temporarily switched off
//    private fun updatePitchPlotMarks(redraw: Boolean = true) {
//        val targetNote = viewModel.targetNote.value ?: return
////        val noteNames = viewModel.noteNames.value ?: return
////        val preferFlat = viewModel.preferFlat.value ?: false
//
//        val nameMinusBound = getString(R.string.cent, -targetNote.toleranceInCents)
//        val namePlusBound = getString(R.string.cent, targetNote.toleranceInCents)
//
//        if (targetNote.isTargetNoteAvailable) {pitchPlot?.setMarks(
//            null,
//            floatArrayOf(
//                targetNote.frequencyLowerTolerance,
//                targetNote.frequencyUpperTolerance
//            ),
//            MARK_ID_TOLERANCE,
//            1,
//            arrayOf(LabelAnchor.NorthWest, LabelAnchor.SouthWest),
//            MarkLabelBackgroundSize.FitLargest,
//            placeLabelsOutsideBoundsIfPossible = false,
//            redraw = false,
//            maxLabelBounds = null
//        ) { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float ->
//            val s = when (index) {
//                0 -> nameMinusBound
//                1 -> namePlusBound
//                else -> ""
//            }
//            StringLabel(
//                s,
//                textPaint,
//                backgroundPaint,
//                cornerRadius,
//                gravity,
//                paddingHorizontal,
//                paddingHorizontal,
//                paddingVertical,
//                paddingVertical
//            )
//        }
//
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
////        val noteNames = viewModel.noteNames.value ?: return
////        val preferFlat = viewModel.preferFlat.value ?: return
//        val musicalScale = viewModel.musicalScale.value ?: return
//
//        val numNotes = musicalScale.noteIndexEnd - musicalScale.noteIndexBegin
//        val noteFrequencies = FloatArray(numNotes) {
//            musicalScale.getNoteFrequency(musicalScale.noteIndexBegin + it)
//        }
//
////        Log.v("Tuner", "TunerFragmentSimple: updatePitchPlotNoteNames: indexBegin=${musicalScale.noteIndexBegin}, noteFreq[57] = ${noteFrequencies[57]}, ${musicalScale.noteNameScale.getNoteOfIndex(musicalScale.noteIndexBegin + 57)}")
//        // Update ticks in pitch history plot
//        pitchPlot?.setYTicks(noteFrequencies, redraw = false,
//            noteNameScale = musicalScale.noteNameScale, noteIndexBegin = musicalScale.noteIndexBegin,
//            notePrintOptions = requireContext().preferenceResources.notePrintOptions.value
//        )
//
//        // Update active y-mark in pitch history plot
//        viewModel.targetNote.value?.let { targetNote ->
//            if (targetNote.isTargetNoteAvailable) {
//                pitchPlot?.setYMark(
//                    targetNote.frequency,
//                    targetNote.note,
//                    requireContext().preferenceResources.notePrintOptions.value,
//                    MARK_ID_FREQUENCY,
//                    LabelAnchor.East,
//                    if (tuningStatus == TargetNote.TuningStatus.InTune) 0 else 2,
//                    placeLabelsOutsideBoundsIfPossible = true,
//                    redraw = redraw
//                )
//            } else {
//                pitchPlot?.removePlotMarks(MARK_ID_FREQUENCY, suppressInvalidate = !redraw)
//            }
//        }
//    }
//
//    private fun updateStringViewNoteNames() {
//        val musicalScale = viewModel.musicalScale.value ?: return
//        val instrument = instrumentsViewModel.instrument.value?.instrument ?: return
////        val noteNames = viewModel.noteNames.value ?: return
//
//        if (instrument.isChromatic) {
//            stringView?.setStrings(null, true, musicalScale.noteNameScale,
//                musicalScale.noteIndexBegin, musicalScale.noteIndexEnd, requireContext().preferenceResources.notePrintOptions.value)
//        } else {
//            stringView?.setStrings(instrument.strings, false, musicalScale.noteNameScale,
//                musicalScale.noteIndexBegin, musicalScale.noteIndexEnd, requireContext().preferenceResources.notePrintOptions.value)
//        }
//    }
//
//    private fun updateInvalidStringsAppearance() {
//        if (viewModel.targetNote.value?.hasStringsNotPartOfMusicalScale == true) {
//            invalidInstrumentWarning?.visibility = View.VISIBLE
//            instrumentTitle?.setStrokeColorResource(R.color.instrument_button_color_error)
//            instrumentTitle?.setTextColor(context?.getColorStateList(R.color.instrument_button_color_error))
//            instrumentTitle?.setIconTintResource(R.color.instrument_button_color_error)
//            instrumentTitle?.backgroundTintList = context?.getColorStateList(R.color.instrument_button_background_color_error)
//            //instrumentTitle?.isEnabled = true
//        } else {
//            invalidInstrumentWarning?.visibility = View.GONE
//            instrumentTitle?.setStrokeColorResource(R.color.instrument_button_color_normal)
//            instrumentTitle?.setTextColor(context?.getColorStateList(R.color.instrument_button_color_normal))
//            instrumentTitle?.setIconTintResource(R.color.instrument_button_color_normal)
//            instrumentTitle?.backgroundTintList = context?.getColorStateList(R.color.instrument_button_background_color_normal)
//            //instrumentTitle?.isEnabled = false
//        }
//    }

    companion object{
        private const val MARK_ID_TOLERANCE = 10L
        private const val MARK_ID_FREQUENCY = 11L
    }
}
