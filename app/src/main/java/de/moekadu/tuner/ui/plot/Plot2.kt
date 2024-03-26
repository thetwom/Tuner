package de.moekadu.tuner.ui.plot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.channels.Channel

class PlotState2(
    val viewPortRaw: Rect,
    val targetRectForAnimation: TargetRectForAnimation? = null
) {
    class TargetRectForAnimation(val target: Rect)
}


@Composable
fun Plot2(
    state: PlotState2,
    modifier: Modifier = Modifier,
    plotWindowPadding: DpRect = DpRect(0.dp, 0.dp, 0.dp, 0.dp),
    plotWindowOutline: PlotWindowOutline = PlotDefaults.windowOutline(),
    onViewPortChanged: (viewPortScreen: Rect) -> Unit = {}
){
    BoxWithConstraints(modifier = modifier.background(Color.LightGray)) {
        // TODO: instead of the next few lines, create a function like "remberUpdatedTransformation"
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

        val outline2 = with(LocalDensity.current) { (plotWindowOutline.lineWidth / 2).roundToPx() }
        val viewPortScreen by rememberUpdatedState(
            with(LocalDensity.current) {
                IntRect(
                    plotWindowPadding.left.roundToPx() + outline2,
                    plotWindowPadding.top.roundToPx() + outline2,
                    widthPx - plotWindowPadding.right.roundToPx() - outline2,
                    heightPx - plotWindowPadding.bottom.roundToPx() - outline2
                )
            }
        )
        val cornerRadiusPx = with(LocalDensity.current) {plotWindowOutline.cornerRadius.toPx()}
        // use updated state here, to avoid having to recreate of the pointerInput modifier

//        Log.v("Tuner", "Plot: screen size: $widthPx x $heightPx")
        val animatedRectRaw = remember {
            Animatable(state.viewPortRaw, Rect.VectorConverter)
        }

        val transformation = remember(state, viewPortScreen, cornerRadiusPx) {
            Transformation(viewPortScreen, state.viewPortRaw, cornerRadiusPx)
        }

        val channel = remember { Channel<PlotState2.TargetRectForAnimation>(Channel.CONFLATED) }
        SideEffect {
            state.targetRectForAnimation?.let { channel.trySend(it) }
        }

        LaunchedEffect(channel) {
            var targetRect: PlotState2.TargetRectForAnimation? = null
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

        // TODO:
        // How to plot and draw stuff?
        // Have plot items an each can draw something to the canvas?
        // Have a list of content, which can layout the way they want?

        Box(
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
                            val originalTopLeft = viewPortScreen //
                            val modifiedTopLeft = Offset(
                                (originalTopLeft.left - centroid.x) / zoom.width + centroid.x - pan.x,
                                (originalTopLeft.top - centroid.y) / zoom.height + centroid.y - pan.y,
                            )
                            //Log.v("Tuner", "Plot: original topLeft=$originalTopLeft, modified topLeft=$modifiedTopLeft, zoom=$zoom")
                            val originalRaw = state.viewPortRaw
                            val zoomedSize = Size(
                                originalRaw.size.width / zoom.width,
                                originalRaw.size.height / zoom.height
                            )

                            val movedTopLeftRaw = transformation.toRaw(modifiedTopLeft)
                            val movedRaw = Rect(movedTopLeftRaw, zoomedSize)
                            animatedRectRaw.snapTo(movedRaw)
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