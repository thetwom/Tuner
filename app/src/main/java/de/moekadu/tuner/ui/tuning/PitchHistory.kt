package de.moekadu.tuner.ui.tuning

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.common.Label
import de.moekadu.tuner.ui.notes.Note
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.TickLevelExplicitRanges
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.plot.HorizontalMark
import de.moekadu.tuner.ui.plot.LineCoordinates
import de.moekadu.tuner.ui.plot.Plot
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.PointMark
import de.moekadu.tuner.ui.plot.PointShape
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.theme.tunerColors
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}
private fun ratioToCents(ratio: Float): Float {
    // ratio = 2**(cent/1200)
    // log2(ratio) * 1200
    return 1200 * log2(ratio)
}

private fun computeToleranceBounds(centerFrequency: Float, toleranceInCents: Int)
        : ClosedFloatingPointRange<Float> {
    val ratio = centsToRatio(toleranceInCents.toFloat())
    val lowerToleranceFreq = centerFrequency / ratio
    val upperToleranceFreq = centerFrequency * ratio
    return lowerToleranceFreq .. upperToleranceFreq
}

private class ResizeableArray(private var maxNumValues: Int) {
    var values = LineCoordinates.create(floatArrayOf())
        private set

    val size get() = values.size
    fun add(value: Float) {
        val c = values.coordinates
        if (c.size == maxNumValues)
            c.removeAt(0)
        c.add(Offset(c.size.toFloat(), value))
        for (i in 0 until c.size)
            c[i] = c[i].copy(x = i.toFloat())
        values = LineCoordinates(c)
    }

    fun resize(newMaxNumValues: Int) {
        maxNumValues = newMaxNumValues
        val c = values.coordinates
        //val resized = FloatArray(newMaxNumValues)
        if (maxNumValues < c.size)
            c.subList(0, c.size - maxNumValues).clear()
        values = LineCoordinates(c)
    }
}

class PitchHistoryState(
    capacity: Int
) {
    private val history = ResizeableArray(capacity)

    var lineCoordinates by mutableStateOf(history.values)
        private set

    var pointCoordinates by mutableStateOf<Offset?>(null)
        private set

    var capacity by mutableIntStateOf(capacity)
        private set

    fun resize(newCapacity: Int) {
        capacity = newCapacity
        history.resize(newCapacity)
    }

    fun addFrequency(value: Float) {
        history.add(value)
        lineCoordinates = history.values // toLineCoordinates()
        pointCoordinates = Offset(history.size - 1f, value)
    }

    companion object {
        /** Compute number of samples to be stored in pitch history.
         * @param duration Duration of pitch history in seconds.
         * @param sampleRate Sample rate of audio signal in Hertz
         * @param windowSize Number of samples for one chunk of data which is used for evaluation.
         * @param overlap Overlap between to succeeding data chunks, where 0 is no overlap and 1 is
         *   100% overlap (1.0 is of course not allowed).
         * @return Number of samples, which must be stored in the pitch history, so that the we match
         *   the given duration.
         */
        fun computePitchHistorySize(
            duration: Float, sampleRate: Int, windowSize: Int, overlap: Float
        ) = (duration / (windowSize.toFloat() / sampleRate.toFloat() * (1.0f - overlap))).roundToInt()
    }
}

