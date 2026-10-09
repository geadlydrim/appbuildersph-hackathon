package app.commutenity.domain

import app.commutenity.ai.Intent
import app.commutenity.ai.ParserOutput
import app.commutenity.ai.Preference as TripPreferenceHint

const val VOICE_HEARD_FEEDBACK = "Heard by voice. Fix any words, then tap Find."
private const val VOICE_NOT_HEARD_FEEDBACK = "I didn't catch that. Try again or type your question."

data class HomeState(
    val origin: Place? = null,
    val destination: Place? = null,
    val activeField: Field? = null,
    val query: String = "",
    val sheet: Sheet = Sheet.Peek,
    val cardExpanded: Boolean = false,
    val asking: Boolean = false,
    val askDraft: String = "",
    val askFeedback: String? = null,
    val listening: Boolean = false,
    val preference: TripPreference = TripPreference.Default,
    val thinking: Boolean = false,
)

sealed interface HomeEvent {
    data class Focus(val field: Field) : HomeEvent
    data class Query(val value: String) : HomeEvent
    data class Pick(val place: Place) : HomeEvent
    data object UseMyLocation : HomeEvent
    data object ClearActive : HomeEvent
    data object DismissSearch : HomeEvent
    data object ExpandCard : HomeEvent
    data object OpenAsk : HomeEvent
    data object CloseAsk : HomeEvent
    data class AskDraft(val value: String) : HomeEvent
    data object SubmitAsk : HomeEvent
    /** The on-device AI started reading the question. */
    data object AskThinking : HomeEvent
    data class AskParsed(val output: ParserOutput) : HomeEvent
    data class AskFailed(val message: String) : HomeEvent
    data object VoiceStart : HomeEvent
    data class VoicePartial(val text: String) : HomeEvent
    data class VoiceResult(val text: String) : HomeEvent
    data class VoiceError(val message: String) : HomeEvent
    data object VoiceStop : HomeEvent
    data class SettleSheet(val sheet: Sheet) : HomeEvent
    /** A tap on the map at a coordinate. */
    data class MapTap(val lat: Double, val lng: Double) : HomeEvent
    /** A long-press on a map pin moved that end to a new coordinate. */
    data class MovePin(val field: Field, val lat: Double, val lng: Double) : HomeEvent
}

/**
 * Rough Makati City bounding box. Taps outside it get the "not in my data" notice (D20). It is a box,
 * not the city boundary, so edge areas of neighbouring cities can pass; the pack decides real coverage.
 */
private const val MAKATI_MIN_LAT = 14.525
private const val MAKATI_MAX_LAT = 14.585
private const val MAKATI_MIN_LNG = 120.995
private const val MAKATI_MAX_LNG = 121.065

/** A place for a point the rider tapped on the map. */
fun pinnedPlace(lat: Double, lng: Double): Place {
    val coords = "%.5f, %.5f".format(java.util.Locale.ROOT, lat, lng)
    return Place(
        id = "pin:$coords",
        name = "Pinned spot",
        area = coords,
        inMakati = lat in MAKATI_MIN_LAT..MAKATI_MAX_LAT && lng in MAKATI_MIN_LNG..MAKATI_MAX_LNG,
        lat = lat,
        lng = lng,
    )
}

/** Which end a map tap fills: the open search box, else A, then B, then start over with a new A. */
private fun applyMapTap(state: HomeState, place: Place): HomeState = when {
    state.activeField == Field.B -> state.copy(destination = place)
    state.activeField == Field.A -> state.copy(origin = place)
    state.origin == null -> state.copy(origin = place)
    state.destination == null -> state.copy(destination = place)
    else -> state.copy(origin = place, destination = null)
}

data class AskMatch(
    val origin: Place?,
    val destination: Place?,
    val feedback: String,
)

fun matchAsk(text: String, source: TripSource): AskMatch {
    val normalized = text.trim().lowercase()
    if (normalized.isEmpty()) {
        return AskMatch(null, null, "Write a question first.")
    }
    val places = source.search(Field.B, "").filterIsInstance<SearchRow.PlaceRow>().map { it.place }
    val mentions = places.mapNotNull { firstMention(normalized, it) }.sortedBy { it.start }.take(2)
    val cues = mentions.mapIndexed { i, mention ->
        lastCue(normalized.substring(if (i == 0) 0 else mentions[i - 1].end, mention.start))
    }
    var origin: Place? = null
    var destination: Place? = null
    when (mentions.size) {
        1 -> if (cues[0] == Cue.From) origin = mentions[0].place else destination = mentions[0].place
        2 -> {
            val reversed = (cues[0] == Cue.To && cues[1] != Cue.To) || (cues[1] == Cue.From && cues[0] != Cue.From)
            origin = mentions[if (reversed) 1 else 0].place
            destination = mentions[if (reversed) 0 else 1].place
        }
    }
    val feedback = when {
        origin == null && destination == null ->
            "AI unavailable on this phone; used simple matching. I didn't understand; pick A and B on the map."
        origin != null && destination != null ->
            "Simple matching: ${origin.name} → ${destination.name}."
        origin != null ->
            "Simple matching found the start only. The destination is still missing."
        else ->
            "Simple matching found the destination only. The start is still missing."
    }
    return AskMatch(origin, destination, feedback)
}

