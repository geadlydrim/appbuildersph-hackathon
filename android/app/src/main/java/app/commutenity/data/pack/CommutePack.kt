package app.commutenity.data.pack

import app.commutenity.domain.Place
import org.json.JSONArray
import org.json.JSONObject

/** The commute pack as stored on disk: places, stops, routes and the ride segments between stops. */
data class CommutePack(
    val shapePrecision: Int,
    val places: List<PackPlaceRecord>,
    val stops: List<PackStop>,
    val routes: List<PackRoute>,
    val segments: List<PackSegment>,
) {
    /**
     * This pack followed by [other]. Ids are expected to be disjoint (e.g. hero ids vs `osm-` ids); a differing
     * polyline precision is rejected because segments are decoded with a single precision.
     */
    fun merge(other: CommutePack): CommutePack {
        require(shapePrecision == other.shapePrecision) {
            "Cannot merge packs with shape precision $shapePrecision and ${other.shapePrecision}"
        }
        return copy(
            places = places + other.places,
            stops = stops + other.stops,
            routes = routes + other.routes,
            segments = segments + other.segments,
        )
    }

    /** The pack's places in the shape [PackPlaceIndex] searches. */
    fun placeIndex(): PackPlaceIndex =
        PackPlaceIndex(places.map { PackPlace(it.toPlace(), it.aliases, preferred = it.sourceClass != SOURCE_OSM) })

    companion object {
        private const val DEFAULT_SHAPE_PRECISION = 6

        /** Provenance class of the team's own data; the default when a record names none. */
        const val SOURCE_KNOWN = "known"

        /** Real OpenStreetMap data that the team has not verified. */
        const val SOURCE_OSM = "osm"

        /** Published information (e.g. an official stop list) the team has not ridden or verified itself. */
        const val SOURCE_PUBLIC = "public"

        /** Data the team collected by riding. */
        const val SOURCE_COLLECTED = "collected"

        fun parse(json: String): CommutePack {
            val root = JSONObject(json)
            return CommutePack(
                shapePrecision = root.optJSONObject("meta")?.optIntOrNull("shape_precision") ?: DEFAULT_SHAPE_PRECISION,
                places = root.objects("places").map { place ->
                    PackPlaceRecord(
                        id = place.getString("id"),
                        name = place.getString("name"),
                        area = place.optStringOrNull("area").orEmpty(),
                        aliases = place.strings("aliases"),
                        lat = place.optDoubleOrNull("lat"),
                        lng = place.optDoubleOrNull("lng"),
                        stopIds = place.strings("stop_ids"),
                        sourceClass = place.optStringOrNull("source_class") ?: SOURCE_KNOWN,
                    )
                },
                stops = root.objects("stops").map { stop ->
                    PackStop(
                        id = stop.getString("id"),
                        name = stop.getString("name"),
                        lat = stop.getDouble("lat"),
                        lng = stop.getDouble("lng"),
                    )
                },
                routes = root.objects("routes").map { route ->
                    PackRoute(
                        id = route.getString("id"),
                        name = route.optStringOrNull("name").orEmpty(),
                        mode = route.optStringOrNull("mode").orEmpty(),
                        signboards = route.strings("signboards"),
                        sourceClass = route.optStringOrNull("source_class") ?: SOURCE_KNOWN,
                        fareRule = route.optStringOrNull("fare_rule"),
                    )
                },
                segments = root.objects("segments").map { segment ->
                    PackSegment(
                        routeId = segment.getString("route_id"),
                        seq = segment.getInt("seq"),
                        fromStopId = segment.getString("from_stop_id"),
                        toStopId = segment.getString("to_stop_id"),
                        farePhp = segment.optIntOrNull("fare_php"),
                        minutes = segment.optIntOrNull("minutes"),
                        minutesEst = segment.optIntOrNull("minutes_est"),
                        distanceM = segment.optIntOrNull("distance_m"),
                        polyline = segment.optJSONObject("shape")?.optStringOrNull("polyline")?.takeIf { it.isNotEmpty() },
                        sourceClass = segment.optJSONObject("provenance")?.optStringOrNull("source_class") ?: SOURCE_KNOWN,
                    )
                },
            )
        }

        private fun JSONObject.objects(name: String): List<JSONObject> =
            optJSONArray(name)?.let { array -> List(array.length()) { array.getJSONObject(it) } }.orEmpty()

        private fun JSONObject.strings(name: String): List<String> =
            optJSONArray(name)?.let { array: JSONArray -> List(array.length()) { array.getString(it) } }.orEmpty()

        private fun JSONObject.optStringOrNull(name: String): String? =
            if (isNull(name)) null else getString(name)

        private fun JSONObject.optIntOrNull(name: String): Int? =
            if (isNull(name)) null else getInt(name)

        private fun JSONObject.optDoubleOrNull(name: String): Double? =
            if (isNull(name)) null else getDouble(name)
    }
}

data class PackPlaceRecord(
    val id: String,
    val name: String,
    val area: String,
    val aliases: List<String>,
    val lat: Double?,
    val lng: Double?,
    val stopIds: List<String>,
    val sourceClass: String = CommutePack.SOURCE_KNOWN,
) {
    fun toPlace(): Place = Place(id = id, name = name, area = area, inMakati = true, lat = lat, lng = lng)
}

data class PackStop(val id: String, val name: String, val lat: Double, val lng: Double)

data class PackRoute(
    val id: String,
    val name: String,
    val mode: String,
    val signboards: List<String>,
    val sourceClass: String = CommutePack.SOURCE_KNOWN,
    /** Fare table rule (see [FareRules]); when set, the fare is computed per ride from its distance. */
    val fareRule: String? = null,
)

data class PackSegment(
    val routeId: String,
    val seq: Int,
    val fromStopId: String,
    val toStopId: String,
    val farePhp: Int?,
    val minutes: Int?,
    /** Estimated ride time for segments with no measured [minutes] (OpenStreetMap data). */
    val minutesEst: Int? = null,
    val distanceM: Int?,
    val polyline: String?,
    /** `provenance.source_class`; "osm" marks unverified OpenStreetMap data. */
    val sourceClass: String = CommutePack.SOURCE_KNOWN,
)
