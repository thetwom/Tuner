package de.moekadu.tuner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments2.NoteNames
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.TemperamentResources
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.temperaments.TemperamentTableLine
import de.moekadu.tuner.ui.temperaments.TemperamentTableLineState
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

interface TemperamentEditorState {
    val name: State<String>
    val abbreviation: State<String>
    val description: State<String>
    val numberOfValues: State<Int>
    val temperamentValues: State<PersistentList<TemperamentTableLineState>>
    val hasErrors: State<Boolean>

    fun modifyName(value: String)
    fun modifyAbbreviation(value: String)
    fun modifyDescription(value: String)
    fun onCentValueChanged(index: Int, value: String)
    fun onNoteNameClicked(index: Int, enharmonic: Boolean)
    fun onCloseNoteEditorClicked(index: Int)
    fun modifyNote(index: Int, note: MusicalNote)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperamentEditor(
    state: TemperamentEditorState,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onAbortClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    onNumberOfNotesClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.temperament_editor)) },
                navigationIcon = {
                    IconButton(onClick = { onAbortClicked() }) {
                        Icon(Icons.Default.Close, "close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSaveClicked() },
                        enabled = !state.hasErrors.value
                    ) {
                        Text(stringResource(id = R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    TextField(
                        value = state.name.value,
                        onValueChange = { state.modifyName(it) },
                        label = { Text(stringResource(id = R.string.temperament_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        trailingIcon = {
                            IconButton(onClick = { state.modifyName("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "clear text")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
                item {
                    TextField(
                        value = state.abbreviation.value,
                        onValueChange = { state.modifyAbbreviation(it) },
                        label = { Text(stringResource(id = R.string.temperament_abbreviation)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        trailingIcon = {
                            IconButton(onClick = { state.modifyAbbreviation("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "clear text")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
                item {
                    TextField(
                        value = state.description.value,
                        onValueChange = { state.modifyDescription(it) },
                        label = { Text(stringResource(id = R.string.temperament_description)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                        trailingIcon = {
                            IconButton(onClick = { state.modifyDescription("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "clear text")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
                item {
                    Card(
                        onClick = onNumberOfNotesClicked,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                stringResource(id = R.string.note_number),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "${state.numberOfValues.value}",
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    .fillMaxWidth(),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                itemsIndexed(state.temperamentValues.value) { index, line ->
                    val keyboardController = LocalSoftwareKeyboardController.current
                    TemperamentTableLine(
                        lineNumber = if (line.isOctaveLine) 0 else index,
                        state = line,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        notePrintOptions = notePrintOptions,
                        onValueChange = { state.onCentValueChanged(index, it) },
                        onNoteNameClicked = { enharmonic ->
                            state.onNoteNameClicked(index, enharmonic)
                            keyboardController?.hide()
                        },
                        onChangeNote = { it?.let { n -> state.modifyNote(index, n) } },
                        onNoteEditorCloseClicked = { state.onCloseNoteEditorClicked(index) }
                    )
                }
            }
        }
    }
}

private class TemperamentEditorStateTest : TemperamentEditorState {
    override val name = mutableStateOf("Test")
    override val abbreviation = mutableStateOf("T")
    override val description = mutableStateOf("Description of temperament")
    override val numberOfValues = mutableIntStateOf(12)

    val noteNames = getSuitableNoteNames(numberOfValues.intValue)

    override fun modifyName(value: String) {
        name.value = value
    }

    override fun modifyAbbreviation(value: String) {
        abbreviation.value = value
    }

    override fun modifyDescription(value: String) {
        description.value = value
    }
    
    override val temperamentValues = mutableStateOf(
        (0..numberOfValues.intValue).map { index ->
            TemperamentTableLineState(
                noteNames?.getOrNull(index)?.copy(octave = 4),
                cent = index * 100.0,
                ratio = null,
                isFirstLine = index == 0,
                isOctaveLine = (index + 1) == numberOfValues.intValue,
                decreasingValueError = false,
                duplicateNoteError = false
            )
        }.toPersistentList()
    )
    override val hasErrors = mutableStateOf(false)

    override fun onCentValueChanged(index: Int, value: String) {
        temperamentValues.value.getOrNull(index)?.changeCentOrRatio(value)
    }

    override fun onNoteNameClicked(index: Int, enharmonic: Boolean) {
        temperamentValues.value.forEachIndexed { i, value ->
            value.setNoteEditor(
                if (i != index
                    || (!enharmonic && value.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard)
                    || (enharmonic && value.noteEditorState == TemperamentTableLineState.NoteEditorState.Enharmonic)
                ) {
                    TemperamentTableLineState.NoteEditorState.Off
                } else if (enharmonic) {
                    TemperamentTableLineState.NoteEditorState.Enharmonic
                } else {
                    TemperamentTableLineState.NoteEditorState.Standard
                }
            )
        }
    }

    override fun onCloseNoteEditorClicked(index: Int) {
        temperamentValues.value.getOrNull(index)?.setNoteEditor(
            TemperamentTableLineState.NoteEditorState.Off
        )
    }

    override fun modifyNote(index: Int, note: MusicalNote) {
        temperamentValues.value.getOrNull(index)?.let {
            it.note = note
        }
    }
}


@Preview(widthDp = 300, heightDp = 600, showBackground = true)
@Preview(widthDp = 600, heightDp = 300, showBackground = true)
@Composable
private fun TemperamentEditorPreview() {
    TunerTheme {
        val state = remember { TemperamentEditorStateTest() }
        TemperamentEditor(
            state = state
        )
    }
}
