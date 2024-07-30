package de.moekadu.tuner.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.ShareInstruments
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.instruments.InstrumentItem2
import de.moekadu.tuner.ui.instruments.InstrumentItemTask
import de.moekadu.tuner.ui.instruments.InstrumentListSection
import de.moekadu.tuner.ui.misc.TunerScaffold
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration

interface InstrumentsData {
    val activeInstrument: StateFlow<Instrument?>

    val predefinedInstruments: ImmutableList<Instrument>
    val predefinedInstrumentsExpanded: StateFlow<Boolean>

    val customInstruments: StateFlow<ImmutableList<Instrument>>
    val customInstrumentsExpanded: StateFlow<Boolean>

    /** When custom instruments is changed, store the state before changing here. */
    val previousCustomInstruments: ImmutableList<Instrument>

    val selectedInstruments: StateFlow<ImmutableSet<Long>>

    /** Backup info when instruments are deleted. */
    data class InstrumentDeleteInfo(
        val backup: ImmutableList<Instrument>,
        val numDeleted: Int = 0
    )
    /** Instrument list before deletion of elements. */
    val customInstrumentsBackup: ReceiveChannel<InstrumentDeleteInfo>

    suspend fun expandPredefinedInstruments(isExpanded: Boolean)
    suspend fun expandCustomInstruments(isExpanded: Boolean)

    fun selectInstrument(id: Long)
    fun deselectInstrument(id: Long)
    fun toggleSelection(id: Long)
    fun clearSelectedInstruments()

    /** Return true, if we moving took place */
    suspend fun moveInstrumentsUp(instrumentKeys: Set<Long>): Boolean
    /** Return true, if we moving took place */
    suspend fun moveInstrumentsDown(instrumentKeys: Set<Long>): Boolean
    suspend fun deleteInstruments(instrumentKeys: Set<Long>)
    suspend fun deleteAllInstruments()
    suspend fun setInstruments(instruments: ImmutableList<Instrument>)
}

fun InstrumentsData.extractSelectedInstruments(): List<Instrument> {
    val allInstruments = this.customInstruments.value
    val selectedKeys = this.selectedInstruments.value
    return if (selectedKeys.isEmpty())
        allInstruments
    else
        customInstruments.value.filter { selectedKeys.contains(it.stableId) }
}

private interface OverflowMenuCallbacks{
    fun onDeleteClicked()
    fun onShareClicked()
    fun onExportClicked()
    fun onImportClicked()
    fun onSettingsClicked()
}

@Composable
private fun OverflowMenu(
    callbacks: OverflowMenuCallbacks
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = {
        expanded = !expanded
    }) {
        Icon(Icons.Default.MoreVert, contentDescription = "menu")
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.delete_instruments)) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "delete") },
            onClick = {
                callbacks.onDeleteClicked()
                expanded = false
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.share)) },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = "share") },
            onClick = {
                callbacks.onShareClicked()
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.save_to_disk)) },
            leadingIcon = {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_archive),
                    contentDescription = "archive"
                )
            },
            onClick = {
                callbacks.onExportClicked()
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.load_from_disk)) },
            leadingIcon = {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.ic_unarchive),
                    contentDescription = "unarchive"
                )
            },
            onClick = {
                callbacks.onImportClicked()
                expanded = false
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.settings)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "settings"
                )
            },
            onClick = {
                callbacks.onSettingsClicked()
                expanded = false
            }
        )
    }
}

