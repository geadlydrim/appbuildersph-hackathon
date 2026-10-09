package app.commutenity.domain

/**
 * The short, fixed-template answer shown on the trip card. No AI: every word that is not template
 * comes from the [Trip]'s own fields, so it can never state a fare, minute or place the data lacks.
 */
object TripAnswer {
    /**
     * English by default, Taglish when [taglish]. Empty when the trip has no ride (except the walk-only trip), or
     * when a ride lacks the stop names the sentence needs (placeholder sample data), because the sentence would be wrong.
     */
    fun compose(trip: Trip, taglish: Boolean = false): String {
        val firstRide = trip.legs.indexOfFirst { it is Leg.Ride }
        val lastRide = trip.legs.indexOfLast { it is Leg.Ride }
        if (firstRide < 0) return walkOnlyAnswer(trip, taglish)
        val rides = trip.legs.filterIsInstance<Leg.Ride>()
        if (rides.any { it.board.isBlank() || it.alight.isBlank() }) return ""

        val parts = mutableListOf<String>()
        if (trip.unverified) parts += if (taglish) "Hindi pa beripikado (OpenStreetMap):" else "Unverified (OpenStreetMap):"
        if (trip.sample) parts += "Sample:"

        trip.legs.forEachIndexed { index, leg ->
            when {
                leg is Leg.Walk && index < firstRide -> {
                    val board = rides.first().board
                    parts += if (taglish) "Maglakad ${leg.meters} papunta sa $board." else "Walk ${leg.meters} to $board."
                }
                leg is Leg.Walk && index > lastRide -> {
                    parts += if (taglish) "Maglakad ${leg.meters}." else "Walk ${leg.meters} to your stop."
                }
                leg is Leg.Walk -> {
                    val next = trip.legs.drop(index + 1).filterIsInstance<Leg.Ride>().first().board
                    parts += if (taglish) "Maglakad ${leg.meters} papunta sa $next." else "Walk ${leg.meters} to $next."
                }
                leg is Leg.Ride -> parts += rideSentences(leg, first = leg === rides.first(), taglish = taglish)
            }
        }

        parts += closing(trip, taglish)
        return parts.joinToString(" ")
    }

    /** The "walk, no ride needed" trip has one Walk leg and no ride; any other ride-less trip gets nothing. */
    private fun walkOnlyAnswer(trip: Trip, taglish: Boolean): String {
        if (trip.key != WALK_ONLY_TRIP_KEY) return ""
        val walk = trip.legs.singleOrNull() as? Leg.Walk ?: return ""
        val time = walk.minutes.takeIf { it.any(Char::isDigit) }
        return if (taglish) {
            "Maglakad ${walk.meters} papunta sa pupuntahan mo" + (time?.let { ", mga $it" } ?: "") +
                ". Hindi na kailangang sumakay."
        } else {
            "Walk ${walk.meters} to your stop" + (time?.let { ", about $it" } ?: "") + ". No ride needed."
        }
    }

    private fun rideSentences(ride: Leg.Ride, first: Boolean, taglish: Boolean): String {
        val sign = ride.signboards.firstOrNull()?.takeIf { it.isNotBlank() } ?: ride.signboard.takeIf { it.isNotBlank() }
        return if (taglish) {
            val lead = if (first) "Sumakay ng" else "Pagkatapos, sumakay ng"
            val mark = sign?.let { " na may karatulang \"$it\"" }.orEmpty()
            "$lead ${modeWord(ride.mode, taglish = true)}$mark. Bumaba sa ${ride.alight} (sabihin \"para\")."
        } else {
            val lead = if (first) "Ride the" else "Then ride the"
            val mark = sign?.let { " marked \"$it\"" }.orEmpty()
            "$lead ${modeWord(ride.mode, taglish = false)}$mark. Get off at ${ride.alight} (say \"para\")."
        }
    }

    private fun modeWord(mode: String, taglish: Boolean): String = when {
        mode.isBlank() -> if (taglish) "sasakyan" else "vehicle"
        mode.equals("jeepney", ignoreCase = true) -> "jeep"
        else -> mode
    }

    /** Fare, then the total time when the trip has one ("Time unknown" has no digits and is left out). */
    private fun closing(trip: Trip, taglish: Boolean): String {
        val hasTime = trip.minutes.any { it.isDigit() }
        // An estimate already reads "~N min (est.)"; "about ~N" would say it twice.
        val estimated = trip.minutes.startsWith("~")
        return when {
            !hasTime -> "${trip.fare}."
            taglish -> if (estimated) "${trip.fare}, ${trip.minutes} lahat." else "${trip.fare}, mga ${trip.minutes} lahat."
            else -> if (estimated) "${trip.fare}, ${trip.minutes} in all." else "${trip.fare}, about ${trip.minutes} in all."
        }
    }
}
