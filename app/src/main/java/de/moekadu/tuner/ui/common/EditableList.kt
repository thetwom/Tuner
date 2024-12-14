package de.moekadu.tuner.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class EditableListData<T>(
    val predefinedItems: ImmutableList<T>,
    val editableItems: StateFlow<PersistentList<T>>,
    val getStableId: (T) -> Long,
    val predefinedItemsExpanded: StateFlow<Boolean>,
    val editableItemsExpanded: StateFlow<Boolean>,
    val activeItem: StateFlow<T?>,
    val setNewItems: (PersistentList<T>) -> Unit,
    val togglePredefinedItemsExpanded: (Boolean) -> Unit,
    val toggleEditableItemsExpanded: (Boolean) -> Unit
) {
    private val _selectedItems = MutableStateFlow(persistentSetOf<Long>())
    val selectedItems get() = _selectedItems.asStateFlow()

    /** Backup info when items are deleted. */
    data class ItemsDeletedInfo<T>(
        val backup: PersistentList<T>,
        val numDeleted: Int = 0
    )
    val editableItemsBackup = Channel<ItemsDeletedInfo<T>>(
        Channel.CONFLATED
    )

    fun selectItem(id: Long) {
        _selectedItems.value = selectedItems.value.add(id)
    }

    fun deselectItem(id: Long) {
        _selectedItems.value = selectedItems.value.remove(id)
    }

    fun toggleSelection(id: Long) {
        if (selectedItems.value.contains(id))
            _selectedItems.value = selectedItems.value.remove(id)
        else
            _selectedItems.value = selectedItems.value.add(id)
    }

    fun clearSelectedItems() {
        _selectedItems.value = persistentSetOf()
    }

    fun moveSelectedItemsUp(): Boolean {
        val original = editableItems.value
        val selected = selectedItems.value
        if (original.size <= 1 || selected.isEmpty())
            return false
        var changed = false
        val modified = original.mutate { items ->
            for (i in 1 until original.size) {
                val item = original[i]
                val itemPrev = items[i-1]
                val itemKey = getStableId(item)
                val itemPrevKey = getStableId(itemPrev)
                if (selected.contains(itemKey) && !selected.contains(itemPrevKey)) {
                    items.add(i - 1, items.removeAt(i))
                    changed = true
                }
            }
        }
        setNewItems(modified)

        return changed
    }

    fun moveSelectedItemsDown(): Boolean {
        val original = editableItems.value
        val selected = selectedItems.value
        if (original.size <= 1 || selected.isEmpty())
            return false
        var changed = false
        val modified = original.mutate { items ->
            for (i in original.size - 2 downTo 0) {
                val item = original[i]
                val itemNext = items[i+1]
                val itemKey = getStableId(item)
                val itemNextKey = getStableId(itemNext)
                if (selected.contains(itemKey) && !selected.contains(itemNextKey)) {
                    items.add(i + 1, items.removeAt(i))
                    changed = true
                }
            }
        }
        setNewItems(modified)

        return changed
    }

    fun deleteItems(keys: ImmutableSet<Long>) {
        val backup = editableItems.value
        val modified = editableItems.value.removeAll { keys.contains(getStableId(it)) }
        setNewItems(modified)

        _selectedItems.value = selectedItems.value.clear()
        if (modified.isEmpty())
            togglePredefinedItemsExpanded(true)
        if (backup.size != modified.size) {
            editableItemsBackup.trySend(
                ItemsDeletedInfo(backup = backup, numDeleted = backup.size - modified.size)
            )
        }
    }
    fun deleteSelectedItems() {
        deleteItems(selectedItems.value)
    }

    fun deleteAllItems() {
        val backup = editableItems.value
        setNewItems(persistentListOf())
        _selectedItems.value = selectedItems.value.clear()
        togglePredefinedItemsExpanded(true)

        if (backup.size > 0) {
            editableItemsBackup.trySend(ItemsDeletedInfo(backup = backup, numDeleted = backup.size))
        }
    }

    fun extractSelectedItems(): List<T> {
        val allItems = this.editableItems.value
        val selectedKeys = this.selectedItems.value
        return if (selectedKeys.isEmpty())
            allItems
        else
            editableItems.value.filter { selectedKeys.contains(getStableId(it)) }
    }
}


