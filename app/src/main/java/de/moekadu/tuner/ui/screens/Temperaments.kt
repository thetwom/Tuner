package de.moekadu.tuner.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.misc.getFilenameFromUri
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.TemperamentIO
import de.moekadu.tuner.temperaments2.TemperamentWithNoteNames
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.ui.common.EditableList
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.common.ImportExportOverflowMenu
import de.moekadu.tuner.ui.common.OverflowMenu
import de.moekadu.tuner.ui.common.OverflowMenuCallbacks
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.temperaments.ActiveTemperament
import de.moekadu.tuner.ui.temperaments.ActiveTemperamentDetailChoice
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface TemperamentsData {
    val listData: EditableListData<TemperamentWithNoteNames>
    val selectedRootNoteIndex: StateFlow<Int>
    val detailChoice: StateFlow<ActiveTemperamentDetailChoice>

    fun changeDetailChoice(choice: ActiveTemperamentDetailChoice)
    fun changeRootNoteIndex(index: Int)
    fun changeActiveTemperament(temperament: TemperamentWithNoteNames)
    fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<TemperamentWithNoteNames>
    )
    fun resetToDefault()
}

@Composable
private fun rememberImportExportCallbacks(
    state: TemperamentsData,
    onLoadTemperaments: (temperaments: List<TemperamentIO.ParsedTemperament>) -> Unit
): OverflowMenuCallbacks {
    val context = LocalContext.current
    val stateUpdated by rememberUpdatedState(newValue = state)
    val onLoadTemperamentsUpdated by rememberUpdatedState(newValue = onLoadTemperaments)

    val saveTemperamentsLauncher =  rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val temperaments = stateUpdated.listData.extractSelectedItems()
            stateUpdated.saveTemperaments(context, uri, temperaments)
            stateUpdated.listData.clearSelectedItems()
            val filename = getFilenameFromUri(context, uri)
            Toast.makeText(
                context,
                context.resources.getQuantityString(
                    R.plurals.database_num_saved,
                    temperaments.size,
                    temperaments.size,
                    filename
                ),
                Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context,
                R.string.failed_to_archive_items, Toast.LENGTH_LONG).show()
        }
    }

    val importTemperamentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val temperaments = TemperamentIO.readTemperamentsFromFile(context, uri)
            if (temperaments.isEmpty()) {
                val filename = getFilenameFromUri(context, uri)
                Toast.makeText(
                    context,
                    context.getString(R.string.file_contains_no_temperaments, filename),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                onLoadTemperamentsUpdated(temperaments)
                state.listData.clearSelectedItems()
            }
        }
    }
    return remember(context) {
        object: OverflowMenuCallbacks {
            override fun onDeleteClicked() {
                if (stateUpdated.listData.selectedItems.value.isNotEmpty())
                    stateUpdated.listData.deleteSelectedItems()
                else
                    stateUpdated.listData.deleteAllItems()
            }
            override fun onShareClicked() {
                val temperaments = stateUpdated.listData.extractSelectedItems()
                if (temperaments.isEmpty()) {
                    Toast.makeText(context, R.string.database_empty_share, Toast.LENGTH_LONG).show()
                } else {
                    //val intent = ShareInstruments.createShareInstrumentsIntent(context, instruments)
                    //shareInstrumentLauncher.launch(intent)
                }
            }
            override fun onExportClicked() {
                if (stateUpdated.listData.editableItems.value.isEmpty()) {
                    Toast.makeText(context, R.string.database_empty, Toast.LENGTH_LONG).show()
                } else {
                    saveTemperamentsLauncher.launch("temperaments.txt")
                }
            }
            override fun onImportClicked() {
                importTemperamentsLauncher.launch(arrayOf("text/plain"))
            }
            override fun onSettingsClicked() {
                // onPreferenceButtonClicked()
            }
        }
    }
}


