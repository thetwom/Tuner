package de.moekadu.tuner.ui.tuning

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.moekadu.tuner.R
import de.moekadu.tuner.notedetection.TuningTarget
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.notes.Note
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.computeMaxNoteSize
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.HorizontalMark
import de.moekadu.tuner.ui.plot.Line
import de.moekadu.tuner.ui.plot.Plot
import de.moekadu.tuner.ui.plot.PlotDefaults
import de.moekadu.tuner.ui.plot.PlotState
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.Point
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.theme.tunerColors
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

private class ResizeableArray(maxNumValues: Int) {
    val values = FloatArray(maxNumValues)
    var size: Int = 0
        private set

    fun add(value: Float) {
        if (size == values.size) {
            values.copyInto(values, 0, 1)
            size--
        }
        values[size] = value
        size++
    }

    fun resize(newMaxNumValues: Int): ResizeableArray {
        val resized = ResizeableArray(newMaxNumValues)
        if (size > newMaxNumValues) {
            values.copyInto(resized.values, 0, size - newMaxNumValues, size)
            resized.size = newMaxNumValues
        } else {
            values.copyInto(resized.values, 0, 0, size)
            resized.size = size
        }
        return resized
    }
}

private fun computeFrequencyRange(noteIndex: Int, musicalScale: MusicalScale, radius: Float)
        : ClosedFloatingPointRange<Float> {
    val outerRadius = 0.5f
    val startIndex = max(noteIndex - radius, musicalScale.noteIndexBegin.toFloat() - outerRadius)
    val endIndex = min(noteIndex + radius, musicalScale.noteIndexEnd.toFloat() + outerRadius)
    val startFrequency = musicalScale.getNoteFrequency(startIndex)
    val endFrequency = musicalScale.getNoteFrequency(endIndex)
    return startFrequency..endFrequency
}

private fun computeFrequencyRange(musicalScale: MusicalScale)
        : ClosedFloatingPointRange<Float> {
    val outerRadius = 0.5f
    val startIndex = musicalScale.noteIndexBegin.toFloat() - outerRadius
    val endIndex =  musicalScale.noteIndexEnd.toFloat() + outerRadius
    val startFrequency = musicalScale.getNoteFrequency(startIndex)
    val endFrequency = musicalScale.getNoteFrequency(endIndex)
    return startFrequency..endFrequency
}

private fun computeViewPortSize(noteIndex: Int, musicalScale: MusicalScale, radius: Float, capacity: Int): Rect {
    val range = computeFrequencyRange(noteIndex, musicalScale, radius)
    val viewPortMarginRelative = 0.05f
    return Rect(0f, range.endInclusive, capacity * (1 + viewPortMarginRelative), range.start)
}

private fun computeViewPortLimits(musicalScale: MusicalScale, capacity: Int): Rect {
    val range = computeFrequencyRange(musicalScale)
    val viewPortMarginRelative = 0.05f
    return Rect(0f, range.endInclusive, capacity * (1 + viewPortMarginRelative), range.start)
}

