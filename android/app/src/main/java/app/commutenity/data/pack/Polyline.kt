package app.commutenity.data.pack

import app.commutenity.domain.GeoPoint
import kotlin.math.pow

/** Google encoded-polyline decoding. The pack stores shapes at precision 6. */
object Polyline {
    fun decode(encoded: String, precision: Int = 6): List<GeoPoint> {
        val factor = 10.0.pow(precision)
        val points = ArrayList<GeoPoint>()
        var index = 0
        var lat = 0L
        var lng = 0L

        fun nextDelta(): Long? {
            var result = 0L
            var shift = 0
            while (index < encoded.length) {
                val chunk = encoded[index++].code - 63
                result = result or ((chunk and 0x1f).toLong() shl shift)
                shift += 5
                if (chunk < 0x20) {
                    return if (result and 1L != 0L) (result shr 1).inv() else result shr 1
                }
            }
            return null // truncated input
        }

        while (index < encoded.length) {
            val dLat = nextDelta() ?: break
            val dLng = nextDelta() ?: break
            lat += dLat
            lng += dLng
            points += GeoPoint(lat / factor, lng / factor)
        }
        return points
    }
}
