package de.moekadu.tuner.ui.plot2

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import de.moekadu.tuner.ui.plot.Transformation

class Point2(
    val position: Offset,
    val shape: @Composable Scope.() -> Unit,
    val transformation: Transformation
) {
    interface Scope {
        fun drawShape(draw: DrawScope.() -> Unit)
    }

    class ScopeImpl : Scope {
        var draw: (DrawScope.() -> Unit)? = null
        override fun drawShape(draw: DrawScope.() -> Unit) {
            this.draw = draw
        }
    }
}