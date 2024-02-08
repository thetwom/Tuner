package de.moekadu.tuner.ui.instruments

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

/** Icon definition for centering to a given string.
 * @param modifier Modifier.
 * @param arrowColor Color of the icon arrows.
 * @param stringColor Color of the string label shape.
 */
@Composable
fun ScrollCenterIcon(
    modifier: Modifier = Modifier,
    arrowColor: Color = MaterialTheme.colorScheme.onSurface,
    stringColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(modifier = modifier) {
        Icon(
            ImageVector.vectorResource(id = R.drawable.ic_scroll_center_only_arrows),
            contentDescription = null,
            tint = arrowColor
        )
        Icon(
            ImageVector.vectorResource(id = R.drawable.ic_scroll_center_only_string),
            contentDescription = null,
            tint = stringColor
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScrollCenterIconPreview() {
    TunerTheme {
        ScrollCenterIcon(stringColor = MaterialTheme.colorScheme.error)
    }
}