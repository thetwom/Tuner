package de.moekadu.tuner.ui.temperaments

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.ShareData
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.misc.getFilenameFromUri
import de.moekadu.tuner.misc.toastPotentialFileCheckError
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.temperaments.Temperament
import de.moekadu.tuner.temperaments.TemperamentIO
import de.moekadu.tuner.temperaments.TemperamentWithNoteNames
import de.moekadu.tuner.ui.common.EditableList
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.common.OverflowMenu
import de.moekadu.tuner.ui.common.OverflowMenuCallbacks
import de.moekadu.tuner.ui.misc.TunerScaffoldWithoutBottomBar
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface TemperamentsManagerData {
    val listData: EditableListData<TemperamentWithNoteNames>
    fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<TemperamentWithNoteNames>
    )
}

@Composable
private fun rememberImportExportCallbacks(
    state: TemperamentsManagerData,
    onLoadTemperaments: (temperaments: List<EditableTemperament>) -> Unit
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

    val shareTemperamentsLauncher = rememberLauncherForActivityResult(
        contract = ShareData.Contract()
    ) {
        state.listData.clearSelectedItems()
    }

    val importTemperamentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
//            val cR = context.contentResolver
//            Log.v("Tuner", "Temperaments import temperament file, mimetype=${cR.getType(uri)}")

            val (readState, temperaments) = TemperamentIO.readTemperamentsFromFile(context, uri)
            readState.toastPotentialFileCheckError(context, uri)
            if (temperaments.isNotEmpty()) {
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
                    val intent = ShareData.createShareDataIntent(
                        context,
                        "tuner-temperaments.txt",
                        TemperamentIO.temperamentsListToString(context, temperaments),
                        temperaments.size
                    )
                    //val intent = ShareInstruments.createShareInstrumentsIntent(context, instruments)
                    shareTemperamentsLauncher.launch(intent)
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
                importTemperamentsLauncher.launch(arrayOf("text/plain", "application/octet-stream"))  // text/plain or */*
            }
            override fun onSettingsClicked() {
                // onPreferenceButtonClicked()
            }
        }
    }
}

@Composable
fun TemperamentsManager(
    state: TemperamentsManagerData,
    modifier: Modifier = Modifier,
    onEditTemperamentClicked: (temperament: TemperamentWithNoteNames, copy: Boolean) -> Unit = { _, _ ->},
    onTemperamentClicked: (temperament: TemperamentWithNoteNames) -> Unit = { },
    onLoadTemperaments: (temperaments: List<EditableTemperament>) -> Unit = { },
    onTemperamentInfoClicked: (temperament: TemperamentWithNoteNames) -> Unit = { },
    onNavigateUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedTemperaments by state.listData.selectedItems.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val overflowCallbacks = rememberImportExportCallbacks(
        state = state,
        onLoadTemperaments = onLoadTemperaments
    )

    TunerScaffoldWithoutBottomBar(
        modifier = modifier,
        title = stringResource(id = R.string.temperaments),
        defaultModeTools = {
            OverflowMenu(
                callbacks = overflowCallbacks,
                showSettings = false
            )
        },
        actionModeActive = selectedTemperaments.isNotEmpty(),
        actionModeTitle = "${selectedTemperaments.size}",
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
            OverflowMenu(overflowCallbacks, showSettings = false)
        },
        onActionModeFinishedClicked = {
            state.listData.clearSelectedItems()
        },
        onNavigateUpClicked = onNavigateUp,
        showPreferenceButton = false,
        floatingActionButton = {
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
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
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
            hasItemInfo = { true },
            state = state.listData,
            modifier = modifier.padding(paddingValues).fillMaxSize(),
            onActivateItemClicked = { onTemperamentClicked(it) },
            onEditItemClicked = onEditTemperamentClicked,
            onItemInfoClicked = onTemperamentInfoClicked,
            snackbarHostState = snackbarHostState,
            listState = listState
        )
    }
}

private class TestTemperamentManagerData : TemperamentsManagerData {
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

    val customTemperaments = MutableStateFlow(
        persistentListOf(
            TemperamentWithNoteNames(testTemperament1.copy(stableId = 1), null),
            TemperamentWithNoteNames(testTemperament2.copy(stableId = 2), null),
            TemperamentWithNoteNames(testTemperament3.copy(stableId = 3), null),
        )
    )
    val predefinedTemperamentsExpanded = MutableStateFlow(false)
    val customTemperamentsExpanded = MutableStateFlow(true)

    val activeTemperament = MutableStateFlow<TemperamentWithNoteNames?>(customTemperaments.value[1])

    override val listData = EditableListData(
        getStableId = { it.stableId },
        predefinedItems = persistentListOf(),
        editableItems = customTemperaments,
        predefinedItemsExpanded = predefinedTemperamentsExpanded,
        editableItemsExpanded = customTemperamentsExpanded,
        activeItem = activeTemperament,
        setNewItems = { customTemperaments.value = it },
        togglePredefinedItemsExpanded = { predefinedTemperamentsExpanded.value = it },
        toggleEditableItemsExpanded = { customTemperamentsExpanded.value = it }
    )

    override fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<TemperamentWithNoteNames>
    ) { }
}

@Preview(widthDp = 300, heightDp = 700, showBackground = true)
@Preview(widthDp = 700, heightDp = 300, showBackground = true)
@Composable
private fun Temperaments2Preview() {
    TunerTheme {
        val temperamentData = remember { TestTemperamentManagerData() }

        TemperamentsManager(
            state = temperamentData
        )
    }
}

