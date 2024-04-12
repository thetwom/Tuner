package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


// TODO: introduce bounds for drag/zoom
class PlotState(
    initialViewPortRaw: Rect,
    initialViewPortRawLimits: Rect = Rect.Zero // no limits
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
        ViewPortTransition(initialViewPortRaw, TargetTransitionType.Snap, BoundsMode.Explicit)
    )
    val viewPortRawTransition get() = _viewPortRawTransition.asStateFlow()
    private var viewPortRawExplicit = initialViewPortRaw

    private val viewPortAnimationDecay = exponentialDecay<Rect>(1f)
    private val viewPortRawAnimation = Animatable(initialViewPortRaw, Rect.VectorConverter)
    val viewPortRaw get() = viewPortRawAnimation.value

    private var viewPortLimits = Rect(
        min(initialViewPortRawLimits.left, initialViewPortRawLimits.right),
        min(initialViewPortRawLimits.top, initialViewPortRawLimits.bottom),
        max(initialViewPortRawLimits.left, initialViewPortRawLimits.right),
        max(initialViewPortRawLimits.top, initialViewPortRawLimits.bottom),
    )

    private val lines = LineGroup()
    private val points = PointGroup()
    private val horizontalMarks = mutableMapOf<Int, HorizontalMarks>()
    private val verticalMarks = mutableMapOf<Int, VerticalMarks>()
    private val pointMarks = PointMarkGroup()

    var boundsMode by mutableStateOf(BoundsMode.Explicit)
        private set

    fun setViewPortLimits(limits: Rect) {
        viewPortLimits = Rect(
            min(limits.left, limits.right), min(limits.top, limits.bottom),
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
            TargetTransitionType.Snap -> viewPortRawAnimation.snapTo(targetInLimits)
            TargetTransitionType.Animate -> viewPortRawAnimation.animateTo(targetInLimits)
        }
    }

    suspend fun flingViewPort(velocity: Velocity, boundsMode: BoundsMode) {
        this.boundsMode = boundsMode

        val currentViewPort = viewPortRaw

        val lowerBound = Rect(
            left = if (currentViewPort.left <= currentViewPort.right)
                viewPortLimits.left else viewPortLimits.left + currentViewPort.width,
            top = if (currentViewPort.top <= currentViewPort.bottom)
                viewPortLimits.top else viewPortLimits.top + currentViewPort.height,
            right = if (currentViewPort.left <= currentViewPort.right)
                viewPortLimits.left + currentViewPort.width else viewPortLimits.left,
            bottom = if (currentViewPort.top <= currentViewPort.bottom)
                viewPortLimits.top + currentViewPort.height else viewPortLimits.top
        )
        val upperBoundPre = Rect(
            left = if (currentViewPort.left <= currentViewPort.right)
                viewPortLimits.right - currentViewPort.width else viewPortLimits.right,
            top = if (currentViewPort.top <= currentViewPort.bottom)
                viewPortLimits.bottom - currentViewPort.height else viewPortLimits.bottom,
            right = if (currentViewPort.left <= currentViewPort.right)
                viewPortLimits.right else viewPortLimits.right - currentViewPort.width,
            bottom = if (currentViewPort.top <= currentViewPort.bottom)
                viewPortLimits.bottom else viewPortLimits.bottom - currentViewPort.height
        )
        val upperBound = Rect(
            max(upperBoundPre.left, lowerBound.left),
            max(upperBoundPre.top, lowerBound.top),
            max(upperBoundPre.right, lowerBound.right),
            max(upperBoundPre.bottom, lowerBound.bottom),
        )

        viewPortRawAnimation.updateBounds(lowerBound, upperBound)

        viewPortRawAnimation.animateDecay(
            Rect(velocity.x, velocity.y, velocity.x, velocity.y), viewPortAnimationDecay
        )

        // TODO: viewportrestriction does not really work since can happen that it zooms
    }
    fun setLine(
        key: Int,
        xValues: FloatArray, yValues: FloatArray,
        indexBegin: Int = 0,
        indexEnd: Int = min(xValues.size, yValues.size),
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null
    ) {
        lines.setLine(key, xValues, yValues, indexBegin, indexEnd, lineWidth, lineColor)
    }

    fun setPoint(
        key: Int,
        position: Offset,
        content: (@Composable PointScope.() -> Unit)? = null,
    ) {
        points.setPoint(key, position, content)
    }

    fun setHorizontalMarks(
        key: Int,
        yValues: ImmutableList<FloatArray>,
        maxLabelHeight: Density.() -> Float,
        horizontalLabelPosition: Float,
        anchor: Anchor,
        lineWidth: Dp,
        clipLabelToPlotWindow: Boolean = false,
        lineColor: @Composable () -> Color = { Color.Unspecified },
        maxNumLabels: Int = -1, // -1 is auto
        label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)? = null
    ) {
        val markLevels = MarkLevelExplicitRanges(yValues)
        horizontalMarks[key] = HorizontalMarks(
            label = label,
            markLevel = markLevels,
            anchor = anchor,
            horizontalLabelPosition = horizontalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            maxLabelHeight = maxLabelHeight,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            maxNumLabels = maxNumLabels
        )
    }

    fun setVerticalMarks(
        key: Int,
        xValues: ImmutableList<FloatArray>,
        maxLabelWidth: Density.() -> Float,
        verticalLabelPosition: Float,
        anchor: Anchor,
        lineWidth: Dp,
        clipLabelToPlotWindow: Boolean = false,
        lineColor: @Composable () -> Color = { Color.Unspecified },
        maxNumLabels: Int = -1, // -1 is auto
        label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)? = null
    ) {
        val markLevels = MarkLevelExplicitRanges(xValues)
        verticalMarks[key] = VerticalMarks(
            label = label,
            markLevel = markLevels,
            anchor = anchor,
            verticalLabelPosition = verticalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            maxLabelWidth = maxLabelWidth,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            maxNumLabels = maxNumLabels
        )
    }

    fun setPointMark(
        key: Int,
        position: Offset,
        anchor: Anchor = Anchor.Center,
        screenOffset: DpOffset = DpOffset.Zero,
        content: (@Composable (modifier: Modifier) -> Unit)? = null
    ) {
        pointMarks.setPointMark(key, position, anchor, screenOffset, content)
    }

    @Composable
    fun Draw(transformation: Transformation) {
        Box(modifier = Modifier.fillMaxSize()) {
            horizontalMarks.forEach { it.value.Draw(transformation) }
            verticalMarks.forEach { it.value.Draw(transformation) }
            lines.Draw(transformation)
            points.Draw(transformation)
            pointMarks.Draw(transformation)
        }
    }
}

