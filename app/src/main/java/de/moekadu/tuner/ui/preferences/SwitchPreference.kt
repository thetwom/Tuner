package de.moekadu.tuner.ui.preferences

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun SwitchPreference(
    name: String,
    checked: Boolean,
    onCheckChange: (checked: Boolean) -> Unit,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    ListItem(
        headlineContent = {
            Text(name)
        },
        supportingContent = supporting?.let {{
            Text(it)
        }},
        leadingContent = {
            Icon(ImageVector.vectorResource(id = iconId), null)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckChange)
        },
        modifier = modifier
    )
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
private fun SwitchPreferencePreview() {
    TunerTheme {
        var checked by remember { mutableStateOf(true) }
        Column(modifier = Modifier.fillMaxSize()) {
            SwitchPreference(
                name = "My preference",
                checked = checked,
                onCheckChange = { checked = it},
                iconId = R.drawable.ic_harmonic_energy,
                supporting = null // "Extra text"
            )
            HorizontalDivider()
        }
    }
}