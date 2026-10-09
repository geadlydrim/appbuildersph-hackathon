package app.commutenity.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.commutenity.domain.Field
import app.commutenity.domain.Place
import app.commutenity.domain.TripPath

/**
 * The map the home screen draws on: the real offline map ([MapLibreSurface]) or the drawn
 * placeholder ([PlaceholderMap]) used by previews and the Q&A route preview.
 */
interface MapSurface {
    /**
     * @param origin point A; a pin is drawn when it has coordinates.
     * @param destination point B; same.
     * @param onTap called with the tapped coordinate, or null when taps are not wanted.
     * @param onMovePin called with the pin's field and the drop coordinate when the rider holds the A or B
     *   pin and drags it somewhere else; null when not wanted.
     * @param path the trip's map geometry; drawn under the pins and framed with them. Null for no trip.
     * @param compassBottom distance from the screen bottom to the map's compass ("reset orientation"),
     *   so it clears whatever covers the bottom of the map (the trip sheet and its buttons).
     */
    @Composable
    fun Content(
        showTrip: Boolean,
        modifier: Modifier,
        origin: Place? = null,
        destination: Place? = null,
        onTap: ((lat: Double, lng: Double) -> Unit)? = null,
        path: TripPath? = null,
        onMovePin: ((field: Field, lat: Double, lng: Double) -> Unit)? = null,
        compassBottom: Dp = 16.dp,
    )
}
