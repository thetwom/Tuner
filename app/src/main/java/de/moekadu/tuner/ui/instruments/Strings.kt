package de.moekadu.tuner.ui.instruments

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.theme.tunerColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Compute line width of a string.
 *
 * String with smaller indices (lower notes) are printed thicker.
 * @param index Index of string
 * @param minLineWidth Line width of string with highest index.
 * @param maxLineWidth Line width of string with smallest index.
 * @param minIndex Smallest index.
 * @param maxIndex Largest index.
 * @return Line width of string.
 */
private fun computeStringLineWidth(
    index: Int, minLineWidth: Dp, maxLineWidth: Dp, minIndex: Int, maxIndex: Int): Dp {
    if (maxIndex == minIndex || index == Int.MAX_VALUE)
        return (minLineWidth + maxLineWidth) / 2
    val relativeWidth = (index - minIndex).toFloat() / (maxIndex - minIndex).toFloat()
    return (maxLineWidth - minLineWidth) * (1 - relativeWidth) + minLineWidth
}

/** Determine the distance of the center a lazy list item below the viewport center.
 * Positive values are below the center, negative values are above the center.
 * @param item Lazy list item in question.
 * @return Distance below center in pixels.
 */
private fun LazyListState.itemDistanceBelowCenter(item: LazyListItemInfo): Int {
    val height = this.layoutInfo.viewportSize.height
    val itemHeight = this.layoutInfo.visibleItemsInfo.getOrNull(0)?.size ?: 2

    val scrollOffset = (height - itemHeight) / 2 + this.layoutInfo.viewportStartOffset
    return -scrollOffset + item.offset
}

/** Find closest highlighted string to view port center.
 * @return Index within strings or -1 if there is no string is found.
 */
private fun findIndexOfClosestScrollableHighlightedString(
    strings: List<StringWithInfo>?,
    highlightedStringKey: Int?,
    highlightedStringNote: MusicalNote?,
    musicalScale: MusicalScale,
    listState: LazyListState
): Int {
    return if (highlightedStringKey == null && highlightedStringNote == null) {
        -1
    } else if (strings == null) { // -> chromatic scale
        if (highlightedStringKey != null)
            highlightedStringKey // in chromatic scale the index in the scale corresponds to the key
        else if (highlightedStringNote != null)
            musicalScale.getNoteIndex(highlightedStringNote) - musicalScale.noteIndexBegin
        else
            -1
    } else {
        val closestIndex = listState.layoutInfo.visibleItemsInfo
            .filter {
                val note = strings[it.index].note
                val key = strings[it.index].key
                note == highlightedStringNote || key == highlightedStringKey
            }
            .minByOrNull { listState.itemDistanceBelowCenter(it).absoluteValue }?.index ?: -1
        if (closestIndex >= 0) {
            closestIndex
        } else {
            val firstIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            val closestIndexBefore = strings.subList(0, firstIndex).indexOfLast {
                it.note == highlightedStringNote || it.key == highlightedStringKey
            }
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val closestIndexAfterLast = strings.subList(lastIndex, strings.size).indexOfFirst {
                it.note == highlightedStringNote || it.key == highlightedStringKey
            }
            val closestIndexAfter = if (closestIndexAfterLast == -1) -1 else closestIndexAfterLast + lastIndex

            if (closestIndexBefore == -1 && closestIndexAfter == -1)
                -1
            else if (closestIndexBefore == -1)
                closestIndexAfter
            else if (closestIndexAfter == -1)
                closestIndexBefore
            else if (closestIndexBefore - firstIndex <= closestIndexAfter - lastIndex)
                closestIndexBefore
            else
                closestIndexAfter
        }
    }
}

/** Find next string of a given note behind the center to which we can scroll.
 * If the scroll position is at the end, we start searching from the top.
 * @param note Note of string for which we are looking.
 * @param strings Strings to be considered.
 * @return Index within strings of the next string with the given note or -1 if no string is found.
 */
