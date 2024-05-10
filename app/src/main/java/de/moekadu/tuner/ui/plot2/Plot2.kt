package de.moekadu.tuner.ui.plot2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.PlotDefaults
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.Transformation
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class Plot2State(
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

interface PlotScope {
    val transformation: Transformation

    fun line(
        coordinates: Line2.Coordinates,
        width: Dp = 1.dp,
        color: Color = Color.Unspecified
    )

    fun point(
        position: Offset,
        shape: @Composable Point2.Scope.() -> Unit
    )

    fun yTicks(
        level: Tick2Level,
        lineColor: Color = Color.Unspecified,
        lineWidth: Dp = 1.dp,
        horizontalPosition: Float = 1f,
        anchor: Anchor = Anchor.West,
        label: @Composable (modifier: Modifier, level: Int, index: Int, value: Float) -> Unit
    )
}

class PlotScopeImpl(transformation: Transformation) : PlotScope {
    override var transformation = transformation
        private set

    val items = ArrayList<PlotItem2>()

    private var count = 0
    fun reset(transformation: Transformation) {
        this.transformation = transformation
        count = 0
    }

    fun finishedAddingItems() {
        if (items.size > count) {
            items.subList(count, items.size).clear()
        }
    }

    override fun line(
        coordinates: Line2.Coordinates,
        width: Dp,
        color: Color,
    ) {
        val item = (items.getOrNull(count) as? Line2)
            ?: Line2.create(coordinates, width, color, transformation)
        items.add(count, item.modify(coordinates, width, color, transformation))
        ++count
    }

    override fun point(
        position: Offset,
        shape: @Composable Point2.Scope.() -> Unit
    ) {
        //items.add(Point2(position, shape, transformation))
    }

    override fun yTicks(
        level: Tick2Level,
        lineColor: Color,
        lineWidth: Dp,
        horizontalPosition: Float,
        anchor: Anchor,
        label: @Composable (modifier: Modifier, level: Int, index: Int, value: Float) -> Unit
    ) {}
}


@Composable
fun Plot2(
    state: Plot2State,
    modifier: Modifier = Modifier,
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotDefaults.windowOutline(),
    lockX: Boolean = false, lockY: Boolean = false,
    content: @Composable PlotScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

        val outlineLineWidth = when (state.boundsMode){
            Plot2State.BoundsMode.Explicit -> plotWindowOutline.lineWidth
            Plot2State.BoundsMode.Gesture -> plotWindowOutline.lineWidthGesture
        }
        val outlineLineColor = when (state.boundsMode){
            Plot2State.BoundsMode.Explicit -> plotWindowOutline.color
            Plot2State.BoundsMode.Gesture -> plotWindowOutline.colorGesture
        }
        val density = LocalDensity.current
        val outline2 = with(LocalDensity.current) { (outlineLineWidth / 2).roundToPx() }
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
//                Log.v("Tuner", "Plot: LaunchedEffect(viewPortRawTransition:collect ${it.targetViewPort}")
                state.setViewPort(it.targetViewPort, it.transition, it.boundsMode)
            }
        }

        val plotScope = remember { PlotScopeImpl(transformation) }
        plotScope.reset(transformation)
        plotScope.content()
        plotScope.finishedAddingItems()

        val clipShape = transformation.rememberClipShape()
        Box(modifier = Modifier
            .fillMaxSize()
            .clip(clipShape)) {
            plotScope.items.filter { it.hasClippedDraw }.forEach {
                it.DrawClipped()
            }
        }

        plotScope.items.filter { it.hasUnclippedDraw }.forEach {
            it.DrawUnclipped()
        }
        LazyColumn {

        }
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRoundRect(
                        outlineLineColor,
                        viewPortScreen.topLeft.toOffset(),
                        viewPortScreen.size.toSize(),
                        cornerRadius = CornerRadius(cornerRadiusPx),
                        style = Stroke(outlineLineWidth.toPx())
                    )
                }
                .dragZoom(state, { transformation }, lockX = lockX, lockY = lockY)
        )
    }
}

@Preview(widthDp = 200, heightDp = 400, showBackground = true)
@Composable
private fun Plot2Preview() {
    TunerTheme {
        val state = remember {
            Plot2State(
                viewPortRaw = Rect(left = -5f, top = -10f, right = 5f, bottom = 10f),
                viewPortRawLimits = Rect(left = -20f, top = 100f, right = 40f, bottom = -100f)
            )
        }
        Plot2(
            state,
            modifier = Modifier.fillMaxSize(),
            plotWindowPadding = DpRect(5.dp, 5.dp, 5.dp, 5.dp)
        ) {
            line(
                coordinates = Line2.Coordinates(
                    floatArrayOf(-4f, -2f, 0f, 2f, 4f),
                    floatArrayOf(1f, -7f, -5f, 1f, 3f),
                ),
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )

            point(
                position = Offset(1f, 2f)
            ) {
                val color = MaterialTheme.colorScheme.onSurface
                drawShape {
                    drawCircle(color, 3.dp.toPx(), Offset.Zero)
                }
            }

            yTicks(
                Tick2Level(),
                lineColor = MaterialTheme.colorScheme.outline,
                lineWidth = 1.dp,
                horizontalPosition = 1f,
                anchor = Anchor.West
            ) { modifier, level, index, value ->
                Text("$index", modifier = modifier)
            }
        }
    }
}
