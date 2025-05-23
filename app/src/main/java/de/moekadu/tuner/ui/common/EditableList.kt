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
package de.moekadu.tuner.ui.common

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

interface EditableListPredefinedSection<T> {
    val sectionStringResourceId: Int
    val size: Int
    val isExpanded: StateFlow<Boolean>

    operator fun get(index: Int): T
    val toggleExpanded: (isExpanded: Boolean) -> Unit
}

class EditableListPredefinedSectionImmutable<T>(
    @StringRes override val sectionStringResourceId: Int,
    private val items: ImmutableList<T>,
    override val isExpanded: StateFlow<Boolean>,
    override val toggleExpanded: (isExpanded: Boolean) -> Unit
) : EditableListPredefinedSection<T> {
    override val size get() = items.size
    override operator fun get(index: Int) = items[index]
}

@Stable
class EditableListData<T>(
    val predefinedItemSections: ImmutableList<EditableListPredefinedSection<T>>,
    @StringRes val editableItemsSectionResId: Int,
    val editableItems: StateFlow<PersistentList<T>>,
    val editableItemsExpanded: StateFlow<Boolean>,
    val toggleEditableItemsExpanded: (Boolean) -> Unit,
    val getStableId: (T) -> Long,
    val activeItem: StateFlow<T?>,
    val setNewItems: (PersistentList<T>) -> Unit
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

    /** Index in the lazy list where the first editable list item is placed.
     * When predefined items are on, we will show header lines, in this case the
     * editable list starts at index 1 (since the header line is at index 0) .
     */
    val editableListStartIndex = if (predefinedItemSections.sumOf { it.size } == 0) 0 else 1

    val lazyListState = LazyListState()
    //val initiateScrollChannel = Channel<Int>(Channel.CONFLATED)
    val initiateScrollChannel = Channel<Pair<Int, Int>>(Channel.CONFLATED)

    val numNonEmptyPredefinedLists = predefinedItemSections.count { it.size > 0 }

    /** Here we collect the info for all predefined sections if it is expanded. */
    val predefinedSectionsExpanded = combine(predefinedItemSections.map { it.isExpanded }) { it }

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
        var minChangedIndex = Int.MAX_VALUE
        var maxChangedIndex = -1
        val modified = original.mutate { items ->
            for (i in 1 until original.size) {
                val item = original[i]
                val itemPrev = items[i-1]
                val itemKey = getStableId(item)
                val itemPrevKey = getStableId(itemPrev)
                if (selected.contains(itemKey) && !selected.contains(itemPrevKey)) {
                    items.add(i - 1, items.removeAt(i))
                    changed = true
                    minChangedIndex = min(minChangedIndex, i - 1)
                    maxChangedIndex = max(maxChangedIndex, i - 1)
                }
            }
        }
        setNewItems(modified)
        if (changed) {
            // this makes sure that before the scrolling we keep the scroll state at the same index
            // if this is not called, it will keep the scroll state at the same key of the top item
            lazyListState.requestScrollToItem(
                lazyListState.firstVisibleItemIndex,
                lazyListState.firstVisibleItemScrollOffset
            )

            scrollTo(
                minChangedIndex + editableListStartIndex,
                maxChangedIndex + editableListStartIndex)
        }

        return changed
    }

    fun moveSelectedItemsDown(): Boolean {
        val original = editableItems.value
        val selected = selectedItems.value
        if (original.size <= 1 || selected.isEmpty())
            return false
        var changed = false
        var minChangedIndex = Int.MAX_VALUE
        var maxChangedIndex = -1

        val modified = original.mutate { items ->
            for (i in original.size - 2 downTo 0) {
                val item = original[i]
                val itemNext = items[i+1]
                val itemKey = getStableId(item)
                val itemNextKey = getStableId(itemNext)
                if (selected.contains(itemKey) && !selected.contains(itemNextKey)) {
                    items.add(i + 1, items.removeAt(i))
                    minChangedIndex = min(minChangedIndex, i + 1)
                    maxChangedIndex = max(maxChangedIndex, i + 1)
                    changed = true
                }
            }
        }
        setNewItems(modified)
        if (changed) {
            // this makes sure that before the scrolling we keep the scroll state at the same index
            // if this is not called, it will keep the scroll state at the same key of the top item
            lazyListState.requestScrollToItem(
                lazyListState.firstVisibleItemIndex,
                lazyListState.firstVisibleItemScrollOffset
            )
            scrollTo(
                minChangedIndex + editableListStartIndex,
                maxChangedIndex + editableListStartIndex
            )
        }

        return changed
    }

    fun deleteItems(keys: ImmutableSet<Long>) {
        val backup = editableItems.value
        val modified = editableItems.value.removeAll { keys.contains(getStableId(it)) }
        setNewItems(modified)

        _selectedItems.value = selectedItems.value.clear()
        if (modified.isEmpty() && predefinedItemSections.size == 1)
            predefinedItemSections[0].toggleExpanded(true)

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
        if (predefinedItemSections.size == 1)
            predefinedItemSections[0].toggleExpanded(true)

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

    fun scrollTo(index: Int) {
        initiateScrollChannel.trySend(Pair(index, index))

    }
    fun scrollTo(index1: Int, index2: Int) {
        initiateScrollChannel.trySend(Pair(index1, index2))
    }
}

