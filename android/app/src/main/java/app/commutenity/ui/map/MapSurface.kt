package app.commutenity.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.commutenity.domain.Place

/**
 * The map the home screen draws on: the real offline map ([MapLibreSurface]) or the drawn
 * placeholder ([PlaceholderMap]) used by previews and the Q&A route preview.
 */
interface MapSurface {
    /**
     * @param origin point A; a pin is drawn when it has coordinates.
     * @param destination point B; same.
     * @param onTap called with the tapped coordinate, or null when taps are not wanted.
     */
    @Composable
    fun Content(
        showTrip: Boolean,
        modifier: Modifier,
        origin: Place? = null,
        destination: Place? = null,
        onTap: ((lat: Double, lng: Double) -> Unit)? = null,
    )
}