class PitchHistoryState(
    capacity: Int,
    val musicalScale: MusicalScale,
    val notePrintOptions: NotePrintOptions
) {
    private var count = FloatArray(capacity) { it.toFloat() }
        private set
    private var _values = ResizeableArray(capacity)
        private set
    private val capacity get() = _values.values.size
    val size get() = _values.size
    val values get() = _values.values

    val currentFrequency get() = if (size == 0) 0f else values[size - 1]

    private val automaticFrequencyRadius = 1.5f
    val plotState = PlotState(
        computeViewPortSize(noteIndex = 0, musicalScale, automaticFrequencyRadius, capacity),
        computeViewPortLimits(musicalScale, capacity)
    )

    init {
        val noteFreqs = listOf(
            FloatArray(musicalScale.noteIndexEnd - musicalScale.noteIndexBegin) {
                musicalScale.getNoteFrequency(musicalScale.noteIndexBegin + it)
            }).toImmutableList()
        plotState.setYTicks(
            key = KEY_YTICKS,
            yValues = noteFreqs,
            maxLabelHeight = {
                computeMaxNoteSize(
                    musicalScale.noteNameScale,
                    notePrintOptions = notePrintOptions,
                    fontSize = 12.sp,
                    fontWeight = null,
                    octaveRange = musicalScale.getNote(musicalScale.noteIndexBegin).octave..musicalScale.getNote(
                        musicalScale.noteIndexEnd
                    ).octave,
                    measurer = rememberTextMeasurer(),
                    resources = LocalContext.current.resources
                ).height.toFloat()
            },
            horizontalLabelPosition = 1f,
            anchor = Anchor.West,
            lineWidth = 1.dp
        ) { modifier, _, index, _ ->
            val note = musicalScale.getNote(musicalScale.noteIndexBegin + index)
            Note(
                note,
                modifier = modifier.padding(horizontal = 4.dp), // TODO: add outline width to padding
                notePrintOptions = notePrintOptions
            )
        }

    }

    fun addFrequency(value: Float) {
        _values.add(value)
        plotState.setLine(KEY_LINE, Line.Coordinates(count, values, 0, size))
        plotState.setPoint(
            KEY_CURRENT_POINT,
            Offset((size - 1).toFloat(), value),
            Point.drawCircle(
                8.dp,
                { MaterialTheme.colorScheme.primary }) // TODO: define other color, point size must come from somewhere else
        )
    }

    fun changeSettings(
        tuningTarget: TuningTarget? = null,
        toleranceInCents: Int = -1,
    ) {
        if (tuningTarget != null) {
            plotState.setYTicks(
                KEY_TARGET_NOTE,
                yValues = listOf(floatArrayOf(tuningTarget.frequency)).toImmutableList(),
                maxLabelHeight = {
                    computeMaxNoteSize(
                        musicalScale.noteNameScale,
                        notePrintOptions = notePrintOptions,
                        fontSize = 12.sp,
                        fontWeight = null,
                        octaveRange = musicalScale.getNote(musicalScale.noteIndexBegin).octave..musicalScale.getNote(
                            musicalScale.noteIndexEnd
                        ).octave,
                        measurer = rememberTextMeasurer(),
                        resources = LocalContext.current.resources
                    ).height.toFloat()
                },
                horizontalLabelPosition = 1f,
                anchor = Anchor.West,
                lineColor = { MaterialTheme.tunerColors.negative },
                lineWidth = 2.dp
            ) { modifier, _, index, _ ->
                val note = musicalScale.getNote(musicalScale.noteIndexBegin + index)
                Surface(
                    modifier = modifier,
                    color = MaterialTheme.tunerColors.negative,
                    contentColor = MaterialTheme.tunerColors.onNegative,
                    shape = MaterialTheme.shapes.small
                ) {
                    Note(
                        note,
                        modifier = modifier.padding(horizontal = 4.dp), // TODO: add outline width to padding
                        notePrintOptions = notePrintOptions
                    )
                }
            }
        }
        if (toleranceInCents > 0 && tuningTarget != null) {
            val ratio = centsToRatio(toleranceInCents.toFloat())
            val lowerToleranceFreq = tuningTarget.frequency / ratio
            val upperToleranceFreq = tuningTarget.frequency * ratio
            plotState.addHorizontalMarks(
                KEY_TOLERANCE,
                persistentListOf(
                    HorizontalMark(
                        position = lowerToleranceFreq,
                        anchor = Anchor.NorthWest,
                        horizontalLabelPosition = 0f,
                        lineWidth = 1.dp,
                        lineColor = { MaterialTheme.tunerColors.negative }
                    ) { modifier ->
                        Surface(
                            modifier = modifier,
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.inverseSurface
                        ) {
                            Text(
                                stringResource(id = R.string.cent, -toleranceInCents),
                                modifier = Modifier.padding(horizontal = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    HorizontalMark(
                        position = upperToleranceFreq,
                        anchor = Anchor.SouthWest,
                        horizontalLabelPosition = 0f,
                        lineWidth = 1.dp,
                        lineColor = { MaterialTheme.tunerColors.negative }
                    ) { modifier ->
                        Surface(
                            modifier = modifier,
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.inverseSurface
                        ) {
                            Text(
                                stringResource(id = R.string.cent, toleranceInCents),
                                modifier = Modifier.padding(horizontal = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                ),
                sameSizeLabels = true
            )

//            plotState.setYTicks(
//                KEY_LOWER_TOLERANCE,
//                yValues = listOf(floatArrayOf(lowerToleranceFreq)).toImmutableList(),
//                maxLabelHeight = {
//                    rememberTextLabelHeight()
//                },
//                horizontalLabelPosition = 0f,
//                anchor = Anchor.NorthWest,
//                lineColor = { MaterialTheme.tunerColors.negative },
//                lineWidth = 1.dp
//            ) { modifier, _, index, _ ->
//                Surface(
//                    modifier = modifier,
//                    color = MaterialTheme.tunerColors.negative,
//                    contentColor = MaterialTheme.tunerColors.onNegative,
//                    shape = MaterialTheme.shapes.small
//                ) {
//                    Surface(
//                        modifier = modifier,
//                        color = MaterialTheme.colorScheme.inverseSurface
//                    ){
//                        Text(
//                            stringResource(id = R.string.cent, -toleranceInCents),
//                            modifier = Modifier.padding(horizontal = 4.dp)
//                        )
//                    }
//                }
//            }

//            plotState.setYTicks(
//                KEY_UPPER_TOLERANCE,
//                yValues = listOf(floatArrayOf(upperToleranceFreq)).toImmutableList(),
//                maxLabelHeight = {
//                    rememberTextLabelHeight()
//                },
//                horizontalLabelPosition = 0f,
//                anchor = Anchor.SouthWest,
//                lineColor = { MaterialTheme.tunerColors.negative },
//                lineWidth = 1.dp
//            ) { modifier, _, index, _ ->
//                Surface(
//                    modifier = modifier,
//                    color = MaterialTheme.tunerColors.negative,
//                    contentColor = MaterialTheme.tunerColors.onNegative,
//                    shape = MaterialTheme.shapes.small
//                ) {
//                    Surface(
//                        modifier = modifier,
//                        color = MaterialTheme.colorScheme.inverseSurface
//                    ){
//                        Text(
//                            stringResource(id = R.string.cent, toleranceInCents),
//                            modifier = Modifier.padding(horizontal = 4.dp)
//                        )
//                    }
//                }
        }
    }


//    private var tuningState = TuningState.Unknown
//    var centDeviationFromTarget: Float = Float.MAX_VALUE
//        private set
//    var targetNote: MusicalNote? = null
//        private set
//    var targetNoteFrequency = 0f
//        private set
//
//    // note print options could go in as more global option
////    var notePrintOptions: NotePrintOptions? = null // maybe we can avoid this and use some global settings instead?
////            private set
//
//    /** Tolerance in cents. */
//    var toleranceInCents = 0
//        private set
//    /** Lower tolerance frequency value. */
//    var lowerToleranceFrequency = 0f
//        private set
//    /** Upper tolerance frequency value. */
//    var upperToleranceFrequency = 0f
//        private set

    // musical scale could go in as more global option
//    /** Scale is needed for ticks. */
//    var musicalScale = MusicalScaleFactory.create(DefaultValues.TEMPERAMENT)
//        private set
//    var musicalScaleFrequencies = computeMusicalScaleFrequencies(musicalScale)
//        private set
//
//    /** Define if currently we are detecting notes or not. */
//    var isCurrentlyDetectingNotes = true
//        private set

//    var useExtraPadding = false
//        private set
//
//        var historyLineStyle = HISTORY_LINE_STYLE_ACTIVE
//            private set
//        var currentFrequencyPointStyle = CURRENT_FREQUENCY_POINT_STYLE_INTUNE
//            private set
//        var tuningDirectionPointStyle = TUNING_DIRECTION_STYLE_TOOLOW_ACTIVE
//            private set
//        var tuningDirectionPointRelativeOffset = TUNING_DIRECTION_OFFSET_TOOLOW
//            private set
//        var tuningDirectionPointVisible = false
//            private set
//        var targetNoteMarkStyle = TARGET_NOTE_MARK_STYLE_INTUNE
//            private set

    companion object {
        const val KEY_LINE = 1
        const val KEY_CURRENT_POINT = 2
        const val KEY_YTICKS = 3
        const val KEY_TARGET_NOTE = 4
        const val KEY_TOLERANCE = 5
    }
}

@Composable
fun PitchHistory(
    state: PitchHistoryState,
    modifier: Modifier = Modifier,
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotDefaults.windowOutline() // TODO: can we remove this and use some system wide defaults?
) {
    Plot(
        state = state.plotState,
        modifier = modifier,
        plotWindowPadding = plotWindowPadding,
        plotWindowOutline = plotWindowOutline,
        lockX = true,
        lockY = false
    )
}

@Preview(widthDp = 200, heightDp = 300, showBackground = true)
@Composable
private fun PitchHistoryPreview() {
    TunerTheme {
        val state = remember {
            PitchHistoryState(
                10,
                musicalScale = MusicalScaleFactory.create(TemperamentType.EDO12),
                notePrintOptions = NotePrintOptions()
            ).apply {
                addFrequency(443f)
                addFrequency(442f)
                addFrequency(420f)
                addFrequency(460f)
                addFrequency(464f)
                addFrequency(440f)
                addFrequency(441f)
                addFrequency(450f)
                addFrequency(456f)
                addFrequency(440f)
                addFrequency(445f)
                val noteIndex = musicalScale.getClosestNoteIndex(443f)
                val freq = musicalScale.getNoteFrequency(noteIndex)
                val note = musicalScale.getNote(noteIndex)
                changeSettings(
                    tuningTarget = TuningTarget(note, freq, true, false),
                    toleranceInCents = 5
                )
            }

        }

        PitchHistory(
            state = state,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            plotWindowPadding = DpRect(0.dp, 0.dp, 20.dp, 0.dp)
        )

    }
}