@Composable
fun Temperaments(
    state: TemperamentsData,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onNewTemperament:  (temperament: TemperamentWithNoteNames, rootNote: MusicalNote) -> Unit = { _, _ ->},
    onAbort: () -> Unit = { },
    onEditTemperamentClicked: (temperament: TemperamentWithNoteNames, copy: Boolean) -> Unit = { _, _ ->},
    onLoadTemperaments: (temperaments: List<TemperamentIO.ParsedTemperament>) -> Unit = { }
) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            TemperamentsLandscape(
                state = state,
                modifier = modifier,
                notePrintOptions = notePrintOptions,
                onNewTemperament = onNewTemperament,
                onAbort = onAbort,
                onEditTemperamentClicked = onEditTemperamentClicked,
                onLoadTemperaments = onLoadTemperaments
            )
        }

        else -> {
            TemperamentsPortrait(
                state = state,
                modifier = modifier,
                notePrintOptions = notePrintOptions,
                onNewTemperament = onNewTemperament,
                onAbort = onAbort,
                onEditTemperamentClicked = onEditTemperamentClicked,
                onLoadTemperaments = onLoadTemperaments
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperamentsPortrait(
    state: TemperamentsData,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onNewTemperament:  (temperament: TemperamentWithNoteNames, rootNote: MusicalNote) -> Unit = { _, _ ->},
    onAbort: () -> Unit = { },
    onEditTemperamentClicked: (temperament: TemperamentWithNoteNames, copy: Boolean) -> Unit = { _, _ ->},
    onLoadTemperaments: (temperaments: List<TemperamentIO.ParsedTemperament>) -> Unit = { }
) {
    val context = LocalContext.current
    val selectedTemperaments by state.listData.selectedItems.collectAsStateWithLifecycle()
    val activeTemperament by state.listData.activeItem.collectAsStateWithLifecycle()
    val noteNames = remember(activeTemperament) {
        val t = activeTemperament
        if (t != null) {
            t.noteNames ?: getSuitableNoteNames(t.temperament.numberOfNotesPerOctave)!!
        } else {
            getSuitableNoteNames(12)!!
        }
    }
    val selectedRootNoteIndex by state.selectedRootNoteIndex.collectAsStateWithLifecycle()

    val detailChoice by state.detailChoice.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val temperamentListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val overflowCallbacks = rememberImportExportCallbacks(
        state = state,
        onLoadTemperaments = onLoadTemperaments
    )

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(
                title = { Text(stringResource(id = R.string.temperaments)) },
                navigationIcon = {
                    IconButton(onClick = { onAbort() }) {
                        Icon(Icons.Default.Close, "close")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val rootNote = noteNames[selectedRootNoteIndex]
                        state.listData.activeItem.value?.let { onNewTemperament(it, rootNote) }
                    }) {
                        Text(stringResource(id = R.string.save))
                    }
                }
            ) },
        bottomBar = {
            BottomAppBar(
                actions = {
                    if (selectedTemperaments.isEmpty()) {
                        IconButton(onClick = { overflowCallbacks.onDeleteClicked() }) {
                            Icon(Icons.Default.Delete, contentDescription = "delete")
                        }
                        IconButton(onClick = { overflowCallbacks.onShareClicked() }) {
                            Icon(Icons.Default.Share, contentDescription = "share")
                        }
                        // TODO: strings must not use the term "instrument"
                        ImportExportOverflowMenu(
                            onExportClicked = { overflowCallbacks.onExportClicked() },
                            onImportClicked = { overflowCallbacks.onImportClicked() }
                        )

//                        IconButton(onClick = { overflowCallbacks.onExportClicked() }) {
//                            Icon(ImageVector.vectorResource(id = R.drawable.ic_archive), contentDescription = "export")
//                        }
//                        IconButton(onClick = { overflowCallbacks.onImportClicked() }) {
//                            Icon(ImageVector.vectorResource(id = R.drawable.ic_unarchive), contentDescription = "import")
//                        }

                    } else {
                        IconButton(onClick = { state.listData.clearSelectedItems() }) {
                            Icon(Icons.Default.Clear, contentDescription = "clear selection")
                        }
                        Text(
                            "${selectedTemperaments.size}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            scope.launch {
                                val changed = state.listData.moveSelectedItemsUp()
                                if (changed) {
                                    temperamentListState.animateScrollToItem(
                                        (temperamentListState.firstVisibleItemIndex - 1).coerceAtLeast(0),
                                        -temperamentListState.firstVisibleItemScrollOffset
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
                                    temperamentListState.animateScrollToItem(
                                        temperamentListState.firstVisibleItemIndex + 1,
                                        -temperamentListState.firstVisibleItemScrollOffset
                                    )
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "move down"
                            )
                        }
                        // TODO: strings should not say "instruments"
                        OverflowMenu(callbacks = overflowCallbacks, showSettings = false)
                    }
                },
                floatingActionButton = {
                    if (selectedTemperaments.isEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                onEditTemperamentClicked(
                                    TemperamentWithNoteNames(
                                        Temperament.create(
                                            StringOrResId(""),
                                            StringOrResId(""),
                                            StringOrResId(""),
                                            12,
                                            Temperament.NO_STABLE_ID
                                        ),
                                        null
                                    ),
                                    true
                                )
                            },
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "new temperament")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            activeTemperament?.let {
                ActiveTemperament(
                    temperament = it.temperament,
                    noteNames = noteNames,
                    rootNoteIndex = selectedRootNoteIndex,
                    detailChoice = detailChoice,
                    onChooseDetail = { state.changeDetailChoice(it) },
                    notePrintOptions = notePrintOptions,
                    onResetClicked = { state.resetToDefault() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(id = R.string.root_note),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            NoteSelector(
                selectedIndex = selectedRootNoteIndex,
                notes = noteNames.notes,
                notePrintOptions = notePrintOptions,
                onIndexChanged = { state.changeRootNoteIndex(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            Text(
                stringResource(id = R.string.temperaments),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
            val iconTextSize = with(LocalDensity.current) { 18.dp.toSp() }
            EditableList(
                itemTitle = { Text(it.temperament.name.value(context)) },
                itemDescription = { Text(it.temperament.description.value(context)) },
                itemIcon = {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, LocalContentColor.current)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${it.temperament.name.value(context).getOrNull(0) ?: ""}",
                                fontSize = iconTextSize
                            )
                        }
                    }
                },
                isItemCopyable = { true },
                state = state.listData,
                modifier = Modifier.weight(1f),
                onActivateItemClicked = { state.changeActiveTemperament(it) },
                onEditItemClicked = onEditTemperamentClicked,
                snackbarHostState = snackbarHostState,
                listState = temperamentListState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperamentsLandscape(
    state: TemperamentsData,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onNewTemperament:  (temperament: TemperamentWithNoteNames, rootNote: MusicalNote) -> Unit = { _, _ -> },
    onAbort: () -> Unit = { },
    onEditTemperamentClicked: (temperament: TemperamentWithNoteNames, copy: Boolean) -> Unit = { _, _ ->},
    onLoadTemperaments: (temperaments: List<TemperamentIO.ParsedTemperament>) -> Unit = { }
) {
    val context = LocalContext.current
    val selectedTemperaments by state.listData.selectedItems.collectAsStateWithLifecycle()
    val activeTemperament by state.listData.activeItem.collectAsStateWithLifecycle()
    val noteNames = remember(activeTemperament) {
        val t = activeTemperament
        if (t != null) {
            t.noteNames ?: getSuitableNoteNames(t.temperament.numberOfNotesPerOctave)!!
        } else {
            getSuitableNoteNames(12)!!
        }
    }
    val selectedRootNoteIndex by state.selectedRootNoteIndex.collectAsStateWithLifecycle()

    val detailChoice by state.detailChoice.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val temperamentListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val overflowCallbacks = rememberImportExportCallbacks(
        state = state, onLoadTemperaments = onLoadTemperaments
    )

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(
            title = {
                if (selectedTemperaments.isEmpty()) {
                    Text(stringResource(id = R.string.temperaments))
                } else {
                    Text("${selectedTemperaments.size}")
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (selectedTemperaments.isEmpty()) {
                        onAbort()
                    } else {
                        state.listData.clearSelectedItems()
                    }
                }) {
                    Icon(Icons.Default.Close, "close")
                }
            },
            actions = {
                if (selectedTemperaments.isEmpty()) {
                    IconButton(onClick = { overflowCallbacks.onDeleteClicked() }) {
                        Icon(Icons.Default.Delete, contentDescription = "delete")
                    }
                    IconButton(onClick = { overflowCallbacks.onShareClicked() }) {
                        Icon(Icons.Default.Share, contentDescription = "share")
                    }
                    // TODO: strings must not use the term "instrument"
                    ImportExportOverflowMenu(
                        onExportClicked = { overflowCallbacks.onExportClicked() },
                        onImportClicked = { overflowCallbacks.onImportClicked() }
                    )

                } else {
                    IconButton(onClick = {
                        scope.launch {
                            val changed = state.listData.moveSelectedItemsUp()
                            if (changed) {
                                temperamentListState.animateScrollToItem(
                                    (temperamentListState.firstVisibleItemIndex - 1).coerceAtLeast(0),
                                    -temperamentListState.firstVisibleItemScrollOffset
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
                                temperamentListState.animateScrollToItem(
                                    temperamentListState.firstVisibleItemIndex + 1,
                                    -temperamentListState.firstVisibleItemScrollOffset
                                )
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "move down"
                        )
                    }
                    // TODO: strings should not say "instruments"
                    OverflowMenu(callbacks = overflowCallbacks, showSettings = false)
                }
                TextButton(onClick = {
                    state.listData.activeItem.value?.let {
                        val rootNote = noteNames[selectedRootNoteIndex]
                        onNewTemperament(it, rootNote)
                    }
                }) {
                    Text(stringResource(id = R.string.save))
                }
            }
        ) },
        floatingActionButton = {
            if (selectedTemperaments.isEmpty()) {
                FloatingActionButton(
                    onClick = {
                        onEditTemperamentClicked(
                            TemperamentWithNoteNames(
                                Temperament.create(
                                    StringOrResId(""),
                                    StringOrResId(""),
                                    StringOrResId(""),
                                    12,
                                    Temperament.NO_STABLE_ID
                                ),
                                null
                            ),
                            true
                        )
                    },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "new temperament")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Row {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .weight(0.5f)
            ) {
                activeTemperament?.let {
                    ActiveTemperament(
                        temperament = it.temperament,
                        noteNames = noteNames,
                        rootNoteIndex = selectedRootNoteIndex,
                        detailChoice = detailChoice,
                        onChooseDetail = { state.changeDetailChoice(it) },
                        notePrintOptions = notePrintOptions,
                        onResetClicked = { state.resetToDefault() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(id = R.string.root_note),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                NoteSelector(
                    selectedIndex = selectedRootNoteIndex,
                    notes = noteNames.notes,
                    notePrintOptions = notePrintOptions,
                    onIndexChanged = { state.changeRootNoteIndex(it) }
                )
            }

            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .weight(0.5f)
            ) {
//                Text(
//                    stringResource(id = R.string.temperaments),
//                    style = MaterialTheme.typography.labelSmall,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(top = 4.dp),
//                    textAlign = TextAlign.Center
//                )
                val iconTextSize = with(LocalDensity.current) { 18.dp.toSp() }
                EditableList(
                    itemTitle = { Text(it.temperament.name.value(context)) },
                    itemDescription = { Text(it.temperament.description.value(context)) },
                    itemIcon = {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, LocalContentColor.current)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${it.temperament.name.value(context)[0]}",
                                    fontSize = iconTextSize
                                )
                            }
                        }
                    },
                    isItemCopyable = { true },
                    state = state.listData,
                    modifier = Modifier.weight(1f),
                    onActivateItemClicked = { state.changeActiveTemperament(it) },
                    onEditItemClicked = onEditTemperamentClicked,
                    snackbarHostState = snackbarHostState,
                    listState = temperamentListState
                )
            }
        }
    }
}

private class TestTemperamentData : TemperamentsData {
    private val testTemperament1 = Temperament.create(
        StringOrResId("Test 1"),
        StringOrResId("T1"),
        StringOrResId("Describing Test 1"),
        12,
        1L
    )

    private val testTemperament2 = Temperament.create(
        StringOrResId("Test 2"),
        StringOrResId("T2"),
        StringOrResId("Describing Test 2"),
        doubleArrayOf(0.0, 12.0, 140.0, 320.0, 410.0, 540.0, 610.0, 720.0, 810.0, 910.0, 1020.0, 1100.0, 1200.0),
        2L
    )

    private val testTemperament3 = Temperament.create(
        StringOrResId("Test 3"),
        StringOrResId("T3"),
        StringOrResId("Describing Test 3, ratios"),
        (0..12).map { RationalNumber(12+it, 12) }.toTypedArray(),
        3L
    )

    val activeTemperament = MutableStateFlow<TemperamentWithNoteNames?>(
        TemperamentWithNoteNames(testTemperament1.copy(stableId = -1), null)
    )

    val predefinedTemperaments = persistentListOf(
        TemperamentWithNoteNames(testTemperament1.copy(stableId = -1), null),
        TemperamentWithNoteNames(testTemperament2.copy(stableId = -2), null)
    )
    val predefinedTemperamentsExpanded = MutableStateFlow(true)

    val customTemperaments = MutableStateFlow(
        persistentListOf(
            TemperamentWithNoteNames(testTemperament1.copy(stableId = 1), null),
            TemperamentWithNoteNames(testTemperament2.copy(stableId = 2), null),
            TemperamentWithNoteNames(testTemperament3.copy(stableId = 3), null),
        )
    )
    val customTemperamentsExpanded = MutableStateFlow(true)

    override val listData = EditableListData(
        getStableId = { it.stableId },
        predefinedItems = predefinedTemperaments,
        editableItems = customTemperaments,
        predefinedItemsExpanded = predefinedTemperamentsExpanded,
        editableItemsExpanded = customTemperamentsExpanded,
        activeItem = activeTemperament,
        setNewItems = { customTemperaments.value = it },
        togglePredefinedItemsExpanded = { predefinedTemperamentsExpanded.value = it },
        toggleEditableItemsExpanded = { customTemperamentsExpanded.value = it }
    )

    override val selectedRootNoteIndex = MutableStateFlow(0)

    override val detailChoice = MutableStateFlow(ActiveTemperamentDetailChoice.Off)

    override fun changeRootNoteIndex(index: Int) {
        selectedRootNoteIndex.value = index
    }

    override fun changeActiveTemperament(temperament: TemperamentWithNoteNames) {
        activeTemperament.value = temperament
    }

    override fun changeDetailChoice(choice: ActiveTemperamentDetailChoice) {
        detailChoice.value = choice
    }

    override fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<TemperamentWithNoteNames>
    ) { }

    override fun resetToDefault() {
        selectedRootNoteIndex.value = 0
        activeTemperament.value = TemperamentWithNoteNames(
            testTemperament1, null
        )
    }
}

@Preview(widthDp = 300, heightDp = 700, showBackground = true)
@Preview(widthDp = 700, heightDp = 300, showBackground = true)
@Composable
private fun TemperamentsPreview() {
    TunerTheme {
        val temperamentData = remember { TestTemperamentData() }

        Temperaments(
            state = temperamentData
        )
    }
}
