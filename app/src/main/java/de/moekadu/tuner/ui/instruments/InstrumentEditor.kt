/*
* Copyright 2024 Michael Moessner
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
package de.moekadu.tuner.ui.instruments

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.temperaments.predefinedTemperamentEDO
import de.moekadu.tuner.ui.notes.NoteDetector
import de.moekadu.tuner.ui.notes.NoteDetectorState
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.screens.TunerPlotStyle
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.min

interface InstrumentEditorData {
    val icon: StateFlow<InstrumentIcon>
    val name: StateFlow<String>

    val strings: StateFlow<ImmutableList<StringWithInfo> >
    val stringsState: StringsState

    /// Index in strings-list of the selected string
    val selectedStringIndex: StateFlow<Int>

    val noteDetectorState: NoteDetectorState

    /** Note which is used in NoteSelector, when no note is available */
    val initializerNote: StateFlow<MusicalNote>

    fun setIcon(icon: InstrumentIcon)
    fun setName(name: String)

    fun selectString(key: Int)
    fun modifySelectedString(note: MusicalNote)

    fun addNote()
    fun deleteNote()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentEditor(
    state: InstrumentEditorData,
    musicalScale: MusicalScale2,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create(),
    onIconButtonClicked: () -> Unit = {},
    onNavigateUpClicked: () -> Unit = {},
    onSaveNewInstrumentClicked: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.edit_instrument)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateUpClicked() }) {
                        Icon(Icons.Default.Close, "close")
                    }
                },
                actions = {
                    TextButton(onClick = { onSaveNewInstrumentClicked() }) {
                        Text(stringResource(id = R.string.save))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                InstrumentEditorLandscape(
                    state = state,
                    musicalScale = musicalScale,
                    modifier = modifier.padding(paddingValues),
                    notePrintOptions = notePrintOptions,
                    tunerPlotStyle = tunerPlotStyle,
                    onIconButtonClicked = onIconButtonClicked
                )
            }
            else -> {
                InstrumentEditorPortrait(
                    state = state,
                    musicalScale = musicalScale,
                    modifier = modifier.padding(paddingValues),
                    notePrintOptions = notePrintOptions,
                    tunerPlotStyle = tunerPlotStyle,
                    onIconButtonClicked = onIconButtonClicked
                )
            }
        }
    }
}

@Composable
fun InstrumentEditorPortrait(
    state: InstrumentEditorData,
    musicalScale: MusicalScale2,
    modifier: Modifier = Modifier,
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
        val noteIndex = musicalScale.getNoteIndex(selectedNote)
        if (noteIndex == Int.MAX_VALUE)
            -musicalScale.noteIndexBegin
        else
            noteIndex - musicalScale.noteIndexBegin
    }

//    Log.v("Tuner", "InstrumentEditor: strings: $strings")
    Column(modifier) {
        OutlinedTextField(
            value = name,
            onValueChange = { state.setName(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(tunerPlotStyle.margin),
            leadingIcon = {
                IconButton(
                    onClick = onIconButtonClicked,
                    modifier = Modifier.width(60.dp)
                ) {
                    Icon(
                        ImageVector.vectorResource(id = icon.resourceId),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)// .background(Color.Green)
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
                .padding(
                    bottom = tunerPlotStyle.margin,
                    start = tunerPlotStyle.margin,
                    end = tunerPlotStyle.margin,
                ),
            onNoteClicked = { state.modifySelectedString(it) }
        )
    }
}

@Composable
fun InstrumentEditorLandscape(
    state: InstrumentEditorData,
    musicalScale: MusicalScale2,
    modifier: Modifier = Modifier,
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
        val noteIndex = musicalScale.getNoteIndex(selectedNote)
        if (noteIndex == Int.MAX_VALUE)
            -musicalScale.noteIndexBegin
        else
            noteIndex - musicalScale.noteIndexBegin
    }

    Row(modifier) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { state.setName(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = tunerPlotStyle.margin,
                        top = tunerPlotStyle.margin
                    ),
                leadingIcon = {
                    IconButton(
                        onClick = onIconButtonClicked,
                        modifier = Modifier.width(60.dp)
                    ) {
                        Icon(
                            ImageVector.vectorResource(id = icon.resourceId),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)// .background(Color.Green)
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

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .padding(
                        top = tunerPlotStyle.margin - 4.dp,
                        start = tunerPlotStyle.margin
                    )
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

            Spacer(Modifier.weight(1f))

            NoteSelector(
                selectedIndex = noteSelectorPosition,
                musicalScale = musicalScale,
                notePrintOptions = notePrintOptions,
                modifier = Modifier.padding(
                    top = tunerPlotStyle.margin - 4.dp,
                ),
                fontSize = tunerPlotStyle.noteSelectorStyle.fontSize,
                onIndexChanged = { state.modifySelectedString(musicalScale.getNote(it + musicalScale.noteIndexBegin)) }
            )

            Spacer(Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(
                    top = tunerPlotStyle.margin,
                    bottom = tunerPlotStyle.margin,
                    start = tunerPlotStyle.margin
                )
            )

            Text(
                stringResource(id = R.string.lately_detected_notes),
                modifier = Modifier.padding(
                    start = tunerPlotStyle.margin
                ),
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
                    .padding(
                        start = tunerPlotStyle.margin,
                        bottom = tunerPlotStyle.margin
                    ),
                onNoteClicked = { state.modifySelectedString(it) }
            )
        }

        Spacer(Modifier.width(tunerPlotStyle.margin))

        Strings(
            strings = strings,
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            modifier = Modifier
                .padding(
                    top = tunerPlotStyle.margin,
                    bottom = tunerPlotStyle.margin,
                    end = tunerPlotStyle.margin
                )
                .fillMaxHeight()
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

    }
}

private class InstrumentEditorDataTest : InstrumentEditorData {
    val musicalScale = MusicalScale2.createTestEdo12()

    override val icon = MutableStateFlow(InstrumentIcon.piano)
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

    override fun setIcon(icon: InstrumentIcon) {
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

        InstrumentEditorPortrait(
            state = state,
            musicalScale = state.musicalScale
        )
    }
}

@Preview(widthDp = 600, heightDp = 300, showBackground = true)
@Composable
private fun InstrumentEditorLandscapePreview() {
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

        InstrumentEditorLandscape(
            state = state,
            musicalScale = state.musicalScale
        )
    }
}