package app.commutenity.ui.qa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.domain.QaRoute
import app.commutenity.ui.map.PlaceholderMap
import app.commutenity.ui.theme.LocalCommuteColors

// The placeholder map is drawn on a 412 × 915 frame; the preview crops the band around the sample trip.
private const val FrameWidth = 412f
private const val FrameHeight = 915f
private const val CropTop = 195f

@Composable
fun RoutePreview(route: QaRoute, modifier: Modifier = Modifier, height: Dp = 200.dp) {
    val colors = LocalCommuteColors.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        val scale = maxWidth / FrameWidth.dp
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .height(FrameHeight.dp * scale)
                .offset(y = -CropTop.dp * scale),
        ) {
            PlaceholderMap.Content(showTrip = true, modifier = Modifier.fillMaxSize())
            PreviewPin("A", x = 270f, y = 334f, scale = scale, a = true)
            PreviewPin("B", x = 90f, y = 234f, scale = scale, a = false)
        }
        Text(
            text = "${route.from} → ${route.to} · sample map",
            color = colors.ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PreviewPin(letter: String, x: Float, y: Float, scale: Float, a: Boolean) {
    val colors = LocalCommuteColors.current
    Box(
        Modifier
            .offset(x = (x + 6f).dp * scale, y = (y + 2f).dp * scale)
            .size(32.dp * scale)
            .background(if (a) colors.pinA else colors.pinB, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = if (a) colors.pinAOn else colors.pinBOn,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}
