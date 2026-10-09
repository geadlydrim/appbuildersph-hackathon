package app.commutenity.data.pack

import android.content.Context
import app.commutenity.domain.Field
import app.commutenity.domain.GeoPoint
import app.commutenity.domain.Leg
import app.commutenity.domain.Place
import app.commutenity.domain.SearchRow
import app.commutenity.domain.Trip
import app.commutenity.domain.TripPath
import app.commutenity.domain.TripPreference
import app.commutenity.domain.TripResult
import app.commutenity.domain.TripSource
import app.commutenity.domain.trips.Preference
import app.commutenity.domain.trips.RideEdge
import app.commutenity.domain.trips.TripCandidate
import app.commutenity.domain.trips.TripFinder
import app.commutenity.domain.trips.TripGraph
import app.commutenity.domain.trips.TripRequest
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Trips and places straight from the commute pack. Every fare, minute, stop, signboard and shape shown
 * comes from [pack]; nothing is invented here.
 */
class PackTripSource(private val pack: CommutePack) : TripSource {
    private val placeIndex = pack.placeIndex()
    private val stopsById = pack.stops.associateBy { it.id }
    private val packPlaceStops = pack.places.associate { it.id to it.stopIds }
    private val segmentsByEdgeId = pack.segments.associateBy { edgeId(it) }
    private val signboardsByRoute = pack.routes.associate { it.id to it.signboards }
    private val finder = TripFinder(
        TripGraph(
            pack.segments
                .filter { it.fromStopId in stopsById && it.toStopId in stopsById }
                .map { segment ->
                    RideEdge(
                        id = edgeId(segment),
                        routeId = segment.routeId,
                        fromStopId = segment.fromStopId,
                        toStopId = segment.toStopId,
                        minutes = segment.minutes ?: 0,
                        farePhp = segment.farePhp,
                        distanceMeters = segment.distanceM ?: 0,
                        signboards = signboardsByRoute[segment.routeId].orEmpty(),
                    )
                },
        ),
    )

    /** No GPS yet: the first pack place stands in, and its name says so. */
    override val myLocation: Place = pack.places.first().toPlace().let {
        it.copy(name = "My location (demo: ${it.name})")
    }

    override fun search(field: Field, query: String): List<SearchRow> = buildList<SearchRow> {
        if (field == Field.A) add(SearchRow.UseMyLocation)
        placeIndex.search(query).forEach { add(SearchRow.PlaceRow(it)) }
    }

    override fun resolve(origin: Place, destination: Place, preference: TripPreference): TripResult {
        if (!origin.inMakati || !destination.inMakati) return TripResult.NotInData
        val originStops = stopsFor(origin)
        val destinationStops = stopsFor(destination)
        if (originStops.isEmpty() || destinationStops.isEmpty()) return TripResult.NotInData

        val candidates = finder.find(TripRequest(originStops, destinationStops, preference.toFinder()))
        val best = candidates.firstOrNull() ?: return TripResult.NotInData
        val trip = buildTrip(best, reasonFor(best, candidates.getOrNull(1), preference), origin, destination)
            ?: return TripResult.NotInData
        return TripResult.Ready(trip)
    }

    /** A pack place uses its declared stops; any other place with coordinates uses every stop within walking range. */
    private fun stopsFor(place: Place): Set<String> {
        packPlaceStops[place.id]?.filter { it in stopsById }?.takeIf { it.isNotEmpty() }?.let { return it.toSet() }
        val lat = place.lat ?: return emptySet()
        val lng = place.lng ?: return emptySet()
        return pack.stops.filter { haversineMeters(lat, lng, it.lat, it.lng) <= MAX_WALK_M }.mapTo(hashSetOf()) { it.id }
    }

