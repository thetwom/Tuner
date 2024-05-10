package de.moekadu.tuner.ui.plot3

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.TickLevel
import de.moekadu.tuner.ui.plot.TicksRange
import de.moekadu.tuner.ui.plot.Transformation
import de.moekadu.tuner.ui.plot.place
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private fun computeRange(
    tickLevel: TickLevel,
    settings: YTicksSettings,
    transformation: Transformation,
    density: Density
): TicksRange {
    val screenOffsetPx = with(density) { settings.screenOffset.y.toPx() }
    val lineWidthPx = with(density) { settings.lineWidth.toPx() }
    val maxNumLabels = if (settings.maxNumLabels <= 0)
        (transformation.viewPortScreen.height / settings.maxLabelHeight / 2f).roundToInt()
    else
       settings.maxNumLabels

    val labelHeightScreen = Rect(
        0f,
        0f,
        1f,
        settings.maxLabelHeight + 0.5f * lineWidthPx + screenOffsetPx.absoluteValue
    )

    val labelHeightRaw = transformation.toRaw(labelHeightScreen).height

    val range = tickLevel.getTicksRange(
        transformation.viewPortRaw.bottom - labelHeightRaw,
        transformation.viewPortRaw.top + labelHeightRaw,
        maxNumLabels,
        labelHeightRaw
    )
    //myLog("rememberRange: $labelHeightScreen, raw=${transformation.viewPortRaw}, screen=${transformation.viewPortScreen}, range=$range")
    return range
}

data class YTicksSettings(
    val maxLabelHeight: Float,
    val anchor: Anchor = Anchor.Center,
    val horizontalLabelPosition: Float = 0.5f,
    val lineWidth: Dp = 1.dp,
    val lineColor: Color = Color.Unspecified,
    val screenOffset: DpOffset = DpOffset.Zero,
    val clipLabelToPlotWindow: Boolean = false,
    val maxNumLabels: Int = -1
)

private data class TickLayoutData(val position: Float):
    ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@TickLayoutData
}

private data class MeasuredTick(
    val position: TickLayoutData,
    val placeable: Placeable
)

@Composable
private fun YTickLabels(
    label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?,
    tickLevel: TickLevel,
    settings: YTicksSettings,
    transformation: () -> Transformation
) {
    Layout(
        content = {
            val density = LocalDensity.current
            val range by remember(settings, tickLevel) {
                derivedStateOf {
                    computeRange(tickLevel, settings, transformation(), density)
                }
            }
            label?.let { l ->
                for (i in range.indexBegin until range.indexEnd) {
                    val y = tickLevel.getTickValue(range.level, i)
                    //          modifier       , level      , index, y
                    key(i){ l(TickLayoutData(y), range.level, i,     y) }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { measureables, constraints ->
        val c = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measureables.map {
            MeasuredTick(
                it.parentData as TickLayoutData,
                it.measure(c)
            )
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val transform = transformation()
            placeables.forEach {
                val p = it.placeable
                val yOffset = Offset(0f, it.position.position)
                val yTransformed = transform.toScreen(yOffset).y
                val vp = transform.viewPortScreen

                p.place(
                    settings.anchor.place(
                        vp.left + settings.horizontalLabelPosition * vp.width + settings.screenOffset.x.toPx(),
                        yTransformed + settings.screenOffset.y.toPx(),
                        p.width.toFloat(),
                        p.height.toFloat(),
                        settings.lineWidth.toPx(),
                        0f
                    ).round()
                )
            }
        }
    }
}

@Composable
private fun YTicks3Lines(
    tickLevel: TickLevel,
    settings: YTicksSettings,
    transformation: () -> Transformation
) {
    val lineColorResolved = settings.lineColor.takeOrElse {
        LocalContentColor.current.takeOrElse {
            MaterialTheme.colorScheme.outline
        }
    }
    val density = LocalDensity.current
    val range by remember(settings, tickLevel) {
        derivedStateOf {
            computeRange(tickLevel, settings, transformation(), density)
        }
    }

    Spacer(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val transformationInstance = transformation()
                for (i in range.indexBegin until range.indexEnd) {
                    val yOffset = Offset(0f, tickLevel.getTickValue(range.level, i))
                    val yTransformed = transformationInstance.toScreen(yOffset).y
                    drawLine(
                        lineColorResolved,
                        Offset(
                            transformationInstance.viewPortScreen.left.toFloat(),
                            yTransformed
                        ),
                        Offset(
                            transformationInstance.viewPortScreen.right.toFloat(),
                            yTransformed
                        ),
                        strokeWidth = settings.lineWidth.toPx()
                    )
                }
            }
    )
}



@Composable
fun YTicks3(
    label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?,
    tickLevel: TickLevel,
    settings: YTicksSettings,
    transformation: () -> Transformation,
    clipped: Boolean
) {
    if (clipped) {
        YTicks3Lines(
            tickLevel,
            settings,
            transformation
        )
    }

    if (clipped == settings.clipLabelToPlotWindow)
        YTickLabels(label = label, tickLevel = tickLevel, settings = settings, transformation = transformation)
}


@Composable
private fun YTicks3Unclipped() {

}
