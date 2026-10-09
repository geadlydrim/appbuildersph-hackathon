package app.commutenity.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.domain.Field
import app.commutenity.domain.Place
import app.commutenity.domain.TripPath
import app.commutenity.ui.theme.LocalCommuteColors

private data class Block(val x: Float, val y: Float, val w: Float, val h: Float, val r: Float, val park: Boolean)

private val blocks = listOf(
    Block(70f, 175f, 78f, 48f, 16f, true),
    Block(200f, 455f, 92f, 40f, 18f, true),
    Block(150f, 300f, 36f, 22f, 3f, false),
    Block(196f, 286f, 28f, 18f, 3f, false),
    Block(168f, 340f, 42f, 16f, 3f, false),
    Block(230f, 330f, 24f, 28f, 3f, false),
    Block(120f, 200f, 30f, 18f, 3f, false),
    Block(90f, 230f, 22f, 26f, 3f, false),
    Block(200f, 210f, 34f, 16f, 3f, false),
    Block(250f, 240f, 20f, 24f, 3f, false),
    Block(140f, 450f, 38f, 18f, 3f, false),
    Block(190f, 470f, 26f, 22f, 3f, false),
    Block(80f, 480f, 30f, 16f, 3f, false),
    Block(300f, 360f, 28f, 20f, 3f, false),
    Block(60f, 360f, 24f, 18f, 3f, false),
    Block(310f, 250f, 22f, 30f, 3f, false),
    Block(40f, 540f, 36f, 16f, 3f, false),
    Block(240f, 520f, 32f, 18f, 3f, false),
)

private data class Street(val name: String, val x: Float, val y: Float)

private val streets = listOf(
    Street("Ayala Ave", 188f, 400f),
    Street("Makati Ave", 96f, 300f),
    Street("Paseo de Roxas", 150f, 448f),
    Street("Dela Rosa St", 24f, 214f),
    Street("EDSA", 348f, 500f),
)

private const val FrameWidth = 412f
private const val FrameHeight = 915f

/** Drawn picture of the T0 frames. It ignores pins and taps; the home screen draws its own pins over it. */
object PlaceholderMap : MapSurface {
    @Composable
    override fun Content(
        showTrip: Boolean,
        modifier: Modifier,
        origin: Place?,
        destination: Place?,
        onTap: ((lat: Double, lng: Double) -> Unit)?,
        path: TripPath?,
        onMovePin: ((field: Field, lat: Double, lng: Double) -> Unit)?,
        compassBottom: Dp,
    ) {
        val colors = LocalCommuteColors.current
        val paths = remember {
            mapRoads.map { shape ->
                shape to PathParser().parsePathString(shape.pathData).toPath()
            }
        }
        Box(modifier) {
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / FrameWidth
                val sy = size.height / FrameHeight
                withTransform({ scale(sx, sy, pivot = Offset.Zero) }) {
                    drawRect(colors.map)
                    blocks.forEach { block ->
                        drawRoundRect(
                            color = if (block.park) colors.park else colors.building,
                            topLeft = Offset(block.x, block.y),
                            size = Size(block.w, block.h),
                            cornerRadius = CornerRadius(block.r, block.r),
                        )
                    }
                    paths.forEach { (shape, path) ->
                        if (shape.onTrip && !showTrip) return@forEach
                        val dx = shape.left - (shape.svgWidth - shape.frameWidth) / 2f
                        val dy = shape.top - (shape.svgHeight - shape.frameHeight) / 2f
                        val color = when (shape.paint) {
                            RoadPaint.Casing -> colors.roadCasing
                            RoadPaint.Surface -> colors.road
                            RoadPaint.Minor -> colors.roadMinor
                            RoadPaint.Route -> colors.route
                            RoadPaint.Fill -> colors.dash
                        }
                        withTransform({ translate(dx, dy) }) {
                            if (shape.paint == RoadPaint.Fill) {
                                drawPath(path, color)
                            } else {
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(
                                        width = shape.strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val scale = maxWidth / 412.dp
                streets.forEach { street ->
                    Text(
                        text = street.name,
                        color = colors.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.offset(x = street.x.dp * scale, y = street.y.dp * scale),
                    )
                }
            }
        }
    }
}