private enum class Cue { From, To }

private data class Mention(val place: Place, val start: Int, val end: Int)

private val fromCue = Regex("\\b(from|galing|mula)\\b")
private val toCue = Regex("\\b(to|papunta|pumunta|punta|patungo)\\b")

private fun firstMention(text: String, place: Place): Mention? =
    listOf(place.name, place.area)
        .filter { it.isNotEmpty() }
        .mapNotNull { key ->
            val at = text.indexOf(key.lowercase())
            if (at < 0) null else Mention(place, at, at + key.length)
        }
        .minByOrNull { it.start }

/** The cue word closest before a place decides its end: "galing X" is the start, "papunta X" the destination. */
private fun lastCue(before: String): Cue? {
    val from = fromCue.findAll(before).lastOrNull()?.range?.first ?: -1
    val to = toCue.findAll(before).lastOrNull()?.range?.first ?: -1
    return when {
        from < 0 && to < 0 -> null
        from > to -> Cue.From
        else -> Cue.To
    }
}

fun reduce(state: HomeState, event: HomeEvent, source: TripSource): HomeState {
    return when (event) {
        is HomeEvent.Focus -> state.copy(
            activeField = event.field,
            cardExpanded = true,
            asking = false,
            listening = false,
            query = when (event.field) {
                Field.A -> state.origin?.name.orEmpty()
                Field.B -> state.destination?.name.orEmpty()
            },
            sheet = Sheet.Peek,
        )
        is HomeEvent.Query -> state.copy(query = event.value)
        HomeEvent.UseMyLocation -> settle(
            state.copy(origin = source.myLocation, activeField = null, query = ""),
            source,
        )
        is HomeEvent.Pick -> {
            val next = when (state.activeField) {
                Field.B -> state.copy(destination = event.place)
                else -> state.copy(origin = event.place)
            }
            settle(next.copy(activeField = null, query = ""), source)
        }
        HomeEvent.ClearActive -> {
            val next = when (state.activeField) {
                Field.B -> state.copy(destination = null)
                Field.A -> state.copy(origin = null)
                null -> state
            }
            settle(next.copy(activeField = null, query = ""), source)
        }
        HomeEvent.DismissSearch -> state.copy(activeField = null, query = "")
        HomeEvent.ExpandCard -> state.copy(cardExpanded = true, asking = false, listening = false, activeField = null)
        HomeEvent.OpenAsk -> state.copy(asking = true, askDraft = "", askFeedback = null, activeField = null)
        HomeEvent.CloseAsk -> state.copy(asking = false, listening = false, askDraft = "", askFeedback = null)
        is HomeEvent.AskDraft -> state.copy(askDraft = event.value, askFeedback = null)
        HomeEvent.SubmitAsk -> submitAsk(state, source).copy(listening = false)
        HomeEvent.AskThinking -> state.copy(thinking = true, askFeedback = "Thinking…")
        is HomeEvent.AskParsed -> applyParsed(state, event.output, source)
        is HomeEvent.AskFailed -> state.copy(thinking = false, askFeedback = event.message)
        HomeEvent.VoiceStart -> state.copy(
            asking = true,
            listening = true,
            askDraft = "",
            askFeedback = null,
            activeField = null,
        )
        is HomeEvent.VoicePartial -> if (state.listening) state.copy(askDraft = event.text) else state
        is HomeEvent.VoiceResult -> state.copy(
            listening = false,
            asking = true,
            askDraft = event.text.trim(),
            askFeedback = if (event.text.isNotBlank()) VOICE_HEARD_FEEDBACK else VOICE_NOT_HEARD_FEEDBACK,
        )
        is HomeEvent.VoiceError -> state.copy(listening = false, asking = true, askFeedback = event.message)
        HomeEvent.VoiceStop -> state.copy(listening = false)
        is HomeEvent.SettleSheet -> {
            if (state.activeField != null) state
            else if (canOpenTrip(state)) state.copy(sheet = event.sheet)
            else state
        }
        is HomeEvent.MapTap -> settle(
            applyMapTap(state, pinnedPlace(event.lat, event.lng)).copy(activeField = null, query = ""),
            source,
        )
        is HomeEvent.MovePin -> {
            val moved = pinnedPlace(event.lat, event.lng)
            val next = when (event.field) {
                Field.A -> state.copy(origin = moved)
                Field.B -> state.copy(destination = moved)
            }
            settle(next.copy(activeField = null, query = ""), source)
        }
    }
}

