/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/** Helper function to compute height of text labels.
 * @param style Text style.
 * @param density Density of measurement environment.
 * @param paddingTop Padding above text.
 * @param paddingBottom Padding below text.
 * @param textMeasurer Text measurer which is used to measure the text.
 * @return Label height in px.
 */
@Composable
fun rememberTextLabelHeight(
    style: TextStyle = LocalTextStyle.current,
    density: Density = LocalDensity.current,
    paddingTop: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): Float {
    return remember(textMeasurer, density, paddingTop, paddingBottom, style) {
        with(density) {
            (textMeasurer.measure("X", style = style, density = density).size.height
                    + paddingTop.toPx()
                    + paddingBottom.toPx())
        }
    }
}

private fun computeRange(
    tickLevel: TickLevel,
    maxLabelHeight: Float,
    lineWidth: Dp,
    screenOffset: DpOffset,
    maxNumLabels: Int,
    transformation: Transformation,
    density: Density
): TicksRange {
    val screenOffsetPx = with(density) { screenOffset.y.toPx() }
    val lineWidthPx = with(density) { lineWidth.toPx() }
    val maxNumLabelsResolved = if (maxNumLabels <= 0)
        (transformation.viewPortScreen.height / maxLabelHeight / 2f).roundToInt()
    else
       maxNumLabels

    val labelHeightScreen = Rect(
        0f,
        0f,
        1f,
        maxLabelHeight + 0.5f * lineWidthPx + screenOffsetPx.absoluteValue
    )

    val labelHeightRaw = transformation.toRaw(labelHeightScreen).height

    val range = tickLevel.getTicksRange(
        transformation.viewPortRaw.bottom - labelHeightRaw,
        transformation.viewPortRaw.top + labelHeightRaw,
        maxNumLabelsResolved,
        labelHeightRaw
    )
    //myLog("rememberRange: $labelHeightScreen, raw=${transformation.viewPortRaw}, screen=${transformation.viewPortScreen}, range=$range")
    return range
}

private data class YTickLayoutData(val position: Float):
    ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@YTickLayoutData
}

private data class MeasuredYTick(
    val position: YTickLayoutData,
    val placeable: Placeable
)

@Composable
private fun YTickLabels(
    label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?,
    tickLevel: TickLevel,
    maxLabelHeight: Float,
    anchor: Anchor,
    horizontalLabelPosition: Float,
    lineWidth: Dp,
    screenOffset: DpOffset,
    maxNumLabels: Int,
    clipLabelToPlotWindow: Boolean,
    transformation: () -> Transformation
) {
    Layout(
        content = {
            val density = LocalDensity.current
            val range by remember(tickLevel, maxLabelHeight, lineWidth, screenOffset, maxNumLabels, density, transformation) {
                derivedStateOf {
                    computeRange(
                        tickLevel,
                        maxLabelHeight,
                        lineWidth,
                        screenOffset,
                        maxNumLabels,
                        transformation(),
                        density
                    )
                }
            }
            label?.let { l ->
                for (i in range.indexBegin until range.indexEnd) {
                    val y = tickLevel.getTickValue(range.level, i)
                    //          modifier       , level      , index, y
                    key(i){ l(YTickLayoutData(y), range.level, i,     y) }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { measureables, constraints ->
        val c = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measureables.map {
            MeasuredYTick(
                it.parentData as YTickLayoutData,
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
                val visible = yTransformed in vp.top.toFloat() .. vp.bottom.toFloat()
                if (clipLabelToPlotWindow || visible) {
                    p.place(
                        anchor.place(
                            vp.left + horizontalLabelPosition * vp.width + screenOffset.x.toPx(),
                            yTransformed + screenOffset.y.toPx(),
                            p.width.toFloat(),
                            p.height.toFloat(),
                            lineWidth.toPx(),
                            0f
                        ).round()
                    )
                }
            }
        }
    }
}

@Composable
private fun YTicksLines(
    tickLevel: TickLevel,
    maxLabelHeight: Float,
    lineWidth: Dp,
    lineColor: Color,
    screenOffset: DpOffset,
    maxNumLabels: Int,
    transformation: () -> Transformation
) {
    val lineColorResolved = lineColor.takeOrElse {
        LocalContentColor.current.takeOrElse {
            MaterialTheme.colorScheme.outline
        }
    }
    val density = LocalDensity.current
    val range by remember(tickLevel, maxLabelHeight, lineWidth, screenOffset, maxNumLabels, density, transformation) {
        derivedStateOf {
            computeRange(
                tickLevel,
                maxLabelHeight,
                lineWidth,
                screenOffset,
                maxNumLabels,
                transformation(),
                density
            )
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
                        strokeWidth = lineWidth.toPx()
                    )
                }
            }
    )
}

@Composable
fun YTicks(
    label: (@Composable (modifier: Modifier, level: Int, index: Int, y: Float) -> Unit)?,
    tickLevel: TickLevel,
    maxLabelHeight: Float,
    anchor: Anchor = Anchor.Center,
    horizontalLabelPosition: Float = 0.5f,
    lineWidth: Dp = 1.dp,
    lineColor: Color = Color.Unspecified,
    screenOffset: DpOffset = DpOffset.Zero,
    maxNumLabels: Int = -1,
    clipLabelToPlotWindow: Boolean = true,
    transformation: () -> Transformation,
    clipped: Boolean
) {
    if (clipped) {
        YTicksLines(
            tickLevel,
            maxLabelHeight,
            lineWidth,
            lineColor,
            screenOffset,
            maxNumLabels,
            transformation
        )
    }

    if (clipped == clipLabelToPlotWindow) {
        YTickLabels(
            label = label,
            tickLevel = tickLevel,
            maxLabelHeight = maxLabelHeight,
            anchor = anchor,
            horizontalLabelPosition = horizontalLabelPosition,
            lineWidth = lineWidth,
            screenOffset = screenOffset,
            maxNumLabels = maxNumLabels,
            clipLabelToPlotWindow = clipLabelToPlotWindow,
            transformation = transformation
        )
    }
}
