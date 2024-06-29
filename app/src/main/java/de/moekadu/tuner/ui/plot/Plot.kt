package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


class PlotScope(
    private val clipped: Boolean,
    private val transformation: () -> Transformation
) {
    @Composable
    fun Line(
        data: LineCoordinates,
        lineColor: Color = Color.Unspecified,
        lineWidth: Dp = 1.dp
    ) {
        if (clipped)
            Line(data, lineColor, lineWidth, transformation)
    }

    @Composable
    fun Point(
        position: Offset,
        shape: DrawScope.() -> Unit
    ) {
        if (clipped)
            Point(position, shape, transformation)
    }

    @Composable
    fun VerticalLines(
        positions: VerticalLinesPositions,
        color: Color = Color.Unspecified,
        lineWidth: Dp = 1.dp
    ) {
        if (clipped)
            VerticalLines(positions, color, lineWidth, transformation)
    }

    @Composable
    fun HorizontalLines(
        positions: HorizontalLinesPositions,
        color: Color = Color.Unspecified,
        lineWidth: Dp = 1.dp
    ) {
        if (clipped)
            HorizontalLines(positions, color, lineWidth, transformation)
    }

    @Composable
    fun HorizontalMarks(
        marks: ImmutableList<HorizontalMark>,
        sameSizeLabels: Boolean = true,
        clipLabelsToWindow: Boolean = true
        ) {
        HorizontalMarks(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    @Composable
    fun VerticalMarks(
        marks: ImmutableList<VerticalMark>,
        sameSizeLabels: Boolean = true,
        clipLabelsToWindow: Boolean = true
    ) {
        VerticalMarks(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    @Composable
    fun PointMarks(
        marks: ImmutableList<PointMark>,
        sameSizeLabels: Boolean = true,
        clipLabelsToWindow: Boolean = true
    ) {
        PointMarks(
            marks = marks,
            clipLabelsToWindow = clipLabelsToWindow,
            sameSizeLabels = sameSizeLabels,
            transformation = transformation,
            clipped = clipped
        )
    }

    @Composable
    fun XTicks(
        tickLevel: TickLevel,
        maxLabelWidth: Float,
        anchor: Anchor = Anchor.Center,
        verticalLabelPosition: Float = 0.5f,
        lineWidth: Dp = 1.dp,
        lineColor: Color = MaterialTheme.colorScheme.outline,
        screenOffset: DpOffset = DpOffset.Zero,
        maxNumLabels: Int = -1,
        clipLabelToPlotWindow: Boolean = true,
        label: @Composable ((modifier: Modifier, level: Int, index: Int, x: Float) -> Unit)?
    ) {
        XTicks(
            label = label,
            tickLevel = tickLevel,
            maxLabelWidth = maxLabelWidth,
            anchor = anchor,
            verticalLabelPosition = verticalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            screenOffset = screenOffset,
            maxNumLabels = maxNumLabels,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            transformation = transformation,
            clipped = clipped
        )
    }

    @Composable
    fun YTicks(
        tickLevel: TickLevel,
        maxLabelHeight: Float,
        anchor: Anchor = Anchor.Center,
        horizontalLabelPosition: Float = 0.5f,
        lineWidth: Dp = 1.dp,
        lineColor: Color = MaterialTheme.colorScheme.outline,
        screenOffset: DpOffset = DpOffset.Zero,
        maxNumLabels: Int = -1,
        clipLabelToPlotWindow: Boolean = true,
        label: @Composable ((modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?
    ) {
        YTicks(
            label = label,
            tickLevel = tickLevel,
            maxLabelHeight = maxLabelHeight,
            anchor = anchor,
            horizontalLabelPosition = horizontalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            screenOffset = screenOffset,
            maxNumLabels = maxNumLabels,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            transformation = transformation,
            clipped = clipped
        )
    }
}

@Composable
fun Plot(
    modifier: Modifier,
    viewPort: Rect,
    viewPortGestureLimits: Rect? = null,
    gestureBasedViewPort: GestureBasedViewPort = remember { GestureBasedViewPort() },
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotWindowOutline(),
    lockX: Boolean = false,
    lockY: Boolean = false,
    content: @Composable PlotScope.() -> Unit
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

        val plotScopeClipped = remember { PlotScope(clipped = true, { transformation }) }
        val plotScopeUnclipped = remember { PlotScope(clipped = false, { transformation }) }
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
private fun PlotPreview() {
    TunerTheme {
        val viewPortRaw = remember { Rect(left = -5f, top = 10f, right = 5f, bottom = -10f) }
        val viewPortRawLimits =
            remember { Rect(left = -20f, top = 100f, right = 40f, bottom = -100f) }
        val gestureBasedViewPort = remember { GestureBasedViewPort() }
        val scope = rememberCoroutineScope()
        Column {
            Plot(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { scope.launch { gestureBasedViewPort.finish() } },
                viewPort = viewPortRaw,
                viewPortGestureLimits = viewPortRawLimits,
                gestureBasedViewPort = gestureBasedViewPort,
                plotWindowPadding = DpRect(5.dp, 5.dp, 5.dp, 5.dp),
                plotWindowOutline = PlotWindowOutline(lineWidth = 2.dp)
            ) {
                YTicks(
                    tickLevel = TickLevelExplicitRanges(
                        persistentListOf(
                            floatArrayOf(
                                -24f,
                                -20f,
                                -16f,
                                -12f,
                                -8f,
                                -4f,
                                -0f,
                                4f,
                                8f,
                                12f,
                                16f,
                                20f,
                                24f
                            )
                        )
                    ),
                    maxLabelHeight = rememberTextLabelHeight(),
                    anchor = Anchor.SouthWest,
                    horizontalLabelPosition = 0f,
                    maxNumLabels = 6,
                    clipLabelToPlotWindow = true
                ) { modifier, level, index, y ->
                    Text(
                        "y=$y",
                        modifier = modifier,//.background(MaterialTheme.colorScheme.secondary),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                XTicks(
                    tickLevel = TickLevelExplicitRanges(
                        persistentListOf(
                            floatArrayOf(-3f, 0f, 3f)
                        )
                    ),
                    maxLabelWidth = 20f,// TODO: compute this somehow
                    anchor = Anchor.South,
                    verticalLabelPosition = 0f,
                    maxNumLabels = 6,
                    clipLabelToPlotWindow = true
                ) { modifier, level, index, x ->
                    Text(
                        "x=$x",
                        modifier = modifier,//.background(MaterialTheme.colorScheme.secondary),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val x = remember { floatArrayOf(-4f, -2f, 0f, 2f, 4f) }
                val y = remember { floatArrayOf(1f, -7f, -5f, 0f, 8f) }
                Line(
                    data = LineCoordinates.create(x, y),
                    lineWidth = 5.dp,
                    lineColor = MaterialTheme.colorScheme.primary
                )

                Point(
                    position = Offset(4f, 8f),
                    shape = PointShape.circle(size = 20.dp, MaterialTheme.colorScheme.error)
                )

                HorizontalMarks(
                    marks = persistentListOf(
                        HorizontalMark(
                            position = 1f,
                            settings = HorizontalMark.Settings(
                                lineWidth = 3.dp,
                                labelPosition = 1f,
                                anchor = Anchor.East,
                                lineColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { m ->
                            Surface(m, color = MaterialTheme.colorScheme.secondary) {
                                Text("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        },
                    ),
                    clipLabelsToWindow = true,
                    sameSizeLabels = true
                )

                HorizontalMarks(
                    marks = persistentListOf(
                        HorizontalMark(
                            position = 1f,
                            settings = HorizontalMark.Settings(
                                lineWidth = 3.dp,
                                labelPosition = 1f,
                                anchor = Anchor.East,
                                lineColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { m ->
                            Surface(m, color = MaterialTheme.colorScheme.secondary) {
                                Text("ABC", modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        },
                    ),
                    clipLabelsToWindow = true,
                    sameSizeLabels = true
                )

                VerticalMarks(
                    marks = persistentListOf(
                        VerticalMark(
                            position = 0.5f,
                            settings = VerticalMark.Settings(
                                lineWidth = 3.dp,
                                labelPosition = 0.95f,
                                anchor = Anchor.NorthEast,
                                lineColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { m ->
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
                        PointMark(
                            position = Offset(-3f, -4f),
                            settings = PointMark.Settings(
                                anchor = Anchor.Center,
                            )
                        ) { m ->
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
}
