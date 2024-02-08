package de.moekadu.tuner.ui.instruments

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.rememberMaxNoteSize
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.theme.tunerColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.roundToInt

/** Compute line width of a string.
 *
 * String with smaller indices (lower notes) are printed thicker.
 * @param index Index of string
 * @param minLineWidth Line width of string with highest index.
 * @param maxLineWidth Line width of string with smallerst index.
 * @param minIndex Smallest index.
 * @param maxIndex Largest index.
 * @return Line width of string.
 */
private fun computeStringLineWidth(
    index: Int, minLineWidth: Dp, maxLineWidth: Dp, minIndex: Int, maxIndex: Int): Dp {
    val relativeWidth = (index - minIndex).toFloat() / (maxIndex - minIndex).toFloat()
    return (maxLineWidth - minLineWidth) * (1 - relativeWidth) + minLineWidth
}

/** Scroll modes. */
enum class StringsScrollMode{
    Manual, /**< Manually scrolling by user. */
    Automatic /**< Scroll automatically to highlighted note. */
}

/** Scroll state.
 * @param firstVisibleIndex First visible index during initialization.
 */
private class ScrollState(firstVisibleIndex: Int) {
    /** The lazy list state. */
    val listState = LazyListState(firstVisibleIndex)
    /** Info the user scrolls manually or if we should center on highlighted note automatically. */
    var scrollMode by mutableStateOf(StringsScrollMode.Automatic)
}

/** Find index of highlighted note within the given strings list or within musical scale.
 * If strings is null, we look for the index in the musical scale.
 * @param strings List of strings.
 * @param musicalScale Musical scale.
 * @param highlightedString Highlighted note.
 * @return Index of highlighted note or -1 if not found.
 */
private fun findHighlightedNoteIndex(
    strings: ImmutableList<StringsState.StringWithInfo>?,
    musicalScale: MusicalScale,
    highlightedString: StringsState.HighlightedString
): Int {
    return when {
        strings != null -> {
            strings.indexOfFirst {
                it.key == highlightedString.key || it.note == highlightedString.note
            }
        }
        highlightedString.key >= 0 -> {
            highlightedString.key
        }
        highlightedString.note != null -> {
            musicalScale.getNoteIndex(highlightedString.note) + musicalScale.noteIndexBegin
        }
        else -> {
            -1
        }
    }
}

/** State for the strings class.
 * @param strings List of instrument strings or null for chromatic scales.
 * @param musicalScale Underlying musical scale, mainly needed for chromatic scales.
 * @param highlightedString Info about which notes should be highlighted.
 * @param tuningState Info about the tuning state of the highlighted notes.
 *   This in the end defines which color the highlighted notes are.
 * @param scrollState Scroll state.
 */