@Composable
fun Instruments(
    state: InstrumentsData,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    musicalScale: MusicalScale = MusicalScaleFactory.create(TemperamentType.EDO12),
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
    val selectedInstruments by state.selectedInstruments.collectAsStateWithLifecycle()
    val customInstruments by state.customInstruments.collectAsStateWithLifecycle()
    val customInstrumentsExpanded by state.customInstrumentsExpanded.collectAsStateWithLifecycle()
    val predefinedInstrumentsExpanded by state.predefinedInstrumentsExpanded.collectAsStateWithLifecycle()
    val activeInstrument by state.activeInstrument.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = context.resources
    val listState = rememberLazyListState()

    val saveInstrumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val instruments = state.extractSelectedInstruments()
            scope.launch(Dispatchers.IO) {
                context.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
                    stream.write(InstrumentIO.instrumentsListToString(context, instruments).toByteArray())
                }
            }
            state.clearSelectedInstruments()
            val filename = InstrumentIO.getFilenameFromUri(context, uri)
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
        state.clearSelectedInstruments()
    }

    val importInstrumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val (readState, instruments) = InstrumentIO.readInstrumentsFromFile(context, uri)
            InstrumentIO.toastFileLoadingResult(context, readState, uri)
            if (instruments.isNotEmpty()) {
                onLoadInstruments(instruments)
                state.clearSelectedInstruments()
            }
        }
    }

    val overflowCallbacks = object: OverflowMenuCallbacks {
        override fun onDeleteClicked() {
            if (selectedInstruments.isNotEmpty())
                scope.launch { state.deleteInstruments(selectedInstruments) }
            else
                scope.launch { state.deleteAllInstruments() }
        }
        override fun onShareClicked() {
            val instruments = state.extractSelectedInstruments()
            if (instruments.isEmpty()) {
                Toast.makeText(context, R.string.database_empty_share, Toast.LENGTH_LONG).show()
            } else {
                val intent = ShareInstruments.createShareInstrumentsIntent(context, instruments)
                shareInstrumentLauncher.launch(intent)
            }
        }
        override fun onExportClicked() {
            if (customInstruments.isEmpty())
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

    // handle recovering deleted instruments
    LaunchedEffect(resources, state, listState) {
        for (delete in state.customInstrumentsBackup) {
            launch {
                val result = snackbarHostState.showSnackbar(
                    resources.getQuantityString(
                        R.plurals.instruments_deleted, delete.numDeleted, delete.numDeleted
                    ),
                    actionLabel = resources.getString(R.string.undo),
                    duration = SnackbarDuration.Long
                )
                when (result) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> {
                        state.setInstruments(delete.backup)
                        listState.animateScrollToItem(0) // otherwise it might look to the user as nothing happened
                    }
                }
            }
        }
    }

//    LaunchedEffect(key1 = customInstruments, key2 = state) {
//        scrollToSuitablePosition(listState, state.previousCustomInstruments, customInstruments)
//    }
    BackHandler(enabled = selectedInstruments.isNotEmpty()) {
        scope.launch { state.clearSelectedInstruments() }
    }

    TunerScaffold(
        modifier = modifier,
        defaultModeTools = { OverflowMenu(callbacks = overflowCallbacks) },
        actionModeActive = selectedInstruments.isNotEmpty(),
        actionModeTitle = "${selectedInstruments.size}",
        actionModeTools = {
            IconButton(onClick = {
                scope.launch {
                    val changed = state.moveInstrumentsUp(selectedInstruments)
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
                    val changed = state.moveInstrumentsDown(selectedInstruments)
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
//            IconButton(onClick = {
//                scope.launch { state.deleteInstruments(selectedInstruments) }
//            }) {
//                Icon(
//                    Icons.Default.Delete,
//                    contentDescription = "delete"
//                )
//            }
            OverflowMenu(overflowCallbacks)
        },
        onActionModeFinishedClicked = {
            state.clearSelectedInstruments()
        },
        onNavigateUpClicked = onNavigateUpClicked,
        onTemperamentClicked = onTemperamentClicked,
        onReferenceNoteClicked = onReferenceNoteClicked,
        onSharpFlatClicked = onSharpFlatClicked,
        musicalScale = musicalScale,
        notePrintOptions = notePrintOptions,
        showPreferenceButton = false,
        // onPreferenceButtonClicked = onPreferenceButtonClicked,
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
        LazyColumn(
            modifier = Modifier.padding(paddingValues = paddingValues),
            state = listState
        ) {
            if ( customInstruments.size > 0) {
                item(contentType = 1) {
                    InstrumentListSection(
                        title = stringResource(id = R.string.custom_instruments),
                        expanded = customInstrumentsExpanded
                    ) {
                        scope.launch { state.expandCustomInstruments(it) }
                    }
                }
            }
            if (customInstrumentsExpanded && customInstruments.size > 0) {
                items(customInstruments, { it.stableId }, { 2 }) { item ->
                    InstrumentItem2(
                        instrument = item,
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier
                            .animateItem()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { state.toggleSelection(item.stableId) },
                                    onTap = {
                                        if (selectedInstruments.size >= 1)
                                            state.toggleSelection(item.stableId)
                                        else
                                            onInstrumentClicked(item)
                                    }
                                )
                            },
                        isActive = (item.stableId == activeInstrument?.stableId),
                        isSelected = (selectedInstruments.contains(item.stableId)),
                        onOptionsClicked = { instrument, task ->
                            when (task) {
                                InstrumentItemTask.Copy -> onEditInstrumentClicked(instrument, true)
                                InstrumentItemTask.Edit -> onEditInstrumentClicked(
                                    instrument,
                                    false
                                )

                                InstrumentItemTask.Delete -> scope.launch {
                                    state.deleteInstruments(setOf(instrument.stableId))
                                }
                            }
                        }
                    )
                }
            }

            if (customInstruments.size > 0) {
                item(contentType = 1) {
                    InstrumentListSection(
                        title = stringResource(id = R.string.predefined_instruments),
                        expanded = predefinedInstrumentsExpanded
                    ) {
                        scope.launch { state.expandPredefinedInstruments(it) }
                    }
                }
            }

            if (predefinedInstrumentsExpanded || customInstruments.isEmpty()) {
                items(state.predefinedInstruments, { it.stableId }, { 3 }) { item ->
                    InstrumentItem2(
                        instrument = item,
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier
                            .animateItem()
                            .clickable {
                                onInstrumentClicked(item)
                            },
                        isActive = (item.stableId == activeInstrument?.stableId),
                        isSelected = false,
                        readOnly = true,
                        onOptionsClicked = { instrument, task ->
                            if (task == InstrumentItemTask.Copy)
                                onEditInstrumentClicked(instrument, true)
                        },
                        isCopyable = !item.isChromatic
                    )
                }
            }

            // extra space to allow scrolling up a bit more, such that the last item
            // doesn't collide with the fab
            item {
                Spacer(modifier.height(80.dp))
            }
        }

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
    iconResource = R.drawable.ic_guitar,
    1L,
    isChromatic = false
)

private val testInstrument2 = Instrument(
    name = null,
    nameResource = R.string.chromatic,
    strings = arrayOf(),
    iconResource = R.drawable.ic_piano,
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
    override val activeInstrument = MutableStateFlow<Instrument?>(null)

    override val predefinedInstruments = persistentListOf(
        testInstrument1.copy(stableId = -1),
        testInstrument2.copy(stableId = -2),
    )
    override val predefinedInstrumentsExpanded = MutableStateFlow(true)

    override val customInstruments = MutableStateFlow(
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
    override val customInstrumentsExpanded = MutableStateFlow(true)

    override val previousCustomInstruments: ImmutableList<Instrument> = customInstruments.value

    override val selectedInstruments = MutableStateFlow(persistentSetOf<Long>())

    override val customInstrumentsBackup = Channel<InstrumentsData.InstrumentDeleteInfo>(
        Channel.CONFLATED
    )

    override suspend fun expandPredefinedInstruments(isExpanded: Boolean) {
        predefinedInstrumentsExpanded.value = isExpanded
    }

    override suspend fun expandCustomInstruments(isExpanded: Boolean) {
        customInstrumentsExpanded.value = isExpanded
    }

    override fun selectInstrument(id: Long) {
        selectedInstruments.value = selectedInstruments.value.add(id)
    }
    override fun deselectInstrument(id: Long) {
        selectedInstruments.value = selectedInstruments.value.remove(id)
    }

    override fun toggleSelection(id: Long) {
        if (selectedInstruments.value.contains(id))
            selectedInstruments.value = selectedInstruments.value.remove(id)
        else
            selectedInstruments.value = selectedInstruments.value.add(id)
    }
    override fun clearSelectedInstruments() {
        selectedInstruments.value = persistentSetOf()
    }

    override suspend fun moveInstrumentsUp(instrumentKeys: Set<Long>): Boolean {
        if (customInstruments.value.isEmpty())
            return false
        var changed = false
        customInstruments.value = customInstruments.value.mutate { instruments ->
            for (i in 1 until customInstruments.value.size) {
                val instrument = customInstruments.value[i]
                val instrumentPrev = instruments[i-1]
                if (instrumentKeys.contains(instrument.stableId)
                    && !instrumentKeys.contains(instrumentPrev.stableId)) {
                    instruments.add(i - 1, instruments.removeAt(i))
                    changed =true
                }
            }
        }
        return changed
    }
    override suspend fun moveInstrumentsDown(instrumentKeys: Set<Long>): Boolean {
        if (customInstruments.value.isEmpty())
            return false
        var changed =  false
        customInstruments.value = customInstruments.value.mutate { instruments ->
            for (i in customInstruments.value.size - 2 downTo 0) {
                val instrument = customInstruments.value[i]
                val instrumentNext = instruments[i+1]
                if (instrumentKeys.contains(instrument.stableId)
                    && !instrumentKeys.contains(instrumentNext.stableId)) {
                    instruments.add(i + 1, instruments.removeAt(i))
                    changed = true
                }
            }
        }
        return changed
    }
    override suspend fun deleteInstruments(instrumentKeys: Set<Long>) {
        customInstruments.value = customInstruments.value.removeAll { instrumentKeys.contains(it.stableId) }
        selectedInstruments.value = selectedInstruments.value.removeAll(instrumentKeys)
    }

    override suspend fun deleteAllInstruments() {
        customInstruments.value = persistentListOf()
        selectedInstruments.value = selectedInstruments.value.clear()
    }

    override suspend fun setInstruments(instruments: ImmutableList<Instrument>) {
        customInstruments.value = instruments.toPersistentList()
    }
}


@Preview(widthDp = 300, heightDp = 700, showBackground = true)
@Composable
private fun InstrumentsPreview() {
    TunerTheme {
        val data = remember{ TestInstrumentsData() }

        Instruments(
            state = data
        )
    }
}