package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class PlotState(
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

    private val items = mutableStateMapOf<Int, PlotItem>()

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
    fun setLine(
        key: Int,
        coordinates: Line.Coordinates? = null,
        styleKey: Int? = null,
    ) {
        val item = items[key]
        if (item is Line) {
            item.modify(coordinates, styleKey)
        } else {
            val newItem = Line(coordinates, styleKey ?: 0)
            items[key] = newItem
        }
    }

    fun setPoint(key: Int, position: Offset? = null, styleKey: Int? = null) {
        val item = items[key]
        if (item is Point) {
            item.modify(position, styleKey)
        } else {
            val newItem = Point(position, styleKey ?: 0)
            items[key] = newItem
        }
        //points.setPoint(key, position, content)
    }

    fun setXTicks(
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
        val tickLevels = TickLevelExplicitRanges(xValues)
        items[key] = XTicks(
            label = label,
            tickLevel = tickLevels,
            anchor = anchor,
            verticalLabelPosition = verticalLabelPosition,
            lineWidth = lineWidth,
            lineColor = lineColor,
            maxLabelWidth = maxLabelWidth,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            maxNumLabels = maxNumLabels
        )
    }
    fun setYTicks(
        key: Int,
        yValues: ImmutableList<FloatArray>,
        maxLabelHeight: @Composable Density.() -> Float,
        clipLabelToPlotWindow: Boolean = false,
        maxNumLabels: Int = -1, // -1 is auto
        styleKey: Int? = null,
        label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)? = null
    ) {
        val tickLevels = TickLevelExplicitRanges(yValues)
        items[key] = YTicks(
            styleKey = styleKey,
            label = label,
            tickLevel = tickLevels,
            maxLabelHeight = maxLabelHeight,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            maxNumLabels = maxNumLabels
        )
    }

    fun addHorizontalMarks(
        key: Int,
        marks: ImmutableList<HorizontalMark>,
        clipLabel: Boolean = false,
        sameSizeLabels: Boolean = false
    ) {
        items[key] = HorizontalMarkGroup(marks, clipLabel, sameSizeLabels)
    }

    fun modifyHorizontalMark(
        key: Int, markIndex: Int,
        position: Float? = null,
        styleKey: Int? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        val marks = items[key] as HorizontalMarkGroup
        marks.modify(markIndex, position, styleKey, content)
    }

    fun addVerticalMarks(
        key: Int,
        marks: ImmutableList<VerticalMark>,
        clipLabel: Boolean = false,
        sameSizeLabels: Boolean = false
    ) {
        items[key] = VerticalMarkGroup(marks, clipLabel, sameSizeLabels)
    }

    fun modifyVerticalMark(
        key: Int, markIndex: Int,
        position: Float? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        verticalLabelPosition: Float? = null,
        lineWidth: Dp? = null,
        lineColor: (@Composable () -> Color)? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        val marks = items[key] as VerticalMarkGroup
        marks.modify(markIndex,
            position, anchor, screenOffset, verticalLabelPosition, lineWidth,
            lineColor, content
        )
    }

    fun addPointMarks(key: Int, marks: ImmutableList<PointMark>) {
        items[key] = PointMarkGroup(marks)
    }

    fun modifyPointMark(
        key: Int, markIndex: Int,
        position: Offset? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null
    ) {
        val marks = items[key] as PointMarkGroup
        marks.modify(markIndex, position, anchor, screenOffset, content)
    }

    @Composable
    fun DrawClipped(transformation: Transformation, plotStyles: ImmutableMap<Int, PlotStyle>) {
        val clipShape = transformation.rememberClipShape()
        Box(modifier = Modifier.fillMaxSize().clip(clipShape)) {
            items.asSequence().filter { it.value.hasClippedDraw }.forEach {
                it.value.DrawClipped(transformation = transformation, plotStyles)
            }
        }
    }

    @Composable
    fun DrawUnclipped(transformation: Transformation, plotStyles: ImmutableMap<Int, PlotStyle>) {
        Box(modifier = Modifier.fillMaxSize()) {
            items.asSequence().filter { it.value.hasUnclippedDraw }.forEach {
                it.value.DrawUnclipped(transformation = transformation, plotStyles = plotStyles)
            }
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
    lockX: Boolean = false, lockY: Boolean = false,
    plotStyles: ImmutableMap<Int, PlotStyle>
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

        state.DrawClipped(transformation = transformation, plotStyles = plotStyles)
        state.DrawUnclipped(transformation = transformation, plotStyles = plotStyles)

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

@Preview(widthDp = 300, heightDp = 500, showBackground = true)
@Composable
private fun PlotPreview() {
    TunerTheme {
        val textLabelHeight = rememberTextLabelHeight()
        val textLabelWidth = rememberTextLabelWidth("XXXXXX")
        val plotStyles = persistentMapOf<Int, PlotStyle>(
            0 to Line.Style(MaterialTheme.colorScheme.primary, 2.dp),
            1 to Line.Style(MaterialTheme.colorScheme.error, 5.dp),
            2 to Point.circleShape(10.dp, MaterialTheme.colorScheme.primary),
            10 to HorizontalMark.Style(anchor = Anchor.West, 0.1f),
            11 to HorizontalMark.Style(anchor = Anchor.West, 0.3f),
            100 to YTicks.Style(
                horizontalLabelPosition = 0.5f,
                anchor = Anchor.Center, //Anchor.East,
                lineWidth = 1.dp,
                lineColor = MaterialTheme.colorScheme.primary
            ),
        )
        val state = remember{
            PlotState(
                viewPortRaw = Rect(left = 2f, top = 20f, right = 10f, bottom = 3f),
                viewPortRawLimits = Rect(left = -1f, top = 100f, right = 40f, bottom = -2f)
            ).apply {
                setLine(
                    key = 0,
                    coordinates = Line.Coordinates(
                        floatArrayOf(3f, 5f, 7f, 9f), floatArrayOf(4f, 8f, 6f, 15f)
                    ),
                    styleKey = 0
                )
                setLine(
                    key = 1,
                    coordinates = Line.Coordinates(
                        floatArrayOf(3f, 5f, 7f, 9f), floatArrayOf(15f, 18f, 12f, 10f)
                    ),
                    styleKey = 1
                )
                setPoint(10, Offset(3f, 4f), styleKey = 2)

                setYTicks(
                    101,
                    listOf(
                        floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 15f, 20f)
                    ).toImmutableList(),
                    maxLabelHeight = { textLabelHeight },
                    clipLabelToPlotWindow = true,
                    styleKey = 100
                ) { modifier, level, index, value ->
                    Text("$index, $value, $level",
                        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer))
                }
                setXTicks(
                    100,
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
                addPointMarks(1010,
                    persistentListOf(
                        PointMark(Offset(5f, 12f)) {
                            Text("XXX", it.background(MaterialTheme.colorScheme.error))
                        }
                    )
                )
                addHorizontalMarks(
                    1001,
                    persistentListOf(
                        HorizontalMark(12f, styleKey = 10) {
                            Text("M 12", modifier = it.background(MaterialTheme.colorScheme.primary), color = MaterialTheme.colorScheme.onPrimary)
                        },
                        HorizontalMark(11f, styleKey = 11) {
                            Text("M 11 :-)", modifier = it.background(MaterialTheme.colorScheme.primary), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    ),
                    sameSizeLabels = true
                )
                addVerticalMarks(
                    2001,
                    persistentListOf(
                        VerticalMark(5f, anchor = Anchor.West, 0.7f) {
                            Text("MV 5", modifier = it.background(MaterialTheme.colorScheme.primary), color = MaterialTheme.colorScheme.onPrimary)
                        },
                        VerticalMark(7f, anchor = Anchor.East, 0.3f) {
                            Text("M 7 :-)", modifier = it.background(MaterialTheme.colorScheme.primary), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    ),
                    sameSizeLabels = true
                )
            }

        }

        Column {
            Button(onClick = {state.resetViewPort(PlotState.TargetTransitionType.Animate)}) {
                Text("Reset")
            }
            Plot(
                state,
                plotWindowPadding = DpRect(left = 5.dp, top = 10.dp, right = 8.dp, bottom = 3.dp),
                modifier = Modifier.fillMaxWidth(),
                plotStyles = plotStyles
            )
        }
    }
}