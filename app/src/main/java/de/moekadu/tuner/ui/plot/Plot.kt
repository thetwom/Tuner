package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxOfOrNull
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
data class PlotItemProvider(
    val state: PlotStateImpl,
    val viewPortScreen: IntRect,
    val viewPortCornerRadius: Float
) : LazyLayoutItemProvider {

    override val itemCount get() = state.itemCount

    val transformation = Transformation(viewPortScreen, state.viewPortRaw, viewPortCornerRadius)

    fun getVisiblePlotItems(density: Density): Sequence<PlotItemPositioned> {
        return state.getVisibleItems(transformation, density)
    }

    fun positionOf(index: Int, localDensity: Density): IntOffset {
        val boundingBoxRaw = state[index].boundingBox.value
        val topLeftRaw = Offset(
            if (boundingBoxRaw.left == Float.NEGATIVE_INFINITY) 0f else boundingBoxRaw.left,
            if (boundingBoxRaw.top == Float.POSITIVE_INFINITY) 0f else boundingBoxRaw.top,
        )
        val topLeftScreen = transformation.toScreen(topLeftRaw)
        val extraExtents = state[index].getExtraExtentsScreen(localDensity)
        val extraLeft = extraExtents.left
        val extraTop = extraExtents.top
        return IntOffset(
            if (boundingBoxRaw.left == Float.NEGATIVE_INFINITY)
                0
            else
                (topLeftScreen.x - extraLeft).roundToInt(),
            if (boundingBoxRaw.top == Float.POSITIVE_INFINITY)
                0
            else
                (topLeftScreen.y - extraTop).roundToInt()
        )
    }

    @Composable
    override fun Item(index: Int, key: Any) { // can we use the key to get quicker access to the item?
        val localDensity = LocalDensity.current
        val localTransformation = remember(transformation, state[index].boundingBox) {
            // Necessary changes here: not only define a rawToScreenMatrix, but a complete
            //   transformation. this should also contain info about the screen viewport
            //   (where are the bounds?)
            val contentPosition = positionOf(index, localDensity)
            val screenPosition = transformation.viewPortScreen.topLeft
            val screenPositionRelative = screenPosition - contentPosition //- screenPosition

            val screenSize = transformation.viewPortScreen.size
//            Log.v("Tuner", "Plot: Item: contentPosition=$contentPosition, screenPosition=$screenPosition, screenRelativ=$screenPositionRelative, sSize=${transformation.viewPortScreen}, sSizeLoc=${screenSize}")
            Transformation(
                IntRect(screenPositionRelative, screenSize),
                transformation.viewPortRaw,
                transformation.viewPortCornerRadius
                )
//            Matrix().apply {
//                val originRaw = transformation.viewPortRaw.topLeft
//                val p = positionOf(index, localDensity).toOffset()
//                val pRaw = transformation.toRaw(p)
//                val translationVector = originRaw - pRaw
//                setFrom(transformation.matrixRawToScreen)
//                translate(translationVector.x, translationVector.y)
//            }
        }
        //state[index].Item(transformToScreen = localTransformation)
        state[index].Item(localTransformation)
    }
}