@Composable
fun PitchHistory(
    state: PitchHistoryState,
    musicalScale: MusicalScale,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    gestureBasedViewPort: GestureBasedViewPort = remember { GestureBasedViewPort() },
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    tuningState: TuningState = TuningState.Unknown,
    // line properties
    lineWidth: Dp = 2.dp,
    lineColor: Color = MaterialTheme.colorScheme.onSurface,
    lineWidthInactive: Dp = 1.dp,
    lineColorInactive: Color = MaterialTheme.colorScheme.outline,
    // point properties
    pointSize: Dp = 10.dp,
    pointSizeInactive: Dp = 7.dp,
    colorInTune: Color = MaterialTheme.tunerColors.positive,
    colorOutOfTune: Color = MaterialTheme.tunerColors.negative,
    colorInactive: Color = MaterialTheme.colorScheme.outline,
    // current cent deviation properties
    centDeviationStyle: TextStyle = MaterialTheme.typography.labelSmall,
    centDeviationColor: Color = MaterialTheme.colorScheme.onSurface,
    // target note properties
    targetNote: MusicalNote = musicalScale.referenceNote,
    targetNoteLineWidth: Dp = 2.dp,
    colorOnInTune: Color = MaterialTheme.tunerColors.onPositive,
    colorOnOutOfTune: Color = MaterialTheme.tunerColors.onNegative,
    colorOnInactive: Color = MaterialTheme.colorScheme.inverseOnSurface, // ???
    // tolerance properties
    toleranceInCents: Int = 5,
    toleranceLineColor: Color = MaterialTheme.colorScheme.outline,
    toleranceLabelStyle: TextStyle = MaterialTheme.typography.labelSmall,
    toleranceLabelColor: Color = MaterialTheme.colorScheme.inverseSurface,
    // tick properties
    tickLineWidth: Dp = 1.dp,
    tickLineColor: Color = MaterialTheme.colorScheme.outline,
    tickLabelStyle: TextStyle = MaterialTheme.typography.labelMedium,
    tickLabelColor: Color = MaterialTheme.colorScheme.onSurface,
    // outline
    plotWindowOutline: PlotWindowOutline = PlotWindowOutline()
) {
    val noteFrequencies = remember(musicalScale) {
        TickLevelExplicitRanges(
            listOf(
                FloatArray(musicalScale.noteIndexEnd - musicalScale.noteIndexBegin) {
                    musicalScale.getNoteFrequency(musicalScale.noteIndexBegin + it)
                }).toImmutableList()
        )
    }

    val limits = remember(musicalScale, state.capacity) {
        Rect(
            left = 0f,
            right = (state.capacity-1) * 1.1f,
            top = musicalScale.getNoteFrequency(musicalScale.noteIndexBegin - 0.2f),
            bottom = musicalScale.getNoteFrequency(musicalScale.noteIndexEnd + 0.2f)
        )
    }

    val targetFrequency = remember(musicalScale, targetNote) {
        val noteIndex = musicalScale.getNoteIndex(targetNote)
        musicalScale.getNoteFrequency(noteIndex)
    }

    val visibleRange = remember(targetNote, musicalScale, state.pointCoordinates?.y) {
        // note indices above/below target note
        val visibleRangeInIndices = 1.5f
        // if current frequency is far away from target note (happens on manual selection)
        // use extend range to next note index + the given value (should be smaller than
        // ~0.4, since otherwise we will get intermediate jumps due to the hysteresis
        // used to jump the the next target note)
        val visibleRangeInIndices2 = 0.38f

        val targetNoteIndex = musicalScale.getNoteIndex(targetNote)
        val currentFrequency = state.pointCoordinates?.y

        val noteIndexRange = if (targetNoteIndex == Int.MAX_VALUE && currentFrequency == null) {
            (-visibleRangeInIndices) .. visibleRangeInIndices
        } else if (currentFrequency == null){
            (targetNoteIndex - visibleRangeInIndices) .. (targetNoteIndex + visibleRangeInIndices)
        } else if (targetNoteIndex == Int.MAX_VALUE) {
            val frequencyNoteIndex = musicalScale.getClosestNoteIndex(currentFrequency)
            (frequencyNoteIndex - visibleRangeInIndices) .. (frequencyNoteIndex + visibleRangeInIndices)
        } else {
            val frequencyNoteIndex = musicalScale.getNoteIndex(currentFrequency)
            min(
                targetNoteIndex - visibleRangeInIndices,
                floor(frequencyNoteIndex) - visibleRangeInIndices2
            ) .. max(
                targetNoteIndex + visibleRangeInIndices,
                ceil(frequencyNoteIndex) + visibleRangeInIndices2
            )
        }

        musicalScale.getNoteFrequency(noteIndexRange.start) .. musicalScale.getNoteFrequency(
            noteIndexRange.endInclusive)
    }

    val viewPort = remember(visibleRange, limits) {
        Rect(
            left = limits.left,
            right = limits.right,
            top = visibleRange.endInclusive,
            bottom = visibleRange.start
        )
    }

    val maxNoteHeight = rememberMaxNoteSize(
        musicalScale.noteNameScale,
        notePrintOptions = notePrintOptions,
        fontSize = tickLabelStyle.fontSize,
        fontWeight = null,
        octaveRange = musicalScale.getNote(musicalScale.noteIndexBegin).octave..musicalScale.getNote(
            musicalScale.noteIndexEnd
        ).octave,
        textMeasurer = rememberTextMeasurer()
    ).height
    val maxNoteHeightPx = with(LocalDensity.current) { maxNoteHeight.toPx() }

    val tuningColor = remember(tuningState) {
        when (tuningState) {
            TuningState.InTune -> colorInTune
            TuningState.TooLow, TuningState.TooHigh -> colorOutOfTune
            TuningState.Unknown -> colorInactive
        }
    }
    val onTuningColor = remember(tuningState) {
        when (tuningState) {
            TuningState.InTune -> colorOnInTune
            TuningState.TooLow, TuningState.TooHigh -> colorOnOutOfTune
            TuningState.Unknown -> colorOnInactive
        }
    }

    Plot(
        modifier = modifier,
        viewPort = viewPort,
        viewPortGestureLimits = limits,
        gestureBasedViewPort = gestureBasedViewPort,
        plotWindowPadding = plotWindowPadding,
        plotWindowOutline = plotWindowOutline,
        lockX = true
    ) {
        YTicks(
            tickLevel = noteFrequencies,
            maxLabelHeight = maxNoteHeightPx,
            horizontalLabelPosition = 1f, // TODO: this must com from outside (left, right)
            anchor = Anchor.West, // TODO: this must com from outside (left, right)
            lineColor = tickLineColor,
            lineWidth = tickLineWidth,
            clipLabelToPlotWindow = false
        ) { noteModifier, _, index, _ ->
            val note = musicalScale.getNote(musicalScale.noteIndexBegin + index)
            Note(
                note,
                modifier = noteModifier.padding(horizontal = 4.dp),
                style = tickLabelStyle,
                color = tickLabelColor,
                notePrintOptions = notePrintOptions
            )
        }

        val targetMark = remember(targetNote, targetFrequency, tuningState, notePrintOptions) {
            persistentListOf(
                HorizontalMark(
                    targetFrequency,
                    HorizontalMark.Settings(
                        anchor = Anchor.West,
                        labelPosition = 1f,
                        lineWidth = targetNoteLineWidth,
                        lineColor = tuningColor
                    ),
                ){ m ->
                    Label(
                        content = {
                            Note(
                                targetNote,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                notePrintOptions = notePrintOptions,
                                style = tickLabelStyle,
                                color = onTuningColor
                            )
                        },
                        color = tuningColor,
                        modifier = m
                    )
                }
            )
        }
        HorizontalMarks(marks = targetMark, sameSizeLabels = true, clipLabelsToWindow = false)

        Line(
            data = state.lineCoordinates,
            lineColor = if (tuningState == TuningState.Unknown) lineColorInactive else lineColor,
            lineWidth = if (tuningState == TuningState.Unknown) lineWidthInactive else lineWidth
        )

        state.pointCoordinates?.let { position ->
            val pointShape = when (tuningState) {
                TuningState.Unknown -> PointShape.circle(pointSizeInactive, tuningColor)
                TuningState.InTune -> PointShape.circle(pointSize, tuningColor)
                TuningState.TooLow -> PointShape.circleWithUpwardTriangleShape(pointSize, tuningColor)
                TuningState.TooHigh -> PointShape.circleWithDownwardTriangleShape(pointSize, tuningColor)
            }
            Point(position, pointShape)

            if (tuningState == TuningState.TooHigh || tuningState == TuningState.TooLow) {
                val centDeviation = remember(targetFrequency, position) {
                    ratioToCents(position.y / targetFrequency).roundToInt()
                }
                val pointMark = persistentListOf(
                    PointMark(
                        position,
                        PointMark.Settings(
                            anchor = if (tuningState == TuningState.TooHigh) Anchor.South else Anchor.North,
                            screenOffset = DpOffset(
                                0.dp,
                                if (tuningState == TuningState.TooHigh) -pointSize/2 else pointSize/2
                            )
                        )
                    ) {
                        Text(
                            stringResource(id = R.string.cent, centDeviation),
                            modifier = it,
                            style = centDeviationStyle,
                            color = centDeviationColor
                        )
                    }
                )

                PointMarks(marks = pointMark)

            }
        }

        val toleranceMarks = remember(toleranceInCents, targetFrequency) {
            val bounds = computeToleranceBounds(targetFrequency, toleranceInCents)
            persistentListOf(
                HorizontalMark(
                    bounds.start,
                    HorizontalMark.Settings(
                        anchor = Anchor.NorthWest,
                        labelPosition = 0f,
                        lineWidth = 1.dp,
                        lineColor = toleranceLineColor
                    ),
                ){ m ->
                    Label(
                        content = { Text(
                            stringResource(id = R.string.cent, -toleranceInCents),
                            modifier = Modifier.padding(horizontal = 4.dp),
                            textAlign = TextAlign.Center,
                            style = toleranceLabelStyle
                        ) },
                        modifier = m,
                        color = toleranceLabelColor
                    )
                },
                HorizontalMark(
                    bounds.endInclusive,
                    HorizontalMark.Settings(
                        anchor = Anchor.SouthWest,
                        labelPosition = 0f,
                        lineWidth = 1.dp,
                        lineColor = toleranceLineColor
                    ),
                ){ m ->
                    Label(
                        content = { Text(
                            stringResource(id = R.string.cent, toleranceInCents),
                            modifier = Modifier.padding(horizontal = 4.dp),
                            textAlign = TextAlign.Center,
                            style = toleranceLabelStyle
                        ) },
                        modifier = m,
                        color = toleranceLabelColor
                    )
                }
            )
        }
        HorizontalMarks(marks = toleranceMarks, sameSizeLabels = true , clipLabelsToWindow = true)
    }
}

@Preview(widthDp = 300, heightDp = 400, showBackground = true)
@Composable
fun PitchHistory2Preview() {
    TunerTheme {
        val notePrintOptions = remember {
            NotePrintOptions()
        }
        val musicalScale = remember { MusicalScaleFactory.create(TemperamentType.EDO12) }
        val state = remember {
            PitchHistoryState(
                capacity = 9
            ).apply {
                addFrequency(440f)
                addFrequency(444f)
                addFrequency(430f)
                addFrequency(435f)
                addFrequency(439f)
                addFrequency(448f)
                addFrequency(450f)
                addFrequency(435f)
                addFrequency(445f)
            }
        }

        PitchHistory(
            state = state,
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            plotWindowPadding = DpRect(left = 4.dp, top = 0.dp, right=40.dp, bottom = 0.dp),
            tuningState = TuningState.TooHigh
        )
    }
}