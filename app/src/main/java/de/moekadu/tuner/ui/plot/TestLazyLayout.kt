package de.moekadu.tuner.ui.plot

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints

@OptIn(ExperimentalFoundationApi::class)
class TestLazyLayoutProvider : LazyLayoutItemProvider {
    override val itemCount: Int = 5
    @Composable
    override fun Item(index: Int, key: Any) {
        Log.v("Tuner", "Creating text $index")
        Text("${index}")
    }
    

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberTestLazyLayoutMeasurePolicy(
    itemProviderLambda: () -> TestLazyLayoutProvider
)
        = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>
{
    { containerConstraints ->
        val itemProvider = itemProviderLambda()
        val placeables = (0 until itemProvider.itemCount -2).map {
            measure(it, containerConstraints)
        }

        layout(100, 100) {
            placeables.forEachIndexed { index, placeable ->
                placeable[0].place(index * 30, index * 20)
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TestLazyLayout(
    modifier: Modifier = Modifier
) {
    val itemProvider = remember {TestLazyLayoutProvider()}
    val measurePolicy = rememberTestLazyLayoutMeasurePolicy({itemProvider})
    LazyLayout(
        itemProvider = { itemProvider },
        measurePolicy = measurePolicy)

}

@Preview(showBackground = true)
@Composable
private fun TestLazyLayoutPreview() {
    TestLazyLayout()
}

