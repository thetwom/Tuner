package de.moekadu.tuner.ui.plot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

class PointMark(
    position: Offset,
    anchor: Anchor = Anchor.Center,
    screenOffset: DpOffset = DpOffset.Zero,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    data class LayoutData(val position: Offset, val screenOffset: DpOffset, val anchor: Anchor): ParentDataModifier {
        override fun Density.modifyParentData(parentData: Any?) = this@LayoutData
    }
    var layoutData by mutableStateOf(LayoutData(position, screenOffset, anchor))
        private set
    var content by mutableStateOf(content)
        private set

    fun modify(
        position: Offset? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        this.layoutData = LayoutData(
            position ?: this.layoutData.position,
            screenOffset ?: this.layoutData.screenOffset,
            anchor ?: this.layoutData.anchor
        )

        if (content != null)
            this.content = content
    }
}

private data class MeasuredPointMark(val position: PointMark.LayoutData, val placeable: Placeable)
class PointMarkGroup(
    private val marks: ImmutableList<PointMark>,
) : PlotItem {

    override val hasClippedDraw = true
    override val hasUnclippedDraw = false

    fun modify(
        markIndex: Int,
        position: Offset? = null,
        anchor: Anchor? = null,
        screenOffset: DpOffset? = null,
        content: (@Composable (modifier: Modifier) -> Unit)? = null,
    ) {
        marks[markIndex].modify(position, anchor, screenOffset, content)
    }

    @Composable
    override fun DrawClipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
        ) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                marks.forEach { it.content(it.layoutData) }
            }
        ) {measureables, constraints ->
            val c = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measureables.map {
                MeasuredPointMark(it.parentData as PointMark.LayoutData, it.measure(c))
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach {
                    val p = it.placeable
                    val positionScreen = transformation.toScreen(it.position.position)

                    p.place(
                        it.position.anchor.place(
                            positionScreen.x + it.position.screenOffset.x.toPx(),
                            positionScreen.y + it.position.screenOffset.y.toPx(),
                            p.width.toFloat(),
                            p.height.toFloat()
                        ).round()
                    )
                }
            }
        }
    }

    @Composable
    override fun DrawUnclipped(
        transformation: Transformation,
        plotStyles: ImmutableMap<Int, PlotStyle>
    ) { }
}

@Composable
private fun rememberTransformation(
    screenWidth: Dp, screenHeight: Dp,
    viewPortRaw: Rect
): Transformation {
    val widthPx = with(LocalDensity.current) { screenWidth.roundToPx() }
    val heightPx = with(LocalDensity.current) { screenHeight.roundToPx() }

    val transformation = remember(widthPx, heightPx, viewPortRaw) {
        Transformation(IntRect(0, 0, widthPx, heightPx), viewPortRaw)
    }
    return transformation
}

@Preview(widthDp = 200, heightDp = 100, showBackground = true)
@Composable
private fun PointMarkGroupPreview() {
    TunerTheme {
        BoxWithConstraints {
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-10f, 5f, 10f, -5f)
            )
            val plotStyles = persistentMapOf<Int, PlotStyle>()

            val pointMarkGroup = remember {
                PointMarkGroup(
                    persistentListOf(
                    PointMark(
                        position = Offset(-5f, -2f),
                        anchor = Anchor.NorthEast,
                        content = { modifier -> Text("-5,-2NE", modifier = modifier.background(Color.Cyan))}
                    ),
                    PointMark(
                        position = Offset(-5f, -2f),
                        anchor = Anchor.SouthWest,
                        content = { modifier -> Text("-5,-2SW", modifier = modifier.background(Color.Magenta))}
                    ),
                    PointMark(
                        position = Offset(-5f, -2f),
                        anchor = Anchor.SouthEast,
                        content = { modifier -> Text("-5,-2SE", modifier = modifier.background(Color.Green))}
                    ),
                    PointMark(
                        position = Offset(-5f, -2f),
                        anchor = Anchor.NorthWest,
                        content = { modifier -> Text("-5,-2NW", modifier = modifier.background(Color.Yellow))}
                    ),
                    PointMark(
                        position = Offset(5f, 2f),
                        anchor = Anchor.North,
                        content = { modifier -> Text("5,2N", modifier = modifier.background(Color.Cyan))}
                    ),
                    PointMark(
                        position = Offset(5f, 2f),
                        anchor = Anchor.South,
                        content = { modifier -> Text("5,2S", modifier = modifier.background(Color.Green))}
                    )
                    )
                )
            }

            pointMarkGroup.DrawClipped(transformation = transformation, plotStyles = plotStyles)
            pointMarkGroup.DrawUnclipped(transformation = transformation, plotStyles = plotStyles)
        }
    }
}