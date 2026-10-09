package app.commutenity.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
     * @param onLongPress called with the pin's field and the pressed coordinate when the rider long-presses
     *   near the A or B pin, so the pin can move there; null when not wanted.
     * @param path the trip's map geometry; drawn under the pins and framed with them. Null for no trip.
     */
    @Composable
    fun Content(
        showTrip: Boolean,
        modifier: Modifier,
        origin: Place? = null,
        destination: Place? = null,
        onTap: ((lat: Double, lng: Double) -> Unit)? = null,
        path: TripPath? = null,
        onLongPress: ((field: Field, lat: Double, lng: Double) -> Unit)? = null,
    )
}
