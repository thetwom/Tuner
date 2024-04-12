package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxOfOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

fun PointerEvent.calculateCentroidSizeComponentWise(useCurrent: Boolean = true): Size {
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
fun Modifier.dragZoom(
    state: PlotState, transformation: () -> Transformation,
    lockX: Boolean = false, lockY: Boolean = false
    ): Modifier {
    return this then if (lockX && lockY) {
        Modifier
    } else if (lockX) {
        pointerInput(state) {
//            val decay = exponentialDecay<Rect>(1f)
            detectPanZoomFlingGesture(
                onGestureStart = {
                    state.stopViewPortAnimation()
                },
                onGesture = { centroid, pan, zoom ->
                    val t = transformation()
                    val modifiedTopLeft = Offset(
                        0f,
                        (t.viewPortScreen.top - centroid.y) / zoom.height + centroid.y - pan.y,
                    )
                    //Log.v("Tuner", "Plot: original topLeft=$originalTopLeft, modified topLeft=$modifiedTopLeft, zoom=$zoom")
                    val zoomedHeight= state.viewPortRaw.size.height / zoom.height

                    val movedTopLeftRaw = t.toRaw(modifiedTopLeft)
                    val newViewPortRaw = t.viewPortRaw.copy(
                        top = movedTopLeftRaw.y,
                        bottom = movedTopLeftRaw.y + zoomedHeight,
                    )
                    state.setViewPort(
                        newViewPortRaw,
                        PlotState.TargetTransitionType.Snap,
                        PlotState.BoundsMode.Gesture
                    )
                },
                onFling = { velocity ->
                    val velocityRaw = transformation().toRaw(velocity)
                    state.flingViewPort(velocityRaw.copy(x = 0f), PlotState.BoundsMode.Gesture)
//                    val t = transformation()
//                    val velocityRaw = (t.toRaw(Offset.Zero) - t.toRaw(Offset(0f, velocity.y)))
//                    state.viewPortRawAnimation.animateDecay(
//                        Rect(0f, velocityRaw.y, 0f, velocityRaw.y), decay
//                    )
                }
            )
        }
    } else if (lockY) {
        pointerInput(state) {
//            val decay = exponentialDecay<Rect>(1f)
            detectPanZoomFlingGesture(
                onGestureStart = {
                    state.stopViewPortAnimation()
                },
                onGesture = { centroid, pan, zoom ->
                    val t = transformation()
                    val modifiedTopLeft = Offset(
                        (transformation().viewPortScreen.left - centroid.x) / zoom.width + centroid.x - pan.x,
                        0f
                    )
                    //Log.v("Tuner", "Plot: original topLeft=$originalTopLeft, modified topLeft=$modifiedTopLeft, zoom=$zoom")
                    val zoomedWidth= state.viewPortRaw.size.width / zoom.width

                    val movedTopLeftRaw = t.toRaw(modifiedTopLeft)
                    val newViewPortRaw = t.viewPortRaw.copy(
                        left = movedTopLeftRaw.x,
                        right = movedTopLeftRaw.x + zoomedWidth,
                    )
                    state.setViewPort(
                        newViewPortRaw,
                        PlotState.TargetTransitionType.Snap,
                        PlotState.BoundsMode.Gesture
                    )
                },
                onFling = { velocity ->
                    //val t = transformation()
                    //val velocityRaw = (t.toRaw(Offset.Zero) - t.toRaw(Offset(velocity.x, 0f)))
                    val velocityRaw = transformation().toRaw(velocity)
                    state.flingViewPort(velocityRaw.copy(y = 0f), PlotState.BoundsMode.Gesture)
//                    state.viewPortRawAnimation.animateDecay(
//                        Rect(velocityRaw.x, 0f, velocityRaw.x, 0f), decay
//                    )
                }
            )
        }
    } else {
        pointerInput(state) {
//            val decay = exponentialDecay<Rect>(1f)
            detectPanZoomFlingGesture(
                onGestureStart = {
                    state.stopViewPortAnimation()
                },
                onGesture = { centroid, pan, zoom ->
                    val t = transformation()
                    val modifiedTopLeft = Offset(
                        (t.viewPortScreen.left - centroid.x) / zoom.width + centroid.x - pan.x,
                        (t.viewPortScreen.top - centroid.y) / zoom.height + centroid.y - pan.y,
                    )
                    //Log.v("Tuner", "Plot: original topLeft=$originalTopLeft, modified topLeft=$modifiedTopLeft, zoom=$zoom")
                    val zoomedSize = Size(
                        state.viewPortRaw.size.width / zoom.width,
                        state.viewPortRaw.size.height / zoom.height
                    )

                    val movedTopLeftRaw = t.toRaw(modifiedTopLeft)
                    state.setViewPort(
                        Rect(movedTopLeftRaw, zoomedSize),
                        PlotState.TargetTransitionType.Snap,
                        PlotState.BoundsMode.Gesture
                    )
                },
                onFling = { velocity ->
                    val velocityRaw = transformation().toRaw(velocity)
                    state.flingViewPort(velocityRaw, PlotState.BoundsMode.Gesture)
//                    val t = transformation()
//                    val velocityRaw = (t.toRaw(Offset.Zero) - t.toRaw(Offset(velocity.x, velocity.y)))
//                    state.viewPortRawAnimation.animateDecay(
//                        Rect(velocityRaw.x, velocityRaw.y, velocityRaw.x, velocityRaw.y), decay
//                    )
                }
            )
        }
    }
}

