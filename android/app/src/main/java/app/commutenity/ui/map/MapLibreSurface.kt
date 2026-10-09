package app.commutenity.ui.map

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.commutenity.domain.Place
import app.commutenity.ui.theme.LocalCommuteColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.io.File

private const val TAG = "MapLibreSurface"
private const val PMTILES = "makati-20261009-z14.pmtiles"
private const val ASSET_PMTILES_URL = "pmtiles://asset://$PMTILES"

/**
 * MapLibre's asset file source ignores byte ranges, so `pmtiles://asset://` cannot be read.
 * Copy the archive to app storage once and point the style at `pmtiles://file://` instead.
 * Blocking I/O: call off the main thread.
 */
internal fun ensureLocalPmtiles(context: Context): File {
    val target = File(context.filesDir, "map/$PMTILES")
    val assetLength = try {
        context.assets.openFd(PMTILES).use { it.length }
    } catch (_: java.io.IOException) {
        -1L
    }
    if (target.isFile && (assetLength < 0 || target.length() == assetLength)) return target

    target.parentFile?.mkdirs()
    val temp = File(target.parentFile, "$PMTILES.tmp")
    context.assets.open(PMTILES).use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
    }
    if (!temp.renameTo(target)) {
        temp.delete()
        error("Could not move $PMTILES into place at ${target.absolutePath}")
    }
    return target
}

/** Bundled style JSON with its vector source rewritten to the locally copied archive. Blocking I/O. */
internal fun loadOfflineStyleJson(context: Context): String {
    val archive = ensureLocalPmtiles(context)
    val json = context.assets.open("map/style.json").use { it.readBytes().toString(Charsets.UTF_8) }
    check(json.contains(ASSET_PMTILES_URL)) { "style.json does not reference $ASSET_PMTILES_URL" }
    return json.replace(ASSET_PMTILES_URL, "pmtiles://file://${archive.absolutePath}")
}

object MapLibreSurface : MapSurface {
    @Composable
    override fun Content(
        showTrip: Boolean,
        modifier: Modifier,
        origin: Place?,
        destination: Place?,
        onTap: ((lat: Double, lng: Double) -> Unit)?,
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val colors = LocalCommuteColors.current
        val density = LocalDensity.current
        // Visible band when both ends are set: below the search card and ask bar, above the half-open
        // trip sheet (520 dp) on the home screen.
        val fitSide = with(density) { 48.dp.roundToPx() }
        val fitTop = with(density) { 300.dp.roundToPx() }
        val fitBottom = with(density) { 560.dp.roundToPx() }
        val latestOnTap by rememberUpdatedState(onTap)
        // The loaded style; null until the map is ready. Pin updates wait for it.
        var style by remember { mutableStateOf<Style?>(null) }
        var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

        // Create the MapView once and keep it for as long as the map is on screen. MapLibre only
        // initializes the map if onDestroy() has not been called yet, so it must never be torn down by
        // an effect re-running.
        val mapView = remember {
            MapLibre.getInstance(context)
            // Texture mode draws inside the view hierarchy, which behaves well inside Compose.
            MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true)).apply {
                onCreate(null)
                addOnDidFailLoadingMapListener { Log.e(TAG, "map failed to load: $it") }
            }
        }

        // Forward lifecycle events only; destruction happens when the map leaves composition.
        DisposableEffect(lifecycleOwner, mapView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        DisposableEffect(mapView) {
            onDispose {
                mapView.onStop()
                mapView.onDestroy()
            }
        }

        // Load the style once: copy the archive off the main thread, then apply it when the map is ready.
        LaunchedEffect(mapView) {
            val json = try {
                withContext(Dispatchers.IO) { loadOfflineStyleJson(context.applicationContext) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prepare offline map archive/style", e)
                return@LaunchedEffect
            }
            mapView.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(json)) { loaded ->
                    // Keep the camera inside the bundled extract (bounds from the PMTiles header) so
                    // the rider never pans into blank space.
                    map.setLatLngBoundsForCameraTarget(LatLngBounds.from(14.6, 121.08, 14.5, 120.99))
                    map.setMinZoomPreference(12.0)
                    // Open on the hero trip (V.A. Rufino St → Dela Rosa St). Zoom 14+ uses the most
                    // detailed tiles; in-between zooms mix tile levels and look patchy.
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(14.5577, 121.0134))
                        .zoom(14.5)
                        .build()
                    addPinLayers(loaded, pinA = colors.pinA.toArgb(), pinB = colors.pinB.toArgb())
                    map.addOnMapClickListener { point ->
                        latestOnTap?.invoke(point.latitude, point.longitude)
                        latestOnTap != null
                    }
                    maplibreMap = map
                    style = loaded
                }
            }
        }

        // Redraw the A/B pins at their coordinates, and frame them, whenever either end changes.
        LaunchedEffect(style, origin, destination) {
            val loaded = style ?: return@LaunchedEffect
            val map = maplibreMap ?: return@LaunchedEffect
            val a = origin?.latLng()
            val b = destination?.latLng()
            loaded.getSourceAs<GeoJsonSource>(PINS_SOURCE)?.setGeoJson(
                FeatureCollection.fromFeatures(
                    listOfNotNull(a?.let { pinFeature(it, "A") }, b?.let { pinFeature(it, "B") }),
                ),
            )
            when {
                a != null && b != null -> map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        LatLngBounds.Builder().include(a).include(b).build(),
                        fitSide, fitTop, fitSide, fitBottom,
                    ),
                )
                a != null || b != null -> map.animateCamera(CameraUpdateFactory.newLatLng(a ?: b!!))
            }
        }

        AndroidView(factory = { mapView }, modifier = modifier)
    }
}

private const val PINS_SOURCE = "commutenity-pins"

private fun Place.latLng(): LatLng? {
    val la = lat ?: return null
    val ln = lng ?: return null
    return LatLng(la, ln)
}

private fun pinFeature(at: LatLng, label: String): Feature =
    Feature.fromGeometry(Point.fromLngLat(at.longitude, at.latitude)).apply { addStringProperty("label", label) }

/** A circle per pin, coloured A/B, with its letter on top (the bundled Noto Sans covers A and B). */
private fun addPinLayers(style: Style, pinA: Int, pinB: Int) {
    style.addSource(GeoJsonSource(PINS_SOURCE))
    val color = Expression.match(
        Expression.get("label"),
        Expression.color(pinA),
        Expression.stop("B", Expression.color(pinB)),
    )
    style.addLayer(
        CircleLayer("commutenity-pins-circle", PINS_SOURCE).withProperties(
            PropertyFactory.circleRadius(14f),
            PropertyFactory.circleColor(color),
            PropertyFactory.circleStrokeWidth(3f),
            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
        ),
    )
    style.addLayer(
        SymbolLayer("commutenity-pins-label", PINS_SOURCE).withProperties(
            PropertyFactory.textField(Expression.get("label")),
            PropertyFactory.textFont(arrayOf("Noto Sans Medium")),
            PropertyFactory.textSize(14f),
            PropertyFactory.textColor(android.graphics.Color.WHITE),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
        ),
    )
}
