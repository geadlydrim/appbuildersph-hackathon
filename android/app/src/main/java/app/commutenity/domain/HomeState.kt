package app.commutenity.domain

data class HomeState(
    val origin: Place? = null,
    val destination: Place? = null,
    val activeField: Field? = null,
    val query: String = "",
    val sheet: Sheet = Sheet.Peek,
)

sealed interface HomeEvent {
    data class Focus(val field: Field) : HomeEvent
    data class Query(val value: String) : HomeEvent
    data class Pick(val place: Place) : HomeEvent
    data object UseMyLocation : HomeEvent
    data object ClearActive : HomeEvent
    data object DismissSearch : HomeEvent
    data class SettleSheet(val sheet: Sheet) : HomeEvent
}

fun reduce(state: HomeState, event: HomeEvent, source: TripSource): HomeState {
    return when (event) {
        is HomeEvent.Focus -> state.copy(
            activeField = event.field,
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
    return state.copy(sheet = sheet)
}

fun canOpenTrip(state: HomeState): Boolean {
    val origin = state.origin ?: return false
    val destination = state.destination ?: return false
    return origin.inMakati && destination.inMakati
}
