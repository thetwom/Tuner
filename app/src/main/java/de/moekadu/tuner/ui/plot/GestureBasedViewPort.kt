package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Velocity
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

private fun restrictViewPortToLimits(viewPort: Rect, viewPortLimits: Rect?): Rect {
    // the restriction process does not just coerce the values in the min/max, since
    // when dragging and reaching bounds, we would start zooming.
    return if (viewPortLimits == null) {
        viewPort
    } else {
        // shrink size if necessary
        var limitedViewPort = Rect(
            left = viewPort.left,
            top = viewPort.top,
            right = if (viewPort.width.absoluteValue > viewPortLimits.width) {
                if (viewPort.width > 0) viewPort.left + viewPortLimits.width else viewPort.left - viewPortLimits.width
            } else {
                viewPort.right
            },
            bottom = if (viewPort.height.absoluteValue > viewPortLimits.height) {
                if (viewPort.height > 0) viewPort.top + viewPortLimits.height else viewPort.top - viewPortLimits.height
            } else {
                viewPort.bottom
            },
        )

        // make sure, that left/top don't exceed limits
        val translateToMatchX = max(0f, viewPortLimits.left - min(viewPort.left, viewPort.right))
        val translateToMatchY = max(0f, viewPortLimits.top - min(viewPort.top, viewPort.bottom))
        limitedViewPort = limitedViewPort.translate(translateToMatchX, translateToMatchY)
        // make sure, that right/bottom don't exceed limits
        val translateToMatchX2 = min(0f, viewPortLimits.right - max(viewPort.left, viewPort.right))
        val translateToMatchY2 = min(0f, viewPortLimits.bottom - max(viewPort.top, viewPort.bottom))
        limitedViewPort = limitedViewPort.translate(translateToMatchX2, translateToMatchY2)

        return limitedViewPort
    }
}

class GestureBasedViewPort {
    var isActive: Boolean by mutableStateOf(false)
        private set
    var viewPort: Rect by mutableStateOf(Rect.Zero)
        private set
    private val animationDecay = exponentialDecay<Rect>(1f)
    private val viewPortRawAnimation = Animatable(viewPort, Rect.VectorConverter)

    suspend fun finish() {
        viewPortRawAnimation.stop()
        isActive = false
    }
    suspend fun setViewPort(value: Rect, limits: Rect?) {
        viewPortRawAnimation.stop()
        isActive = true
        viewPort = restrictViewPortToLimits(value, limits)
    }
    suspend fun flingViewPort(velocity: Velocity, limits: Rect?) {
        isActive = true
        viewPortRawAnimation.snapTo(viewPort)
        viewPortRawAnimation.animateDecay(
            Rect(velocity.x, velocity.y, velocity.x, velocity.y), animationDecay
        ) {
            val targetInLimits = restrictViewPortToLimits(value, limits)
            viewPort = targetInLimits
        }
    }
}