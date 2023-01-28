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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.misc.WaveFileWriterIntent
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.temperaments.Notation
import de.moekadu.tuner.temperaments.TargetNote
import de.moekadu.tuner.viewmodels.TunerViewModel
import de.moekadu.tuner.views.*
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max

class TunerFragment : Fragment() {
    val viewModel: TunerViewModel by activityViewModels() // ? = null
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
    private var correlationPlot: PlotView? = null
    private var pitchPlot: PlotView? = null
    private var volumeMeter: VolumeMeter? = null
    private var recordFab: FloatingActionButton? = null

    private var isPitchInactive = false
    private var tuningStatus = TargetNote.TuningStatus.Unknown

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
            false
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
            ), false
        ) { _, i ->
            getString(R.string.hertz, 1 / i)
        }
        correlationPlot?.setYTicks(floatArrayOf(0f), false) { _, _ -> "" }

        pitchPlot?.yRange(400f, 500f, PlotView.NO_REDRAW)
//        viewModel.standardDeviation.observe(viewLifecycleOwner) { standardDeviation ->
//            volumeMeter?.volume = log10(max(1e-12f, standardDeviation))
//        }

        viewModel.musicalScale.observe(viewLifecycleOwner) { tuningFrequencies ->
            updatePitchPlotNoteNames()
            // TODO: should we extend the limits slightly, that the whole mark is visible?
            val firstFrequencyIndex = tuningFrequencies.noteIndexBegin
            val lastFrequencyIndex = tuningFrequencies.noteIndexEnd - 1
            val firstFrequency = tuningFrequencies.getNoteFrequency(firstFrequencyIndex)
            val lastFrequency = tuningFrequencies.getNoteFrequency(lastFrequencyIndex)
            pitchPlot?.setYTouchLimits(firstFrequency, lastFrequency, 0L)
        }

//        viewModel.noteNames.observe(viewLifecycleOwner) { // noteNames ->
//            updatePitchPlotNoteNames()
//        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                requireContext().preferenceResources.notePrintOptions.collect {
                    updatePitchPlotMarks(redraw = false)
                    updatePitchPlotNoteNames()
                    val needExtraPadding = (it.notation == Notation.solfege)
                    pitchPlot?.enableExtraPadding = needExtraPadding
                    spectrumPlot?.enableExtraPadding = needExtraPadding
                    correlationPlot?.enableExtraPadding = needExtraPadding
                }
            }
        }

//        viewModel.preferFlat.observe(viewLifecycleOwner) {
//            updatePitchPlotMarks(redraw = false)
//            updatePitchPlotNoteNames()
//        }

        viewModel.tunerResults.observe(viewLifecycleOwner) { results ->
            if (results.pitchFrequency == null) {
                correlationPlot?.removePlotMarks(null, false)
                spectrumPlot?.removePlotMarks(null, false)
            }
            else {
                results.pitchFrequency?.let { pitchFrequency ->
                    val label = getString(R.string.hertz, pitchFrequency)
                    correlationPlot?.setXMark(1.0f / pitchFrequency, label,
                        MARK_ID_FREQUENCY, LabelAnchor.SouthWest,
                        placeLabelsOutsideBoundsIfPossible = false)
                    spectrumPlot?.setXMark(pitchFrequency, label, MARK_ID_FREQUENCY,
                        LabelAnchor.SouthWest,
                        placeLabelsOutsideBoundsIfPossible = false)
                }
            }

            correlationPlot?.setXTouchLimits(0f, results.correlationTimes.last(),
                PlotView.NO_REDRAW
            )
            correlationPlot?.plot(results.correlationTimes, results.correlation)

            spectrumPlot?.setXTouchLimits(0f, results.ampSpecSqrFrequencies.last(),
                PlotView.NO_REDRAW
            )
            spectrumPlot?.plot(results.ampSpecSqrFrequencies, results.ampSqrSpec)

//            volumeMeter?.volume = results.noise
        }

        viewModel.pitchHistory.sizeAsLiveData.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.sizeAsLiveData: $it")
            pitchPlot?.xRange(0f, 1.08f * it.toFloat(), PlotView.NO_REDRAW)
        }

        //viewModel.pitchHistory.frequencyPlotRangeAveraged.observe(viewLifecycleOwner) {
        viewModel.frequencyPlotRange.observe(viewLifecycleOwner) {
//            Log.v("TestRecordFlow", "TunerFragment.plotRange: ${it[0]} -- ${it[1]}")
            pitchPlot?.yRange(it[0], it[1], 600)
        }

        viewModel.pitchHistory.historyAveraged.observe(viewLifecycleOwner) {
            if (it.size > 0) {
                val frequency = it.last()
                tuningStatus = viewModel.targetNote.value?.getTuningStatus(frequency) ?: TargetNote.TuningStatus.Unknown

                setStyles(isPitchInactive, tuningStatus, redraw = false)
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), frequency), redraw = false)
                pitchPlot?.setPoints(floatArrayOf((it.size - 1).toFloat(), frequency), tag = 1L, redraw = false)
                pitchPlot?.plot(it)
            }
        }

        viewModel.targetNote.observe(viewLifecycleOwner) { targetNote ->
            viewModel.pitchHistory.historyAveraged.value?.lastOrNull()?.let { frequency ->
                tuningStatus = targetNote.getTuningStatus(frequency)
            }
            setStyles(isPitchInactive, tuningStatus, redraw = false)
            updatePitchPlotMarks(redraw = true)
        }

        viewModel.pitchHistory.numValuesSinceLastLineUpdate.observe(viewLifecycleOwner) { numValuesSinceLastUpdate ->
            val maxTimeBeforeInactive = 0.3f // seconds
            val maxNumValuesBeforeInactive = max(1f, floor(maxTimeBeforeInactive / (viewModel.pitchHistoryUpdateInterval.value ?: 1f)))
            isPitchInactive = (numValuesSinceLastUpdate > maxNumValuesBeforeInactive)
            setStyles(isPitchInactive, tuningStatus, redraw = true)
        }

        // plot the values if available, since the plots currently cant store the plot lines.
        viewModel.tunerResults.value?.let { results ->
//            Log.v("Tuner", "TunerFragment: results: 0, ${results.correlationTimes.last()}")
//            correlationPlot?.xRange(0f, results.correlationTimes.last(), PlotView.NO_REDRAW)
            correlationPlot?.plot(results.correlationTimes, results.correlation)
            spectrumPlot?.plot(results.ampSpecSqrFrequencies, results.ampSqrSpec)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                requireContext().preferenceResources.waveWriterDurationInSeconds.collect { duration ->
                    recordFab?.visibility = if (duration == 0) View.GONE else View.VISIBLE
                }
            }
        }
