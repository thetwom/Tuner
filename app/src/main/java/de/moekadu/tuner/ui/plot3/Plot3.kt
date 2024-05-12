package de.moekadu.tuner.ui.plot3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.PlotDefaults
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.TickLevel
import de.moekadu.tuner.ui.plot.TickLevelExplicitRanges
import de.moekadu.tuner.ui.plot.Transformation
import de.moekadu.tuner.ui.plot.rememberTextLabelHeight
import de.moekadu.tuner.ui.plot2.Line2
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

interface Plot3Scope {

    @Composable
    fun Line(
        data: Line3Coordinates,
        color: Color,
        width: Dp
    )

    @Composable
    fun Point(
        position: Offset,
        shape: DrawScope.() -> Unit
    )

    @Composable
    fun HorizontalMarks(
        marks: ImmutableList<HorizontalMark3>,
        sameSizeLabels: Boolean,
        clipLabelsToWindow: Boolean
    )

    @Composable
    fun PointMarks(
        marks: ImmutableList<PointMark3>,
        sameSizeLabels: Boolean,
        clipLabelsToWindow: Boolean
    )

    @Composable
    fun YTicks(
        tickLevel: TickLevel,
        settings: YTicksSettings,
        label: @Composable ((modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?
    )
}

class Plot3ScopeImpl(
    private val clipped: Boolean,
    private val transformation: () -> Transformation
) : Plot3Scope {
    @Composable
    override fun Line(
        data: Line3Coordinates,
        color: Color,
        width: Dp
    ) {
        if (clipped)
            Line3(data, color, width, transformation)
    }

    @Composable
    override fun Point(
        position: Offset,
        shape: DrawScope.() -> Unit
    ) {
        if (clipped)
            Point3(position, shape, transformation)
    }

    @Composable
    override fun HorizontalMarks(
        marks: ImmutableList<HorizontalMark3>,
        sameSizeLabels: Boolean,
        clipLabelsToWindow: Boolean
        ) {
        HorizontalMarks3(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    @Composable
    override fun PointMarks(
        marks: ImmutableList<PointMark3>,
        sameSizeLabels: Boolean,
        clipLabelsToWindow: Boolean
    ) {
        PointMarks3(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    @Composable
    override fun YTicks(
        tickLevel: TickLevel,
        settings: YTicksSettings,
        label: @Composable ((modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?
    ) {
        YTicks3(
            label = label,
            tickLevel = tickLevel,
            settings = settings,
            transformation = transformation,
            clipped = clipped
        )
    }


}

object Plot3Defaults {
    @Composable
    fun windowOutline(
        lineWidth: Dp = 1.dp,
        cornerRadius: Dp = 8.dp,
        color: Color = MaterialTheme.colorScheme.onSurface,
    ) = Plot3WindowOutline(lineWidth, cornerRadius, color)
}

@Composable
fun Plot3(
    modifier: Modifier,
    viewPort: Rect,
    viewPortGestureLimits: Rect? = null,
    gestureBasedViewPort: GestureBasedViewPort = remember { GestureBasedViewPort() },
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: Plot3WindowOutline = Plot3Defaults.windowOutline(),
    lockX: Boolean = false,
    lockY: Boolean = false,
    content: @Composable Plot3Scope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
        val cornerRadiusPx = with(LocalDensity.current) { plotWindowOutline.cornerRadius.toPx() }
        val density = LocalDensity.current
        val viewPortScreen = remember(density, plotWindowPadding, widthPx, heightPx) {
            with(density) {
                IntRect(
                    plotWindowPadding.left.roundToPx(),
                    plotWindowPadding.top.roundToPx(),
                    widthPx - plotWindowPadding.right.roundToPx(),
                    heightPx - plotWindowPadding.bottom.roundToPx()
                )
            }
        }
        val resolvedViewPortRaw by animateRectAsState(
            targetValue = if (gestureBasedViewPort.isActive) gestureBasedViewPort.viewPort else viewPort,
            label = "animate viewport",
            animationSpec = if (gestureBasedViewPort.isActive) snap(0) else spring()
        )
        val resolvedLimits = remember(viewPortGestureLimits) {
            if (viewPortGestureLimits == null) {
                null
            } else {
                Rect(
                    min(viewPortGestureLimits.left, viewPortGestureLimits.right),
                    min(viewPortGestureLimits.top, viewPortGestureLimits.bottom),
                    max(viewPortGestureLimits.left, viewPortGestureLimits.right),
                    max(viewPortGestureLimits.top, viewPortGestureLimits.bottom),
                )
            }
        }

        // use updated state here, to avoid having to recreate of the pointerInput modifier
        val transformation by rememberUpdatedState(
            Transformation(
                viewPortScreen,
                if (gestureBasedViewPort.isActive) gestureBasedViewPort.viewPort else resolvedViewPortRaw,
                cornerRadiusPx
            )
        )

        val plotScopeClipped = remember { Plot3ScopeImpl(clipped = true, { transformation }) }
        val plotScopeUnclipped = remember { Plot3ScopeImpl(clipped = false, { transformation }) }
        val clipShape = transformation.rememberClipShape()
        Box(modifier = Modifier
            .fillMaxSize()
            .clip(clipShape)) {
            plotScopeClipped.content()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .createPlotWindowOutline(plotWindowOutline, { transformation.viewPortScreen })
        ) {
            plotScopeUnclipped.content()
        }

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .dragZoom(
                    gestureBasedViewPort,
                    { resolvedLimits },
                    { transformation },
                    lockX = lockX,
                    lockY = lockY
                )
        )
    }
}

@Preview(widthDp = 250, heightDp = 200, showBackground = true)
@Composable
private fun Plot3Preview() {
    TunerTheme {
        val viewPortRaw = remember { Rect(left = -5f, top = 10f, right = 5f, bottom = -10f) }
        val viewPortRawLimits = remember { Rect(left = -20f, top = 100f, right = 40f, bottom = -100f) }
        val gestureBasedViewPort = remember { GestureBasedViewPort() }
        val scope = rememberCoroutineScope()
        Plot3(
            modifier = Modifier
                .fillMaxSize()
                .clickable { scope.launch { gestureBasedViewPort.finish() } },
            viewPort = viewPortRaw,
            viewPortGestureLimits = viewPortRawLimits,
            gestureBasedViewPort = gestureBasedViewPort,
            plotWindowPadding = DpRect(5.dp, 5.dp, 5.dp, 5.dp),
            plotWindowOutline = Plot3Defaults.windowOutline(lineWidth = 2.dp)
        ) {
            YTicks(
                tickLevel = TickLevelExplicitRanges(persistentListOf(
                    floatArrayOf(-24f, -20f, -16f, -12f, -8f, -4f, -0f, 4f, 8f, 12f, 16f, 20f, 24f)
                )),
                YTicksSettings(
                    maxLabelHeight = rememberTextLabelHeight(),
                    anchor = Anchor.SouthWest,
                    horizontalLabelPosition = 0f,
                    maxNumLabels = 6,
                    clipLabelToPlotWindow = true
                ),
            ) { modifier, level, index, y ->
                Text(
                    "y=$y",
                    modifier = modifier,//.background(MaterialTheme.colorScheme.secondary),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val x = remember { floatArrayOf(-4f, -2f, 0f, 2f, 4f) }
            val y = remember { floatArrayOf(1f, -7f, -5f, 0f, 8f) }
            Line(
                data = Line3Coordinates(x.size, { x[it] }, { y[it] }),
                width = 5.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Point(
                position = Offset(4f, 8f),
                shape = Point3Shape.circle(size = 20.dp, MaterialTheme.colorScheme.error)
            )

            HorizontalMarks(
                marks = persistentListOf(
                    HorizontalMark3(
                        position = 1f,
                        settings = HorizontalMark3.Settings(
                            lineWidth = 3.dp,
                            labelPosition = 1f,
                            anchor = Anchor.East,
                            lineColor = MaterialTheme.colorScheme.secondary
                        )
                    )  { m ->
                        Surface(m, color = MaterialTheme.colorScheme.secondary) {
                            Text("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                        }
                    },
                ),
                clipLabelsToWindow = true,
                sameSizeLabels = true
            )

            PointMarks(
                marks = persistentListOf(
                    PointMark3(
                        position = Offset(-3f, -4f),
                        settings = PointMark3.Settings(
                            anchor = Anchor.Center,
                        )
                    )  { m ->
                        Surface(m, color = MaterialTheme.colorScheme.secondary) {
                            Text("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                        }
                    },
                ),
                clipLabelsToWindow = true,
                sameSizeLabels = true
            )
        }
    }
}
