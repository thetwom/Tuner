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
package de.moekadu.tuner.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.instruments.ShareInstruments
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.misc.getFilenameFromUri
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments2.MusicalScale2
import de.moekadu.tuner.temperaments2.MusicalScale2Factory
import de.moekadu.tuner.temperaments2.StretchTuning
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.ui.common.EditableList
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.common.OverflowMenu
import de.moekadu.tuner.ui.common.OverflowMenuCallbacks
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface InstrumentsData {
    val listData: EditableListData<Instrument>
    fun saveInstruments(context: Context, uri: Uri, instruments: List<Instrument>)
}

@Composable
fun Instruments(
    state: InstrumentsData,
    musicalScale: MusicalScale2,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onInstrumentClicked: (instrument: Instrument) -> Unit = { },
    onEditInstrumentClicked: (instrument: Instrument, copy: Boolean) -> Unit = {_,_ -> },
    onCreateNewInstrumentClicked: () -> Unit = {},
    onNavigateUpClicked: () -> Unit = {},
    onReferenceNoteClicked: () -> Unit = {},
    onTemperamentClicked: () -> Unit = {},
    onSharpFlatClicked: () -> Unit = {},
    onLoadInstruments: (instruments: List<Instrument>) -> Unit = {},
    onPreferenceButtonClicked: () -> Unit = {}
) {
    val selectedInstruments by state.listData.selectedItems.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val saveInstrumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val instruments = state.listData.extractSelectedItems()
            state.saveInstruments(context, uri, instruments)
            state.listData.clearSelectedItems()
            val filename = getFilenameFromUri(context, uri)
            Toast.makeText(
                context,
                context.resources.getQuantityString(
                    R.plurals.database_num_saved,
                    instruments.size,
                    instruments.size,
                    filename
                ),
                Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context,
                R.string.failed_to_archive_instruments, Toast.LENGTH_LONG).show()
        }
    }
    val shareInstrumentLauncher = rememberLauncherForActivityResult(
        contract = ShareInstruments.Contract()
    ) {
        state.listData.clearSelectedItems()
    }

    val importInstrumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val (readState, instruments) = InstrumentIO.readInstrumentsFromFile(context, uri)
            InstrumentIO.toastFileLoadingResult(context, readState, uri)
            if (instruments.isNotEmpty()) {
                onLoadInstruments(instruments)
                state.listData.clearSelectedItems()
            }
        }
    }

    val overflowCallbacks = object: OverflowMenuCallbacks {
        override fun onDeleteClicked() {
            if (state.listData.selectedItems.value.isNotEmpty())
                state.listData.deleteSelectedItems()
            else
                state.listData.deleteAllItems()
        }
        override fun onShareClicked() {
            val instruments = state.listData.extractSelectedItems()
            if (instruments.isEmpty()) {
                Toast.makeText(context, R.string.database_empty_share, Toast.LENGTH_LONG).show()
            } else {
                val intent = ShareInstruments.createShareInstrumentsIntent(context, instruments)
                shareInstrumentLauncher.launch(intent)
            }
        }
        override fun onExportClicked() {
            if (state.listData.editableItems.value.isEmpty())
                Toast.makeText(context, R.string.database_empty, Toast.LENGTH_LONG).show()
            else
                saveInstrumentLauncher.launch("tuner.txt")
        }
        override fun onImportClicked() {
            importInstrumentLauncher.launch(arrayOf("text/plain"))
        }
        override fun onSettingsClicked() {
            onPreferenceButtonClicked()
        }
    }

    TunerScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.instruments),
        defaultModeTools = { OverflowMenu(callbacks = overflowCallbacks) },
        actionModeActive = selectedInstruments.isNotEmpty(),
        actionModeTitle = "${selectedInstruments.size}",
        actionModeTools = {
            IconButton(onClick = {
                scope.launch {
                    val changed = state.listData.moveSelectedItemsUp()
                    if (changed) {
                        listState.animateScrollToItem(
                            (listState.firstVisibleItemIndex - 1).coerceAtLeast(0),
                            -listState.firstVisibleItemScrollOffset
                        )
                    }
                }
            }) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "move up"
                )
            }
            IconButton(onClick = {
                scope.launch {
                    val changed = state.listData.moveSelectedItemsDown()
                    if (changed) {
                        listState.animateScrollToItem(
                            listState.firstVisibleItemIndex + 1,
                            -listState.firstVisibleItemScrollOffset
                        )
                    }
                }
            }) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "move down"
                )
            }
            OverflowMenu(overflowCallbacks)
        },
        onActionModeFinishedClicked = {
            state.listData.clearSelectedItems()
        },
        onNavigateUpClicked = onNavigateUpClicked,
        onTemperamentClicked = onTemperamentClicked,
        onReferenceNoteClicked = onReferenceNoteClicked,
        onSharpFlatClicked = onSharpFlatClicked,
        musicalScale = musicalScale,
        notePrintOptions = notePrintOptions,
        showPreferenceButton = false,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewInstrumentClicked
            ) {
                Icon(Icons.Default.Add, contentDescription = "create new instrument")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        EditableList(
            itemTitle = { Text(it.getNameString(context)) },
            itemDescription = { instrument ->
                val style = LocalTextStyle.current
                val stringsString = remember(context, style, instrument, notePrintOptions) {
                    instrument.getStringsString(
                        context = context,
                        notePrintOptions = notePrintOptions,
                        fontSize = style.fontSize,
                        fontWeight = style.fontWeight
                    )
                }
                Text(stringsString)
            },
            itemIcon = { instrument ->
                Icon(
                    ImageVector.vectorResource(id = instrument.icon.resourceId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            },
            isItemCopyable = { !it.isChromatic },
            state = state.listData,
            modifier = Modifier.padding(paddingValues = paddingValues),
            onActivateItemClicked = onInstrumentClicked,
            onEditItemClicked = onEditInstrumentClicked,
            snackbarHostState = snackbarHostState,
            listState = listState
        )

    }
}

private val testInstrument1 = Instrument(
    name = null,
    nameResource = R.string.guitar_eadgbe,
    strings = arrayOf(
        MusicalNote(BaseNote.A, NoteModifier.None, octave = 4),
        MusicalNote(BaseNote.G, NoteModifier.None, octave = 4),
        MusicalNote(BaseNote.D, NoteModifier.Sharp, octave = 3),
        MusicalNote(BaseNote.E, NoteModifier.None, octave = 2),
    ),
    icon = InstrumentIcon.guitar,
    1L,
    isChromatic = false
)

private val testInstrument2 = Instrument(
    name = null,
    nameResource = R.string.chromatic,
    strings = arrayOf(),
    icon = InstrumentIcon.piano,
    stableId = 2L,
    isChromatic = true
)

private val testInstrument3 = testInstrument2.copy(stableId = 3L)
private val testInstrument4 = testInstrument2.copy(stableId = 4L)
private val testInstrument5 = testInstrument2.copy(stableId = 5L)
private val testInstrument6 = testInstrument2.copy(stableId = 6L)
private val testInstrument7 = testInstrument2.copy(stableId = 7L)
private val testInstrument8 = testInstrument2.copy(stableId = 8L)
private val testInstrument9 = testInstrument2.copy(stableId = 9L)

private class TestInstrumentsData : InstrumentsData {

    val activeInstrument = MutableStateFlow<Instrument?>(null)

    val predefinedInstruments = persistentListOf(
        testInstrument1.copy(stableId = -1),
        testInstrument2.copy(stableId = -2),
    )
    val predefinedInstrumentsExpanded = MutableStateFlow(true)

    val customInstruments = MutableStateFlow(
        persistentListOf(
            testInstrument1,
            testInstrument2,
            testInstrument3,
            testInstrument4,
            testInstrument5,
            testInstrument6,
            testInstrument7,
            testInstrument8,
            testInstrument9
        )
    )
    val customInstrumentsExpanded = MutableStateFlow(true)

    override val listData = EditableListData(
        getStableId = { it.stableId },
        predefinedItems = predefinedInstruments,
        editableItems = customInstruments,
        predefinedItemsExpanded = predefinedInstrumentsExpanded,
        editableItemsExpanded = customInstrumentsExpanded,
        activeItem = activeInstrument,
        setNewItems = { customInstruments.value = it },
        togglePredefinedItemsExpanded = { predefinedInstrumentsExpanded.value = it },
        toggleEditableItemsExpanded = { customInstrumentsExpanded.value = it }
    )

    override fun saveInstruments(context: Context, uri: Uri, instruments: List<Instrument>) {}
}


@Preview(widthDp = 300, heightDp = 700, showBackground = true)
@Composable
private fun InstrumentsPreview() {
    TunerTheme {
        val data = remember{ TestInstrumentsData() }
        val musicalScale = remember { MusicalScale2Factory.createTestEdo12() }
        Instruments(
            state = data,
            musicalScale = musicalScale
        )
    }
}