private data class PositionedPlaceable(
    val placeable: Placeable,
    val positionX: Int,
    val positionY: Int,
)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberPlotMeasurePolicy(
    itemProviderLambda: () -> PlotItemProvider
): LazyLayoutMeasureScope.(Constraints) -> MeasureResult {
    return remember {
        { containerConstraints ->
            val itemProvider = itemProviderLambda()

            val density = this as Density

            val placeables = itemProvider.getVisiblePlotItems(density).map {
                val bb = it.boundingBox
                val l = if (bb.left == Float.NEGATIVE_INFINITY) 0 else bb.left.roundToInt()
                val r = if (bb.right == Float.POSITIVE_INFINITY) containerConstraints.maxWidth else bb.right.roundToInt()
                val t = if (bb.top == Float.NEGATIVE_INFINITY) 0 else bb.top.roundToInt()
                val b = if (bb.bottom == Float.POSITIVE_INFINITY) containerConstraints.maxHeight else bb.bottom.roundToInt()

                PositionedPlaceable(
                    measure(
                        it.globalIndex,
                        Constraints.fixed(
                            r - l,
                            b - t
                        )
                    )[0],
                    positionX =  l, positionY = t
                )
            }.toList()

            layout(containerConstraints.maxWidth, containerConstraints.maxHeight) {
                placeables.forEach {
                    it.placeable.place(it.positionX, it.positionY)
                }
            }
        }
    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Plot(
    state: PlotState,
    modifier: Modifier = Modifier,
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotDefaults.windowOutline(),
    onViewPortChanged: (viewPortScreen: Rect) -> Unit = {}
) {
    BoxWithConstraints(modifier = modifier.background(Color.LightGray)) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

        val outline2 = with(LocalDensity.current) { (plotWindowOutline.lineWidth / 2).roundToPx() }
        val viewPortScreen = with(LocalDensity.current) {
            IntRect(
                plotWindowPadding.left.roundToPx() + outline2,
                plotWindowPadding.top.roundToPx() + outline2,
                widthPx - plotWindowPadding.right.roundToPx() - outline2,
                heightPx - plotWindowPadding.bottom.roundToPx() - outline2
            )
        }
        val cornerRadiusPx = with(LocalDensity.current) {plotWindowOutline.cornerRadius.toPx()}
        // use updated state here, to avoid having to recreate of the pointerInput modifier
        val itemProvider by rememberUpdatedState(
            PlotItemProvider(state.state, viewPortScreen, cornerRadiusPx)
        )

//        Log.v("Tuner", "Plot: screen size: $widthPx x $heightPx")
        val animatedRectRaw = remember {
            Animatable(state.state.viewPortRaw, Rect.VectorConverter)
        }

        val measurePolicy = rememberPlotMeasurePolicy { itemProvider }

        val channel = remember { Channel<PlotStateImpl.TargetRectForAnimation>(Channel.CONFLATED) }
        SideEffect {
            state.state.targetRectForAnimation?.let { channel.trySend(it) }
        }

        LaunchedEffect(channel) {
            var targetRect: PlotStateImpl.TargetRectForAnimation? = null
            for (target in channel) {
                if (target != targetRect) {
                    targetRect = target
                    animatedRectRaw.animateTo(target.target)
                }
            }
        }

        LaunchedEffect(animatedRectRaw) {
            snapshotFlow { animatedRectRaw.value }.collect { onViewPortChanged(it) }
        }

        LazyLayout(
            itemProvider = { itemProvider },
            measurePolicy = measurePolicy,
            modifier = Modifier
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        plotWindowOutline.color,
                        viewPortScreen.topLeft.toOffset(),
                        viewPortScreen.size.toSize(),
                        cornerRadius = CornerRadius(plotWindowOutline.cornerRadius.toPx()),
                        style = Stroke(plotWindowOutline.lineWidth.toPx())
                    )
                }
                .pointerInput(Unit) {
                    val decay = exponentialDecay<Rect>(1f)

                    detectPanZoomFlingGesture(
                        onGestureStart = { animatedRectRaw.stop() },
                        onGesture = { centroid, pan, zoom ->
                            val originalTopLeft = itemProvider.viewPortScreen
                            val modifiedTopLeft = Offset(
                                (originalTopLeft.left - centroid.x) / zoom.width + centroid.x - pan.x,
                                (originalTopLeft.top - centroid.y) / zoom.height + centroid.y - pan.y,
                            )
                            //Log.v("Tuner", "Plot: original topLeft=$originalTopLeft, modified topLeft=$modifiedTopLeft, zoom=$zoom")
                            val originalRaw = state.state.viewPortRaw
                            val zoomedSize = Size(
                                originalRaw.size.width / zoom.width,
                                originalRaw.size.height / zoom.height
                            )

                            val movedTopLeftRaw = itemProvider.transformation.toRaw(modifiedTopLeft)
                            val movedRaw = Rect(movedTopLeftRaw, zoomedSize)
                            animatedRectRaw.snapTo(movedRaw)
                        },
                        onFling = { velocity ->
                            val velocityRaw = (
                                    itemProvider.transformation.toRaw(Offset.Zero)
                                            - itemProvider.transformation.toRaw(
                                        Offset(
                                            velocity.x,
                                            velocity.y
                                        )
                                    )
                                    )
                            animatedRectRaw.animateDecay(
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
        )
    }

}

@Preview(widthDp = 150, heightDp = 50, showBackground = true)
@Composable
private fun PlotPreview() {
    TunerTheme {
        val initialRawSize = Rect(0f, 30f, 30f, 0f)
        val plotState = remember {
            PlotState.create(initialRawSize).apply {
                addLine(floatArrayOf(15f, 28f), floatArrayOf(15f, 28f), 2.dp)
                addLine(floatArrayOf(15f, 2f), floatArrayOf(15f, 28f), 2.dp, { MaterialTheme.colorScheme.error })
                addLine(floatArrayOf(3f, 10f, 20f), floatArrayOf(2f, 10f, 5f), 2.dp)
                addHorizontalMarks(
                    floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 15f, 20f),
                    //floatArrayOf(0f),
                    maxLabelHeight = {d -> with(d){20.dp.toPx()}},
                    horizontalLabelPosition = 0.5f,
                    anchor = Anchor.Center, //Anchor.East,
                    lineWidth = 1.dp,
                    clipLabelToPlotWindow = true,
                    lineColor = {_, _ -> MaterialTheme.colorScheme.primary},
                ) { index, value ->
                    Text("$index, $value",
                        modifier = Modifier.background(Color.Cyan))
                }
            }
//            mutableStateOf(
//                PlotState.create(1, initialRawSize)
//                    .addLine(floatArrayOf(15f, 28f), floatArrayOf(15f, 28f), 2.dp)
//                    .addLine(floatArrayOf(15f, 2f), floatArrayOf(15f, 28f), 2.dp)
//                    .addLine(floatArrayOf(3f, 10f, 20f), floatArrayOf(2f, 10f, 5f), 2.dp)
//                    .addHorizontalMarks(floatArrayOf(10f, 13f), 0.5f, Anchor.North) { index, yPosition ->
//                        Label(
//                            content = { Text("$index", style = MaterialTheme.typography.bodySmall) },
//                            modifier = Modifier.padding(horizontal = 8.dp)
//                        )
//                    }
//            )
        }
        Plot(plotState,
            onViewPortChanged = { plotState.setViewPort(it) },
            plotWindowPadding = DpRect(left = 80.dp, top = 3.dp, right = 10.dp, bottom = 10.dp),
            plotWindowOutline = PlotDefaults.windowOutline(lineWidth = 4.dp),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                plotState.animateToViewPort(Rect(10f, 40f, 40f, 10f))
            }
        )

    }
}