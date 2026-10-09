package app.commutenity.map

import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.commutenity.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class OfflineMapIntegrationTest {
    @Test
    fun loadsTheBundledMakatiPmtilesStyle() {
        val styleLoaded = CountDownLatch(1)
        lateinit var mapView: MapView
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.assets.openFd("makati-20261009-z14.pmtiles").close()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                MapLibre.getInstance(activity)
                mapView = MapView(activity)
                activity.addContentView(
                    mapView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                mapView.onStart()
                mapView.getMapAsync { map ->
                    map.setStyle("asset://map/style.json") {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(14.5547, 121.0244))
                            .zoom(13.2)
                            .build()
                        styleLoaded.countDown()
                    }
                }
            }

            assertTrue("offline style did not finish loading", styleLoaded.await(20, TimeUnit.SECONDS))
            scenario.onActivity {
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }
}
