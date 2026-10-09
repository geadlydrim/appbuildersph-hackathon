package app.commutenity.domain

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
    data object VoiceStart : HomeEvent
    data class VoicePartial(val text: String) : HomeEvent
    data class VoiceResult(val text: String) : HomeEvent
    data class VoiceError(val message: String) : HomeEvent
    data object VoiceStop : HomeEvent
    data class SettleSheet(val sheet: Sheet) : HomeEvent
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
    val origin = places.firstOrNull { place ->
        normalized.contains(place.name.lowercase()) ||
            (place.area.isNotEmpty() && normalized.contains(place.area.lowercase()))
    }
    val destination = places.firstOrNull { place ->
        place != origin && (
            normalized.contains(place.name.lowercase()) ||
                (place.area.isNotEmpty() && normalized.contains(place.area.lowercase()))
            )
    }
    val feedback = when {
        origin == null && destination == null ->
            "Not understood. Pick A and B on the map. This build has no on-device model."
        origin != null && destination != null ->
            "Sample match only: ${origin.name} → ${destination.name}. This is not the on-device model."
        origin != null ->
            "Sample match for the start only. The destination is still missing."
        else ->
            "Sample match for the destination only. The start is still missing."
    }
    return AskMatch(origin, destination, feedback)
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
    }
}

private fun settle(state: HomeState, source: TripSource): HomeState {
    val origin = state.origin
    val destination = state.destination
    val sheet = when {
        origin == null || destination == null -> Sheet.Peek
        source.resolve(origin, destination) is TripResult.NotInData -> Sheet.Notice
        else -> Sheet.Half
    }
    return state.copy(sheet = sheet, cardExpanded = false)
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
    return origin.inMakati && destination.inMakati
}
