package app.commutenity.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.ui.theme.LocalCommuteColors
import app.commutenity.ui.theme.PlusJakarta
import kotlinx.coroutines.launch

@Composable
fun HomeMenuDrawer(
    drawerState: DrawerState,
    onOpenQuestions: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalCommuteColors.current
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swiping only closes the drawer so it never fights with map drags.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                modifier = Modifier.width(300.dp),
            ) {
                MenuContent(
                    onQuestions = {
                        scope.launch {
                            drawerState.close()
                            onOpenQuestions()
                        }
                    },
                )
            }
        },
        content = content,
    )
}

@Composable
private fun MenuContent(onQuestions: () -> Unit) {
    val colors = LocalCommuteColors.current
    Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark(size = 40.dp)
            Text(
                text = "CommuteNity",
                color = colors.ink,
                fontFamily = PlusJakarta,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Row(
            Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.sample)
                .clickable(role = Role.Button, onClick = onQuestions)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BubbleIcon()
            Column {
                Text("Questions", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Sample posts · no login", color = colors.muted, fontSize = 13.sp)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 20.dp), color = colors.handle)
        Text("About this demo", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            text = "Trips are worked out on this phone and work offline. " +
                "Places, trips, and questions are sample data. There is no account and no login.",
            color = colors.muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "On-device · Offline OK",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(colors.map)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun BubbleIcon() {
    val colors = LocalCommuteColors.current
    Canvas(Modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        val bubbleHeight = size.height * 0.78f
        drawRoundRect(
            color = colors.ink,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, bubbleHeight - stroke),
            cornerRadius = CornerRadius(size.width * 0.25f),
            style = Stroke(width = stroke),
        )
        val tail = Path().apply {
            moveTo(size.width * 0.28f, bubbleHeight - stroke)
            lineTo(size.width * 0.22f, size.height)
            lineTo(size.width * 0.5f, bubbleHeight - stroke)
        }
        drawPath(tail, colors.ink, style = Stroke(width = stroke))
    }
}
