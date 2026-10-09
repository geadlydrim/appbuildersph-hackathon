package app.commutenity.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The map the home screen draws on. The T0 frames use a drawn placeholder.
 * A later MapLibre + PMTiles implementation can replace [PlaceholderMap]
 * without changing the sheets.
 */
fun interface MapSurface {
    @Composable
    fun Content(showTrip: Boolean, modifier: Modifier)
}
