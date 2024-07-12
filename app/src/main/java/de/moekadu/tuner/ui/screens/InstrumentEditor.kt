package de.moekadu.tuner.ui.screens

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.instruments.StringWithInfo
import de.moekadu.tuner.ui.instruments.Strings
import de.moekadu.tuner.ui.instruments.StringsScrollMode
import de.moekadu.tuner.ui.instruments.StringsSidebarPosition
import de.moekadu.tuner.ui.instruments.StringsState
import de.moekadu.tuner.ui.notes.NoteDetector
import de.moekadu.tuner.ui.notes.NoteDetectorState
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.min

interface InstrumentEditorData {
    val icon: StateFlow<Int>
    val name: StateFlow<String>

    val strings: StateFlow<ImmutableList<StringWithInfo> >
    val stringsState: StringsState

    /// Index in strings-list of the selected string
    val selectedStringIndex: StateFlow<Int>

    val noteDetectorState: NoteDetectorState

    /** Note which is used in NoteSelector, when no note is available */
    val initializerNote: StateFlow<MusicalNote>

    fun setIcon(@DrawableRes icon: Int)
    fun setName(name: String)

    fun selectString(key: Int)
    fun modifySelectedString(note: MusicalNote)

    fun addNote()
    fun deleteNote()
}

// TODO:
// 1. if there is no note, the note selector should still work, and then, when adding a note, it should take this note
// 2. changing icons

