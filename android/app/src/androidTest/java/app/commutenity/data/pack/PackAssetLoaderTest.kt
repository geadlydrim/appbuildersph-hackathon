package app.commutenity.data.pack

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackAssetLoaderTest {
    @Test
    fun loadsBundledFixtureAndFindsAliasesOffline() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val source = PackAssetLoader.load(assets, "commute-pack-fixture.json")

        val ayala = source.search("glorieta").single()
        val delaRosa = source.search("dela rosa").single()

        assertEquals("Ayala Center", ayala.name)
        assertEquals("Dela Rosa Street", delaRosa.name)
    }
}
