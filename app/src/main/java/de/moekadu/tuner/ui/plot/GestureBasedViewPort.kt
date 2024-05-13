package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Velocity

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