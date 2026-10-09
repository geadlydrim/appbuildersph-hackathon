package app.commutenity.ui.map

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
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
        val lifecycleOwner = LocalLifecycleOwner.current
        var mapView by remember { mutableStateOf<MapView?>(null) }

        DisposableEffect(lifecycleOwner, mapView) {
            val view = mapView ?: return@DisposableEffect onDispose {}
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> view.onStart()
                    Lifecycle.Event.ON_RESUME -> view.onResume()
                    Lifecycle.Event.ON_PAUSE -> view.onPause()
                    Lifecycle.Event.ON_STOP -> view.onStop()
                    Lifecycle.Event.ON_DESTROY -> view.onDestroy()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.onStart()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                view.onStop()
                view.onDestroy()
            }
        }

        AndroidView(
            factory = { context ->
                MapLibre.getInstance(context)
                // Texture mode draws inside the view hierarchy. The default SurfaceView sits behind the
                // window and the screen's opaque background covered it, leaving the map blank.
                MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true)).also { view ->
                    // MapLibre only initializes the map (and runs getMapAsync callbacks) after onCreate.
                    view.onCreate(null)
                    mapView = view
                    Thread {
                        try {
                            val json = loadOfflineStyleJson(context.applicationContext)
                            Log.i(TAG, "style json ready (${json.length} chars)")
                            view.post {
                                try {
                                    view.addOnDidFailLoadingMapListener { Log.e(TAG, "map failed to load: $it") }
                                    view.getMapAsync { map ->
                                        Log.i(TAG, "map ready; applying style")
                                        map.setStyle(Style.Builder().fromJson(json)) {
                                            Log.i(TAG, "style loaded")
                                            map.cameraPosition = CameraPosition.Builder()
                                                .target(LatLng(14.5547, 121.0244))
                                                .zoom(13.2)
                                                .build()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to apply offline map style", e)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to prepare offline map archive/style", e)
                        }
                    }.start()
                }
            },
            modifier = modifier,
        )
    }
}
