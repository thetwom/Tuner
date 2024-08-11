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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private fun Rect.scale(
    scaleX: Float = 1f, scaleY: Float = 1f,
    centerX: Float = 0f, centerY: Float = 0f
): Rect {
    return Rect(
        left = (this.left - centerX) * scaleX + centerX,
        top = (this.top - centerY) * scaleY + centerY,
        right = (this.right - centerX) * scaleX + centerX,
        bottom = (this.bottom - centerY) * scaleY + centerY,
    )
}

private fun Rect.fitInto(limits: Rect?): Rect {
    if (limits == null)
        return this
    return Rect(
        left = this.left.coerceIn(limits.left, limits.right),
        top = this.top.coerceIn(limits.top, limits.bottom),
        right = this.right.coerceIn(limits.left, limits.right),
        bottom = this.bottom.coerceIn(limits.top, limits.bottom),
    )
}

private fun Rect.translateWithinLimits(translateX: Float, translateY: Float, limits: Rect?): Rect {
    if (limits == null)
        return this.translate(translateX, translateY)
    val xLimited = if (translateX < 0f) {
        val left = min(this.left, this.right)
        max(limits.left - left, translateX)
    } else {
        val right = max(this.left, this.right)
        min(limits.right - right, translateX)
    }

    val yLimited = if (translateY < 0f) {
        val top = min(this.top, this.bottom)
        max(limits.top - top, translateY)
    } else {
        val bottom = max(this.top, this.bottom)
        min(limits.bottom - bottom, translateY)
    }
    return this.translate(xLimited, yLimited)
}

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
    state: GestureBasedViewPort,
    limits: () -> Rect?,
    transformation: () -> Transformation,
    lockX: Boolean = false,
    lockY: Boolean = false
): Modifier {
    return this then if (lockX && lockY) {
        Modifier
    } else if (lockX) {
        Modifier.pointerInput(state) {
            detectPanZoomFlingGesture(
                onGestureStart = {
                    state.setViewPort(transformation().viewPortRaw, null)
                },
                onGesture = { centroid, pan, zoom ->
                    val t = transformation()
                    val panRaw = t.toRaw(pan) - t.toRaw(Offset.Zero)
                    val centroidRaw = t.toRaw(centroid)
                    val transformed = state.viewPort
                        .scale(scaleY = 1.0f / zoom.height, centerY = centroidRaw.y)
                        .fitInto(limits())
                        .translateWithinLimits(0f, translateY = -panRaw.y, limits())
                    state.setViewPort(transformed, null)

                },
                onFling = { velocity ->
                    val velocityRaw = transformation().toRaw(velocity)
                    state.flingViewPort(velocityRaw.copy(x = 0f), limits())
                }
            )
        }
    } else if (lockY) {
        Modifier.pointerInput(state) {
            detectPanZoomFlingGesture(
                onGestureStart = {
                    state.setViewPort(transformation().viewPortRaw, null)
                },
                onGesture = { centroid, pan, zoom ->
                    val t = transformation()
                    val panRaw = t.toRaw(pan) - t.toRaw(Offset.Zero)
                    val centroidRaw = t.toRaw(centroid)
                    val transformed = state.viewPort
                        .scale(scaleX = 1.0f / zoom.width, centerX = centroidRaw.x)
                        .fitInto(limits())
                        .translateWithinLimits(-panRaw.x, 0f, limits())
                    state.setViewPort(transformed, null)
                },
                onFling = { velocity ->
                    val velocityRaw = transformation().toRaw(velocity)
                    state.flingViewPort(velocityRaw.copy(y = 0f), limits())
                }
            )
        }
    } else {
        Modifier.pointerInput(state) {
            detectPanZoomFlingGesture(
                onGestureStart = {
                    state.setViewPort(transformation().viewPortRaw, null)
                },
                onGesture = { centroid, pan, zoom ->
                    val t = transformation()
                    val panRaw = t.toRaw(pan) - t.toRaw(Offset.Zero)
                    val centroidRaw = t.toRaw(centroid)
                    val transformed = state.viewPort
                        .scale(
                            scaleX = 1.0f / zoom.width, scaleY = 1.0f / zoom.height,
                            centerX = centroidRaw.x, centerY = centroidRaw.y
                        )
                        .fitInto(limits())
                        .translateWithinLimits(-panRaw.x, -panRaw.y, limits())
                    state.setViewPort(transformed, null)
                },
                onFling = { velocity ->
                    val velocityRaw = transformation().toRaw(velocity)
                    state.flingViewPort(velocityRaw, limits())
                }
            )
        }
    }
}

