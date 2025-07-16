package de.moekadu.tuner.ui.temperaments

import android.content.Context
import android.net.Uri
import android.widget.Space
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.ShareData
import de.moekadu.tuner.misc.getFilenameFromUri
import de.moekadu.tuner.misc.toastPotentialFileCheckError
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.TemperamentIO
import de.moekadu.tuner.ui.common.EditableList
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.common.EditableListItem
import de.moekadu.tuner.ui.common.ListItemTask
import de.moekadu.tuner.ui.common.OverflowMenu
import de.moekadu.tuner.ui.common.OverflowMenuCallbacks
import de.moekadu.tuner.ui.misc.TunerScaffoldWithoutBottomBar
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface TemperamentsDialog2Data {
    val listData: EditableListData<Temperament3>
    val defaultTemperament: Temperament3
    fun proposeRootNote(temperament: Temperament3): MusicalNote

    fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<Temperament3Custom>
    )
}

@Composable
private fun rememberImportExportCallbacks(
    state: TemperamentsDialog2Data,
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
            stateUpdated.saveTemperaments(
                context, uri, temperaments.filterIsInstance<Temperament3Custom>()
            )
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
fun TemperamentsDialog2(
    state: TemperamentsDialog2Data,
    modifier: Modifier = Modifier,
    onEditTemperamentClicked: (temperament: Temperament3, copy: Boolean) -> Unit = { _, _ ->},
    onTemperamentClicked: (temperament: Temperament3, rootNote: MusicalNote) -> Unit = { _, _ -> },
    onLoadTemperaments: (temperaments: List<EditableTemperament>) -> Unit = { },
    onTemperamentInfoClicked: (temperament: Temperament3) -> Unit = { },
    onNavigateUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedTemperaments by state.listData.selectedItems.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val maxExpectedHeightForFab = 72.dp

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
            Row {
                ExtendedFloatingActionButton(
                    onClick = {
                        onTemperamentClicked(state.defaultTemperament, state.defaultTemperament.possibleRootNotes()[0])
                    },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    icon = {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_reset),
                            contentDescription = "reset"
                        )
                    },
                    text = { Text(stringResource(R.string.use_default)) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FloatingActionButton(
                    onClick = {
                        onEditTemperamentClicked(
                            Temperament3Custom(
                                _name = "", _abbreviation = "", _description = "",
                                cents = DoubleArray(13) { it * 100.0 },
                                _rationalNumbers = arrayOf(),
                                _noteNames = null,
                                stableId = Temperament3.NO_STABLE_ID
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
        val iconTextSize = with(LocalDensity.current) { 18.dp.toSp() }
        val layoutDirection = LocalLayoutDirection.current
        EditableList(
            state = state.listData,
            modifier = modifier.consumeWindowInsets(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + maxExpectedHeightForFab
            ),
            onActivateItemClicked = { onTemperamentClicked(it, state.proposeRootNote(it)) },
            snackbarHostState = snackbarHostState
        ) { item, itemInfo, itemModifier ->
            EditableListItem(
                title = { Text(item.name.value(context)) },
                description = { Text(item.description.value(context)) },
                icon = {
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
                                text = "${item.name.value(context).getOrNull(0) ?: ""}",
                                fontSize = iconTextSize
                            )
                        }
                    }
                },
                modifier = itemModifier,
                onOptionsClicked = {
                    when (it) {
                        ListItemTask.Edit -> onEditTemperamentClicked(item, false)
                        ListItemTask.Copy -> onEditTemperamentClicked(item, true)
                        ListItemTask.Delete -> {
                            state.listData.deleteItems(persistentSetOf(item.stableId))
                        }
                        ListItemTask.Info -> onTemperamentInfoClicked(item)
                    }
                },
                isActive = itemInfo.isActive,
                isSelected = itemInfo.isSelected,
                readOnly = itemInfo.readOnly,
                isCopyable = true,
                hasInfo = true
            )
        }
    }
}

private class TestTemperamentDialog2Data : TemperamentsDialog2Data {
    private val testTemperament1 = Temperament3Custom(
        _name = "Test1", _abbreviation = "T1", _description = "Describing Test 1",
        cents = DoubleArray(13) { it * 100.0 }, _rationalNumbers = arrayOf(),
        _noteNames = null,
        stableId = 1L
    )

    private val testTemperament2 = Temperament3Custom(
        _name = "Test 2", _abbreviation = "T2", _description = "Describing Test 2",
        cents = doubleArrayOf(
            0.0, 12.0, 140.0, 320.0, 410.0, 540.0, 610.0, 720.0, 810.0, 910.0, 1020.0, 1100.0, 1200.0
        ),
        _rationalNumbers = arrayOf(), _noteNames = null,
        2L
    )

    private val testTemperament3 = Temperament3Custom(
        _name = "Test 3", _abbreviation = "T3", _description = "Describing Test 3, ratios",
        cents = doubleArrayOf(),
        _rationalNumbers = (0..12).map {
            RationalNumber(12+it, 12)
        }.toTypedArray(),
        _noteNames = null,
        3L
    )

    val customTemperaments = MutableStateFlow(
        persistentListOf(testTemperament1, testTemperament2, testTemperament3)
    )
    val predefinedTemperamentsExpanded = MutableStateFlow(false)
    val customTemperamentsExpanded = MutableStateFlow(true)

    val activeTemperament = MutableStateFlow<Temperament3?>(customTemperaments.value[1])

    override val defaultTemperament = testTemperament1

    override val listData = EditableListData(
        predefinedItemSections = persistentListOf(),
        editableItemsSectionResId = R.string.custom_temperaments,
        getStableId = { it.stableId },
        editableItems = customTemperaments,
        editableItemsExpanded = customTemperamentsExpanded,
        activeItem = activeTemperament,
        setNewItems = {
            customTemperaments.value = it.filterIsInstance<Temperament3Custom>().toPersistentList()
        },
        toggleEditableItemsExpanded = { customTemperamentsExpanded.value = it }
    )

    override fun proposeRootNote(temperament: Temperament3): MusicalNote {
       return temperament.possibleRootNotes()[0]
    }
    override fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<Temperament3Custom>
    ) { }
}

@Preview(widthDp = 300, heightDp = 700, showBackground = true)
@Preview(widthDp = 700, heightDp = 300, showBackground = true)
@Composable
private fun TemperamentsDialog2Preview() {
    TunerTheme {
        val temperamentData = remember { TestTemperamentDialog2Data() }

        TemperamentsDialog2(
            state = temperamentData
        )
    }
}

