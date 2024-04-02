package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

enum class TargetTransitionType {
    Animate,
    Snap
}
data class ViewPortTransition(
    val targetViewPort: Rect,
    val transition: TargetTransitionType
)

class PlotState2(
    initialViewPortRaw: Rect
) {
    private val _viewPortRawTransition = MutableStateFlow(
        ViewPortTransition(initialViewPortRaw, TargetTransitionType.Snap)
    )
    val viewPortRawTransition get() = _viewPortRawTransition.asStateFlow()

    val viewPortRawAnimation = Animatable(initialViewPortRaw, Rect.VectorConverter)
    val viewPortRaw get() = viewPortRawAnimation.value

    private val lines = LineGroup()
    private val horizontalMarks = mutableMapOf<Int, HorizontalMarks3>()

    fun snapViewPortTo(target: Rect) {
        _viewPortRawTransition.value = ViewPortTransition(target, TargetTransitionType.Snap)
    }

    fun animateViewPortTo(target: Rect) {
        _viewPortRawTransition.value = ViewPortTransition(target, TargetTransitionType.Animate)
    }

    fun setLine(
        xValues: FloatArray, yValues: FloatArray,
        key: Int = 0,
        indexBegin: Int = 0,
        indexEnd: Int = min(xValues.size, yValues.size),
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null
    ) {
        lines.setLine(xValues, yValues, key, indexBegin, indexEnd, lineWidth, lineColor)
    }

    fun setHorizontalMarks(
        yValues: ImmutableList<FloatArray>,
        maxLabelHeight: Density.() -> Float,
        horizontalLabelPosition: Float,
        anchor: Anchor,
        lineWidth: Dp,
        key: Int = 0,
        clipLabelToPlotWindow: Boolean = false,
        lineColor: @Composable () -> Color = { Color.Unspecified },
        maxNumLabels: Int = -1, // -1 is auto
        label: (@Composable (level: Int, index: Int, y: Float) -> Unit)? = null
    ) {
        val markLevels = MarkLevelExplicitRanges3(yValues)
        horizontalMarks[key] = HorizontalMarks3(
            label = label,
            markLevel = markLevels,
            defaultAnchor = anchor,
            defaultHorizontalLabelPosition = horizontalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            maxLabelHeight = maxLabelHeight,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            maxNumLabels = maxNumLabels
        )
    }

    @Composable
    fun Draw(transformation: Transformation) {
        Box(modifier = Modifier.fillMaxSize()) {
            horizontalMarks.forEach { it.value.Draw(transformation) }
            lines.Draw(transformation)
        }
    }
}


@Composable
fun Plot2(
    state: PlotState2,
    modifier: Modifier = Modifier,
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotDefaults.windowOutline(),
){
    BoxWithConstraints(modifier = modifier.background(Color.LightGray)) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

        val outline2 = with(LocalDensity.current) { (plotWindowOutline.lineWidth / 2).roundToPx() }
        val density = LocalDensity.current
        val viewPortScreen = remember(density, plotWindowPadding, outline2, widthPx, heightPx) {
            with(density) {
                IntRect(
                    plotWindowPadding.left.roundToPx() + outline2,
                    plotWindowPadding.top.roundToPx() + outline2,
                    widthPx - plotWindowPadding.right.roundToPx() - outline2,
                    heightPx - plotWindowPadding.bottom.roundToPx() - outline2
                )
            }
        }
        val cornerRadiusPx = with(LocalDensity.current) { plotWindowOutline.cornerRadius.toPx() }

        // use updated state here, to avoid having to recreate of the pointerInput modifier
        val transformation by rememberUpdatedState(
            Transformation(viewPortScreen, state.viewPortRaw, cornerRadiusPx)
        )

        LaunchedEffect(state) {
            state.viewPortRawTransition.collect {
                when (it.transition) {
                    TargetTransitionType.Snap -> state.viewPortRawAnimation.snapTo(it.targetViewPort)
                    TargetTransitionType.Animate -> state.viewPortRawAnimation.animateTo(it.targetViewPort)
                }
            }
        }

        Box(
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        plotWindowOutline.color,
                        viewPortScreen.topLeft.toOffset(),
                        viewPortScreen.size.toSize(),
                        cornerRadius = CornerRadius(cornerRadiusPx),
                        style = Stroke(plotWindowOutline.lineWidth.toPx())
                    )
                }
                .pointerInput(Unit) {
                    val decay = exponentialDecay<Rect>(1f)
                    detectPanZoomFlingGesture(
                        onGestureStart = {
                            state.viewPortRawAnimation.stop()
                        },
                        onGesture = { centroid, pan, zoom ->
                            val modifiedTopLeft = Offset(
                                (transformation.viewPortScreen.left - centroid.x) / zoom.width + centroid.x - pan.x,
                                (transformation.viewPortScreen.top - centroid.y) / zoom.height + centroid.y - pan.y,
                            )
                            //Log.v("Tuner", "Plot: original topLeft=$originalTopLeft, modified topLeft=$modifiedTopLeft, zoom=$zoom")
                            val zoomedSize = Size(
                                state.viewPortRaw.size.width / zoom.width,
                                state.viewPortRaw.size.height / zoom.height
                            )

                            val movedTopLeftRaw = transformation.toRaw(modifiedTopLeft)
                            state.viewPortRawAnimation.snapTo(Rect(movedTopLeftRaw, zoomedSize))
                        },
                        onFling = { velocity ->
                            val velocityRaw = (
                                    transformation.toRaw(Offset.Zero) - transformation.toRaw(
                                        Offset(
                                            velocity.x,
                                            velocity.y
                                        )
                                    )
                                    )
                            state.viewPortRawAnimation.animateDecay(
                                Rect(
                                    velocityRaw.x,
                                    velocityRaw.y,
                                    velocityRaw.x,
                                    velocityRaw.y
                                ), decay
                            )
                        }
                    )
                }
        ) {
            state.Draw(transformation = transformation)
        }
    }

}

@Preview(widthDp = 300, heightDp = 500, showBackground = true)
@Composable
private fun Plot2Preview() {
    TunerTheme {
        val textLabelHeight = rememberTextLabelHeight()
        val state = remember{
            PlotState2(
                initialViewPortRaw = Rect(left = 2f, top = 20f, right = 10f, bottom = 3f)
            ).apply {
                setLine(floatArrayOf(3f, 5f, 7f, 9f), floatArrayOf(4f, 8f, 6f, 15f))
                setHorizontalMarks(
                    listOf(
                        floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 15f, 20f)
                    ).toImmutableList(),
                    maxLabelHeight = { textLabelHeight },
                    horizontalLabelPosition = 0.5f,
                    anchor = Anchor.Center, //Anchor.East,
                    lineWidth = 1.dp,
                    clipLabelToPlotWindow = true,
                    lineColor = { MaterialTheme.colorScheme.primary },
                ) { level, index, value ->
                    Text("$index, $value, $level",
                        modifier = Modifier.background(Color.Cyan))
                }
            }

        }

        Plot2(
            state,
            plotWindowPadding = DpRect(left = 5.dp, top = 10.dp, right = 8.dp, bottom = 3.dp )
        )
    }
}