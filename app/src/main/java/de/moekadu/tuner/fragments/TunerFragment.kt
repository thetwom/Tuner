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
import de.moekadu.tuner.misc.WaveFileWriterIntent
import de.moekadu.tuner.models.PitchHistoryModel
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.viewmodels.TunerViewModel
import de.moekadu.tuner.views.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class TunerFragment : Fragment() {
    val viewModel: TunerViewModel by viewModels {
        TunerViewModel.Factory(requireActivity().preferenceResources, null)
    }

    private val waveFileWriterIntent = WaveFileWriterIntent(this)

    private var spectrumPlot: PlotView? = null
    private var spectrumPlotChangeId = -1
    private var correlationPlot: PlotView? = null
    private var correlationPlotChangeId = -1
    private var pitchPlot: PlotView? = null
    private var pitchPlotChangeId = -1
    private var volumeMeter: VolumeMeter? = null
    private var recordFab: FloatingActionButton? = null

//    /** Instance for requesting audio recording permission.
//     * This will create the sourceJob as soon as the permissions are granted.
//     */
//    private val askForPermissionAndNotifyViewModel = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { result ->
//        if (result) {
//            viewModel.startSampling()
//        } else {
//            Toast.makeText(activity, getString(R.string.no_audio_recording_permission), Toast.LENGTH_LONG)
//                .show()
//            Log.v(
//                "Tuner",
//                "TunerFragment.askForPermissionAnNotifyViewModel: No audio recording permission is granted."
//            )
//        }
//    }
//
//    private val menuProvider = object : MenuProvider {
//        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//            menuInflater.inflate(R.menu.toolbar, menu)
//        }
//
//        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
//            when (menuItem.itemId) {
//                R.id.action_settings -> {
//                    // User chose the "Settings" item, show the app settings UI...
//                    (activity as MainActivity?)?.loadSettingsFragment()
//                    return true
//                }
//            }
//            return false
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
////        Log.v("Tuner", "TunerFragment.onCreateView")
//        val view = inflater.inflate(R.layout.diagrams, container, false)
//
//        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
//
//        pitchPlot = view.findViewById(R.id.pitch_plot)
//        spectrumPlot = view.findViewById(R.id.spectrum_plot)
//        correlationPlot = view.findViewById(R.id.correlation_plot)
//        volumeMeter = view.findViewById(R.id.volume_meter)
//        recordFab = view.findViewById(R.id.record)
//
//        addTicksToSpectrumPlot()
//        spectrumPlot?.setYTouchLimits(0f, Float.POSITIVE_INFINITY)
//
//        addTicksToCorrelationPlot()
//        correlationPlot?.addYTicksLevel(floatArrayOf(0f)) { _, _ -> "" }
//
//        viewModel.spectrumPlotModel.observe(viewLifecycleOwner) { model ->
////            Log.v("Tuner", "TunerFragment: spectrumModel.observe: changeId=${model.changeId}, noteDetectionId=${model.noteDetectionChangeId}, targetId=${model.targetChangeId}")
//            if (model.changeId < spectrumPlotChangeId)
//                spectrumPlotChangeId = -1
//            if (model.noteDetectionChangeId > spectrumPlotChangeId) {
//                spectrumPlot?.plot(model.frequencies, model.squaredAmplitudes)
//                spectrumPlot?.setMarks(model.harmonicsFrequencies, null, HARMONIC_ID,
//                    indexEnd = model.numHarmonics)
//                val label = getString(R.string.hertz_1f, model.detectedFrequency)
//                if (model.detectedFrequency > 0f) {
//                    spectrumPlot?.setXMark(
//                        model.detectedFrequency, label, MARK_ID_FREQUENCY, LabelAnchor.SouthWest,
//                        placeLabelsOutsideBoundsIfPossible = false,
//                    )
//                } else {
//                    spectrumPlot?.removePlotMarks(MARK_ID_FREQUENCY)
//                }
//                if (model.frequencies.isNotEmpty())
//                    spectrumPlot?.setXTouchLimits(0f, model.frequencies.last())
//            }
//
//            if (model.targetChangeId > spectrumPlotChangeId) {
//                spectrumPlot?.xRange(model.frequencyRange[0], model.frequencyRange[1], 300L)
//            }
//            spectrumPlot?.enableExtraPadding = model.useExtraPadding
//            spectrumPlotChangeId = model.changeId
//        }
//
//        viewModel.correlationPlotModel.observe(viewLifecycleOwner) { model ->
////            Log.v("Tuner", "TunerFragment: spectrumModel.observe: changeId=${model.changeId}, noteDetectionId=${model.noteDetectionChangeId}, targetId=${model.targetChangeId}")
//            if (model.changeId < correlationPlotChangeId)
//                correlationPlotChangeId = -1
//
//            if (model.noteDetectionChangeId > correlationPlotChangeId) {
//                correlationPlot?.plot(model.timeShifts, model.correlationValues)
//                val label = getString(R.string.hertz_1f, model.detectedFrequency)
//                if (model.detectedFrequency > 0f) {
//                    correlationPlot?.setXMark(
//                        1.0f / model.detectedFrequency,
//                        label,
//                        MARK_ID_FREQUENCY,
//                        LabelAnchor.SouthWest,
//                        placeLabelsOutsideBoundsIfPossible = false
//                    )
//                } else {
//                    correlationPlot?.removePlotMarks(MARK_ID_FREQUENCY)
//                }
//                if (model.timeShifts.isNotEmpty())
//                    correlationPlot?.setXTouchLimits(0f, model.timeShifts.last())
//            }
//
//            if (model.targetChangeId > correlationPlotChangeId) {
//                correlationPlot?.xRange(model.timeShiftRange[0], model.timeShiftRange[1], 300L)
//            }
//
//            correlationPlot?.enableExtraPadding = model.useExtraPadding
//            correlationPlotChangeId = model.changeId
//        }
//
//        viewModel.pitchHistoryModel.observe(viewLifecycleOwner) { model ->
//            if (model.changeId < pitchPlotChangeId)
//                pitchPlotChangeId = -1
//
//            if (model.musicalScaleChangeId > pitchPlotChangeId || model.notePrintOptionsChangeId > pitchPlotChangeId) {
//                addTicksToHistoryPlot(model)
//
//                pitchPlot?.setYTouchLimits(model.musicalScaleFrequencies[0], model.musicalScaleFrequencies.last(), 0L)
//                pitchPlot?.enableExtraPadding = model.useExtraPadding
//            }
//            if (model.historyValuesChangeId > pitchPlotChangeId) {
//                if (model.numHistoryValues == 0 || model.currentFrequency <= 0f) {
//                    pitchPlot?.removePlotPoints(PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG)
//                    pitchPlot?.removePlotPoints(PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
//                    pitchPlot?.removePlotMarks(PitchHistoryModel.CENT_DEVIATION_MARK_TAG)
//                } else {
//                    val point = floatArrayOf(model.numHistoryValues - 1f, model.currentFrequency)
//                    pitchPlot?.setPoints(point, tag = PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG)
//                    pitchPlot?.setPoints(point, tag = PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
//
//                    // In theory this must be also reset, when the tolerance changes, but
//                    // in practice, we this should never be an issue, since after tolerance is
//                    // changed in the settings and when switching back to the tuner, we reevaluate
//                    // the current frequency anyway.
//                    if (model.centDeviationFromTarget.absoluteValue > model.toleranceInCents
//                        && model.centDeviationFromTarget != Float.MAX_VALUE
//                        && model.centDeviationFromTarget.absoluteValue <= PitchHistoryModel.MAX_SHOWN_CENT_DEVIATION) {
//                        val pointSize = pitchPlot?.pointSizes?.get(model.currentFrequencyPointStyle) ?: 1f
//                        pitchPlot?.setMark(
//                            point[0], point[1],
//                            label = getString(
//                                R.string.cent,
//                                model.centDeviationFromTarget.roundToInt()
//                            ),
//                            tag = PitchHistoryModel.CENT_DEVIATION_MARK_TAG,
//                            anchor = if (model.centDeviationFromTarget < 0)
//                                LabelAnchor.NorthEast else LabelAnchor.SouthEast,
//                            style = PitchHistoryModel.CENT_DEVIATION_MARK_STYLE,
//                            offsetX = pointSize,
//                            offsetY = -pointSize * model.tuningDirectionPointRelativeOffset
//                        )
//                    } else {
//                        pitchPlot?.removePlotMarks(PitchHistoryModel.CENT_DEVIATION_MARK_TAG)
//                    }
//                }
//                pitchPlot?.plot(
//                    model.historyValues, PitchHistoryModel.HISTORY_LINE_TAG,
//                    indexBegin = 0, indexEnd = model.numHistoryValues
//                )
//                pitchPlot?.xRange(0f, 1.08f * model.historyValues.size)
//            }
//
//            if (model.yRangeChangeId > pitchPlotChangeId) {
//                pitchPlot?.yRange(model.yRangeAuto[0], model.yRangeAuto[1], 600L)
//            }
//
//            if (model.targetNoteChangeId > pitchPlotChangeId || model.notePrintOptionsChangeId > pitchPlotChangeId) {
//                val targetNote = model.targetNote
//                val printer = model.noteNamePrinter
//                if (model.targetNoteFrequency > 0f && targetNote != null && printer != null) {
//                    pitchPlot?.setYMark(
//                        model.targetNoteFrequency,
//                        targetNote,
//                        printer,
//                        PitchHistoryModel.TARGET_NOTE_MARK_TAG,
//                        LabelAnchor.East,
//                        model.targetNoteMarkStyle,
//                        placeLabelsOutsideBoundsIfPossible = true
//                    )
//                } else {
//                    pitchPlot?.removePlotMarks(PitchHistoryModel.TARGET_NOTE_MARK_TAG)
//                }
//            }
//
//            if (model.toleranceChangeId > pitchPlotChangeId) {
//                if (model.lowerToleranceFrequency > 0f && model.upperToleranceFrequency > 0f) {
////                    Log.v("Tuner","TunerFragment: setting tolerance in pitchhistory: ${model.lowerToleranceFrequency} -- ${model.upperToleranceFrequency}, plotrange=${model.yRangeAuto[0]} -- ${model.yRangeAuto[1]}, currentFreq=${model.currentFrequency}")
//                    pitchPlot?.setMarks(
//                        null,
//                        floatArrayOf(
//                            model.lowerToleranceFrequency,
//                            model.upperToleranceFrequency
//                        ),
//                        PitchHistoryModel.TOLERANCE_MARK_TAG,
//                        styleIndex = PitchHistoryModel.TOLERANCE_STYLE,
//                        anchors = arrayOf(LabelAnchor.NorthWest, LabelAnchor.SouthWest),
//                        backgroundSizeType = MarkLabelBackgroundSize.FitLargest,
//                        placeLabelsOutsideBoundsIfPossible = false,
//                        maxLabelBounds = null
//                    ) { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float ->
//                        val s = when (index) {
//                            0 -> getString(R.string.cent, -model.toleranceInCents)
//                            1 -> getString(R.string.cent, model.toleranceInCents)
//                            else -> ""
//                        }
//                        StringLabel(
//                            s,
//                            textPaint,
//                            backgroundPaint,
//                            cornerRadius,
//                            gravity,
//                            paddingHorizontal,
//                            paddingHorizontal,
//                            paddingVertical,
//                            paddingVertical
//                        )
//                    }
//                } else {
//                    pitchPlot?.removePlotMarks(PitchHistoryModel.TOLERANCE_MARK_TAG)
//                }
//            }
//
//            pitchPlot?.setLineStyle(model.historyLineStyle, PitchHistoryModel.HISTORY_LINE_TAG)
//            pitchPlot?.setPointStyle(model.currentFrequencyPointStyle, PitchHistoryModel.CURRENT_FREQUENCY_POINT_TAG)
//            pitchPlot?.setPointStyle(model.tuningDirectionPointStyle, PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
//            val pointSize = pitchPlot?.pointSizes?.get(model.currentFrequencyPointStyle) ?: 1f
//            pitchPlot?.setPointOffset(
//                0f, pointSize * model.tuningDirectionPointRelativeOffset,
//                PitchHistoryModel.TUNING_DIRECTION_POINT_TAG
//            )
////            Log.v("Tuner", "TunerFragment: tuningDirectionPointVisible = ${model.tuningDirectionPointVisible}, offset=${pointSize * model.tuningDirectionPointRelativeOffset}")
//            pitchPlot?.setPointVisible(model.tuningDirectionPointVisible, PitchHistoryModel.TUNING_DIRECTION_POINT_TAG)
//            pitchPlot?.setMarkStyle(model.targetNoteMarkStyle, PitchHistoryModel.TARGET_NOTE_MARK_TAG)
//            pitchPlotChangeId = model.changeId
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.showWaveWriterFab.collect {
//                    recordFab?.visibility = if (it) View.VISIBLE else View.GONE
//                }
//            }
//        }
//
//        recordFab?.setOnClickListener {
//            viewLifecycleOwner.lifecycleScope.launch {
//                viewModel.waveWriter.storeSnapshot()
//                waveFileWriterIntent.launch()
//            }
//        }
//
//        return view
//    }
//
//    override fun onStart() {
//        super.onStart()
//        askForPermissionAndNotifyViewModel.launch(Manifest.permission.RECORD_AUDIO)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        activity?.let {
//            it.setTitle(R.string.app_name)
//            if (it is MainActivity) {
//                it.setStatusAndNavigationBarColors()
//                it.setPreferenceBarVisibilty(View.VISIBLE)
//            }
//        }
//    }
//
//    override fun onStop() {
//        viewModel.stopSampling()
//        super.onStop()
//    }

    private fun addTicksToSpectrumPlot() {
        val resolutions = intArrayOf(10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000)
        val maxValue = 50_000
        for (resolution in resolutions) {
            val numValues = if (maxValue % resolution == 0)
                maxValue / resolution + 1
            else
                maxValue / resolution
            val ticks = FloatArray(numValues) { (it * resolution).toFloat() }
            spectrumPlot?.addXTicksLevel(
                ticks,
                maxLabelBounds = { paint ->
                    val longestLabel = getString(R.string.hertz_str, maxValue.toString())
                    StringLabel.getBounds(longestLabel, paint)
                },
                labelCreator = { i, _, _, paint, backgroundPaint, gravity, paddingHorizontal, paddingVertical, cornerRadius ->
                    StringLabel(
                        getString(R.string.hertz_str, (i * resolution).toString()),
                        paint, backgroundPaint, cornerRadius, gravity,
                        paddingHorizontal, paddingHorizontal,
                        paddingVertical, paddingVertical
                    )
                }
            )
        }
    }

    private fun addTicksToCorrelationPlot() {
        val resolutions = floatArrayOf(0.0001f, 0.0002f, 0.0004f, 0.0008f, 0.0016f, 0.0032f, 0.0064f, 0.0128f, 0.0256f)
        val maxValue = 0.1f
        for (resolution in resolutions) {
            val numValues = (maxValue / resolution).toInt()
            val ticks = FloatArray(numValues) {
                val proposedTick = (it + 1) * resolution // we can't use 0 * resolution, otherwise we get division by zero later ...
                val closestIntegerFrequency = (1.0f / proposedTick).roundToInt()
                1.0f / closestIntegerFrequency
            }
            correlationPlot?.addXTicksLevel(
                ticks,
                maxLabelBounds = { paint ->
                    val longestLabel = getString(R.string.hertz_str, (1.0 / resolutions[0]).roundToInt().toString())
                    StringLabel.getBounds(longestLabel, paint)
                },
                labelCreator = { i, _, _, paint, backgroundPaint, gravity, paddingHorizontal, paddingVertical, cornerRadius ->
                    val proposedTick = (i + 1) * resolution // do it the same way as the ticks-computation a few lines before
                    val closestIntegerFrequency = (1.0f / proposedTick).roundToInt()
                    StringLabel(
                        getString(R.string.hertz_str, closestIntegerFrequency.toString()),
                        paint, backgroundPaint, cornerRadius, gravity,
                        paddingHorizontal, paddingHorizontal,
                        paddingVertical, paddingVertical
                    )
                }
            )
        }
    }

    private fun addTicksToHistoryPlot(model: PitchHistoryModel) {
        pitchPlot?.clearYTicks()
        model.noteNamePrinter?.let { printer ->
            pitchPlot?.addYTicksLevel(
                model.musicalScaleFrequencies,
                noteNameScale = model.musicalScale.noteNameScale,
                noteIndexBegin = model.musicalScale.noteIndexBegin,
                noteNamePrinter = printer
            )
        }
    }

    companion object{
        private const val MARK_ID_FREQUENCY = 11L
        private const val HARMONIC_ID = 12L
    }
}