data class PlotWindowOutline(
    val lineWidth: Dp,
    val cornerRadius: Dp,
    val color: Color,
    val lineWidthGesture: Dp,
    val colorGesture: Color
)

object PlotDefaults {
    @Composable
    fun windowOutline(
        lineWidth: Dp = 1.dp,
        cornerRadius: Dp = 8.dp,
        color: Color = MaterialTheme.colorScheme.onSurface,
        lineWidthGesture: Dp = 2.dp,
        colorGesture: Color = MaterialTheme.colorScheme.primary,
    ) = PlotWindowOutline(lineWidth, cornerRadius, color, lineWidthGesture, colorGesture)
}

@Composable
fun Plot(
    state: PlotState,
    modifier: Modifier = Modifier,
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotDefaults.windowOutline(),
    lockX: Boolean = false, lockY: Boolean = false
){
    BoxWithConstraints(
        modifier = modifier
            //.background(Color.LightGray)
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

        val outlineLineWidth = when (state.boundsMode){
            PlotState.BoundsMode.Explicit -> plotWindowOutline.lineWidth
            PlotState.BoundsMode.Gesture -> plotWindowOutline.lineWidthGesture
        }
        val outlineLineColor = when (state.boundsMode){
            PlotState.BoundsMode.Explicit -> plotWindowOutline.color
            PlotState.BoundsMode.Gesture -> plotWindowOutline.colorGesture
        }

        val outline2 = with(LocalDensity.current) { (outlineLineWidth / 2).roundToPx() }
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
//                Log.v("Tuner", "Plot: LaunchedEffect(viewPortRawTransition:collect ${it.targetViewPort}")
                state.setViewPort(it.targetViewPort, it.transition, it.boundsMode)
            }
        }

        Box(
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        outlineLineColor,
                        viewPortScreen.topLeft.toOffset(),
                        viewPortScreen.size.toSize(),
                        cornerRadius = CornerRadius(cornerRadiusPx),
                        style = Stroke(outlineLineWidth.toPx())
                    )
                }
                .dragZoom(state,  {transformation}, lockX = lockX, lockY = lockY)
        ) {
            state.Draw(transformation = transformation)
        }
    }

}

@Preview(widthDp = 300, heightDp = 500, showBackground = true)
@Composable
private fun PlotPreview() {
    TunerTheme {
        val textLabelHeight = rememberTextLabelHeight()
        val textLabelWidth = rememberTextLabelWidth("XXXXXX")
        val state = remember{
            PlotState(
                initialViewPortRaw = Rect(left = 2f, top = 20f, right = 10f, bottom = 3f),
                initialViewPortRawLimits = Rect(left = -1f, top = 100f, right = 40f, bottom = -2f)
            ).apply {
                setLine(0, floatArrayOf(3f, 5f, 7f, 9f), floatArrayOf(4f, 8f, 6f, 15f)) {MaterialTheme.colorScheme.primary}
                setPoint(0,Offset(3f, 4f), Point.drawCircle(10.dp) { MaterialTheme.colorScheme.primary })
                setHorizontalMarks(
                    0,
                    listOf(
                        floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 15f, 20f)
                    ).toImmutableList(),
                    maxLabelHeight = { textLabelHeight },
                    horizontalLabelPosition = 0.5f,
                    anchor = Anchor.Center, //Anchor.East,
                    lineWidth = 1.dp,
                    clipLabelToPlotWindow = true,
                    lineColor = { MaterialTheme.colorScheme.primary },
                ) { modifier, level, index, value ->
                    Text("$index, $value, $level",
                        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer))
                }
                setVerticalMarks(
                    0,
                    listOf(
                        floatArrayOf(0f, 3f, 8f, 15f)
                    ).toImmutableList(),
                    maxLabelWidth = { textLabelWidth },
                    verticalLabelPosition = 0.9f,
                    anchor = Anchor.Center, //Anchor.East,
                    lineWidth = 1.dp,
                    clipLabelToPlotWindow = true,
                    lineColor = { MaterialTheme.colorScheme.primary },
                ) { modifier, level, index, value ->
                    Text("$index, $value",
                        modifier = modifier.background(Color.Cyan))
                }
                setPointMark(0, Offset(5f, 12f)) {
                    Text("XXX", it.background(MaterialTheme.colorScheme.error))
                }
            }

        }

        Column {
            Button(onClick = {state.resetViewPort(PlotState.TargetTransitionType.Animate)}) {
                Text("Reset")
            }
            Plot(
                state,
                plotWindowPadding = DpRect(left = 5.dp, top = 10.dp, right = 8.dp, bottom = 3.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}