//        viewModel.waveWriterSize.observe(viewLifecycleOwner) { waveWriterSize ->
//            recordFab?.visibility = if (waveWriterSize == 0) View.GONE else View.VISIBLE
//        }
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
        viewModel.setInstrument(instrumentDatabase[0])
        viewModel.setTargetNote(-1, null) // TunerViewModel.AUTOMATIC_TARGET_NOTE_DETECTION)
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

    private fun updatePitchPlotMarks(redraw: Boolean = true) {
        val targetNote = viewModel.targetNote.value ?: return
        //val musicalScale = viewModel.musicalScale.value ?: return
        //val noteNames = viewModel.noteNames.value ?: return
        //val preferFlat = viewModel.preferFlat.value ?: false

        val nameMinusBound = getString(R.string.cent, -targetNote.toleranceInCents)
        val namePlusBound = getString(R.string.cent, targetNote.toleranceInCents)

        if (targetNote.isTargetNoteAvailable) {
            pitchPlot?.setMarks(
                null,
                floatArrayOf(
                    targetNote.frequencyLowerTolerance,
                    targetNote.frequencyUpperTolerance
                ),
                MARK_ID_TOLERANCE,
                1,
                arrayOf(LabelAnchor.NorthWest, LabelAnchor.SouthWest),
                MarkLabelBackgroundSize.FitLargest,
                placeLabelsOutsideBoundsIfPossible = false,
                redraw = false,
                maxLabelBounds = null
            ) { index: Int, _: Float?, _: Float?, textPaint: TextPaint, backgroundPaint: Paint?, gravity: LabelGravity, paddingHorizontal: Float, paddingVertical: Float, cornerRadius: Float ->
                val s = when (index) {
                    0 -> nameMinusBound
                    1 -> namePlusBound
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
            pitchPlot?.setYMark(
                targetNote.frequency,
                targetNote.note,
                requireContext().preferenceResources.notePrintOptions.value,
                MARK_ID_FREQUENCY,
                LabelAnchor.East,
                if (tuningStatus == TargetNote.TuningStatus.InTune) 0 else 2,
                placeLabelsOutsideBoundsIfPossible = true,
                redraw = redraw
            )
        } else {
            pitchPlot?.removePlotMarks(MARK_ID_TOLERANCE, suppressInvalidate = true)
            pitchPlot?.removePlotMarks(MARK_ID_FREQUENCY, suppressInvalidate = !redraw)
        }
    }

    private fun updatePitchPlotNoteNames(redraw: Boolean = true) {
        //val noteNames = viewModel.noteNames.value ?: return
        //val preferFlat = viewModel.preferFlat.value ?: return
        val musicalScale = viewModel.musicalScale.value ?: return

        val numNotes = musicalScale.noteIndexEnd - musicalScale.noteIndexBegin
        val noteFrequencies = FloatArray(numNotes) {
            musicalScale.getNoteFrequency(musicalScale.noteIndexBegin + it)
        }

        // Update ticks in pitch history plot
        pitchPlot?.setYTicks(noteFrequencies, redraw = false,
            noteNameScale = musicalScale.noteNameScale, noteIndexBegin = musicalScale.noteIndexBegin,
            requireContext().preferenceResources.notePrintOptions.value
        )
//        { _, f ->
//            val toneIndex = musicalScale.getClosestNoteIndex(f)
//            noteNames.getNoteName(requireContext(), toneIndex, preferFlat = preferFlat)
//        }

        // Update active y-mark in pitch history plot
        viewModel.targetNote.value?.let { targetNote ->
            if (targetNote.isTargetNoteAvailable) {
                pitchPlot?.setYMark(
                    targetNote.frequency,
                    targetNote.note,
                    requireContext().preferenceResources.notePrintOptions.value,
                    //noteNames.getNoteName(requireContext(), targetNote.noteIndex, preferFlat),
                    MARK_ID_FREQUENCY,
                    LabelAnchor.East,
                    style = if (tuningStatus == TargetNote.TuningStatus.InTune) 0 else 2,
                    placeLabelsOutsideBoundsIfPossible = true,
                    redraw = redraw
                )
            } else {
                pitchPlot?.removePlotMarks(MARK_ID_FREQUENCY, suppressInvalidate = !redraw)
            }
        }
    }

    companion object{
        private const val MARK_ID_TOLERANCE = 10L
        private const val MARK_ID_FREQUENCY = 11L
    }
}
