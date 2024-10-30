package de.moekadu.tuner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.StringOrResId
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
    val name: State<StringOrResId>
    val abbreviation: State<StringOrResId>
    val description: State<StringOrResId>
    val numberOfValues: State<Int>
    val temperamentValues: State<PersistentList<TemperamentTableLineState>>
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperamentEditor(
    state: TemperamentEditorState,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onAbortClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    onNumberOfNotesClicked: () -> Unit = {},
    onDescriptionClicked: () -> Unit = {},
    onCentValueChanged: (index: Int, value: String) -> Unit = {_,_ -> },
    onNoteNameClicked: (index: Int) -> Unit = { }
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(
            title = { Text(stringResource(id = R.string.temperament_editor)) },
            navigationIcon = {
                IconButton(onClick = { onAbortClicked() }) {
                    Icon(Icons.Default.Close, "close")
                }
            },
            actions = {
                TextButton(onClick = {
                    onSaveClicked()
                }) {
                    Text(stringResource(id = R.string.save))
                }
            }
        ) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Card(
                onClick = onDescriptionClicked,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ){
                Column {
                    Text(
                        stringResource(id = R.string.description),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "${state.name.value.value(context)} (${state.abbreviation.value.value(context)})",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        state.description.value.value(context),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                onClick = onNumberOfNotesClicked,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
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

            LazyColumn(modifier = Modifier.padding(end=16.dp)) {
                itemsIndexed(state.temperamentValues.value) { index, line ->
                    TemperamentTableLine(
                        lineNumber = index,
                        state = line,
                        notePrintOptions = notePrintOptions,
                        onValueChange = { onCentValueChanged(index, it) },
                        onNoteNameClicked = { onNoteNameClicked(index) }
                    )
                }
            }

        }
    }
}

private class TemperamentEditorStateTest : TemperamentEditorState {
    override val name = mutableStateOf(StringOrResId("Test"))
    override val abbreviation = mutableStateOf(StringOrResId("T"))
    override val description = mutableStateOf(StringOrResId("Description of temperament"))
    override val numberOfValues = mutableIntStateOf(12)

    val noteNames = getSuitableNoteNames(numberOfValues.intValue)
    override val temperamentValues = mutableStateOf(
        (0..numberOfValues.intValue).map { index ->
            TemperamentTableLineState(
                noteNames?.getOrNull(index)?.copy(octave = 4),
                cent = index * 100.0,
                ratio = null,
                isReferenceNote = index == 5,
                isOctaveLine = (index + 1) == numberOfValues.intValue
            )
        }.toPersistentList()
    )
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