data class EditableListItemInfo(
    val isActive: Boolean,
    val isSelected: Boolean,
    val readOnly: Boolean,
    val listIndex: Int
)

private suspend fun LazyListState.niceAnimatedScroll(index1: Int, index2: Int, overScroll: Int) {
    val indexMin = min(index1, index2)
    val indexMax = max(index1, index2)

    val offsetBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
//    Log.v("Metronome", "EditableListData.niceAnimatedScroll:  offsetBottom=$offsetBottom")
//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: index elem0 = ${layoutInfo.visibleItemsInfo.getOrNull(0)?.index}, offset elem0 = ${layoutInfo.visibleItemsInfo.getOrNull(0)?.offset}")
//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: index elem1 = ${layoutInfo.visibleItemsInfo.getOrNull(1)?.index}, offset elem1 = ${layoutInfo.visibleItemsInfo.getOrNull(1)?.offset}")
//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: index elem2 = ${layoutInfo.visibleItemsInfo.getOrNull(2)?.index}, offset elem2 = ${layoutInfo.visibleItemsInfo.getOrNull(2)?.offset}")

    val visibleItemMin = layoutInfo.visibleItemsInfo.firstOrNull { it.offset + it.size > 0 } ?: return
    val visibleItemMax = layoutInfo.visibleItemsInfo.lastOrNull { it.offset < offsetBottom } ?: return

//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: visibleMin=${visibleItemMin.index}, visibleMax=${visibleItemMax.index}, index1=$index1, index2=$index2 ")

    // min/max items currently not visible: they are before visible items
    // -> scroll first item to start (least amount of scroll if index1 == index2)
    if (indexMax < visibleItemMin.index) {
//        Log.v("Metronome", "EditableListData.niceAnimatedScroll: Elements above")
        animateScrollToItem(indexMin, -overScroll)
        return
    }

    val maxItemSize = layoutInfo.visibleItemsInfo.maxOf { it.size }

    // min/max items currently not visible: they are behind visible items
    // -> scroll last item to end (least amount of scroll if index1 == index2)
    if (indexMin > visibleItemMax.index) {
//        Log.v("Metronome", "EditableListData.niceAnimatedScroll: Elements below")
        animateScrollToItem(indexMax, -(offsetBottom - maxItemSize - overScroll))
        return
    }

    // min item is visible, max item is not
    // -> scroll min item to start (so max item might appear)
    //    and it's stable if you call function repeatedly
    if (indexMin >= visibleItemMin.index && indexMax > visibleItemMax.index) {
//        Log.v("Metronome", "EditableListData.niceAnimatedScroll: Bottom element below")
        animateScrollToItem(indexMin, -overScroll)
        return
    }

    // max item is visible, min item is not
    // -> scroll max item to end (so min item might appear)
    //    and it's stable if you call function repeatedly
    if (indexMax <= visibleItemMax.index && indexMin < visibleItemMin.index) {
//        Log.v("Metronome", "EditableListData.niceAnimatedScroll: Top element above")
        animateScrollToItem(indexMax, -(offsetBottom - maxItemSize - overScroll))
        return
    }

    // so now we handled all cases, where at least one item is out of viewport initially
    // now we can assume that both items are visible
    val index0 = layoutInfo.visibleItemsInfo.getOrNull(0)?.index ?: return
    val itemMin = layoutInfo.visibleItemsInfo.getOrNull(indexMin - index0) ?: return
    val itemMax = layoutInfo.visibleItemsInfo.getOrNull(indexMax - index0) ?: return

    val offsetMin = itemMin.offset
    val offsetMax = itemMax.offset + itemMax.size
//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: offsetMin=$offsetMin, offsetMax=$offsetMax")
//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: index check: indexMin=$indexMin, itemMin.index=${itemMin.index}")

    // min element is fully visible, max element only partly
    // - try to scroll to show max element fully, but do not partly hide min element
    if (offsetMin >= 0 && offsetMax > offsetBottom) {
//        Log.v("Metronome", "EditableListData.niceAnimatedScroll: min fully visible, max partly")
        val maxAllowedScroll = offsetMin
        val scrollAmount = (offsetMax - offsetBottom + overScroll).coerceAtMost(maxAllowedScroll)
        animateScrollToItem(indexMax, -(itemMax.offset - scrollAmount))
        return
    }

//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: offsetMin=$offsetMin, offsetMax=$offsetMax, offsetBottom=$offsetBottom")
    // max element is fully visible, min element only partly
    // - try to scroll to sow min element fully, but  do not partly hide max element
    if (offsetMin < 0 && offsetMax <= offsetBottom) {
//        Log.v("Metronome", "EditableListData.niceAnimatedScroll: max fully visible, min partly")
        val maxAllowedScroll = offsetBottom - offsetMax
        val scrollAmount = (-offsetMin + overScroll).coerceAtMost(maxAllowedScroll)
        animateScrollToItem(indexMin, -(itemMin.offset + scrollAmount))
        return
    }

//    Log.v("Metronome", "EditableListData.niceAnimatedScroll: both elements fully or partly visible")
    // remaining cases, in which we do nothing:
    // - both elements fully visible
    // - both elements partly visible
}

