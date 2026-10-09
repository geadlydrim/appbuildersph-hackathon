package app.commutenity.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import app.commutenity.R
import app.commutenity.domain.Field
import app.commutenity.domain.Place
import app.commutenity.ui.theme.LocalCommuteColors
import app.commutenity.ui.theme.PlusJakarta

@Composable
fun WordmarkRow(modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark()
            Text(
                text = "CommuteNity",
                color = colors.ink,
                fontFamily = PlusJakarta,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = "Offline",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier
                .background(colors.surface, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LogoMark() {
    val context = LocalContext.current
    val bitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.logo)?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    } else {
        val colors = LocalCommuteColors.current
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.ink),
        )
    }
}

@Composable
fun OfflineBadge(modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Text(
        text = "On-device · Offline OK",
        color = colors.ink,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = modifier
            .background(colors.surface, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun TripBuilderCard(
    origin: Place?,
    destination: Place?,
    compact: Boolean,
    onField: (Field) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCommuteColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .background(colors.surface, shape)
            .then(if (compact) Modifier.clickable(onClick = onExpand) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (compact && origin != null && destination != null) {
            Text(
                text = "${origin.name}  →  ${destination.name}",
                color = colors.ink,
                fontFamily = PlusJakarta,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        } else {
            EndpointRow(
                filled = origin != null,
                dot = colors.pinA,
                title = origin?.name ?: "Saan ka manggagaling?",
                onClick = { onField(Field.A) },
            )
            if (origin != null && origin.area.isNotEmpty()) {
                Text(
                    text = origin.area,
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            EndpointRow(
                filled = destination != null,
                dot = colors.pinB,
                title = destination?.name ?: "Saan ka pupunta?",
                onClick = { onField(Field.B) },
            )
        }
    }
}

@Composable
private fun EndpointRow(
    filled: Boolean,
    dot: Color,
    title: String,
    onClick: () -> Unit,
) {
    val colors = LocalCommuteColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(dot, CircleShape),
        )
        Text(
            text = title,
            color = if (filled) colors.ink else colors.muted,
            fontSize = 15.sp,
            fontWeight = if (filled) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
fun AskBar(modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .background(colors.surface, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {},
            )
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Magtanong…",
            color = colors.muted.copy(alpha = if (pressed) 0.6f else 1f),
            fontSize = 14.sp,
        )
        Box(
            Modifier
                .size(32.dp)
                .background(colors.pinA, CircleShape),
        )
    }
}

@Composable
fun MyLocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(6.dp, CircleShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .background(colors.surface, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .background(colors.pinA, CircleShape),
        )
    }
}

@Composable
fun AttributionLabel(modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Text(
        text = "© OpenStreetMap contributors",
        color = colors.ink,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        modifier = modifier
            .background(colors.surface.copy(alpha = 0.86f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