    private fun buildTrip(candidate: TripCandidate, reason: String, origin: Place, destination: Place): Trip? {
        val rides = candidate.legs.filterIsInstance<RideEdge>()
        val firstRide = rides.firstOrNull() ?: return null
        val board = stopsById[firstRide.fromStopId] ?: return null
        val alight = stopsById[rides.last().toStopId] ?: return null
        val segments = rides.map { segmentsByEdgeId[it.id] ?: return null }

        val firstWalkM = walkMeters(origin, board.lat, board.lng)
        val lastWalkM = walkMeters(destination, alight.lat, alight.lng)
        val walkMinutes = walkMinutes(firstWalkM) + walkMinutes(lastWalkM)

        val rideMinutes = segments.map { it.minutes }
        val minutes = if (rideMinutes.any { it == null }) {
            "Time unknown"
        } else {
            "${walkMinutes + rideMinutes.sumOf { it!! }} min"
        }
        val fare = candidate.totalFarePhp?.let { "₱$it" } ?: "Fare unknown"
        val kilometres = (segments.sumOf { it.distanceM ?: 0 } + firstWalkM + lastWalkM) / 1000.0

        val legs = buildList<Leg> {
            if (firstWalkM >= MIN_WALK_M) add(Leg.Walk("~$firstWalkM m"))
            rides.forEachIndexed { index, ride ->
                val from = stopsById.getValue(ride.fromStopId)
                val to = stopsById.getValue(ride.toStopId)
                val fareText = segments[index].farePhp?.let { "₱$it" } ?: "Fare unknown"
                val timeText = segments[index].minutes?.let { "$it min" } ?: "time unknown"
                add(
                    Leg.Ride(
                        stops = "${from.name}  →  ${to.name}",
                        fareAndMinutes = "$fareText  ·  $timeText",
                        signboard = ride.signboards.joinToString(" / "),
                    ),
                )
            }
            if (lastWalkM >= MIN_WALK_M) add(Leg.Walk("~$lastWalkM m"))
            add(Leg.Para(alight.name))
        }

        val boardPoint = GeoPoint(board.lat, board.lng)
        val alightPoint = GeoPoint(alight.lat, alight.lng)
        val path = TripPath(
            rides = rides.mapIndexed { index, ride ->
                val decoded = segments[index].polyline?.let { Polyline.decode(it, pack.shapePrecision) }.orEmpty()
                decoded.takeIf { it.size >= 2 } ?: listOf(
                    stopsById.getValue(ride.fromStopId).let { GeoPoint(it.lat, it.lng) },
                    stopsById.getValue(ride.toStopId).let { GeoPoint(it.lat, it.lng) },
                )
            },
            walks = buildList<List<GeoPoint>> {
                val from = origin.point()
                if (from != null && firstWalkM >= MIN_WALK_M) add(listOf(from, boardPoint))
                val to = destination.point()
                if (to != null && lastWalkM >= MIN_WALK_M) add(listOf(alightPoint, to))
            },
            boardStops = rides.map { ride -> stopsById.getValue(ride.fromStopId).let { GeoPoint(it.lat, it.lng) } },
            para = alightPoint,
        )

        return Trip(
            key = candidate.candidateKey,
            fare = fare,
            minutes = minutes,
            transfers = candidate.transfers.toString(),
            distanceLine = String.format(Locale.US, "%.1f km   ·   walk %d min", kilometres, walkMinutes),
            reason = reason,
            sample = false,
            legs = legs,
            path = path,
        )
    }

    /** Straight-line walk to a stop, rounded to 10 m; 0 when the place has no coordinates. */
    private fun walkMeters(place: Place, stopLat: Double, stopLng: Double): Int {
        val lat = place.lat ?: return 0
        val lng = place.lng ?: return 0
        val metres = haversineMeters(lat, lng, stopLat, stopLng)
        return (metres / 10.0).roundToInt() * 10
    }

    private fun walkMinutes(meters: Int): Int =
        if (meters < MIN_WALK_M) 0 else ceil(meters / WALK_M_PER_MIN).toInt()

    private fun Place.point(): GeoPoint? = if (lat != null && lng != null) GeoPoint(lat, lng) else null

    /** Names the first criterion, in the D16 order for [preference], where [best] beats [runnerUp]. */
    private fun reasonFor(best: TripCandidate, runnerUp: TripCandidate?, preference: TripPreference): String {
        if (runnerUp == null) return "Only trip in my data"
        val transfers = "Fewest transfers" to { c: TripCandidate -> c.transfers }
        val minutes = "Fastest" to { c: TripCandidate -> c.totalMinutes }
        val cheapest = "Cheapest" to { c: TripCandidate -> c.totalFarePhp ?: Int.MAX_VALUE }
        val walking = "Least walking" to { c: TripCandidate -> c.walkMinutes }
        val order = when (preference) {
            TripPreference.Fastest -> listOf(minutes, transfers, cheapest, walking)
            TripPreference.Cheapest -> listOf(cheapest, transfers, minutes, walking)
            TripPreference.Default, TripPreference.FewestTransfers -> listOf(transfers, minutes, cheapest, walking)
        }
        return order.firstOrNull { (_, measure) -> measure(best) < measure(runnerUp) }?.first
            ?: "Tied with another trip"
    }

    private fun TripPreference.toFinder(): Preference = when (this) {
        TripPreference.Default -> Preference.Default
        TripPreference.Fastest -> Preference.Fastest
        TripPreference.Cheapest -> Preference.Cheapest
        TripPreference.FewestTransfers -> Preference.FewestTransfers
    }

    companion object {
        const val MAX_WALK_M = 800.0
        const val WALK_M_PER_MIN = 80.0
        private const val MIN_WALK_M = 10
        private const val ASSET_PATH = "pack/hero-trip.json"
        private const val EARTH_RADIUS_M = 6_371_000.0

        fun fromAssets(context: Context): PackTripSource =
            PackTripSource(
                CommutePack.parse(context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }),
            )

        private fun edgeId(segment: PackSegment) = "${segment.routeId}#${segment.seq}"

        private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
            return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
