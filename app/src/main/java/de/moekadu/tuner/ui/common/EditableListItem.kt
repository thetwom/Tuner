package de.moekadu.tuner.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

enum class ListItemTask {
    Copy,
    Edit,
    Delete,
    Info
}

private fun numMenuOptions(readOnly: Boolean, isCopyable: Boolean, hasInfo: Boolean): Int {
    var count = 0
    if (hasInfo)
        ++count // enable info
    if (isCopyable)
        ++count // enable copy
    if (!readOnly)
        count += 2  // enable edit and delete
    return count
}

@Composable
fun EditableListItem(
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onOptionsClicked: (task: ListItemTask) -> Unit = { },
    isActive: Boolean = false,
    isSelected: Boolean = false,
    readOnly: Boolean = false, // disable delete/edit options
    isCopyable: Boolean = true, // disable copy-option
    hasInfo: Boolean = false
) {
    val variantColor = if (isActive)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    var menuExpanded by remember{ mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(vertical = 8.dp)
        ) {
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.size(40.dp)) {
                CompositionLocalProvider(LocalContentColor provides variantColor) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        )
                    } else {
                        icon()
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyLarge
                ) {
                    title()
                }
                Spacer(Modifier.height(4.dp))
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    LocalContentColor provides variantColor
                )  {
                    description()
                }
            }
            Box {
                val numOptions = numMenuOptions(readOnly, isCopyable, hasInfo)
                if (numOptions == 1 && isCopyable) {
                    IconButton(
                        onClick = { onOptionsClicked(ListItemTask.Copy) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_copy),
                            contentDescription = "copy",
                            tint = variantColor
                        )
                    }
                } else if (numOptions == 1 && hasInfo) {
                    IconButton(
                        onClick = { onOptionsClicked(ListItemTask.Info) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "details",
                            tint = variantColor
                        )
                    }

                } else if (numOptions > 1) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = variantColor
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (hasInfo) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.details)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "details"
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOptionsClicked(ListItemTask.Info)
                                }
                            )
                            HorizontalDivider()
                        }
                        if (!readOnly) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.edit)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "edit"
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOptionsClicked(ListItemTask.Edit)
                                }
                            )
                        }
                        if (isCopyable) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.copy_)) },
                                leadingIcon = {
                                    Icon(
                                        ImageVector.vectorResource(id = R.drawable.ic_copy),
                                        contentDescription = "copy"
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOptionsClicked(ListItemTask.Copy)
                                }
                            )
                        }
                        if (!readOnly) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.delete)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "delete"
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOptionsClicked(ListItemTask.Delete)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun EditableListItemPreview() {
    TunerTheme {
        Column {
            EditableListItem(
                title = { Text("Title 1") },
                description = { Text("Description 1") },
                icon = {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.ic_trumpet),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )

        }
    }
}