private fun findNextScrollableStringIndex(
    strings: ImmutableList<StringWithInfo>?,
    note: MusicalNote?,
    numHighlightedStrings: Int,
    listState: LazyListState
): Int {
    if (note == null || strings == null)
        return -1
    if (numHighlightedStrings == 1)
        return strings.indexOfFirst { it.note == note }
    if (!listState.canScrollBackward && !listState.canScrollForward)
        return -1
    if (!listState.canScrollForward)
        return strings.indexOfFirst { it.note == note }

    val firstIndexBelowCenter = listState.layoutInfo.visibleItemsInfo
        .firstOrNull{ strings[it.index].note == note && listState.itemDistanceBelowCenter(it) > 0 }
        ?.index ?: -1
    return if (firstIndexBelowCenter >= 0) {
        firstIndexBelowCenter
    } else {
        val lastIndex = listState.layoutInfo.visibleItemsInfo.last().index
        val j = strings.subList(lastIndex, strings.size).indexOfFirst {
            it.note == note
        }
        if (j < 0)
            strings.indexOfFirst { it.note == note }
        else
            j + lastIndex
    }
}


/** Sidebar position (the base which shows the scroll buttons).*/
enum class StringsSidebarPosition {
    Start, /**< Sidebar is before the strings viewport. */
    End /**< Sidebar is behind the strings viewport. */
}

/** String with extra infos.
 * @param note String note.
 * @param key Unique key for the string. For chromatic scales, this is simply the
 *   index of the string, starting with 0 for the lowest note.
 // * @param musicalScaleIndex Index of note in the musical scale.
 */
data class StringWithInfo(
    val note: MusicalNote,
    val key: Int
    //val musicalScaleIndex: Int
) {
    companion object {
        fun generateKey(existingList: List<StringWithInfo>): Int {
            while (true) {
                val key = Random.nextInt(0, Int.MAX_VALUE - 1)
                if (existingList.firstOrNull {it.key == key} == null)
                    return key
            }
        }
    }
}

enum class StringsScrollMode{
    Manual, /**< Manually scrolling by user. */
    Automatic /**< Scroll automatically to highlighted note. */
}

class StringsState(firstVisibleIndex: Int) {
    /** The lazy list state. */
    val listState = LazyListState(firstVisibleIndex)
    /** Info the user scrolls manually or if we should center on highlighted note automatically. */
    var scrollMode by mutableStateOf(StringsScrollMode.Automatic)

    /** Scroll to the string with the given list index (first string is 0, next is 1, ...)
     * @param index String index.
     * @param coroutineContext Coroutine context within we scroll. This must be a compose context.
     */
    suspend fun scrollTo(index: Int, coroutineContext: CoroutineContext) {
        if ((!listState.canScrollForward && !listState.canScrollBackward) || index < 0)
            return
        withContext(coroutineContext) {
            val height = listState.layoutInfo.viewportSize.height
            val itemHeight = listState.layoutInfo.visibleItemsInfo.getOrNull(0)?.size ?: 2
            val scrollOffset = (height - itemHeight) / 2 + listState.layoutInfo.viewportStartOffset
            listState.animateScrollToItem(index, -scrollOffset)
        }
    }
}

/** Helper composable for showing the sidebar content.
 * @param stringColor Background color of highlighted string.
 * @param scrollMode Defines if the user is scrolling manually or not.
 * @param numHighlightedStrings Number of highlighted strings.
 * @param modifier Modifier.
 * @param onCenterClicked Callback if the scroll-to-center-button is clicked.
 * @param onNextClicked Callback if the scroll-to-next-button is clicked.
 */
@Composable
private fun StringsSidebar(
    stringColor: Color,
    scrollMode: StringsScrollMode,
    numHighlightedStrings: Int,
    modifier: Modifier = Modifier,
    onCenterClicked: () -> Unit = {},
    onNextClicked: () -> Unit = {},
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (scrollMode == StringsScrollMode.Manual) {
            ScrollCenterIcon(
                stringColor = stringColor,
                modifier = Modifier.clickable { onCenterClicked() }
            )
        } else if (scrollMode == StringsScrollMode.Automatic && numHighlightedStrings > 1) {
            ScrollNextIcon(
                stringColor = stringColor,
                modifier = Modifier.clickable { onNextClicked() }
            )
        }
    }
}


