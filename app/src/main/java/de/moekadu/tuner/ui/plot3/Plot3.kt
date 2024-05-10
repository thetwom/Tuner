package de.moekadu.tuner.ui.plot3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
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
class Plot3State(
    viewPortRaw: Rect,
    viewPortRawLimits: Rect = Rect.Zero // no limits
) {
    enum class TargetTransitionType {
        Animate,
        Snap
    }

    enum class BoundsMode {
        Explicit,
        Gesture
    }

    class ViewPortTransition(
        val targetViewPort: Rect,
        val transition: TargetTransitionType,
        val boundsMode: BoundsMode
    )

    private val _viewPortRawTransition = MutableStateFlow(
        ViewPortTransition(viewPortRaw, TargetTransitionType.Snap, BoundsMode.Explicit)
    )
    val viewPortRawTransition get() = _viewPortRawTransition.asStateFlow()
    private var viewPortRawExplicit = viewPortRaw

    private val viewPortAnimationDecay = exponentialDecay<Rect>(1f)
    private val viewPortRawAnimation = Animatable(viewPortRaw, Rect.VectorConverter)
    var viewPortRaw by mutableStateOf(viewPortRaw)
        private set

    private var viewPortLimits = Rect(
        min(viewPortRawLimits.left, viewPortRawLimits.right),
        min(viewPortRawLimits.top, viewPortRawLimits.bottom),
        max(viewPortRawLimits.left, viewPortRawLimits.right),
        max(viewPortRawLimits.top, viewPortRawLimits.bottom),
    )

    var boundsMode by mutableStateOf(BoundsMode.Explicit)
        private set

    fun setViewPortLimits(limits: Rect) {
        viewPortLimits = Rect(
            min(limits.left,limits.right), min(limits.top, limits.bottom),
            max(limits.left, limits.right), max(limits.top, limits.bottom),
        )

        val restrictedViewPort = restrictViewPortToLimits(viewPortRaw)
        if (viewPortRaw != restrictedViewPort) {
            setViewPort(restrictedViewPort, TargetTransitionType.Snap)
        }
    }

    fun setViewPort(target: Rect, transition: TargetTransitionType) {
        viewPortRawExplicit = target
        _viewPortRawTransition.value = ViewPortTransition(target, transition, BoundsMode.Explicit)
    }

    fun resetViewPort(transition: TargetTransitionType) {
        setViewPort(viewPortRawExplicit, transition)
    }

    private fun restrictViewPortToLimits(target: Rect): Rect {
        // the restriction process does not just coerce the values in the min/max, since
        // when dragging and reaching bounds, we would start zooming.
        return if (viewPortLimits == Rect.Zero) {
            target
        } else {
            // shrink size if necessary
            var newTarget = Rect(
                left = target.left,
                top = target.top,
                right = if (target.width.absoluteValue > viewPortLimits.width) {
                    if (target.width > 0) target.left + viewPortLimits.width else target.left - viewPortLimits.width
                } else {
                    target.right
                },
                bottom = if (target.height.absoluteValue > viewPortLimits.height) {
                    if (target.height > 0) target.top + viewPortLimits.height else target.top - viewPortLimits.height
                } else {
                    target.bottom
                },
            )

            // make sure, that left/top don't exceed limits
            val translateToMatchX = max(0f, viewPortLimits.left - min(target.left, target.right))
            val translateToMatchY = max(0f, viewPortLimits.top - min(target.top, target.bottom))
            newTarget = newTarget.translate(translateToMatchX, translateToMatchY)
            // make sure, that right/bottom don't exceed limits
            val translateToMatchX2 = min(0f, viewPortLimits.right - max(target.left, target.right))
            val translateToMatchY2 = min(0f, viewPortLimits.bottom - max(target.top, target.bottom))
            newTarget = newTarget.translate(translateToMatchX2, translateToMatchY2)

            return newTarget
        }
    }
    suspend fun stopViewPortAnimation() {
        viewPortRawAnimation.stop()
    }

    suspend fun setViewPort(target: Rect, transition: TargetTransitionType, boundsMode: BoundsMode) {
        this.boundsMode = boundsMode
        viewPortRawAnimation.updateBounds(null, null)

        val targetInLimits = restrictViewPortToLimits(target)
        when (transition) {
            TargetTransitionType.Snap -> viewPortRaw = targetInLimits
            TargetTransitionType.Animate -> {
                viewPortRawAnimation.snapTo(viewPortRaw)
                viewPortRawAnimation.animateTo(targetInLimits) {
                    viewPortRaw = value
                }
            }
        }
    }

    suspend fun flingViewPort(velocity: Velocity, boundsMode: BoundsMode) {
        this.boundsMode = boundsMode
        viewPortRawAnimation.snapTo(viewPortRaw)
        viewPortRawAnimation.animateDecay(
            Rect(velocity.x, velocity.y, velocity.x, velocity.y), viewPortAnimationDecay
        ) {
            val targetInLimits = restrictViewPortToLimits(value)
            viewPortRaw = targetInLimits
        }
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
    state: Plot3State,
    modifier: Modifier,
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

        // use updated state here, to avoid having to recreate of the pointerInput modifier
        val transformation by rememberUpdatedState(
            Transformation(viewPortScreen, state.viewPortRaw, cornerRadiusPx)
        )

        LaunchedEffect(state) {
            state.viewPortRawTransition.collect {
//                Log.v("Tuner", "Plot: LaunchedEffect(viewPortRawTransition:collect ${it.targetViewPort}")
                state.setViewPort(it.targetViewPort, it.transition, it.boundsMode)
            }
        }

//        // use updated state here, to avoid having to recreate of the pointerInput modifier
//        val transformation by rememberUpdatedState(
//            Transformation(viewPortScreen, Rect(-5f, 10f, 5f, -10f))
//        )

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
                .dragZoom(state, { transformation }, lockX = lockX, lockY = lockY)
        )
    }
}

@Preview(widthDp = 250, heightDp = 200, showBackground = true)
@Composable
private fun Plot3Preview() {
    TunerTheme {
        val state = remember {
            Plot3State(
                viewPortRaw = Rect(left = -5f, top = 10f, right = 5f, bottom = -10f),
                viewPortRawLimits = Rect(left = -20f, top = 100f, right = 40f, bottom = -100f)
            )
        }
        Plot3(
            state,
            modifier = Modifier.fillMaxSize(),
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
