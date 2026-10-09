package app.commutenity.ui.map

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.log.Logger
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
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
    override fun Content(showTrip: Boolean, modifier: Modifier) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // Create the MapView once and keep it for as long as the map is on screen. MapLibre only
        // initializes the map if onDestroy() has not been called yet, so it must never be torn down by
        // an effect re-running.
        val mapView = remember {
            MapLibre.getInstance(context)
            Logger.setVerbosity(Logger.INFO)
            // Texture mode draws inside the view hierarchy, which behaves well inside Compose.
            MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true)).apply {
                onCreate(null)
                addOnDidFailLoadingMapListener { Log.e(TAG, "map failed to load: $it") }
                addOnDidFinishLoadingStyleListener { Log.i(TAG, "style finished loading") }
                addOnDidFinishRenderingMapListener { fully -> Log.i(TAG, "map rendered (fully=$fully)") }
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
                Log.i(TAG, "map left the screen; destroying")
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
            Log.i(TAG, "style json ready (${json.length} chars); waiting for map")
            mapView.getMapAsync { map ->
                Log.i(TAG, "map ready; applying style")
                map.setStyle(Style.Builder().fromJson(json)) {
                    Log.i(TAG, "style applied")
                    // Keep the camera inside the bundled extract (bounds from the PMTiles header) so
                    // the rider never pans into blank space.
                    map.setLatLngBoundsForCameraTarget(
                        LatLngBounds.from(14.6, 121.08, 14.5, 120.99),
                    )
                    map.setMinZoomPreference(12.0)
                    // Open on the hero trip (V.A. Rufino St → Dela Rosa St). Zoom 14+ uses the most
                    // detailed tiles; in-between zooms mix tile levels and look patchy.
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(14.5577, 121.0134))
                        .zoom(14.5)
                        .build()
                }
            }
        }

        AndroidView(
            factory = {
                mapView.also { view ->
                    view.post { Log.i(TAG, "attached=${view.isAttachedToWindow} size=${view.width}x${view.height}") }
                }
            },
            modifier = modifier,
        )
    }
}