private fun settle(state: HomeState, source: TripSource): HomeState {
    val origin = state.origin
    val destination = state.destination
    val sheet = when {
        origin == null || destination == null -> Sheet.Peek
        origin.id == destination.id -> Sheet.Peek
        source.resolve(origin, destination, state.preference) is TripResult.NotInData -> Sheet.Notice
        else -> Sheet.Half
    }
    return state.copy(sheet = sheet, cardExpanded = false)
}

private const val TRIP_EXAMPLE_FEEDBACK = "Which places? Try: V.A. Rufino to Dela Rosa St."
private const val OTHER_INTENT_FEEDBACK = "I can help with trips in Makati. Try: V.A. Rufino to Dela Rosa St."
private const val ASK_START_FEEDBACK = "Where are you starting? Tap the map, use my location, or type it."
private const val ASK_DESTINATION_FEEDBACK = "Where are you going? Tap the map or type it."

private fun firstPlace(source: TripSource, field: Field, text: String): Place? =
    source.search(field, text).filterIsInstance<SearchRow.PlaceRow>().firstOrNull()?.place

private fun TripPreferenceHint?.toPreference(): TripPreference = when (this) {
    TripPreferenceHint.CHEAPEST -> TripPreference.Cheapest
    TripPreferenceHint.FASTEST -> TripPreference.Fastest
    TripPreferenceHint.FEWEST_TRANSFERS -> TripPreference.FewestTransfers
    null -> TripPreference.Default
}

/** Applies what the on-device parser read. The parser only names things; [source] decides which real places match. */
private fun applyParsed(state: HomeState, output: ParserOutput, source: TripSource): HomeState {
    val base = state.copy(thinking = false, listening = false)
    return when (output.intent) {
        Intent.VEHICLE_CHECK -> {
            val read = output.vehicleText.orEmpty()
            base.copy(askFeedback = VehicleCheck.message(read, VehicleCheck.check(read, nextRideSignboards(state, source))))
        }
        Intent.OTHER -> base.copy(askFeedback = OTHER_INTENT_FEEDBACK)
        Intent.TRIP -> {
            val originText = output.origin?.takeIf { it.isNotBlank() }
            val destinationText = output.destination?.takeIf { it.isNotBlank() }
            if (originText == null && destinationText == null) {
                return base.copy(askFeedback = TRIP_EXAMPLE_FEEDBACK)
            }
            val foundOrigin = originText?.let { firstPlace(source, Field.A, it) }
            val foundDestination = destinationText?.let { firstPlace(source, Field.B, it) }
            val next = base.copy(
                origin = foundOrigin ?: base.origin,
                destination = foundDestination ?: base.destination,
                preference = output.preference.toPreference(),
                activeField = null,
                query = "",
            )
            when {
                originText != null && foundOrigin == null ->
                    next.copy(askFeedback = "I couldn't find \"$originText\" in Makati yet.")
                destinationText != null && foundDestination == null ->
                    next.copy(askFeedback = "I couldn't find \"$destinationText\" in Makati yet.")
                next.origin != null && next.destination != null -> settle(
                    next.copy(asking = false, askDraft = "", askFeedback = null),
                    source,
                )
                next.destination != null -> next.copy(askFeedback = ASK_START_FEEDBACK)
                else -> next.copy(askFeedback = ASK_DESTINATION_FEEDBACK)
            }
        }
    }
}

/** Signboards of the first ride of the trip on screen; empty when A and B aren't both set. */
private fun nextRideSignboards(state: HomeState, source: TripSource): List<String> {
    if (!canOpenTrip(state)) return emptyList()
    val trip = (source.resolve(state.origin!!, state.destination!!, state.preference) as? TripResult.Ready)?.trip
        ?: return emptyList()
    return trip.legs.filterIsInstance<Leg.Ride>().firstOrNull()?.signboards.orEmpty()
}

private fun submitAsk(state: HomeState, source: TripSource): HomeState {
    val match = matchAsk(state.askDraft, source)
    val next = state.copy(
        origin = match.origin ?: state.origin,
        destination = match.destination ?: state.destination,
        asking = match.origin == null && match.destination == null,
        askFeedback = match.feedback,
        askDraft = if (match.origin == null && match.destination == null) state.askDraft else "",
    )
    return if (match.origin != null || match.destination != null) settle(next, source) else next
}

fun canOpenTrip(state: HomeState): Boolean {
    val origin = state.origin ?: return false
    val destination = state.destination ?: return false
    return origin.inMakati && destination.inMakati && origin.id != destination.id
}

fun peekMessage(state: HomeState): String {
    val origin = state.origin
    val destination = state.destination
    return when {
        origin == null || destination == null -> "Pick A and B on the map."
        origin.id == destination.id -> "Start and destination are the same place. Change one."
        outsideField(state) != null -> "Not in my data yet. Only Makati is covered for now."
        else -> "${origin.name} → ${destination.name}. Swipe up to see the trip."
    }
}

/** Which end the not-in-data notice should ask the rider to change. */
fun outsideField(state: HomeState): Field? = when {
    state.origin?.inMakati == false -> Field.A
    state.destination?.inMakati == false -> Field.B
    else -> null
}