@Composable
fun Strings(
    strings: ImmutableList<StringWithInfo>?, // if null, we assume chromatic
    musicalScale: MusicalScale,
    modifier: Modifier = Modifier,
    tuningState: TuningState = TuningState.Unknown,
    highlightedNoteKey: Int? = null,
    highlightedNote: MusicalNote? = null,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    defaultColor: Color = MaterialTheme.colorScheme.inverseSurface,
    onDefaultColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
    inTuneColor: Color = MaterialTheme.tunerColors.positive,
    onInTuneColor: Color = MaterialTheme.tunerColors.onPositive,
    outOfTuneColor: Color = MaterialTheme.tunerColors.negative,
    onOutOfTuneColor: Color = MaterialTheme.tunerColors.onNegative,
    unknownTuningColor: Color = MaterialTheme.colorScheme.primary,
    onUnknownTuningColor: Color = MaterialTheme.colorScheme.onPrimary,
    fontSize: TextUnit = TextUnit.Unspecified,
    sidebarPosition: StringsSidebarPosition = StringsSidebarPosition.End,
    sidebarWidth: Dp = 30.dp,
    outerLabelPadding: Dp = 4.dp,
    outline: PlotWindowOutline = PlotWindowOutline(),
    state: StringsState = remember { StringsState(0) },
    onStringClicked: (key: Int, note: MusicalNote) -> Unit = {_, _ -> }
) {
    val scope = rememberCoroutineScope()
    val fontSizeResolved = fontSize.takeOrElse {
        LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
    }

    val minOctave = remember(strings, musicalScale) {
        strings?.minOfOrNull { it.note.octave }
            ?: musicalScale.noteNameScale.getNoteOfIndex(musicalScale.noteIndexBegin).octave
    }
    val maxOctave = remember(strings, musicalScale) {
        strings?.maxOfOrNull { it.note.octave }
            ?: musicalScale.noteNameScale.getNoteOfIndex(musicalScale.noteIndexEnd - 1).octave
    }
    val minNoteIndex = remember(strings, musicalScale) {
        strings?.minOfOrNull {
            val index = musicalScale.getNoteIndex(it.note)
            if (index == Int.MAX_VALUE)
                musicalScale.noteIndexBegin
            else
                index
        } ?: musicalScale.noteIndexBegin
    }
    val maxNoteIndex = remember(strings, musicalScale) {
        strings?.maxOfOrNull {
            val index = musicalScale.getNoteIndex(it.note)
            if (index == Int.MAX_VALUE)
                musicalScale.noteIndexEnd - 1
            else
                index
        } ?: (musicalScale.noteIndexEnd - 1)
    }
    val numStrings = strings?.size ?: (maxNoteIndex - minNoteIndex)

    val numHighlightedStrings = remember(highlightedNoteKey, highlightedNote) {
        if (highlightedNoteKey != null)
            strings?.count { it.key == highlightedNoteKey }?: 1
        else
            strings?.count { it.note == highlightedNote } ?: 1
    }

    val labelSize = rememberMaxNoteSize(
        noteNameScale = musicalScale.noteNameScale,
        notePrintOptions = notePrintOptions,
        fontSize = fontSizeResolved,
        octaveRange = minOctave..maxOctave
    ) + DpSize(16.dp, 4.dp)

    val tuningColor = remember(tuningState, inTuneColor, outOfTuneColor, unknownTuningColor) {
        when (tuningState) {
            TuningState.InTune -> inTuneColor
            TuningState.TooLow, TuningState.TooHigh -> outOfTuneColor
            else -> unknownTuningColor
        }
    }

    val onTuningColor = remember(tuningState, onInTuneColor, onOutOfTuneColor, onUnknownTuningColor) {
        when (tuningState) {
            TuningState.InTune -> onInTuneColor
            TuningState.TooLow, TuningState.TooHigh -> onOutOfTuneColor
            else -> onUnknownTuningColor
        }
    }

    val minStringWidth = 1.dp
    val maxStringWidth = 4.dp
    val space = -(labelSize.height - maxStringWidth) / 2 + 8.dp

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y != 0.0f)
                    state.scrollMode = StringsScrollMode.Manual
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    LaunchedEffect(key1 = highlightedNoteKey, key2 = highlightedNote) {
        if (state.scrollMode == StringsScrollMode.Automatic) {
            val i = findIndexOfClosestScrollableHighlightedString(
                strings, highlightedNoteKey, highlightedNote, musicalScale, state.listState
            )
            state.scrollTo(i, coroutineContext)
        }
    }

    val outlineColor = outline.color.takeOrElse {
        LocalContentColor.current.takeOrElse { MaterialTheme.colorScheme.onSurface } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    outlineColor,
                    cornerRadius = CornerRadius(outline.cornerRadius.toPx()),
                    style = Stroke(outline.lineWidth.toPx()),
                    topLeft = Offset(
                        x = if (sidebarPosition == StringsSidebarPosition.Start)
                            sidebarWidth.toPx() + 0.5f * outline.lineWidth.toPx()
                        else
                            0.5f * outline.lineWidth.toPx(),
                        y = 0.5f * outline.lineWidth.toPx()
                    ),
                    size = Size(
                        size.width - sidebarWidth.toPx() - outline.lineWidth.toPx(),
                        size.height - outline.lineWidth.toPx())
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )  {
                state.scrollMode = StringsScrollMode.Automatic
                val i = findIndexOfClosestScrollableHighlightedString(
                    strings,
                    highlightedNoteKey,
                    highlightedNote,
                    musicalScale,
                    state.listState
                )
                scope.launch { state.scrollTo(i, coroutineContext) }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sidebarPosition == StringsSidebarPosition.Start) {
            StringsSidebar(
                stringColor = tuningColor,
                scrollMode = state.scrollMode,
                numHighlightedStrings = numHighlightedStrings,
                modifier = Modifier.width(sidebarWidth),
                onCenterClicked = {
                    state.scrollMode = StringsScrollMode.Automatic
                    val i = findIndexOfClosestScrollableHighlightedString(
                        strings,
                        highlightedNoteKey,
                        highlightedNote,
                        musicalScale,
                        state.listState
                    )
                    scope.launch { state.scrollTo(i, coroutineContext) }
                },
                onNextClicked = {
                    val i = findNextScrollableStringIndex(
                        strings, highlightedNote, numHighlightedStrings, state.listState
                    )
                    scope.launch { state.scrollTo(i, coroutineContext) }
                }
            )
        }

        val outlineShape = remember(outline) { RoundedCornerShape(CornerSize(outline.cornerRadius)) }
        //val outlineColor = outline.color.takeOrElse { MaterialTheme.colorScheme.onSurface }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                //.border(outline.lineWidth, outlineColor, outlineShape)
                .clip(outlineShape)
        ) {
            val numLabelsPerStair = (maxWidth.value / (labelSize.width + 16.dp).value).roundToInt()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(space),
                modifier = Modifier
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(vertical = 4.dp),
                state = state.listState
            ) {
                items(numStrings, key = { strings?.get(it)?.key ?: it }) { index ->
                    val key = strings?.get(index)?.key ?: index
                    val noteInfo = strings?.get(index)
                    val noteNameScaleIndex = remember(noteInfo, musicalScale, index) {
                        if (noteInfo == null)
                            (musicalScale.noteIndexBegin + index)
                        else
                            musicalScale.getNoteIndex(noteInfo.note)
                        //noteInfo?.musicalScaleIndex ?: (musicalScale.noteIndexBegin + index)
                    }

                    val note = remember(noteInfo, musicalScale) {
                        noteInfo?.note ?: musicalScale.getNote(noteNameScaleIndex)
                    }

                    // we prefer key over highlighted note
                    val stringColor = if (highlightedNoteKey != null) {
                        if (key == highlightedNoteKey) tuningColor else defaultColor
                    } else {
                        if (note == highlightedNote) tuningColor else defaultColor
                    }
                    val onStringColor = if (highlightedNoteKey != null) {
                        if (key == highlightedNoteKey) onTuningColor else onDefaultColor
                    } else {
                        if (note == highlightedNote)  onTuningColor else onDefaultColor
                    }

                    SingleString(
                        note = note,
                        positionIndex = index % numLabelsPerStair,
                        numPositions = numLabelsPerStair,
                        fontSize = fontSizeResolved,
                        outerPadding = outerLabelPadding,
                        notePrintOptions = notePrintOptions,
                        stringLineWidth = computeStringLineWidth(
                            noteNameScaleIndex,
                            minStringWidth,
                            maxStringWidth,
                            minNoteIndex,
                            maxNoteIndex
                        ),
                        labelSize = labelSize,
                        color = stringColor,
                        contentColor = onStringColor,
                        onClick = { onStringClicked(key, note) }
                    )
                }
            }
        }
        if (sidebarPosition == StringsSidebarPosition.End) {
            StringsSidebar(
                stringColor = tuningColor,
                scrollMode = state.scrollMode,
                numHighlightedStrings = numHighlightedStrings,
                modifier = Modifier.width(sidebarWidth),
                onCenterClicked = {
                    state.scrollMode = StringsScrollMode.Automatic
                    val i = findIndexOfClosestScrollableHighlightedString(
                        strings,
                        highlightedNoteKey,
                        highlightedNote,
                        musicalScale,
                        state.listState
                    )
                    scope.launch { state.scrollTo(i, coroutineContext) }
                },
                onNextClicked = {
                    val i = findNextScrollableStringIndex(
                        strings, highlightedNote, numHighlightedStrings, state.listState
                    )
                    scope.launch { state.scrollTo(i, coroutineContext) }
                }
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 320, showBackground = true)
@Composable
private fun StringsPreview() {
    TunerTheme {
        val musicalScale = remember { MusicalScaleFactory.create(TemperamentType.EDO12) }
        val noteNameScale = musicalScale.noteNameScale
        val strings = remember(noteNameScale) {
            listOf<MusicalNote>(
                noteNameScale.notes[0].copy(octave = 2),
                noteNameScale.notes[1].copy(octave = 3),
                noteNameScale.notes[4].copy(octave = 3),
                noteNameScale.notes[2].copy(octave = 3),
                noteNameScale.notes[4].copy(octave = 3),
                noteNameScale.notes[3].copy(octave = 4),
                noteNameScale.notes[5].copy(octave = 4),
                noteNameScale.notes[7].copy(octave = 4),
                noteNameScale.notes[4].copy(octave = 3),
                noteNameScale.notes[9].copy(octave = 4),
                noteNameScale.notes[11].copy(octave = 5),
                noteNameScale.notes[6].copy(octave = 5),
                noteNameScale.notes[4].copy(octave = 6),
                noteNameScale.notes[5].copy(octave = 7),
                noteNameScale.notes[3].copy(octave = 8),
                noteNameScale.notes[4].copy(octave = 3),
                noteNameScale.notes[4].copy(octave = 8),
                noteNameScale.notes[10].copy(octave = 8),
            ).mapIndexed { index, note ->
                StringWithInfo(note, index) //, musicalScale.getNoteIndex(note))
            }.toPersistentList()
        }
        val notePrintOptions = NotePrintOptions()
        var highlightedNote by remember { mutableStateOf(strings.getOrNull(2)?.note) }
        Column {
            Strings(
                strings = strings,
                musicalScale = musicalScale,
                tuningState = TuningState.InTune,
                highlightedNote = highlightedNote,
                notePrintOptions = notePrintOptions,
                sidebarPosition = StringsSidebarPosition.End,
                onStringClicked = { key, note ->
                    highlightedNote = note
                },
                outline = PlotWindowOutline(lineWidth = 2.dp),
                modifier = Modifier.weight(0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            var highlightedNoteKey by remember { mutableStateOf(10) }
            Strings(
                strings = null,
                musicalScale = musicalScale,
                tuningState = TuningState.InTune,
                highlightedNoteKey = highlightedNoteKey,
                notePrintOptions = notePrintOptions,
                sidebarPosition = StringsSidebarPosition.End,
                onStringClicked = { key, note ->
                    highlightedNoteKey = highlightedNoteKey
                },
                modifier = Modifier.weight(0.5f)
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 320, showBackground = true)
@Composable
private fun StringsPreview2() {
    TunerTheme {
        val musicalScale = remember { MusicalScaleFactory.create(TemperamentType.EDO12) }
        val noteNameScale = musicalScale.noteNameScale
        val strings = remember(noteNameScale) {
            listOf(
                noteNameScale.notes[0].copy(octave = 2),
                noteNameScale.notes[0].copy(octave = 2),
            ).mapIndexed { index, note ->
                StringWithInfo(note, index) //, musicalScale.getNoteIndex(note))
            }.toPersistentList()
        }
        val notePrintOptions = NotePrintOptions()
        var highlightedNote by remember { mutableStateOf(strings.getOrNull(2)?.note) }
        Column {
            Strings(
                strings = strings,
                musicalScale = musicalScale,
                tuningState = TuningState.InTune,
                highlightedNote = highlightedNote,
                notePrintOptions = notePrintOptions,
                sidebarPosition = StringsSidebarPosition.End,
                onStringClicked = { key, note ->
                    highlightedNote = note
                },
                modifier = Modifier.padding(8.dp).fillMaxSize()
            )
        }
    }
}