private const val CONTENT_TYPE_SECTION = 1
private const val CONTENT_TYPE_EMPTY_MESSAGE = 2
private const val CONTENT_TYPE_ITEM = 3


@Composable
fun <T>EditableList(
    state: EditableListData<T>,
    modifier: Modifier = Modifier,
    onActivateItemClicked: (T) -> Unit = { },
    snackbarHostState: SnackbarHostState? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    noItemsMessage: (@Composable () -> Unit)? = null,
    drawItem: @Composable (T, EditableListItemInfo, Modifier) -> Unit
) {
    val selectedItems by state.selectedItems.collectAsStateWithLifecycle()
    val editableItems by state.editableItems.collectAsStateWithLifecycle()
    val editableItemsExpanded by state.editableItemsExpanded.collectAsStateWithLifecycle()
    val activeItem by state.activeItem.collectAsStateWithLifecycle()
    val activeItemId = activeItem?.let { state.getStableId(it) }
    val resources = LocalContext.current.resources
    val snackbarHostStateUpdated by rememberUpdatedState(newValue = snackbarHostState)
    val overScrollPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    val predefinedExpanded by state.predefinedSectionsExpanded.collectAsStateWithLifecycle(
        Array(state.predefinedItemSections.size){ false }
    )

    LaunchedEffect(state.initiateScrollChannel) {
//        Log.v("Tuner", "EditableList: Launching effect for initiate scroll channel")
        for (index in state.initiateScrollChannel) {
//            Log.v("Tuner", "EditableList: initiating scroll to index = $index")
            state.lazyListState.niceAnimatedScroll(
                index.first, index.second, overScrollPx
            )
        }
    }

    // handle recovering deleted items
    LaunchedEffect(resources, state) {
//        Log.v("Tuner", "EditableList: Launching effect for recovering items")
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

                        val currentItems = state.editableItems.value
                        val backupItems = delete.backup
                        val firstChangedIndex = backupItems.zip(currentItems).indexOfFirst {
                            it.first != it.second
                        }.coerceAtLeast(0)

                        state.setNewItems(delete.backup)
                        // delay a bit, since setNewItems runs internally on another coroutine, and
                        // we must let this finish. 100ms is of course not really save, but on the
                        // other hand, if the scroll to 0 fails, it is not really a big problem.
                        delay(100)
                        state.lazyListState.niceAnimatedScroll(
                            firstChangedIndex,
                            firstChangedIndex + state.editableListStartIndex,
                            overScrollPx
                        )
                    }
                    null -> {}
                }
            }
        }
    }

    BackHandler(enabled = selectedItems.isNotEmpty()) {
        state.clearSelectedItems()
    }

