package de.moekadu.tuner.ui.preferences

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun SliderPreference(
    name: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (newValue: Float) -> Unit,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    ListItem(
        headlineContent = {
            Text(name)
        },
        supportingContent = supporting?.let {{
            Column {
                Text(it)
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    steps = steps
                )

//                Slider(
//                    value = 0.5f,
//                    onValueChange = {}
//                )
            }
        }},
        leadingContent = {
            Icon(ImageVector.vectorResource(id = iconId), null)
        },
        modifier = modifier
    )
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true)
@Composable
private fun SliderPreferencePreview() {
    TunerTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            var value by remember { mutableFloatStateOf(50f)}
            SliderPreference(
                value = value,
                valueRange = 0f..100f,
                steps = 9,
                name = "My preference",
                onValueChange = { value = it },
                iconId = R.drawable.ic_harmonic_energy,
                supporting = "Extra text ${value}"
            )
            HorizontalDivider()
        }
    }
}