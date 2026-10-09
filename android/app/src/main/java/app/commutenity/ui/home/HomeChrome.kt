package app.commutenity.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import app.commutenity.R
import app.commutenity.domain.Field
import app.commutenity.domain.Place
import app.commutenity.ui.theme.LocalCommuteColors
import app.commutenity.ui.theme.PlusJakarta

@Composable
fun WordmarkRow(modifier: Modifier = Modifier, onMenu: (() -> Unit)? = null) {
    val colors = LocalCommuteColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onMenu != null) {
                MenuButton(onMenu, Modifier.padding(end = 8.dp))
            }
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
private fun MenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Canvas(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Menu" }
            .padding(horizontal = 11.dp, vertical = 13.dp),
    ) {
        val stroke = 2.dp.toPx()
        listOf(0f, size.height / 2f, size.height).forEach { y ->
            drawLine(
                color = colors.ink,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun LogoMark(size: Dp = 32.dp) {
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
                .size(size)
                .clip(RoundedCornerShape(size / 4)),
        )
    } else {
        val colors = LocalCommuteColors.current
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 4))
                .background(colors.ink),
        )
    }
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
                title = origin?.name ?: "Where from?",
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
                title = destination?.name ?: "Where to?",
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
fun AskBar(onClick: () -> Unit, onMic: (() -> Unit)? = null, modifier: Modifier = Modifier) {
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
                onClick = onClick,
            )
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Ask…",
            color = colors.muted.copy(alpha = if (pressed) 0.6f else 1f),
            fontSize = 14.sp,
        )
        if (onMic != null) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.pinA, CircleShape)
                    .semantics { contentDescription = "Ask by voice" }
                    .clickable(role = Role.Button, onClick = onMic),
                contentAlignment = Alignment.Center,
            ) {
                MicGlyph()
            }
        } else {
            Box(
                Modifier
                    .size(32.dp)
                    .background(colors.pinA, CircleShape),
            )
        }
    }
}

@Composable
private fun MicGlyph() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 8.dp, height = 12.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
        )
        Box(
            Modifier
                .padding(top = 1.dp)
                .size(width = 2.dp, height = 3.dp)
                .background(Color.White),
        )
        Box(
            Modifier
                .size(width = 8.dp, height = 2.dp)
                .background(Color.White, RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
fun AskComposer(
    draft: String,
    feedback: String?,
    onDraft: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    listening: Boolean = false,
    thinking: Boolean = false,
    aiStatus: String? = null,
    onMic: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCommuteColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .background(colors.surface, shape)
            .padding(14.dp),
    ) {
        if (listening) {
            Text(
                text = "● Listening",
                color = colors.pinA,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        val hint = if (listening) "Listening… speak in English" else aiStatus
        if (hint != null) {
            Text(
                text = hint,
                color = colors.muted,
                fontSize = 12.sp,
            )
        }
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            singleLine = true,
            textStyle = TextStyle(color = colors.ink, fontSize = 16.sp),
            cursorBrush = SolidColor(colors.ink),
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            decorationBox = { inner ->
                if (draft.isEmpty()) {
                    Text("Example: V.A. Rufino to Dela Rosa St", color = colors.faint, fontSize = 15.sp)
                }
                inner()
            },
        )
        if (feedback != null) {
            Text(
                text = feedback,
                color = colors.ink,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (thinking) "Thinking…" else "Find",
                color = colors.paraOn,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.ink)
                    .clickable(enabled = !thinking, onClick = onSubmit)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Text(
                text = "Close",
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            if (onMic != null) {
                Text(
                    text = if (listening) "Stop" else "Speak",
                    color = colors.ink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onMic)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
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
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Use my location (sample, not GPS)" },
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
