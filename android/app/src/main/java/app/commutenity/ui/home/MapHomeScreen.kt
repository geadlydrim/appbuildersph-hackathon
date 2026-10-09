package app.commutenity.ui.home

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.domain.Field
import app.commutenity.domain.HomeEvent
import app.commutenity.domain.HomeState
import app.commutenity.domain.SearchRow
import app.commutenity.domain.Sheet
import app.commutenity.domain.TripResult
import app.commutenity.domain.TripSource
import app.commutenity.domain.canOpenTrip
import app.commutenity.ui.map.MapSurface
import app.commutenity.ui.map.PlaceholderMap
import app.commutenity.ui.theme.LocalCommuteColors
import app.commutenity.ui.theme.PlusJakarta
import kotlinx.coroutines.launch

private val PeekHeight = 112.dp
private val HalfHeight = 520.dp
private val NoticeHeight = 240.dp

@Composable
fun MapHomeScreen(
    state: HomeState,
    source: TripSource,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
    map: MapSurface = PlaceholderMap,
    designStatusBar: Boolean = false,
) {
    val colors = LocalCommuteColors.current
    val density = LocalDensity.current
    val reduceMotion = animatorScale() == 0f
    val scope = rememberCoroutineScope()
    val sheetHeight = remember { Animatable(PeekHeight, Dp.VectorConverter) }
    val target = when (state.sheet) {
        Sheet.Peek -> PeekHeight
        Sheet.Half -> HalfHeight
        Sheet.Notice -> NoticeHeight
    }
    LaunchedEffect(target, reduceMotion) {
        if (reduceMotion) sheetHeight.snapTo(target) else sheetHeight.animateTo(target)
    }
    val trip = if (canOpenTrip(state)) {
        source.resolve(state.origin!!, state.destination!!) as? TripResult.Ready
    } else {
        null
    }
    val searching = state.activeField != null

    Box(modifier.fillMaxSize().background(colors.map)) {
        map.Content(
            showTrip = trip != null && state.sheet == Sheet.Half,
            modifier = Modifier.fillMaxSize(),
        )
        PinLayer(state = state, showTrip = trip != null && state.sheet == Sheet.Half)
        if (designStatusBar) {
            Text(
                text = "9:41",
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp),
            )
        }
        Column(
            Modifier
                .align(Alignment.TopStart)
                .then(if (designStatusBar) Modifier else Modifier.statusBarsPadding())
                .padding(start = 16.dp, end = 16.dp, top = if (designStatusBar) 28.dp else 8.dp),
        ) {
            WordmarkRow()
            OfflineBadge(Modifier.padding(top = 8.dp))
            val field = state.activeField
            if (field != null) {
                PlaceSearchCard(
                    field = field,
                    query = state.query,
                    rows = source.search(field, state.query),
                    showClear = when (field) {
                        Field.A -> state.origin != null
                        Field.B -> state.destination != null
                    },
                    onQuery = { onEvent(HomeEvent.Query(it)) },
                    onRow = { row ->
                        when (row) {
                            SearchRow.UseMyLocation -> onEvent(HomeEvent.UseMyLocation)
                            is SearchRow.PlaceRow -> onEvent(HomeEvent.Pick(row.place))
                        }
                    },
                    onClear = { onEvent(HomeEvent.ClearActive) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                TripBuilderCard(
                    origin = state.origin,
                    destination = state.destination,
                    compact = canOpenTrip(state),
                    onField = { onEvent(HomeEvent.Focus(it)) },
                    onExpand = { onEvent(HomeEvent.Focus(Field.A)) },
                    modifier = Modifier.padding(top = 8.dp),
                )
                AskBar(Modifier.padding(top = 8.dp))
            }
        }
        MyLocationButton(
            onClick = { onEvent(HomeEvent.UseMyLocation) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = sheetHeight.value + 16.dp),
        )
        AttributionLabel(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = sheetHeight.value - 8.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight.value)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.surface),
        ) {
            SheetHandle(
                Modifier.pointerInput(state.sheet, searching) {
                    if (searching || state.sheet == Sheet.Notice || !canOpenTrip(state)) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            val dragged = with(density) { dragAmount.toDp() }
                            scope.launch {
                                sheetHeight.snapTo(
                                    (sheetHeight.value - dragged).coerceIn(PeekHeight, HalfHeight),
                                )
                            }
                        },
                        onDragEnd = {
                            val midway = (PeekHeight + HalfHeight) / 2
                            val next = if (sheetHeight.value < midway) Sheet.Peek else Sheet.Half
                            onEvent(HomeEvent.SettleSheet(next))
                        },
                    )
                },
            )
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    state.sheet == Sheet.Notice -> NotInDataMessage()
                    trip != null && state.sheet == Sheet.Half -> BestTripContent(trip.trip)
                    else -> PeekCopy()
                }
            }
        }
    }
}

@Composable
private fun PinLayer(state: HomeState, showTrip: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = maxWidth / 412.dp
        val origin = state.origin
        val destination = state.destination
        if (origin != null) {
            val spot = if (showTrip) PinSpot(270f, 334f) else PinSpot(270f, 370f)
            LetterPin("A", spot, scale, a = true)
        }
        if (destination != null) {
            val spot = when {
                showTrip -> PinSpot(90f, 234f)
                destination.inMakati -> PinSpot(98f, 256f)
                else -> PinSpot(36f, 250f)
            }
            LetterPin("B", spot, scale, a = false)
        }
        if (showTrip) {
            StopPin(PinSpot(234f, 306f), scale)
            ParaPin(PinSpot(118f, 211f), scale)
        }
    }
}

private data class PinSpot(val x: Float, val y: Float)

@Composable
private fun LetterPin(letter: String, spot: PinSpot, scale: Float, a: Boolean) {
    val colors = LocalCommuteColors.current
    val fill = if (a) colors.pinA else colors.pinB
    val on = if (a) colors.pinAOn else colors.pinBOn
    Box(
        Modifier
            .offset(x = spot.x.dp * scale, y = spot.y.dp * scale)
            .size(width = 44.dp * scale, height = 52.dp * scale),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = 2.dp * scale)
                .size(32.dp * scale)
                .background(fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, color = on, fontFamily = PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StopPin(spot: PinSpot, scale: Float) {
    val colors = LocalCommuteColors.current
    Box(
        Modifier
            .offset(x = spot.x.dp * scale, y = spot.y.dp * scale)
            .size(width = 44.dp * scale, height = 52.dp * scale),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = 11.dp * scale)
                .size(18.dp * scale)
                .border(3.dp, colors.pinA, CircleShape)
                .background(colors.surface, CircleShape),
        )
    }
}

@Composable
private fun ParaPin(spot: PinSpot, scale: Float) {
    val colors = LocalCommuteColors.current
    Box(
        Modifier
            .offset(x = spot.x.dp * scale, y = spot.y.dp * scale)
            .size(width = 44.dp * scale, height = 52.dp * scale),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = 2.dp * scale)
                .size(36.dp * scale)
                .background(colors.reason.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(28.dp * scale)
                    .background(colors.pinA, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("Para", color = colors.pinAOn, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun animatorScale(): Float {
    val context = LocalContext.current
    return Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
}
