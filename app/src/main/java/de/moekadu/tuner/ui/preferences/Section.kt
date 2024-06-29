package de.moekadu.tuner.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        leadingContent = {
            Spacer(modifier = Modifier.size(24.dp))
        },
        modifier = modifier
    )
}

@Preview(widthDp = 300, heightDp = 100, showBackground = true)
@Composable
private fun SectionPreview() {
    TunerTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Section("My section")
            HorizontalDivider()
        }
    }
}