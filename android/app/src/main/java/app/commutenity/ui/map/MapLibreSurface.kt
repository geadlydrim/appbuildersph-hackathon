package app.commutenity.ui.map

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
import org.maplibre.android.maps.MapView

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
                MapView(context).also { view ->
                    mapView = view
                    view.getMapAsync { map ->
                        map.setStyle("asset://map/style.json") {
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(14.5547, 121.0244))
                                .zoom(13.2)
                                .build()
                        }
                    }
                }
            },
            modifier = modifier,
        )
    }
}