@Composable
fun InstrumentEditor(
    state: InstrumentEditorData,
    modifier: Modifier = Modifier,
    musicalScale: MusicalScale = MusicalScaleFactory.create(TemperamentType.EDO12),
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create(),
    onIconButtonClicked: () -> Unit = {}
) {
    val icon by state.icon.collectAsStateWithLifecycle()
    val name by state.name.collectAsStateWithLifecycle()
    val strings by state.strings.collectAsStateWithLifecycle()
    val selectedStringIndex by state.selectedStringIndex.collectAsStateWithLifecycle()
    val initializerNote by state.initializerNote.collectAsStateWithLifecycle()
    val selectedNoteWithInfo = strings.getOrNull(selectedStringIndex)
    val selectedNoteKey = selectedNoteWithInfo?.key ?: 0
    val selectedNote = selectedNoteWithInfo?.note ?: initializerNote
    val noteSelectorPosition = remember(musicalScale, selectedNote) {
        musicalScale.getNoteIndex(selectedNote) - musicalScale.noteIndexBegin
    }

//    Log.v("Tuner", "InstrumentEditor: strings: $strings")
    Column(modifier) {

        OutlinedTextField(
            value = name,
            onValueChange = { state.setName(it) },
            modifier = Modifier.fillMaxWidth().padding(tunerPlotStyle.margin),
            leadingIcon = {
                IconButton(onClick = onIconButtonClicked) {
                    Icon(
                        ImageVector.vectorResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            },
            trailingIcon = {
                IconButton(onClick = { state.setName("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "clear")
                }
            },
            placeholder = {
                Text(stringResource(id = R.string.instrument_name))
            },
            singleLine = true
        )

        Strings(
            strings = strings,
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            modifier = Modifier
                .padding(
                    start = tunerPlotStyle.margin,
                    end = tunerPlotStyle.margin
                )
                .fillMaxWidth()
                .weight(1f),

            tuningState = TuningState.InTune,
            highlightedNoteKey = selectedNoteKey,
            defaultColor = tunerPlotStyle.stringColor,
            onDefaultColor = tunerPlotStyle.onStringColor,
            inTuneColor = MaterialTheme.colorScheme.primary,
            onInTuneColor = MaterialTheme.colorScheme.onPrimary,
            fontSize = tunerPlotStyle.stringFontStyle.fontSize,
            sidebarPosition = StringsSidebarPosition.End,
            outline = if (state.stringsState.scrollMode == StringsScrollMode.Manual)
                tunerPlotStyle.plotWindowOutlineDuringGesture
            else
                tunerPlotStyle.plotWindowOutline,
            state = state.stringsState,
            onStringClicked = { key, _ ->
                state.selectString(key)
            }
        )
        Row(
            modifier = Modifier
                .padding(horizontal = tunerPlotStyle.margin)
                .padding(top = tunerPlotStyle.margin - 4.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = { state.addNote() },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.add_note))
            }

            Spacer(modifier = Modifier.width(tunerPlotStyle.margin))

            Button(
                onClick = { state.deleteNote() },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(id = R.string.delete_note))
            }
        }
        NoteSelector(
            selectedIndex = noteSelectorPosition,
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            modifier = Modifier.padding(top = tunerPlotStyle.margin - 4.dp),
            fontSize = tunerPlotStyle.noteSelectorStyle.fontSize,
            onIndexChanged = { state.modifySelectedString(musicalScale.getNote(it + musicalScale.noteIndexBegin)) }
        )
        HorizontalDivider(
            modifier = Modifier.padding(tunerPlotStyle.margin)
        )
        Text(
            stringResource(id = R.string.lately_detected_notes),
            modifier = Modifier.padding(horizontal = tunerPlotStyle.margin),
            style = MaterialTheme.typography.labelMedium
        )

        NoteDetector(
            state = state.noteDetectorState,
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            textStyle = tunerPlotStyle.noteSelectorStyle,
            textColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(tunerPlotStyle.margin),
            onNoteClicked = { state.modifySelectedString(it) }
        )
    }
}

private class InstrumentEditorDataTest : InstrumentEditorData {
    val musicalScale = MusicalScaleFactory.create(TemperamentType.EDO12)

    override val icon = MutableStateFlow(R.drawable.ic_guitar)
    override val name = MutableStateFlow("Test name")

    override var strings = MutableStateFlow(
        persistentListOf(
            StringWithInfo(musicalScale.getNote(0), 0)
        )
    )
    override val stringsState = StringsState(0)

    override var selectedStringIndex = MutableStateFlow(strings.value.size - 1)

    override val noteDetectorState = NoteDetectorState()

    override val initializerNote = MutableStateFlow(musicalScale.referenceNote)

    override fun setIcon(icon: Int) {
        this.icon.value = icon
    }

    override fun setName(name: String) {
        this.name.value = name
    }

    override fun selectString(key: Int) {
        val index = strings.value.indexOfFirst { it.key == key }
        selectedStringIndex.value = index
    }

    override fun modifySelectedString(note: MusicalNote) {
        val stringsValue = strings.value
        val index = selectedStringIndex.value
        if (index in stringsValue.indices) {
            strings.value = stringsValue.mutate {
                it[index] = stringsValue[index].copy(note = note)
            }
        } else {
            initializerNote.value = note
        }
    }

    override fun addNote() {
        val stringsValue = strings.value
        if (stringsValue.size == 0) {
            strings.value = persistentListOf(StringWithInfo(initializerNote.value, 1))
            selectedStringIndex.value = 0
        } else {
            val newKey = StringWithInfo.generateKey(existingList = stringsValue)
            val note = stringsValue.getOrNull(selectedStringIndex.value)?.note ?: initializerNote.value
            val insertionPosition = min(selectedStringIndex.value + 1, stringsValue.size)
            strings.value = stringsValue.mutate {
                it.add(insertionPosition, StringWithInfo(note, newKey))
            }
            selectedStringIndex.value = insertionPosition
        }
    }

    override fun deleteNote() {
        val stringsValue = strings.value
        val index = selectedStringIndex.value
        if (index in stringsValue.indices) {
            strings.value = stringsValue.mutate {
                it.removeAt(index)
            }
            selectedStringIndex.value = if (stringsValue.size <= 1)
                0
            else
                index.coerceIn(0, stringsValue.size - 2)

            if (stringsValue.size == 1)
                initializerNote.value = stringsValue[0].note
        }
    }
}

@Preview(widthDp = 300, heightDp = 600, showBackground = true)
@Composable
private fun InstrumentEditorPreview() {
    TunerTheme {
        val state = remember {
            InstrumentEditorDataTest()
        }
        LaunchedEffect(Unit) {
            while (true) {
                val note = state.musicalScale.getNote((0..8).random())
                state.noteDetectorState.hitNote(note)
                delay(1000)
            }
        }

        InstrumentEditor(
            state = state,
            musicalScale = state.musicalScale
        )
    }
}