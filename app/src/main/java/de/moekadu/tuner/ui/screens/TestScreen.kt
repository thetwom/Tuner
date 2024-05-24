package de.moekadu.tuner.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.viewmodels.TestViewModel

@Composable
fun TestScreen(
    viewModel: TestViewModel = viewModel()
) {
    Text(viewModel.test.a)
}

@Preview(widthDp = 100, heightDp = 20, showBackground = true)
@Composable
private fun TestTestScreen() {
    TunerTheme {
        TestScreen()
    }
}