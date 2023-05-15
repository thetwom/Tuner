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
import de.moekadu.tuner.viewmodels.TunerViewModel
import de.moekadu.tuner.views.*
import kotlinx.coroutines.launch

class TunerFragmentSimple : Fragment() {
    // TODO: can we make lower strings in string view a bit thicker?
    private val viewModel: TunerViewModel by viewModels {
        TunerViewModel.Factory(
            requireActivity().preferenceResources,
            requireActivity().instrumentResources
        )
    }

    private var pitchPlot: PlotView? = null
    private var volumeMeter: VolumeMeter? = null
    private var stringView: StringView? = null
    private var instrumentTitle: MaterialButton? = null
    private var invalidInstrumentWarning: TextView? = null

    private var pitchPlotChangeId = -1
    private var stringViewChangeId = -1

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
                stringView?.visibility = if (model.isVisible) View.VISIBLE else View.GONE
                stringView?.enableExtraPadding = model.useExtraPadding
                val printer = model.noteNamePrinter
                if (model.instrument.isChromatic && printer != null) {
                    stringView?.setStrings(
                        null,
                        true,
                        model.musicalScale.noteNameScale,
                        model.musicalScale.noteIndexBegin,
                        model.musicalScale.noteIndexEnd,
                        printer
                    )
                } else if (printer != null) {
                    stringView?.setStrings(
                        model.instrument.strings,
                        false,
                        model.musicalScale.noteNameScale,
                        model.musicalScale.noteIndexBegin,
                        model.musicalScale.noteIndexEnd,
                        printer
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
                model.noteNamePrinter?.let { printer ->
                    pitchPlot?.clearYTicks()
                    pitchPlot?.addYTicksLevel(
                        model.musicalScaleFrequencies,
                        noteNameScale = model.musicalScale.noteNameScale,
                        noteIndexBegin = model.musicalScale.noteIndexBegin,
                        noteNamePrinter = printer
                    )
                }
                pitchPlot?.setYTouchLimits(model.musicalScaleFrequencies[0], model.musicalScaleFrequencies.last(), 0L)
                pitchPlot?.enableExtraPadding = model.useExtraPadding
            }
            if (model.historyValuesChangeId > pitchPlotChangeId) {
                if (model.numHistoryValues == 0 || model.currentFrequency <= 0f) {
                    pitchPlot?.removePlotPoints(PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG)
                    pitchPlot?.removePlotPoints(PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
                } else {
                    val point = floatArrayOf(model.numHistoryValues - 1f, model.currentFrequency)
                    pitchPlot?.setPoints(point, tag = PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG)
                    pitchPlot?.setPoints(point, tag = PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
                }
                pitchPlot?.plot(
                    model.historyValues, PitchHistoryModel.HISTORY_LINE_TAG,
                    indexBegin = 0, indexEnd = model.numHistoryValues
                )
                pitchPlot?.xRange(0f, 1.08f * model.historyValues.size)
            }

            if (model.yRangeChangeId > pitchPlotChangeId) {
                pitchPlot?.yRange(model.yRangeAuto[0], model.yRangeAuto[1], 600L)
            }

            if (model.targetNoteChangeId > pitchPlotChangeId || model.notePrintOptionsChangeId > pitchPlotChangeId) {
                val targetNote = model.targetNote
                val printer = model.noteNamePrinter
                if (model.targetNoteFrequency > 0f && targetNote != null && printer != null) {
                    pitchPlot?.setYMark(
                        model.targetNoteFrequency,
                        targetNote,
                        printer,
                        PitchHistoryModel.TARGET_NOTE_MARK_TAG,
                        LabelAnchor.East,
                        model.targetNoteMarkStyle,
                        placeLabelsOutsideBoundsIfPossible = true
                    )
                } else {
                    pitchPlot?.removePlotMarks(PitchHistoryModel.TARGET_NOTE_MARK_TAG)
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
                    pitchPlot?.removePlotMarks(PitchHistoryModel.TOLERANCE_MARK_TAG)
                }
            }

            pitchPlot?.setLineStyle(model.historyLineStyle, PitchHistoryModel.HISTORY_LINE_TAG)
            pitchPlot?.setPointStyle(model.currentFrequencyPointStyle, PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG)
            pitchPlot?.setPointStyle(model.tuningDirectionPointStyle, PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
            val pointSize = pitchPlot?.pointSizes?.get(model.currentFrequencyPointStyle) ?: 1f
            pitchPlot?.setPointOffset(
                0f, pointSize * model.tuningDirectionPointRelativeOffset,
                PitchHistoryModel.TUNING_DIRECTION_POINT_TAG
            )
//            Log.v("Tuner", "TunerFragment: tuningDirectionPointVisible = ${model.tuningDirectionPointVisible}, offset=${pointSize * model.tuningDirectionPointRelativeOffset}")
            pitchPlot?.setPointVisible(model.tuningDirectionPointVisible, PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
            pitchPlot?.setMarkStyle(model.targetNoteMarkStyle, PitchHistoryModel.TARGET_NOTE_MARK_TAG)
            pitchPlotChangeId = model.changeId
        }

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
}