@Composable
fun <T>EditableList(
    itemTitle: @Composable (T) -> Unit,
    itemDescription: @Composable (T) -> Unit,
    itemIcon: @Composable (T) -> Unit,
    isItemCopyable: (T) -> Boolean,
    hasItemInfo: (T) -> Boolean,
    state: EditableListData<T>,
    modifier: Modifier = Modifier,
    onActivateItemClicked: (T) -> Unit = { },
    onEditItemClicked: (T, copy: Boolean) -> Unit = {_, _ -> },
    onItemInfoClicked: (T) -> Unit = { },
    snackbarHostState: SnackbarHostState? = null,
    listState: LazyListState = rememberLazyListState()
) {
    val selectedItems by state.selectedItems.collectAsStateWithLifecycle()
    val editableItems by state.editableItems.collectAsStateWithLifecycle()
    val editableItemsExpanded by state.editableItemsExpanded.collectAsStateWithLifecycle()
    val predefinedItemsExpanded by state.predefinedItemsExpanded.collectAsStateWithLifecycle()
    val activeItem by state.activeItem.collectAsStateWithLifecycle()
    val activeItemId = activeItem?.let { state.getStableId(it) }
    val resources = LocalContext.current.resources
    val snackbarHostStateUpdated by rememberUpdatedState(newValue = snackbarHostState)

    LaunchedEffect(editableItems, listState) {
        listState.animateScrollToItem(0)
    }
    // handle recovering deleted items
    LaunchedEffect(resources, state, listState) {
        for (delete in state.editableItemsBackup) {
            launch {
                val result = snackbarHostStateUpdated?.showSnackbar(
                    resources.getQuantityString(
                        R.plurals.items_deleted, delete.numDeleted, delete.numDeleted
                    ),
                    actionLabel = resources.getString(R.string.undo),
                    duration = SnackbarDuration.Long
                )
                when (result) {
                    SnackbarResult.Dismissed -> {}
                    SnackbarResult.ActionPerformed -> {
                        state.setNewItems(delete.backup)
                        // delay a bit, since setNewItems runs internally on another coroutine, and
                        // we must let this finish. 100ms is of course not really save, but on the
                        // other hand, if the scroll to 0 fails, it is not really  a bit problem.
                        delay(100)
                        listState.animateScrollToItem(0) // otherwise it might look to the user as nothing happened
                    }
                    null -> {}
                }
            }
        }
    }

    BackHandler(enabled = selectedItems.isNotEmpty()) {
        state.clearSelectedItems()
    }

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        if ( editableItems.size > 0 && state.predefinedItems.size > 0) {
            item(contentType = 1) {
                EditableListSection(
                    title = stringResource(id = R.string.custom_item),
                    expanded = editableItemsExpanded
                ) {
                    state.toggleEditableItemsExpanded(it)
                }
            }
        }
        if (editableItemsExpanded && editableItems.size > 0) {
            items(editableItems, { state.getStableId(it) }, { 2 }) { listItem ->
                val interactionSource = remember { MutableInteractionSource() }
                EditableListItem(
                    title = { itemTitle(listItem) },
                    description = { itemDescription(listItem) },
                    icon = { itemIcon(listItem) },
                    Modifier
                        .animateItem()
                        .indication(
                            interactionSource = interactionSource,
                            indication = ripple()
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { state.toggleSelection(state.getStableId(listItem)) },
                                onTap = {
                                    if (selectedItems.size >= 1)
                                        state.toggleSelection(state.getStableId(listItem))
                                    else
                                        onActivateItemClicked(listItem)
                                },
                                onPress = {
                                    val press = PressInteraction.Press(it)
                                    interactionSource.tryEmit(press)
                                    tryAwaitRelease()
                                    interactionSource.tryEmit(PressInteraction.Release(press))
                                }
                            )
                        },
                    onOptionsClicked = { task ->
                        when (task) {
                            ListItemTask.Copy -> onEditItemClicked(listItem, true)
                            ListItemTask.Edit -> onEditItemClicked(listItem, false)
                            ListItemTask.Delete -> state.deleteItems(
                                persistentSetOf(state.getStableId(listItem)
                                )
                            )
                            ListItemTask.Info -> onItemInfoClicked(listItem)
                        }
                    },
                    isActive = (state.getStableId(listItem) == activeItemId),
                    isSelected = selectedItems.contains(state.getStableId(listItem)),
                    readOnly = false,
                    isCopyable = isItemCopyable(listItem),
                    hasInfo = hasItemInfo(listItem)
                )
            }
        }
        if (editableItems.size > 0 && state.predefinedItems.size > 0) {
            item(contentType = 1) {
                EditableListSection(
                    title = stringResource(id = R.string.predefined_items),
                    expanded = predefinedItemsExpanded
                ) {
                    state.togglePredefinedItemsExpanded(it)
                }
            }
        }

        if (predefinedItemsExpanded || editableItems.isEmpty()) {
            items(state.predefinedItems, { state.getStableId(it) }, { 3 }) { listItem ->
                EditableListItem(
                    title = { itemTitle(listItem) },
                    description = { itemDescription(listItem) },
                    icon = { itemIcon(listItem) },
                    Modifier
                        .animateItem()
                        .clickable {
                            onActivateItemClicked(listItem)
                        },
                    onOptionsClicked = { task ->
                        if (task == ListItemTask.Copy) {
                            onEditItemClicked(listItem, true)
                        } else if (task == ListItemTask.Info) {
                            onItemInfoClicked(listItem)
                        }
                    },
                    isActive = (state.getStableId(listItem) == activeItemId),
                    isSelected = false,
                    readOnly = true,
                    isCopyable = isItemCopyable(listItem),
                    hasInfo = hasItemInfo(listItem)
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

private data class TestItem(val title: String, val key: Long)

@Preview(widthDp = 300, heightDp = 700, showBackground = true)
@Composable
private fun EditableListTest() {
    val predefinedItems = remember {
        persistentListOf(
            TestItem("P", -1L)
        )
    }
    val editableItems = remember {
        MutableStateFlow(
            persistentListOf(
                TestItem("A", 1L),
                TestItem("B", 2L),
                TestItem("C", 3L),
                TestItem("D", 4L),
            TestItem("E", 5L)
            )
        )
    }
    val predefinedItemsExpanded = remember {
        MutableStateFlow(true)
    }
    val editableItemsExpanded = remember {
        MutableStateFlow(true)
    }
    val activeItem = remember {
        MutableStateFlow(editableItems.value[0])
    }

    val listData = EditableListData(
        getStableId = {it.key},
        predefinedItems = predefinedItems,
        editableItems = editableItems,
        predefinedItemsExpanded = predefinedItemsExpanded,
        editableItemsExpanded = editableItemsExpanded,
        activeItem = activeItem,
        setNewItems = { editableItems.value = it },
        togglePredefinedItemsExpanded = { predefinedItemsExpanded.value = it },
        toggleEditableItemsExpanded = { editableItemsExpanded.value = it }
    )
    TunerTheme {
        EditableList(
            itemTitle = {Text(it.title)},
            itemDescription = {Text(it.title)} ,
            itemIcon = {Icon(Icons.Default.Edit, null)},
            isItemCopyable = { true },
            hasItemInfo = { true },
            state = listData,
            onActivateItemClicked = { activeItem.value = it },
            onEditItemClicked = {_,_ -> }
        )
    }
}