data class StringsState private constructor (
    val strings: PersistentList<StringWithInfo>?,
    val musicalScale: MusicalScale,
    val highlightedString: HighlightedString,
    val tuningState: TuningState = TuningState.Unknown,
    private val scrollState: ScrollState = ScrollState(
        max(0, findHighlightedNoteIndex(strings, musicalScale, highlightedString))
    )
) {
    /** String with extra infos.
     * @param note String note.
     * @param key Unique key for the string. For chromatic scales, this is simply the
     *   index of the string, starting with 0 for the lowest note.
     * @param musicalScaleIndex Index of note in the musical scale.
     */
    data class StringWithInfo(
        val note: MusicalNote,
        val key: Int,
        val musicalScaleIndex: Int
    )

    /** Highlighted string definition.
     *
     * A note can be highlighted by the key of the StringWithInfo or by its note.
     * @param key Key of Note to be highlighted or -1 if the string should not be highlighted by
     *   key.
     * @param note Note of strings to be highlighted (which means that more than one strings can
     *   be highlighted if they share the same note) or null to not highlight by note.
     */
    data class HighlightedString(val key: Int = -1, val note: MusicalNote? = null)

    /** Quick access the scroll state of the underlying lazy list. */
    val lazyListState get() = scrollState.listState

    /** Quick access to scroll mode (manual, automatic). */
    var scrollMode
        get() = scrollState.scrollMode
        set(value) { scrollState.scrollMode = value}

    /** Number of strings. */
    val numStrings get() = strings?.size ?: (musicalScale.noteIndexEnd - musicalScale.noteIndexBegin)

    /** Number of highlighted strings. */
    val numHighlightedStrings = when {
        highlightedString.key >= 0 -> strings?.count { it.key == highlightedString.key } ?: 1
        highlightedString.note != null -> strings?.count { it.note == highlightedString.note } ?: 1
        else -> 0
    }

    /** Change the musical scale and return the updated state.
     * @param musicalScale New musical scale.
     * @return Updated state.
     */
    fun modifyMusicalScale(musicalScale: MusicalScale): StringsState {
        return if (musicalScale == this.musicalScale)
            this
        else
            this.copy(musicalScale = musicalScale)
    }

    /** Highlight a string by key and return the updated state.
     * @param key Key of note to highlight.
     * @return Updated state.
     */
    fun highlightString(key: Int)
            = this.copy(highlightedString = HighlightedString(key = key))

    /** Highlight strings by note and return the updated state.
     * @param note Note of strings to highlight.
     * @return Updated state.
     */
    fun highlightString(note: MusicalNote)
            = this.copy(highlightedString = HighlightedString(note = note))

    /** Set tuning state of highlighted strings and return the updated state.
     * @param tuningState Tuning state.
     * @return Updated state.
     */
    fun setTuningStateOfHighlightedStrings(tuningState: TuningState)
            = this.copy(tuningState = tuningState)

    /** Move selected string to another position and return the updated state.
     * @param toIndex Index where to move the selected string.
     * @return Updated state.
     */
    fun moveSelectedString(toIndex: Int, scope: CoroutineScope): StringsState {
        val noteIndex = findHighlightedNoteIndex(strings, musicalScale, highlightedString)
        if (noteIndex == toIndex || noteIndex == -1 || strings == null)
            return this
        val newStrings = strings.mutate {
            val note = it.removeAt(noteIndex)
            it.add(toIndex, note)
        }
        scope.launch {
            scrollTo(toIndex, coroutineContext)
        }
        return this.copy(strings = newStrings)
    }

    /** Deleted the selected string return the updated state.
     * @return Updated state.
     */
    fun deleteSelectedNote(): StringsState {
        val noteIndex = findHighlightedNoteIndex(strings, musicalScale, highlightedString)
        if (noteIndex == -1 || strings == null)
            return this
        val newStrings = strings.mutate {
            it.removeAt(noteIndex)
        }
        val newKey = if (highlightedString.key < 0)
            -1
        else if (noteIndex in strings.indices)
            newStrings[noteIndex].key
        else if (newStrings.size > 0)
            newStrings.last().key
        else
            -1

        return this.copy(strings = newStrings, highlightedString = highlightedString.copy(key = newKey))
    }

    /** Copy the selected string and return the updated state.
     * The copied string will be positioned directly behind its source.
     * @return Updated state.
     */
    fun copySelectedString(): StringsState {
        val noteIndex = findHighlightedNoteIndex(strings, musicalScale, highlightedString)
        if (noteIndex == -1 || strings == null)
            return this
        val newKey = strings.maxOf { it.key } + 1
        val newStrings = strings.mutate {
            it.add(noteIndex + 1, strings[noteIndex].copy(key = newKey))
        }
        return this.copy(strings = newStrings, highlightedString = HighlightedString(newKey))
    }

    /** Modify the note of the selected string and return the updated state.
     * @param note New note for the selected string.
     * @return Updated state.
     */
    fun modifySelectedString(note: MusicalNote): StringsState {
        val noteIndex = findHighlightedNoteIndex(strings, musicalScale, highlightedString)
        if (noteIndex == -1 || strings == null)
            return this
        val newStrings = strings.mutate {
            it[noteIndex] = strings[noteIndex].copy(note = note)
        }
        return this.copy(strings = newStrings)
    }

    /** Determine the distance of the center a lazy list item below the viewport center.
     * Positive values are below the center, negative values are above the center.
     * @param item Lazy list item in question.
     * @return Distance below center in pixels.
     */
    private fun itemDistanceBelowCenter(item: LazyListItemInfo): Int {
        val height = scrollState.listState.layoutInfo.viewportSize.height
        val itemHeight = scrollState.listState.layoutInfo.visibleItemsInfo.getOrNull(0)?.size ?: 2

        val scrollOffset = (height - itemHeight) / 2 + scrollState.listState.layoutInfo.viewportStartOffset
        return -scrollOffset + item.offset
    }

    /** Find next string of a given note behind the center to which we can scroll.
     * If the scroll position is at the end, we start searching from the top.
     * @param note Note of string for which we are looking.
     * @param strings Strings to be considered.
     * @return Index within strings of the next string with the given note or -1 if no string is found.
     */
    private fun findNextScrollableStringIndex(note: MusicalNote, strings: ImmutableList<StringWithInfo>): Int {
        if (numHighlightedStrings == 1) {
            return strings.indexOfFirst { it.note == note }
        }
        if (!scrollState.listState.canScrollBackward && !scrollState.listState.canScrollForward)
            return -1
        if (!scrollState.listState.canScrollForward) {
            return strings.indexOfFirst { it.note == note }
        }

        val firstIndexBelowCenter = scrollState.listState.layoutInfo.visibleItemsInfo
            .firstOrNull{ strings[it.index].note == note && itemDistanceBelowCenter(it) > 0 }
            ?.index ?: -1
        return if (firstIndexBelowCenter >= 0) {
            firstIndexBelowCenter
        } else {
            val lastIndex = scrollState.listState.layoutInfo.visibleItemsInfo.last().index
            val j = strings.subList(lastIndex, strings.size).indexOfFirst {
                it.note == note
            }
            if (j < 0)
                strings.indexOfFirst { it.note == note }
            else
                j + lastIndex
        }
    }

    /** Find closest highlighted string to view port center.
     * If the scroll position is at the end, we start searching from the top.
     * @return Index within strings or -1 if there is no string is found.
     */
    private fun findIndexOfClosestScrollableHighlightedString(): Int {
        return if (strings == null) {
            if (highlightedString.key >= 0)
                highlightedString.key
            else if (highlightedString.note != null)
                musicalScale.getNoteIndex(highlightedString.note) - musicalScale.noteIndexBegin
            else
                -1
        } else {
            val closestIndex = scrollState.listState.layoutInfo.visibleItemsInfo
                .filter {
                    val note = strings[it.index].note
                    val key = strings[it.index].key
                    note == highlightedString.note || key == highlightedString.key
                }
                .minByOrNull { itemDistanceBelowCenter(it).absoluteValue }?.index ?: -1
            if (closestIndex >= 0) {
                closestIndex
            } else {
                val firstIndex = scrollState.listState.layoutInfo.visibleItemsInfo.first().index
                val closestIndexBefore = strings.subList(0, firstIndex).indexOfLast {
                    it.note == highlightedString.note || it.key == highlightedString.key
                }
                val lastIndex = scrollState.listState.layoutInfo.visibleItemsInfo.last().index
                val closestIndexAfterLast = strings.subList(lastIndex, strings.size).indexOfFirst {
                    it.note == highlightedString.note || it.key == highlightedString.key
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

    /** Scroll to closest highlighted string.
     * @param coroutineContext Coroutine context within we scroll. This must be a compose context.
     */
    suspend fun scrollToClosestHighlightedString(coroutineContext: CoroutineContext) {
        if (!lazyListState.canScrollForward && !lazyListState.canScrollBackward)
            return
        val index = if (numHighlightedStrings == 0){
            -1
        } else {
            findIndexOfClosestScrollableHighlightedString()
        }
        if (index >= 0)
            scrollTo(index, coroutineContext)
    }

    /** Scroll to next highlighted string below the viewport center.
     * If we are already at the bottom, scroll to the first string.
     * @param coroutineContext Coroutine context within we scroll. This must be a compose context.
     */
    suspend fun scrollToNextHighlightedString(coroutineContext: CoroutineContext) {
        if (!lazyListState.canScrollForward && !lazyListState.canScrollBackward)
            return
        val index = if (strings == null || highlightedString.note == null || numHighlightedStrings <= 1){
            -1
        } else {
            findNextScrollableStringIndex(highlightedString.note, strings)
        }
        if (index >= 0)
            scrollTo(index, coroutineContext)
    }

    /** Scroll to the string with the given key.
     * @param key Key of string.
     * @param coroutineContext Coroutine context within we scroll. This must be a compose context.
     */
    suspend fun scrollToStringWithKey(key: Int, coroutineContext: CoroutineContext) {
        val index = strings?.indexOfFirst { it.key == key } ?: key
        if (index > 0)
            scrollTo(index, coroutineContext)
    }

    /** Scroll to the string with the given list index (first string is 0, next is 1, ...)
     * @param index String index.
     * @param coroutineContext Coroutine context within we scroll. This must be a compose context.
     */
    suspend fun scrollTo(index: Int, coroutineContext: CoroutineContext) {
        withContext(coroutineContext) {
            val height = scrollState.listState.layoutInfo.viewportSize.height
            val itemHeight = scrollState.listState.layoutInfo.visibleItemsInfo.getOrNull(0)?.size ?: 2
            val scrollOffset = (height - itemHeight) / 2 + scrollState.listState.layoutInfo.viewportStartOffset
            scrollState.listState.animateScrollToItem(index, -scrollOffset)
        }
    }

    companion object {
        /** Factory for creating a new state object.
         * @param strings List of string notes or null to use a chromatic scale.
         * @param musicalScale Musical scale, mainly needed for chromatic scale.
         * @param highlightedNote Note of strings which should be highlighted or null to
         *   not highlight any strings.
         */
        fun create(
            strings: Array<MusicalNote>?,
            musicalScale: MusicalScale,
            highlightedNote: MusicalNote? = null
        ): StringsState {
            return StringsState(
                strings?.mapIndexed { index, note ->  StringWithInfo(
                    note, index, musicalScale.getNoteIndex(note)) }
                    ?.toPersistentList(),
                musicalScale,
                highlightedString = HighlightedString(note = highlightedNote)
            )
        }
    }
}

/** Color definition of a given string as well as the text label.
 * @param background Color of string and the label box.
 * @param content Color of the label text.
 */
data class StringBackgroundAndContentColor(
    val background: Color = Color.Unspecified,
    val content: Color = Color.Unspecified
)

/** Possible string colors.
 * @param default Color definitions for not highlighted strings.
 * @param inTune Color definitions for highlighted strings which are in tune.
 * @param outOfTune Color definitions for highlighted strings which are out of tune.
 */
data class StringsColors(
    val default: StringBackgroundAndContentColor,
    val inTune: StringBackgroundAndContentColor,
    val outOfTune: StringBackgroundAndContentColor,
)

/** Default colors for the strings composable. */
object StringsDefaults {
    /** Color definitions for the strings composable.
     * @param default Background for strings which are not highlighted.
     * @param defaultContent Label text for strings which are not highlighted.
     * @param inTune Background for strings which are highlighted and in tune.
     * @param inTuneContent Label text for strings which are highlighted and in tune.
     * @param outOfTune Background for strings which are highlighted and out of tune.
     * @param outOfTuneContent Label text for strings which are highlighted and out of tune.
     */
    @Composable
    fun colors(
        default: Color = Color.Unspecified,
        defaultContent: Color = Color.Unspecified,
        inTune: Color = MaterialTheme.tunerColors.positive,
        inTuneContent: Color = MaterialTheme.tunerColors.onPositive,
        outOfTune: Color = MaterialTheme.tunerColors.negative,
        outOfTuneContent: Color = MaterialTheme.tunerColors.onNegative,
    ) = StringsColors(
        StringBackgroundAndContentColor(default, defaultContent),
        StringBackgroundAndContentColor(inTune, inTuneContent),
        StringBackgroundAndContentColor(outOfTune, outOfTuneContent)
    )
}

/** Sidebar position (the base which shows the scroll buttons).*/
enum class StringsSidebarPosition {
    Start, /**< Sidebar is before the strings viewport. */
    End /**< Sidebar is behind the strings viewport. */
}

/** The strings composable.
 * @param state State of the composable.
 * @param modifier Modifier.
 * @param outerLabelPadding Space between outer label bounds and outline of the strings window.
 * @param notePrintOptions How to print notes.
 * @param fontSize The font size of the note labels.
 * @param colors Colors definitions.
 * @param onStringClicked Callback when a string label is clicked.
 * @param sidebarPosition Position of the sidebar which shows the scroll buttons.
 * @param sidebarWidth Width of the sidebar.
 */
@Composable
fun Strings(
    state: StringsState,
    modifier: Modifier = Modifier,
    outerLabelPadding: Dp = 4.dp,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    fontSize: TextUnit = TextUnit.Unspecified,
    colors: StringsColors = StringsDefaults.colors(),
    onStringClicked: (key: Int, note: MusicalNote) -> (Unit) = {_, _ -> },
    sidebarPosition: StringsSidebarPosition = StringsSidebarPosition.Start,
    sidebarWidth: Dp = 33.dp
) {
    val scope = rememberCoroutineScope()
    val fontSizeResolved = fontSize.takeOrElse {
        LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
    }

    val minOctave = remember(state.strings, state.musicalScale) {
        state.strings?.minOf { it.note.octave }
            ?: state.musicalScale.noteNameScale.getNoteOfIndex(state.musicalScale.noteIndexBegin).octave
    }
    val maxOctave = remember(state.strings, state.musicalScale) {
        state.strings?.maxOf { it.note.octave }
            ?: state.musicalScale.noteNameScale.getNoteOfIndex(state.musicalScale.noteIndexEnd - 1).octave
    }

    val minNoteIndex = remember(state.strings, state.musicalScale) {
        state.strings?.minOf { it.musicalScaleIndex } ?: state.musicalScale.noteIndexBegin
    }
    val maxNoteIndex = remember(state.strings, state.musicalScale) {
        state.strings?.maxOf { it.musicalScaleIndex } ?: (state.musicalScale.noteIndexEnd - 1)
    }

    val highlightedStringColor = remember(state.tuningState) {
        when(state.tuningState) {
            TuningState.Unknown -> colors.default
            TuningState.InTune -> colors.inTune
            else -> colors.outOfTune
        }
    }

    val labelSize = rememberMaxNoteSize(
        noteNameScale = state.musicalScale.noteNameScale,
        notePrintOptions = notePrintOptions,
        fontSize = fontSizeResolved,
        octaveRange = minOctave..maxOctave
    ) + DpSize(16.dp, 4.dp)

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
                state.scrollMode = StringsScrollMode.Manual
                return super.onPostScroll(consumed, available, source)
            }

        }
    }

    LaunchedEffect(key1 = state.highlightedString) {
        if (state.scrollMode == StringsScrollMode.Automatic)
            state.scrollToClosestHighlightedString(coroutineContext)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sidebarPosition == StringsSidebarPosition.Start) {
            StringsSidebar(
                stringColor = highlightedStringColor.background,
                scrollMode = state.scrollMode,
                numHighlightedStrings = state.numHighlightedStrings,
                modifier = Modifier.width(sidebarWidth),
                onCenterClicked = {
                    state.scrollMode = StringsScrollMode.Automatic
                    scope.launch { state.scrollToClosestHighlightedString(coroutineContext) }
                },
                onNextClicked = {
                    scope.launch { state.scrollToNextHighlightedString(coroutineContext) }
                }
            )
        }
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val numLabelsPerStair = (maxWidth.value / (labelSize.width + 16.dp).value).roundToInt()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(space),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.large)
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(vertical = 4.dp),
                state = state.lazyListState
            ) {
                //items(state.strings, key = { it.key }) { note ->
                items(state.numStrings, key = { state.strings?.get(it)?.key ?: it }) { index ->
                    val key = state.strings?.get(index)?.key ?: index
                    val noteInfo = state.strings?.get(index)
                    val noteNameScaleIndex =
                        noteInfo?.musicalScaleIndex ?: (state.musicalScale.noteIndexBegin + index)
                    val note = noteInfo?.note ?: state.musicalScale.getNote(noteNameScaleIndex)
                    val color = if (note != state.highlightedString.note && key != state.highlightedString.key)
                        colors.default
                    else
                        highlightedStringColor

                    SingleString(
                        note = note,
                        positionIndex = index % numLabelsPerStair,
                        numPositions = numLabelsPerStair,
                        fontSize = fontSizeResolved,
                        outerPadding = outerLabelPadding,
                        stringLineWidth = computeStringLineWidth(
                            noteNameScaleIndex,
                            minStringWidth,
                            maxStringWidth,
                            minNoteIndex,
                            maxNoteIndex
                        ),
                        labelSize = labelSize,
                        color = color.background,
                        contentColor = color.content,
                        onClick = { onStringClicked(key, note) }
                    )
                }
            }
        }
        if (sidebarPosition == StringsSidebarPosition.End) {
            StringsSidebar(
                stringColor = highlightedStringColor.background,
                scrollMode = state.scrollMode,
                numHighlightedStrings = state.numHighlightedStrings,
                modifier = Modifier.width(sidebarWidth),
                onCenterClicked = {
                    state.scrollMode = StringsScrollMode.Automatic
                    scope.launch { state.scrollToClosestHighlightedString(coroutineContext) }
                },
                onNextClicked = {
                    scope.launch { state.scrollToNextHighlightedString(coroutineContext) }
                }
            )
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

@Preview(showBackground = true, widthDp = 200, heightDp = 320)
@Composable
private fun StringsPreview() {
    TunerTheme {
        val musicalScale = MusicalScaleFactory.create(TemperamentType.EDO12)
        val noteNameScale = musicalScale.noteNameScale
        val strings = arrayOf(
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
        )
        val notePrintOptions = NotePrintOptions()

        var state by remember {
            mutableStateOf(
                StringsState.create(strings, musicalScale, highlightedNote = strings[0])
                    .setTuningStateOfHighlightedStrings(TuningState.InTune)
            )
        }
        var stateChromatic by remember {
            mutableStateOf(
                StringsState.create(null, musicalScale, highlightedNote = strings[0])
                    .setTuningStateOfHighlightedStrings(TuningState.InTune)
            )
        }

        Column {
            Strings(
                state,
                notePrintOptions = notePrintOptions,
                fontSize = 20.sp,
                onStringClicked = { key, note ->
                    state = state.highlightString(note)
                },
                sidebarPosition = StringsSidebarPosition.Start,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(8.dp))
            Strings(
                stateChromatic,
                notePrintOptions = notePrintOptions,
                fontSize = 20.sp,
                onStringClicked = { key, note ->
                    stateChromatic = stateChromatic.highlightString(note)
                },
                sidebarPosition = StringsSidebarPosition.Start,
                modifier = Modifier.weight(1f)
            )

        }
    }
}