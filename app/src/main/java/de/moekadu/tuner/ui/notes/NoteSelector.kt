package de.moekadu.tuner.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import de.moekadu.tuner.temperaments.NoteNameScale
import de.moekadu.tuner.temperaments.createNoteNameScale53Tone
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.absoluteValue

/** Select between available notes.
 * @param selectedIndex List index of selected note in noteNameScale.notes
 * @param noteNameScale Scale of the available note names.
 * @param notePrintOptions How to print the notes.
 * @param modifier Modifier.
 * @param fontSize Font size of notes.
 * @param textStyle Text style of notes.
 * @param onIndexChanged Callback when another note index was selected. This refers to the
 *   note of noteNameScale.notes
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteSelector(
    selectedIndex: Int,
    noteNameScale: NoteNameScale,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    textStyle: TextStyle? = MaterialTheme.typography.labelLarge,
    onIndexChanged: (index: Int) -> Unit = {}
) {
    val fontSizeResolved = fontSize.takeOrElse {
        (textStyle?.fontSize ?: TextUnit.Unspecified).takeOrElse {
            LocalTextStyle.current.fontSize.takeOrElse { 12.sp }
        }
    }
    val fontWeightResolved = textStyle?.fontWeight

    val minSingleNoteSize = rememberMaxNoteSize(
        noteNameScale = noteNameScale,
        notePrintOptions = notePrintOptions,
        fontSize = fontSizeResolved,
        fontWeight = fontWeightResolved,
        octaveRange = null
    ) + DpSize(16.dp, 4.dp)

    val singleNoteSize = DpSize(
        if (minSingleNoteSize.width >= 48.dp) minSingleNoteSize.width else 48.dp,
        if (minSingleNoteSize.height >= 40.dp) minSingleNoteSize.height else 40.dp
    )

    val state = rememberLazyListState(selectedIndex)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection  {
            var preScrollIndex = Int.MAX_VALUE
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                preScrollIndex = state.layoutInfo
                    .visibleItemsInfo
                    .minByOrNull { it.offset.absoluteValue }?.index ?: 0
                return super.onPreScroll(available, source)
            }
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
//                Log.v("Tuner", "onPostScroll: o0 = ${state.layoutInfo.visibleItemsInfo[0].offset}")
                val postScrollIndex = state.layoutInfo
                    .visibleItemsInfo
                    .minByOrNull { it.offset.absoluteValue }?.index ?: 0
                if (preScrollIndex != postScrollIndex) {
//                    Log.v("Tuner", "onPostScroll: new index: $postScrollIndex")
                    onIndexChanged(postScrollIndex)
                }

                return super.onPostScroll(consumed, available, source)
            }

        }
    }

    LaunchedEffect(key1 = selectedIndex, key2 = state) {
        if (!state.isScrollInProgress)
            state.animateScrollToItem(selectedIndex)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(singleNoteSize),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.secondaryContainer
            ) { }

        LazyRow(
            modifier = Modifier.nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = (maxWidth - singleNoteSize.width)/2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
        ) {
            itemsIndexed(noteNameScale.notes) { index, note ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(singleNoteSize)
                        .clickable { onIndexChanged(index) }
                ) {
                    Note(
                        note,
                        notePrintOptions = notePrintOptions,
                        withOctave = false,
                        fontSize = fontSizeResolved,
                        style = textStyle,
                        color = if (selectedIndex == index)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200)
@Composable
private fun NoteSelectorPreview() {
    TunerTheme {
        val noteNameScale = remember { createNoteNameScale53Tone(null) }

        val notePrintOptions = remember {
            NotePrintOptions(
                sharpFlatPreference = NotePrintOptions.SharpFlatPreference.Sharp,
                helmholtzNotation = false,
                notationType = NotationType.Standard
            )
        }

        var selectedIndex by remember { mutableIntStateOf(7) }

        Column {
            NoteSelector(
                selectedIndex = selectedIndex,
                noteNameScale = noteNameScale,
                notePrintOptions = notePrintOptions
            ) { selectedIndex = it }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { /*TODO*/ }) {
                Text("Text button")
            }
        }
    }
}