package app.commutenity.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.domain.Leg
import app.commutenity.domain.Trip
import app.commutenity.ui.theme.LocalCommuteColors
import app.commutenity.ui.theme.PlusJakarta

@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.handle),
        )
    }
}

@Composable
fun PeekCopy(text: String = "Pick A and B on the map.", modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Text(
        text = text,
        color = colors.ink,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
    )
}

@Composable
fun NotInDataMessage(
    onChangeDestination: () -> Unit,
    modifier: Modifier = Modifier,
    changeLabel: String = "Change destination",
) {
    val colors = LocalCommuteColors.current
    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "Not in my data yet. Only Makati is covered for now.",
            color = colors.ink,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
        Text(
            text = changeLabel,
            color = colors.paraOn,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.ink)
                .clickable(onClick = onChangeDestination)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun BestTripContent(
    trip: Trip,
    onOpenQuestions: () -> Unit = {},
    workedCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCommuteColors.current
    Column(modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Stat(trip.fare, "Fare", Modifier.weight(1f))
            Stat(trip.minutes, "Time", Modifier.weight(1f))
            Stat(trip.transfers, "Transfers", Modifier.weight(1f))
        }
        Text(
            text = trip.distanceLine,
            color = colors.faint,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(Modifier.padding(top = 8.dp)) {
            Chip(trip.reason, colors.reason, colors.reasonOn)
            if (trip.sample) {
                Chip("Sample", colors.sample, colors.sampleOn, Modifier.padding(start = 8.dp))
            }
        }
        Column(Modifier.padding(top = 16.dp)) {
            trip.legs.forEachIndexed { index, leg ->
                StepRow(leg = leg, last = index == trip.legs.lastIndex)
            }
        }
        if (workedCount > 0) {
            val riders = if (workedCount == 1) "1 rider says" else "$workedCount riders say"
            Text(
                text = "$riders this works · sample",
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClick = onOpenQuestions),
            )
        }
        Text(
            text = "Questions",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = onOpenQuestions),
        )
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalCommuteColors.current
    Column(modifier) {
        Text(value, color = colors.ink, fontFamily = PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = colors.muted, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    }
}

@Composable
private fun Chip(text: String, background: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = content,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = modifier
            .background(background, RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun StepRow(leg: Leg, last: Boolean) {
    val colors = LocalCommuteColors.current
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(16.dp)) {
            val marker = when (leg) {
                is Leg.Ride -> colors.reason
                is Leg.Para -> colors.ink
                is Leg.Walk -> colors.step
            }
            Box(Modifier.padding(top = 4.dp).size(12.dp).background(marker, CircleShape))
            if (!last) {
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .width(2.dp)
                        .height(if (leg is Leg.Ride) 56.dp else 36.dp)
                        .background(colors.connector),
                )
            }
        }
        when (leg) {
            is Leg.Walk -> Column(Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                Text("Walk", color = colors.ink, fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(leg.meters, color = colors.muted, fontSize = 13.sp)
            }
            is Leg.Ride -> Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)) {
                Text(leg.stops, color = colors.ink, fontFamily = PlusJakarta, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(leg.fareAndMinutes, color = colors.muted, fontSize = 13.sp)
                Text(leg.signboard, color = colors.faint, fontSize = 12.sp)
            }
            is Leg.Para -> Text(
                text = "Para  ·  ${leg.landmark}",
                color = colors.paraOn,
                fontFamily = PlusJakarta,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth()
                    .background(colors.para, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}
