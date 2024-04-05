package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
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
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxOfOrNull
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

enum class TargetTransitionType {
    Animate,
    Snap
}
data class ViewPortTransition(
    val targetViewPort: Rect,
    val transition: TargetTransitionType
)

class PlotState(
    initialViewPortRaw: Rect
) {
    private val _viewPortRawTransition = MutableStateFlow(
        ViewPortTransition(initialViewPortRaw, TargetTransitionType.Snap)
    )
    val viewPortRawTransition get() = _viewPortRawTransition.asStateFlow()

    val viewPortRawAnimation = Animatable(initialViewPortRaw, Rect.VectorConverter)
    val viewPortRaw get() = viewPortRawAnimation.value

    private val lines = LineGroup()
    private val points = PointGroup()
    private val horizontalMarks = mutableMapOf<Int, HorizontalMarks>()
    private val verticalMarks = mutableMapOf<Int, VerticalMarks>()
    private val pointMarks = PointMarkGroup()

    fun snapViewPortTo(target: Rect) {
        _viewPortRawTransition.value = ViewPortTransition(target, TargetTransitionType.Snap)
    }

    fun animateViewPortTo(target: Rect) {
        _viewPortRawTransition.value = ViewPortTransition(target, TargetTransitionType.Animate)
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
    val color: Color
)

object PlotDefaults {
    @Composable
    fun windowOutline(
        lineWidth: Dp = 1.dp,
        cornerRadius: Dp = 8.dp,
        color: Color = MaterialTheme.colorScheme.onSurface
    ) = PlotWindowOutline(lineWidth, cornerRadius, color)
}

fun PointerEvent.calculateCentroidSizeComponentWise(useCurrent: Boolean = true):  Size {
    val centroid = calculateCentroid(useCurrent)
    if (centroid == Offset.Unspecified) {
        return Size.Zero
    }

    var distanceToCentroidX = 0f
    var distanceToCentroidY = 0f
    var distanceWeight = 0
    changes.fastForEach { change ->
        if (change.pressed && change.previousPressed) {
            val position = if (useCurrent) change.position else change.previousPosition
            distanceToCentroidX += (position.x - centroid.x).absoluteValue
            distanceToCentroidY += (position.y - centroid.y).absoluteValue
            distanceWeight++
        }
    }
//    Log.v("Tuner", "Plot: centroid size = $distanceToCentroidX, $distanceToCentroidY")
    return Size(
        distanceToCentroidX / distanceWeight.toFloat(),
        distanceToCentroidY / distanceWeight.toFloat()
    )
}

/** Compute zoom component wise for x and y.
 *  @param minimumCentroidSize The required centroid size to calculate a zoom value in pixels.
 *    Unlimited small value would make the zoom behavior very sensitive.
 *  @param minimumAspectRatioForSingleDirectionZoom Aspect ratio of centroid for single direction
 *    zoom. This "locks" in to only x or only z zoom if the aspect ratio of the centroid is larger
 *    than the given value.
 *  @return Zoom in x and z direction (width and height)
 */
fun PointerEvent.calculateZoomComponentWise(
    minimumCentroidSize: Int,
    minimumAspectRatioForSingleDirectionZoom: Float = 3f
): Size {
    val currentCentroidSize = calculateCentroidSizeComponentWise(useCurrent = true)
    val previousCentroidSize = calculateCentroidSizeComponentWise(useCurrent = false)

    val x = currentCentroidSize.width
    val y = currentCentroidSize.height
    val xPrev = previousCentroidSize.width
    val yPrev = previousCentroidSize.height

    val zoomX = if (x * xPrev > 0 && x > minimumCentroidSize && xPrev > minimumCentroidSize)
        x / xPrev
    else
        1f
    val zoomY = if (y * yPrev > 0 && y > minimumCentroidSize && yPrev > minimumCentroidSize)
        y / yPrev
    else
        1f

    return if (x > minimumAspectRatioForSingleDirectionZoom * y)
        Size(zoomX, 1f)
    else if (y > minimumAspectRatioForSingleDirectionZoom * x)
        Size(1f, zoomY)
    else
        Size(zoomX, zoomY)
}

suspend fun PointerInputScope.detectPanZoomFlingGesture(
    onGestureStart: suspend () -> Unit,
    onGesture: suspend (centroid: Offset, pan: Offset, zoom: Size) -> Unit,
    onFling: suspend (velocity: Velocity) -> Unit,
    minimumCentroidSize: Dp = 5.dp,
    maximumFlingVelocity: Dp = 4000.dp  // dp / sec
) = coroutineScope {
    awaitEachGesture {
        var zoomX = 1f
        var zoomY = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        val velocityTracker = VelocityTracker()
        val maximumVelocity = maximumFlingVelocity.toPx()
        var numPointers = 0

        awaitFirstDown(requireUnconsumed = false)
        launch { onGestureStart() }
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed } //&& !event.changes.fastAny { it.pressed }
            if (!canceled) {
                val zoomChange = event.calculateZoomComponentWise(minimumCentroidSize.roundToPx())
                val centroid = event.calculateCentroid(useCurrent = true)

//                Log.v("Tuner", "Plot: zoom change = $zoomChange")
                val panChange = event.calculatePan()

                // track velocity
                if (centroid != Offset.Unspecified) {
                    if (numPointers == event.changes.size) { // is this the best way?
                        velocityTracker.addPosition(event.changes.fastMaxOfOrNull { it.uptimeMillis }
                            ?: 0L, centroid)
                    } else {
                        velocityTracker.resetTracking()
                        numPointers = event.changes.size
                    }
                }

                if (!pastTouchSlop) {
                    zoomX *= zoomChange.width
                    zoomY *= zoomChange.height
                    pan += panChange

                    val centroidSize = event.calculateCentroidSizeComponentWise(useCurrent = false)

                    val zoomMotion = sqrt(
                        ((1 - zoomX) * centroidSize.width).pow(2)
                                + ((1 - zoomY) * centroidSize.height).pow(2)
                    )

                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop)
                        pastTouchSlop = true
                }

                if (pastTouchSlop) {
//                    Log.v("Tuner", "Plot: centroid: $centroid, num pointers = ${event.changes.size}")

                    if (zoomChange.width != 1f  || zoomChange.height != 1f|| panChange != Offset.Zero) {
                        launch { onGesture(centroid, panChange, zoomChange) }
                    }

                    event.changes.fastForEach {
                        if (it.positionChanged())
                            it.consume()
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })

        if (pastTouchSlop) {
            val velocity = velocityTracker.calculateVelocity()
//            Log.v("Tuner", "Plot: velocity = ${velocity}, maximum=$maximumVelocity")
            if (velocity.x.pow(2) + velocity.y.pow(2) < maximumVelocity.pow(2))
                launch { onFling(velocity) }
        }
    }
}


@Composable
fun Plot(
    state: PlotState,
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
private fun PlotPreview() {
    TunerTheme {
        val textLabelHeight = rememberTextLabelHeight()
        val textLabelWidth = rememberTextLabelWidth("XXXXXX")
        val state = remember{
            PlotState(
                initialViewPortRaw = Rect(left = 2f, top = 20f, right = 10f, bottom = 3f)
            ).apply {
                setLine(0, floatArrayOf(3f, 5f, 7f, 9f), floatArrayOf(4f, 8f, 6f, 15f))
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
                        modifier = modifier.background(Color.Cyan))
                }
                setVerticalMarks(
                    0,
                    listOf(
                        floatArrayOf(-5f, 3f, 8f, 15f)
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

        Plot(
            state,
            plotWindowPadding = DpRect(left = 5.dp, top = 10.dp, right = 8.dp, bottom = 3.dp )
        )
    }
}