//    Log.v("Metronome", "EditableList: contentPadding: top=${contentPadding.calculateTopPadding()}, bottom=${contentPadding.calculateBottomPadding()}")
    LazyColumn(
        modifier = modifier.imePadding(),
        contentPadding = contentPadding,
        state = state.lazyListState
    ) {
        val showSections = (
                (editableItems.size + state.numNonEmptyPredefinedLists) > 1 ||
                        state.numNonEmptyPredefinedLists > 1
                )
        if (editableItems.isEmpty() && state.numNonEmptyPredefinedLists == 0 && noItemsMessage != null) {
            item(contentType = CONTENT_TYPE_EMPTY_MESSAGE) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    noItemsMessage()
                }
            }
        }

        if (editableItems.size > 0) {
            if (showSections) {
                item(contentType = CONTENT_TYPE_SECTION) {
                    EditableListSection(
                        title = stringResource(id = state.editableItemsSectionResId),
                        expanded = editableItemsExpanded
                    ) {
                        state.toggleEditableItemsExpanded(it)
                    }
                }
            }
            if (editableItemsExpanded) {
                itemsIndexed(
                    editableItems,
                    { _, key -> state.getStableId(key) },
                    { _, _ -> CONTENT_TYPE_ITEM }
                ) { index, listItem ->
                    val interactionSource = remember { MutableInteractionSource() }
                    drawItem(
                        listItem,
                        EditableListItemInfo(
                            isActive = (state.getStableId(listItem) == activeItemId),
                            isSelected = selectedItems.contains(state.getStableId(listItem)),
                            readOnly = false,
                            listIndex = index
                        ),
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
                            }
                    )
                }
            }
        }

        state.predefinedItemSections.forEachIndexed { predefinedListIndex, predefinedList ->
            if (showSections && predefinedList.size > 0) {
                item(contentType = CONTENT_TYPE_SECTION) {
                    EditableListSection(
                        title = stringResource(id = predefinedList.sectionStringResourceId),
                        expanded = predefinedExpanded[predefinedListIndex]
                    ) {
                        predefinedList.toggleExpanded(it)
                    }
                }
            }

            if (predefinedExpanded[predefinedListIndex]
                || (predefinedList.size > 0 && editableItems.isEmpty() && state.numNonEmptyPredefinedLists == 1)
            ) {
                items(
                    count = predefinedList.size,
                    key = { state.getStableId(predefinedList[it]) },
                    contentType = { CONTENT_TYPE_ITEM }
                ) {
                    val listItem = predefinedList[it]
                    drawItem(
                        listItem,
                        EditableListItemInfo(
                            isActive = (state.getStableId(listItem) == activeItemId),
                            isSelected = false,
                            readOnly = true,
                            listIndex = it
                        ),
                        Modifier
                            .animateItem()
                            .clickable {
                                onActivateItemClicked(listItem)
                            }
                    )
                }

            }
        }
    }

//        item {
//            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
//        }

//        // extra space to allow scrolling up a bit more, such that the last item
//        // doesn't collide with the fab
//        item {
//            Spacer(Modifier.height(80.dp))
//        }
}

private data class TestItem(val title: String, val key: Long)

@Preview(widthDp = 300, heightDp = 700, showBackground = true)
@Composable
private fun EditableListTest() {
    val predefinedItemsExpanded = remember {
        MutableStateFlow(true)
    }
    val predefinedItemsList = remember {
        persistentListOf(
            EditableListPredefinedSectionImmutable(
                R.string.predefined_items,
                persistentListOf(
                    TestItem("P", -1L)
                ),
                predefinedItemsExpanded,
                { predefinedItemsExpanded.value = it }
            )
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

    val editableItemsExpanded = remember {
        MutableStateFlow(true)
    }
    val activeItem = remember {
        MutableStateFlow(editableItems.value[0])
    }

    val listData = EditableListData(
        predefinedItemsList,
        getStableId = {it.key},
        editableItemsSectionResId = R.string.custom_item,
        editableItems = editableItems,
        editableItemsExpanded = editableItemsExpanded,
        toggleEditableItemsExpanded = { editableItemsExpanded.value = it },
        activeItem = activeItem,
        setNewItems = { editableItems.value = it }
    )
    TunerTheme {
        EditableList(
            state = listData,
            onActivateItemClicked = { activeItem.value = it },
        ) { item, info, modifier ->
            Row(modifier.padding(12.dp)) {
                Text(
                    item.title,
                    color = if (info.isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
            }
        }
    }
}
